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
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AlertOrchestrationFeature(
    systemEventBus: SystemEventBus,
    private val weatherRadioEventBus: WeatherRadioEventBus,
    private val datafill: AlertOrchestrationDatafill,
    private val clock: Clock,
) : ControllableService(systemEventBus) {

    private val log = LoggerFactory.getLogger(javaClass)
    private var subscription: Subscription? = null
    private val activeAlerts = ConcurrentHashMap<AlertKey, ActiveAlertRecord>()

    override fun onStart() {
        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "alert-orchestration",
                status = "STARTING",
                details = mapOf(
                    "localCountyCodes" to datafill.localCountyCodes,
                    "triggerRecordingOnOpen" to datafill.triggerRecordingOnOpen,
                    "duplicateSuppressionMinutes" to datafill.duplicateSuppressionMinutes,
                ),
            )
        )

        subscription = weatherRadioEventBus.subscribe<SameHeaderDecodedEvent> { event ->
            handleDecodedHeader(event)
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
        activeAlerts.clear()

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
            val age = Duration.between(existing.lastSeenAt, now).toMinutes()
            if (age < datafill.duplicateSuppressionMinutes) {
                if (datafill.debugLogging) {
                    log.debug(
                        "Suppressing duplicate alert stationId={} eventCode={} senderId={} ageMinutes={}",
                        event.stationId,
                        header.eventCode,
                        header.senderId,
                        age,
                    )
                }
                return
            }
        }

        val alertId = UUID.randomUUID().toString()
        val record = ActiveAlertRecord(
            alertId = alertId,
            alertKey = alertKey,
            stationId = event.stationId,
            header = header,
            locallyRelevant = true,
            openedAt = now,
            lastSeenAt = now,
            state = AlertLifecycleState.OPEN,
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
    }

    private fun isLocallyRelevant(countyCodes: List<String>): Boolean {
        if (datafill.localCountyCodes.isEmpty()) {
            return true
        }
        val configured = datafill.localCountyCodes.toSet()
        return countyCodes.any { it in configured }
    }
}