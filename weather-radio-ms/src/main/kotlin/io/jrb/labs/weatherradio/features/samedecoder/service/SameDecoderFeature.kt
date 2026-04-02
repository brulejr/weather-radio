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

package io.jrb.labs.weatherradio.features.samedecoder.service

import io.jrb.labs.commons.eventbus.EventBus.Subscription
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.metrics.FeatureMetricsFactory
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.weatherradio.domain.radio.AudioFrame
import io.jrb.labs.weatherradio.events.AudioFrameAvailableEvent
import io.jrb.labs.weatherradio.events.FeatureHeartbeatEvent
import io.jrb.labs.weatherradio.events.SameHeaderDecodedEvent
import io.jrb.labs.weatherradio.events.SameHeaderDetectedEvent
import io.jrb.labs.weatherradio.events.SameHeaderRejectedEvent
import io.jrb.labs.weatherradio.events.WeatherRadioEventBus
import io.jrb.labs.weatherradio.features.samedecoder.SameDecoderDatafill
import io.jrb.labs.weatherradio.features.samedecoder.model.SameDecodeAttempt
import io.jrb.labs.weatherradio.features.samedecoder.model.SameDecodeResult
import io.jrb.labs.weatherradio.features.samedecoder.support.SameFrameDecoder
import org.slf4j.LoggerFactory
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

class SameDecoderFeature(
    systemEventBus: SystemEventBus,
    private val weatherRadioEventBus: WeatherRadioEventBus,
    private val datafill: SameDecoderDatafill,
    private val decoder: SameFrameDecoder,
    private val clock: Clock,
    private val metricsFactory: FeatureMetricsFactory,
) : ControllableService(systemEventBus) {

    private val log = LoggerFactory.getLogger(javaClass)
    private var subscription: Subscription? = null

    private val framesByStation = ConcurrentHashMap<String, MutableList<AudioFrame>>()

    override fun onStart() {
        if (!datafill.enabled) {
            weatherRadioEventBus.send(
                FeatureHeartbeatEvent(
                    featureId = "same-decoder",
                    status = "DISABLED",
                )
            )
            return
        }

        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "same-decoder",
                status = "STARTING",
                details = mapOf(
                    "windowDurationMs" to datafill.windowDurationMs,
                    "minFramesPerWindow" to datafill.minFramesPerWindow,
                    "minConfidence" to datafill.minConfidence,
                ),
            )
        )

        subscription = weatherRadioEventBus.subscribe<AudioFrameAvailableEvent> { event ->
            handleAudioFrame(event)
        }

        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "same-decoder",
                status = "RUNNING",
            )
        )
    }

    override fun onStop() {
        subscription?.cancel()
        subscription = null
        framesByStation.clear()

        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "same-decoder",
                status = "STOPPED",
                details = mapOf(
                    "stoppedAt" to clock.instant().toString(),
                ),
            )
        )
    }

    private suspend fun handleAudioFrame(event: AudioFrameAvailableEvent) {
        val bucket = framesByStation.computeIfAbsent(event.stationId) { mutableListOf() }
        bucket.add(event.frame)

        trimWindow(bucket)

        if (bucket.size < datafill.minFramesPerWindow) {
            return
        }

        val first = bucket.first()
        val last = bucket.last()
        val durationMs = java.time.Duration.between(first.frameStart, last.frameEnd).toMillis()

        if (durationMs < datafill.windowDurationMs) {
            return
        }

        val attempt = SameDecodeAttempt(
            stationId = event.stationId,
            frames = bucket.toList(),
            attemptStartedAt = first.frameStart,
            attemptEndedAt = last.frameEnd,
        )

        when (val result = decoder.decode(attempt)) {
            is SameDecodeResult.Decoded -> {
                if (result.confidence >= datafill.minConfidence) {
                    weatherRadioEventBus.publish(
                        SameHeaderDetectedEvent(
                            stationId = event.stationId,
                            confidence = result.confidence,
                            burstCount = result.burstCount,
                        )
                    )

                    weatherRadioEventBus.publish(
                        SameHeaderDecodedEvent(
                            stationId = event.stationId,
                            header = result.header,
                            confidence = result.confidence,
                        )
                    )

                    if (datafill.debugLogging) {
                        log.debug(
                            "Decoded SAME header stationId={} eventCode={} senderId={} confidence={}",
                            event.stationId,
                            result.header.eventCode,
                            result.header.senderId,
                            result.confidence,
                        )
                    }

                    bucket.clear()
                } else {
                    weatherRadioEventBus.publish(
                        SameHeaderRejectedEvent(
                            stationId = event.stationId,
                            reason = "Decode confidence below threshold",
                            confidence = result.confidence,
                        )
                    )
                    bucket.clear()
                }
            }

            is SameDecodeResult.Rejected -> {
                weatherRadioEventBus.publish(
                    SameHeaderRejectedEvent(
                        stationId = event.stationId,
                        reason = result.reason,
                        confidence = result.confidence,
                    )
                )
                bucket.clear()
            }

            SameDecodeResult.NoSignal -> {
                // keep buffering, but do not let the window grow forever
                if (bucket.size > datafill.minFramesPerWindow * 4) {
                    bucket.removeFirst()
                }
            }
        }
    }

    private fun trimWindow(bucket: MutableList<AudioFrame>) {
        if (bucket.isEmpty()) return

        val newestEnd = bucket.last().frameEnd
        while (bucket.isNotEmpty()) {
            val oldest = bucket.first()
            val ageMs = java.time.Duration.between(oldest.frameStart, newestEnd).toMillis()
            if (ageMs <= datafill.windowDurationMs * 2) {
                break
            }
            bucket.removeFirst()
        }
    }

}