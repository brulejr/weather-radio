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

package io.jrb.labs.weatherradio.features.observability.service

import io.jrb.labs.commons.eventbus.EventBus.Subscription
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.weatherradio.events.AlertArtifactStoredEvent
import io.jrb.labs.weatherradio.events.AlertAudioCaptureFailedEvent
import io.jrb.labs.weatherradio.events.AlertAudioCaptureStartedEvent
import io.jrb.labs.weatherradio.events.AlertAudioCapturedEvent
import io.jrb.labs.weatherradio.events.AlertAudioFileCreatedEvent
import io.jrb.labs.weatherradio.events.AlertAudioFileCreationFailedEvent
import io.jrb.labs.weatherradio.events.AlertExpiredEvent
import io.jrb.labs.weatherradio.events.AlertIgnoredEvent
import io.jrb.labs.weatherradio.events.AlertOpenedEvent
import io.jrb.labs.weatherradio.events.AlertRecordingRequestedEvent
import io.jrb.labs.weatherradio.events.AlertStateStoredEvent
import io.jrb.labs.weatherradio.events.AlertStoreFailedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptCreatedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptFileCreatedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptFileCreationFailedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptionFailedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptionStartedEvent
import io.jrb.labs.weatherradio.events.AudioFramePublishedEvent
import io.jrb.labs.weatherradio.events.FeatureHeartbeatEvent
import io.jrb.labs.weatherradio.events.SameHeaderDecodedEvent
import io.jrb.labs.weatherradio.events.SameHeaderDetectedEvent
import io.jrb.labs.weatherradio.events.SameHeaderRejectedEvent
import io.jrb.labs.weatherradio.events.WeatherRadioEventBus
import io.jrb.labs.weatherradio.features.observability.ObservabilityDatafill
import org.slf4j.LoggerFactory

class ObservabilityEventLoggerFeature(
    systemEventBus: SystemEventBus,
    private val weatherRadioEventBus: WeatherRadioEventBus,
    private val datafill: ObservabilityDatafill,
) : ControllableService(systemEventBus) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val subscriptions = mutableListOf<Subscription>()

    override fun onStart() {
        if (!datafill.enabled) return

        subscriptions += weatherRadioEventBus.subscribe<FeatureHeartbeatEvent> { event ->
            if (datafill.logHeartbeats) {
                log.info(
                    "feature-heartbeat featureId={} status={} stationId={} details={}",
                    event.featureId,
                    event.status,
                    event.stationId,
                    event.details,
                )
            }
        }

        subscriptions += weatherRadioEventBus.subscribe<SameHeaderDetectedEvent> { event ->
            log.info(
                "same-detected stationId={} confidence={} burstCount={}",
                event.stationId,
                event.confidence,
                event.burstCount,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<SameHeaderDecodedEvent> { event ->
            log.info(
                "same-decoded stationId={} eventCode={} senderId={} counties={} confidence={}",
                event.stationId,
                event.header.eventCode,
                event.header.senderId,
                event.header.countyCodes,
                event.confidence,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<SameHeaderRejectedEvent> { event ->
            if (datafill.logSameRejected) {
                log.warn(
                    "same-rejected stationId={} reason={} confidence={}",
                    event.stationId,
                    event.reason,
                    event.confidence,
                )
            }
        }

        subscriptions += weatherRadioEventBus.subscribe<AudioFramePublishedEvent> { event ->
            if (datafill.logAudioFrames) {
                log.debug(
                    "audio-frame stationId={} sampleRateHz={} channelCount={} sampleCount={} rmsLevel={}",
                    event.stationId,
                    event.sampleRateHz,
                    event.channelCount,
                    event.sampleCount,
                    event.rmsLevel,
                )
            }
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertOpenedEvent> { event ->
            log.info(
                "alert-opened stationId={} alertId={} eventCode={} senderId={} counties={} locallyRelevant={}",
                event.stationId,
                event.alertId,
                event.header.eventCode,
                event.header.senderId,
                event.header.countyCodes,
                event.locallyRelevant,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertIgnoredEvent> { event ->
            log.info(
                "alert-ignored stationId={} eventCode={} senderId={} reason={}",
                event.stationId,
                event.header.eventCode,
                event.header.senderId,
                event.reason,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertRecordingRequestedEvent> { event ->
            log.info(
                "alert-recording-requested stationId={} alertId={} eventCode={} reason={}",
                event.stationId,
                event.alertId,
                event.header.eventCode,
                event.reason,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertAudioCaptureStartedEvent> { event ->
            log.info(
                "alert-audio-capture-started stationId={} alertId={} reason={}",
                event.stationId,
                event.alertId,
                event.reason,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertAudioCapturedEvent> { event ->
            log.info(
                "alert-audio-captured stationId={} alertId={} frameCount={} sampleRateHz={} channelCount={}",
                event.stationId,
                event.alertId,
                event.capture.frameCount,
                event.capture.sampleRateHz,
                event.capture.channelCount,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertAudioCaptureFailedEvent> { event ->
            log.warn(
                "alert-audio-capture-failed stationId={} alertId={} reason={}",
                event.stationId,
                event.alertId,
                event.reason,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertStateStoredEvent> { event ->
            log.info(
                "alert-state-stored stationId={} alertId={} state={}",
                event.stationId,
                event.alertId,
                event.state,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertArtifactStoredEvent> { event ->
            log.info(
                "alert-artifact-stored stationId={} alertId={} artifactType={}",
                event.stationId,
                event.alertId,
                event.artifactType,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertStoreFailedEvent> { event ->
            log.warn(
                "alert-store-failed stationId={} alertId={} reason={}",
                event.stationId,
                event.alertId,
                event.reason,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertTranscriptionStartedEvent> { event ->
            log.info(
                "alert-transcription-started stationId={} alertId={} engineName={}",
                event.stationId,
                event.alertId,
                event.engineName,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertTranscriptCreatedEvent> { event ->
            log.info(
                "alert-transcript-created stationId={} alertId={} engineName={} confidence={} transcriptText={}",
                event.stationId,
                event.alertId,
                event.transcript.engineName,
                event.transcript.confidence,
                event.transcript.transcriptText,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertTranscriptionFailedEvent> { event ->
            log.warn(
                "alert-transcription-failed stationId={} alertId={} reason={}",
                event.stationId,
                event.alertId,
                event.reason,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertExpiredEvent> { event ->
            log.info(
                "alert-expired stationId={} alertId={} eventCode={} senderId={} expiredAt={}",
                event.stationId,
                event.alertId,
                event.header.eventCode,
                event.header.senderId,
                event.expiredAt,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertAudioFileCreatedEvent> { event ->
            log.info(
                "alert-audio-file-created stationId={} alertId={} filePath={} format={}",
                event.stationId,
                event.alertId,
                event.artifact.filePath,
                event.artifact.format,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertAudioFileCreationFailedEvent> { event ->
            log.warn(
                "alert-audio-file-creation-failed stationId={} alertId={} reason={}",
                event.stationId,
                event.alertId,
                event.reason,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertTranscriptFileCreatedEvent> { event ->
            log.info(
                "alert-transcript-file-created stationId={} alertId={} textFilePath={}",
                event.stationId,
                event.alertId,
                event.artifact.textFilePath,
            )
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertTranscriptFileCreationFailedEvent> { event ->
            log.warn(
                "alert-transcript-file-creation-failed stationId={} alertId={} reason={}",
                event.stationId,
                event.alertId,
                event.reason,
            )
        }

    }

    override fun onStop() {
        subscriptions.forEach { it.cancel() }
        subscriptions.clear()
    }

}