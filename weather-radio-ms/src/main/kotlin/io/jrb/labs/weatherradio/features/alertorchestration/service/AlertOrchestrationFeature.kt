/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.jrb.labs.weatherradio.features.alertorchestration.service

import io.jrb.labs.commons.eventbus.EventBus.Subscription
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.weatherradio.events.AlertExpiredEvent
import io.jrb.labs.weatherradio.events.AlertIgnoredEvent
import io.jrb.labs.weatherradio.events.AlertOpenedEvent
import io.jrb.labs.weatherradio.events.AlertRecordingRequestedEvent
import io.jrb.labs.weatherradio.events.FeatureHeartbeatEvent
import io.jrb.labs.weatherradio.events.SameHeaderDecodedEvent
import io.jrb.labs.weatherradio.events.WeatherRadioEventBus
import io.jrb.labs.weatherradio.features.alertorchestration.AlertOrchestrationDatafill
import io.jrb.labs.weatherradio.features.alertorchestration.model.ActiveAlertRecord
import io.jrb.labs.weatherradio.features.alertorchestration.model.AlertKey
import io.jrb.labs.weatherradio.features.alertorchestration.model.AlertLifecycleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AlertOrchestrationFeature(
    systemEventBus: SystemEventBus,
    private val weatherRadioEventBus: WeatherRadioEventBus,
    private val datafill: AlertOrchestrationDatafill,
    private val clock: Clock,
) : ControllableService(systemEventBus) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var subscription: Subscription? = null
    private var expiryJob: Job? = null

    private val activeAlerts = ConcurrentHashMap<AlertKey, ActiveAlertRecord>()
    private val expiredAlerts = ConcurrentHashMap<String, ActiveAlertRecord>()

    override fun onStart() {
        if (!datafill.enabled) {
            weatherRadioEventBus.send(
                FeatureHeartbeatEvent(
                    featureId = "alert-orchestration",
                    status = "DISABLED",
                )
            )
            return
        }

        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "alert-orchestration",
                status = "STARTING",
                details = mapOf(
                    "localCountyCodes" to datafill.localCountyCodes,
                    "triggerRecordingOnOpen" to datafill.triggerRecordingOnOpen,
                    "duplicateSuppressionMinutes" to datafill.duplicateSuppressionMinutes,
                    "expirySweepIntervalMs" to datafill.expirySweepIntervalMs,
                    "retainedExpiredAlertsMinutes" to datafill.retainedExpiredAlertsMinutes,
                ),
            )
        )

        subscription = weatherRadioEventBus.subscribe<SameHeaderDecodedEvent> { event ->
            handleDecodedHeader(event)
        }

        expiryJob = scope.launch {
            while (isActive) {
                delay(datafill.expirySweepIntervalMs)
                expireAlerts()
                pruneExpiredAlerts()
            }
        }

        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "alert-orchestration",
                status = "RUNNING",
            )
        )
    }

    override fun onStop() {
        subscription?.cancel()
        subscription = null

        expiryJob?.cancel()
        expiryJob = null

        scope.cancel()

        activeAlerts.clear()
        expiredAlerts.clear()

        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "alert-orchestration",
                status = "STOPPED",
                details = mapOf(
                    "stoppedAt" to clock.instant().toString(),
                ),
            )
        )
    }

    private suspend fun handleDecodedHeader(event: SameHeaderDecodedEvent) {
        val header = event.header
        val alertKey = AlertKey(
            senderId = header.senderId,
            eventCode = header.eventCode,
            countyCodes = header.countyCodes.toSortedSet(),
            purgeTime = header.purgeTime,
        )

        val locallyRelevant = isLocallyRelevant(header.countyCodes)
        if (!locallyRelevant) {
            weatherRadioEventBus.publish(
                AlertIgnoredEvent(
                    stationId = event.stationId,
                    header = header,
                    reason = "No matching local county code",
                    correlationId = event.correlationId,
                    causationId = event.eventId,
                )
            )
            return
        }

        val now = clock.instant()
        val existing = activeAlerts[alertKey]
        if (existing != null) {
            val ageMinutes = Duration.between(existing.lastSeenAt, now).toMinutes()

            if (ageMinutes < datafill.duplicateSuppressionMinutes) {
                if (datafill.debugLogging) {
                    log.debug(
                        "Suppressing duplicate alert stationId={} alertId={} eventCode={} senderId={} ageMinutes={}",
                        event.stationId,
                        existing.alertId,
                        header.eventCode,
                        header.senderId,
                        ageMinutes,
                    )
                }

                activeAlerts[alertKey] = existing.copy(
                    lastSeenAt = now,
                )
                return
            }
        }

        val alertId = UUID.randomUUID().toString()
        val expiresAt = calculateExpiresAt(
            openedAt = now,
            purgeTime = header.purgeTime,
        )

        val record = ActiveAlertRecord(
            alertId = alertId,
            alertKey = alertKey,
            stationId = event.stationId,
            header = header,
            locallyRelevant = true,
            openedAt = now,
            lastSeenAt = now,
            expiresAt = expiresAt,
            state = AlertLifecycleState.OPEN,
            expiredAt = null,
        )
        activeAlerts[alertKey] = record

        weatherRadioEventBus.publish(
            AlertOpenedEvent(
                stationId = event.stationId,
                alertId = alertId,
                header = header,
                locallyRelevant = true,
                correlationId = event.correlationId,
                causationId = event.eventId,
            )
        )

        if (datafill.triggerRecordingOnOpen) {
            weatherRadioEventBus.publish(
                AlertRecordingRequestedEvent(
                    stationId = event.stationId,
                    alertId = alertId,
                    header = header,
                    reason = "Alert opened from decoded SAME header",
                    correlationId = event.correlationId,
                    causationId = event.eventId,
                )
            )
        }

        if (datafill.debugLogging) {
            log.debug(
                "Opened alert stationId={} alertId={} eventCode={} senderId={} expiresAt={}",
                event.stationId,
                alertId,
                header.eventCode,
                header.senderId,
                expiresAt,
            )
        }
    }

    private suspend fun expireAlerts() {
        val now = clock.instant()

        activeAlerts.entries
            .filter { (_, record) -> record.expiresAt <= now }
            .forEach { (key, record) ->
                val expiredRecord = record.copy(
                    state = AlertLifecycleState.EXPIRED,
                    expiredAt = now,
                    lastSeenAt = now,
                )

                activeAlerts.remove(key)
                expiredAlerts[record.alertId] = expiredRecord

                weatherRadioEventBus.publish(
                    AlertExpiredEvent(
                        stationId = record.stationId,
                        alertId = record.alertId,
                        header = record.header,
                        expiredAt = now.toString(),
                        correlationId = null,
                        causationId = null,
                    )
                )

                if (datafill.debugLogging) {
                    log.debug(
                        "Expired alert stationId={} alertId={} eventCode={} senderId={} expiredAt={}",
                        record.stationId,
                        record.alertId,
                        record.header.eventCode,
                        record.header.senderId,
                        now,
                    )
                }
            }
    }

    private fun pruneExpiredAlerts() {
        val now = clock.instant()
        val retention = Duration.ofMinutes(datafill.retainedExpiredAlertsMinutes)

        expiredAlerts.entries
            .filter { (_, record) ->
                val expiredAt = record.expiredAt ?: return@filter false
                Duration.between(expiredAt, now) > retention
            }
            .forEach { (alertId, record) ->
                expiredAlerts.remove(alertId)

                if (datafill.debugLogging) {
                    log.debug(
                        "Pruned expired alert stationId={} alertId={} eventCode={} senderId={}",
                        record.stationId,
                        record.alertId,
                        record.header.eventCode,
                        record.header.senderId,
                    )
                }
            }
    }

    private fun isLocallyRelevant(countyCodes: List<String>): Boolean {
        if (datafill.localCountyCodes.isEmpty()) {
            return true
        }
        val configured = datafill.localCountyCodes.toSet()
        return countyCodes.any { it in configured }
    }

    private fun calculateExpiresAt(
        openedAt: Instant,
        purgeTime: String,
    ): Instant {
        val minutes = purgeTime.toLongOrNull() ?: 30L
        return openedAt.plus(Duration.ofMinutes(minutes))
    }
}