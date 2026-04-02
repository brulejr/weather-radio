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

package io.jrb.labs.weatherradio.features.radioinput.service

import io.jrb.labs.commons.metrics.FeatureMetricsFactory
import io.jrb.labs.weatherradio.domain.radio.RadioStation
import io.jrb.labs.weatherradio.events.AudioFramePublishedEvent
import io.jrb.labs.weatherradio.events.FeatureHeartbeatEvent
import io.jrb.labs.weatherradio.events.WeatherRadioEventBus
import io.jrb.labs.weatherradio.features.radioinput.RadioInputDatafill
import io.jrb.labs.weatherradio.ports.radio.RadioAudioSource
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Clock

class RadioInputFeature(
    private val datafill: RadioInputDatafill,
    private val audioSource: RadioAudioSource,
    private val eventBus: WeatherRadioEventBus,
    private val clock: Clock,
    private val metricsFactory: FeatureMetricsFactory,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun start() {
        if (!datafill.enabled) {
            eventBus.send(
                FeatureHeartbeatEvent(
                    featureId = "radio-input",
                    status = "DISABLED",
                )
            )
            return
        }

        val stations = datafill.stations
            .filter { it.enabled }
            .map {
                RadioStation(
                    stationId = it.stationId,
                    displayName = it.displayName,
                    frequencyMHz = it.frequencyMHz,
                    enabled = it.enabled,
                    countyCodes = it.countyCodes,
                )
            }

        eventBus.send(
            FeatureHeartbeatEvent(
                featureId = "radio-input",
                status = "STARTING",
                details = mapOf(
                    "mode" to datafill.mode.name,
                    "stationCount" to stations.size,
                    "startedAt" to clock.instant().toString(),
                ),
            )
        )

        stations.forEach { station ->
            scope.launch {
                log.info(
                    "Starting radio-input stream for station={} frequencyMHz={}",
                    station.stationId,
                    station.frequencyMHz,
                )

                audioSource.stream(station).collect { frame ->
                    eventBus.publish(
                        AudioFramePublishedEvent(
                            stationId = frame.stationId,
                            sampleRateHz = frame.sampleRateHz,
                            channelCount = frame.channelCount,
                            sampleCount = frame.sampleCount,
                            frameStart = frame.frameStart,
                            frameEnd = frame.frameEnd,
                            rmsLevel = frame.rmsLevel(),
                        )
                    )
                }
            }
        }

        eventBus.send(
            FeatureHeartbeatEvent(
                featureId = "radio-input",
                status = "RUNNING",
                details = mapOf(
                    "mode" to datafill.mode.name,
                    "stationCount" to stations.size,
                ),
            )
        )
    }

    @PreDestroy
    fun stop() {
        scope.cancel()

        eventBus.send(
            FeatureHeartbeatEvent(
                featureId = "radio-input",
                status = "STOPPED",
                details = mapOf(
                    "stoppedAt" to clock.instant().toString(),
                ),
            )
        )
    }
}