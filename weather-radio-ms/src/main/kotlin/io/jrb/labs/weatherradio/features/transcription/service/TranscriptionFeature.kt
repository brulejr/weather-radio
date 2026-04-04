/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Jon Brule
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
import io.jrb.labs.weatherradio.events.AlertAudioFileCreatedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptCreatedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptFileCreatedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptFileCreationFailedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptionFailedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptionFallbackSelectedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptionSkippedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptionStartedEvent
import io.jrb.labs.weatherradio.events.FeatureHeartbeatEvent
import io.jrb.labs.weatherradio.events.WeatherRadioEventBus
import io.jrb.labs.weatherradio.features.transcription.TranscriptionDatafill
import io.jrb.labs.weatherradio.features.transcription.model.AlertTranscriptRecord
import io.jrb.labs.weatherradio.features.transcription.port.AudioFileTranscriber
import io.jrb.labs.weatherradio.features.transcription.port.NormalizedTranscript
import io.jrb.labs.weatherradio.features.transcription.port.TranscriptArtifactWriter
import io.jrb.labs.weatherradio.features.transcription.port.TranscriptNormalizer
import io.jrb.labs.weatherradio.features.transcription.port.TranscriptionDecisionPolicy
import org.slf4j.LoggerFactory
import java.time.Clock

class TranscriptionFeature(
    systemEventBus: SystemEventBus,
    private val weatherRadioEventBus: WeatherRadioEventBus,
    private val datafill: TranscriptionDatafill,
    private val audioFileTranscriber: AudioFileTranscriber,
    private val fallbackAudioFileTranscriber: AudioFileTranscriber,
    private val transcriptArtifactWriter: TranscriptArtifactWriter,
    private val transcriptNormalizer: TranscriptNormalizer,
    private val transcriptionDecisionPolicy: TranscriptionDecisionPolicy,
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
                    "writeTranscriptFiles" to datafill.writeTranscriptFiles,
                    "artifactDirectory" to datafill.artifactDirectory,
                    "enableFallbackTranscription" to datafill.enableFallbackTranscription,
                    "fallbackWhenPoorQuality" to datafill.fallbackWhenPoorQuality,
                    "fallbackWhenPrimaryFails" to datafill.fallbackWhenPrimaryFails,
                    "primaryEngineName" to datafill.primaryEngineName,
                    "fallbackEngineName" to datafill.fallbackEngineName,
                ),
            )
        )

        subscription = weatherRadioEventBus.subscribe<AlertAudioFileCreatedEvent> {
            handleAudioFileCreated(it)
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

    private suspend fun handleAudioFileCreated(event: AlertAudioFileCreatedEvent) {
        try {
            val decision = transcriptionDecisionPolicy.decide(event)

            if (!decision.shouldTranscribe) {
                weatherRadioEventBus.publish(
                    AlertTranscriptionSkippedEvent(
                        stationId = event.stationId,
                        alertId = event.alertId,
                        reason = decision.reason,
                        correlationId = event.correlationId,
                        causationId = event.eventId,
                    )
                )
                return
            }

            val selectedTranscriber = if (decision.useFallback) {
                weatherRadioEventBus.publish(
                    AlertTranscriptionFallbackSelectedEvent(
                        stationId = event.stationId,
                        alertId = event.alertId,
                        reason = decision.reason,
                        primaryEngineName = datafill.primaryEngineName,
                        fallbackEngineName = datafill.fallbackEngineName,
                        correlationId = event.correlationId,
                        causationId = event.eventId,
                    )
                )
                fallbackAudioFileTranscriber
            } else {
                audioFileTranscriber
            }

            val selectedEngineName = if (decision.useFallback) {
                datafill.fallbackEngineName
            } else {
                datafill.primaryEngineName
            }

            weatherRadioEventBus.publish(
                AlertTranscriptionStartedEvent(
                    stationId = event.stationId,
                    alertId = event.alertId,
                    engineName = selectedEngineName,
                    correlationId = event.correlationId,
                    causationId = event.eventId,
                )
            )

            val rawTranscript = try {
                selectedTranscriber.transcribe(event)
            } catch (ex: Exception) {
                if (!decision.useFallback &&
                    datafill.enableFallbackTranscription &&
                    datafill.fallbackWhenPrimaryFails
                ) {
                    weatherRadioEventBus.publish(
                        AlertTranscriptionFallbackSelectedEvent(
                            stationId = event.stationId,
                            alertId = event.alertId,
                            reason = "Primary transcription failed: ${ex.message ?: "Unknown error"}",
                            primaryEngineName = datafill.primaryEngineName,
                            fallbackEngineName = datafill.fallbackEngineName,
                            correlationId = event.correlationId,
                            causationId = event.eventId,
                        )
                    )
                    fallbackAudioFileTranscriber.transcribe(event)
                } else {
                    throw ex
                }
            }

            val normalized = if (datafill.normalizeTranscriptText) {
                transcriptNormalizer.normalize(rawTranscript.transcriptText)
            } else {
                NormalizedTranscript(
                    rawText = rawTranscript.transcriptText,
                    normalizedText = rawTranscript.transcriptText,
                    wasChanged = false,
                )
            }

            val finalText = normalized.normalizedText

            if (finalText.length < datafill.minimumTranscriptLength) {
                if (datafill.emitTranscriptSkippedEvent) {
                    weatherRadioEventBus.publish(
                        AlertTranscriptionSkippedEvent(
                            stationId = event.stationId,
                            alertId = event.alertId,
                            reason = "Transcript length ${finalText.length} below minimum ${datafill.minimumTranscriptLength}",
                            correlationId = event.correlationId,
                            causationId = event.eventId,
                        )
                    )
                }
                return
            }

            val transcript = AlertTranscriptRecord(
                alertId = event.alertId,
                stationId = event.stationId,
                transcriptText = finalText,
                confidence = rawTranscript.confidence,
                createdAt = clock.instant(),
                engineName = rawTranscript.engineName,
                details = rawTranscript.details + mapOf(
                    "language" to datafill.defaultLanguage,
                    "rawTranscriptText" to normalized.rawText.takeIf { datafill.preserveRawTranscriptText },
                    "rawTextLength" to normalized.rawText.length,
                    "normalizedTextLength" to finalText.length,
                    "wasNormalized" to normalized.wasChanged,
                    "rawTextPreserved" to datafill.preserveRawTranscriptText,
                    "sourceAudioFilePath" to event.artifact.filePath,
                    "sourceAudioFormat" to event.artifact.format,
                    "sourceAudioSampleRateHz" to event.artifact.sampleRateHz,
                    "sourceAudioChannelCount" to event.artifact.channelCount,
                    "sourceAudioFrameCount" to event.artifact.frameCount,
                    "sourceAudioCreatedAt" to event.artifact.createdAt.toString(),
                    "sourceAudioStartedAt" to event.artifact.startedAt.toString(),
                    "sourceAudioCompletedAt" to event.artifact.completedAt.toString(),
                    "sourceAudioDurationMillis" to event.artifact.durationMillis,
                    "sourceAudioByteLength" to event.artifact.byteLength,
                    "sourceAudioCaptureReason" to event.artifact.captureReason,
                    "sourceAudioWasPartial" to event.artifact.wasPartial,
                    "sourceAudioQualityClassification" to event.artifact.qualityClassification,
                    "sourceAudioAcceptableForTranscription" to event.artifact.acceptableForTranscription,
                    "sourceAudioPeakAmplitude" to event.artifact.qualityMetrics?.peakAmplitude,
                    "sourceAudioRmsAmplitude" to event.artifact.qualityMetrics?.rmsAmplitude,
                    "sourceAudioSilenceFraction" to event.artifact.qualityMetrics?.silenceFraction,
                    "sourceAudioClippedFraction" to event.artifact.qualityMetrics?.clippedFraction,
                )
            )

            weatherRadioEventBus.publish(
                AlertTranscriptCreatedEvent(
                    stationId = event.stationId,
                    alertId = event.alertId,
                    transcript = transcript,
                    correlationId = event.correlationId,
                    causationId = event.eventId,
                )
            )

            if (datafill.writeTranscriptFiles) {
                try {
                    val artifact = transcriptArtifactWriter.writeTranscript(transcript)
                    weatherRadioEventBus.publish(
                        AlertTranscriptFileCreatedEvent(
                            stationId = event.stationId,
                            alertId = event.alertId,
                            artifact = artifact,
                            correlationId = event.correlationId,
                            causationId = event.eventId,
                        )
                    )
                } catch (ex: Exception) {
                    weatherRadioEventBus.publish(
                        AlertTranscriptFileCreationFailedEvent(
                            stationId = event.stationId,
                            alertId = event.alertId,
                            reason = ex.message ?: "Unknown transcript file creation failure",
                            correlationId = event.correlationId,
                            causationId = event.eventId,
                        )
                    )
                }
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