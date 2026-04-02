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

package io.jrb.labs.weatherradio.features.transcription.service

import io.jrb.labs.commons.eventbus.EventBus.Subscription
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.weatherradio.events.AlertAudioCapturedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptCreatedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptionFailedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptionStartedEvent
import io.jrb.labs.weatherradio.events.FeatureHeartbeatEvent
import io.jrb.labs.weatherradio.events.WeatherRadioEventBus
import io.jrb.labs.weatherradio.features.transcription.TranscriptionDatafill
import io.jrb.labs.weatherradio.features.transcription.port.AudioTranscriber
import org.slf4j.LoggerFactory
import java.time.Clock

class TranscriptionFeature(
    systemEventBus: SystemEventBus,
    private val weatherRadioEventBus: WeatherRadioEventBus,
    private val datafill: TranscriptionDatafill,
    private val audioTranscriber: AudioTranscriber,
    private val clock: Clock,
) : ControllableService(systemEventBus) {

    private val log = LoggerFactory.getLogger(javaClass)
    private var subscription: Subscription? = null

    override fun onStart() {
        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "transcription",
                status = "STARTING",
                details = mapOf(
                    "syntheticMode" to datafill.syntheticMode,
                    "includeDebugTranscriptDetails" to datafill.includeDebugTranscriptDetails,
                ),
            )
        )

        subscription = weatherRadioEventBus.subscribe<AlertAudioCapturedEvent> { event ->
            handleAudioCaptured(event)
        }

        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "transcription",
                status = "RUNNING",
            )
        )
    }

    override fun onStop() {
        subscription?.cancel()
        subscription = null

        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "transcription",
                status = "STOPPED",
                details = mapOf("stoppedAt" to clock.instant().toString()),
            )
        )
    }

    private suspend fun handleAudioCaptured(event: AlertAudioCapturedEvent) {
        try {
            weatherRadioEventBus.publish(
                AlertTranscriptionStartedEvent(
                    stationId = event.stationId,
                    alertId = event.alertId,
                    engineName = "synthetic-audio-transcriber",
                    correlationId = event.correlationId,
                    causationId = event.eventId,
                )
            )

            val transcript = audioTranscriber.transcribe(event)

            weatherRadioEventBus.publish(
                AlertTranscriptCreatedEvent(
                    stationId = event.stationId,
                    alertId = event.alertId,
                    transcript = transcript,
                    correlationId = event.correlationId,
                    causationId = event.eventId,
                )
            )

            if (datafill.debugLogging) {
                log.debug(
                    "Transcribed alert stationId={} alertId={} engine={} confidence={}",
                    event.stationId,
                    event.alertId,
                    transcript.engineName,
                    transcript.confidence,
                )
            }
        } catch (ex: Exception) {
            weatherRadioEventBus.publish(
                AlertTranscriptionFailedEvent(
                    stationId = event.stationId,
                    alertId = event.alertId,
                    reason = ex.message ?: "Unknown transcription failure",
                    correlationId = event.correlationId,
                    causationId = event.eventId,
                )
            )
        }
    }
}