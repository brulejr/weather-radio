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

package io.jrb.labs.weatherradio.features.alertstore.service

import io.jrb.labs.commons.eventbus.EventBus.Subscription
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.weatherradio.events.AlertArtifactStoredEvent
import io.jrb.labs.weatherradio.events.AlertAudioCaptureFailedEvent
import io.jrb.labs.weatherradio.events.AlertAudioCaptureSkippedEvent
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
import io.jrb.labs.weatherradio.events.AlertTranscriptionSkippedEvent
import io.jrb.labs.weatherradio.events.AlertTranscriptionStartedEvent
import io.jrb.labs.weatherradio.events.FeatureHeartbeatEvent
import io.jrb.labs.weatherradio.events.WeatherRadioEvent
import io.jrb.labs.weatherradio.events.WeatherRadioEventBus
import io.jrb.labs.weatherradio.features.alertstore.AlertStoreDatafill
import io.jrb.labs.weatherradio.features.alertstore.model.StoredAlertArtifact
import io.jrb.labs.weatherradio.features.alertstore.model.StoredAlertRecord
import io.jrb.labs.weatherradio.features.alertstore.port.AlertStoreRepository
import org.slf4j.LoggerFactory
import java.time.Clock

class AlertStoreFeature(
    systemEventBus: SystemEventBus,
    private val weatherRadioEventBus: WeatherRadioEventBus,
    private val datafill: AlertStoreDatafill,
    private val repository: AlertStoreRepository,
    private val clock: Clock,
) : ControllableService(systemEventBus) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val subscriptions = mutableListOf<Subscription>()

    override fun onStart() {
        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "alert-store",
                status = "STARTING",
                details = mapOf(
                    "storeAudioFramesInMemory" to datafill.storeAudioFramesInMemory,
                ),
            )
        )

        subscriptions += weatherRadioEventBus.subscribe<AlertOpenedEvent> { handleAlertOpened(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertIgnoredEvent> { handleAlertIgnored(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertRecordingRequestedEvent> { handleRecordingRequested(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertAudioCaptureStartedEvent> { handleAudioCaptureStarted(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertAudioCapturedEvent> { handleAudioCaptured(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertAudioCaptureFailedEvent> { handleAudioCaptureFailed(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertTranscriptionStartedEvent> { handleTranscriptionStarted(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertTranscriptCreatedEvent> { handleTranscriptCreated(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertTranscriptionFailedEvent> { handleTranscriptionFailed(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertExpiredEvent> { handleAlertExpired(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertAudioFileCreatedEvent> { handleAudioFileCreated(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertAudioFileCreationFailedEvent> { handleAudioFileCreationFailed(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertTranscriptFileCreatedEvent> { handleTranscriptFileCreated(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertTranscriptFileCreationFailedEvent> { handleTranscriptFileCreationFailed(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertTranscriptionSkippedEvent> { handleTranscriptionSkipped(it) }
        subscriptions += weatherRadioEventBus.subscribe<AlertAudioCaptureSkippedEvent> { handleAudioCaptureSkipped(it) }

        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "alert-store",
                status = "RUNNING",
            )
        )
    }

    override fun onStop() {
        subscriptions.forEach { it.cancel() }
        subscriptions.clear()

        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "alert-store",
                status = "STOPPED",
                details = mapOf("stoppedAt" to clock.instant().toString()),
            )
        )
    }

    private suspend fun handleAlertOpened(event: AlertOpenedEvent) {
        try {
            repository.upsert(
                StoredAlertRecord(
                    alertId = event.alertId,
                    stationId = event.stationId,
                    state = "OPEN",
                    header = event.header,
                    locallyRelevant = event.locallyRelevant,
                    openedAt = clock.instant(),
                    updatedAt = clock.instant(),
                )
            )

            weatherRadioEventBus.publish(
                AlertStateStoredEvent(
                    stationId = event.stationId,
                    alertId = event.alertId,
                    state = "OPEN",
                    correlationId = event.correlationId,
                    causationId = event.eventId,
                )
            )
        } catch (ex: Exception) {
            storeFailed(event.stationId, event.alertId, "Failed to store alert-opened state: ${ex.message}", event)
        }
    }

    private suspend fun handleAlertIgnored(event: AlertIgnoredEvent) {
        try {
            val alertId = "ignored:${event.stationId}:${event.header.senderId}:${event.header.eventCode}"
            repository.upsert(
                StoredAlertRecord(
                    alertId = alertId,
                    stationId = event.stationId,
                    state = "IGNORED",
                    header = event.header,
                    locallyRelevant = false,
                    updatedAt = clock.instant(),
                    artifacts = listOf(
                        StoredAlertArtifact(
                            artifactType = "ignore-reason",
                            createdAt = clock.instant(),
                            details = mapOf("reason" to event.reason),
                        )
                    ),
                )
            )

            weatherRadioEventBus.publish(
                AlertStateStoredEvent(
                    stationId = event.stationId,
                    alertId = alertId,
                    state = "IGNORED",
                    correlationId = event.correlationId,
                    causationId = event.eventId,
                )
            )
        } catch (ex: Exception) {
            storeFailed(event.stationId, null, "Failed to store alert-ignored state: ${ex.message}", event)
        }
    }

    private suspend fun handleRecordingRequested(event: AlertRecordingRequestedEvent) {
        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "recording-requested",
                createdAt = clock.instant(),
                details = mapOf("reason" to event.reason),
            ),
            source = event,
        )
    }

    private suspend fun handleAudioCaptureStarted(event: AlertAudioCaptureStartedEvent) {
        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "audio-capture-started",
                createdAt = clock.instant(),
                details = mapOf("reason" to event.reason),
            ),
            source = event,
        )
    }

    private suspend fun handleAudioCaptured(event: AlertAudioCapturedEvent) {
        val details = mutableMapOf<String, Any?>(
            "frameCount" to event.capture.frameCount,
            "sampleRateHz" to event.capture.sampleRateHz,
            "channelCount" to event.capture.channelCount,
            "startedAt" to event.capture.startedAt.toString(),
            "completedAt" to event.capture.completedAt.toString(),
            "durationMillis" to event.capture.durationMillis,
            "captureReason" to event.capture.captureReason,
            "preRollFrameCount" to event.capture.preRollFrameCount,
            "wasPartial" to event.capture.wasPartial,
        )

        if (datafill.storeAudioFramesInMemory) {
            details["frames"] = event.capture.frames
        }

        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "audio-captured",
                createdAt = clock.instant(),
                details = details,
            ),
            source = event,
        )
    }

    private suspend fun handleAudioCaptureFailed(event: AlertAudioCaptureFailedEvent) {
        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "audio-capture-failed",
                createdAt = clock.instant(),
                details = mapOf("reason" to event.reason),
            ),
            source = event,
        )
    }

    private suspend fun handleTranscriptionStarted(event: AlertTranscriptionStartedEvent) {
        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "transcription-started",
                createdAt = clock.instant(),
                details = mapOf("engineName" to event.engineName),
            ),
            source = event,
        )
    }

    private suspend fun handleTranscriptCreated(event: AlertTranscriptCreatedEvent) {
        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "transcript-created",
                createdAt = clock.instant(),
                details = mapOf(
                    "engineName" to event.transcript.engineName,
                    "confidence" to event.transcript.confidence,
                    "transcriptText" to event.transcript.transcriptText,
                    "language" to event.transcript.details["language"],
                    "rawTextLength" to event.transcript.details["rawTextLength"],
                    "normalizedTextLength" to event.transcript.details["normalizedTextLength"],
                    "wasNormalized" to event.transcript.details["wasNormalized"],
                    "rawTextPreserved" to event.transcript.details["rawTextPreserved"],
                    "rawTranscriptText" to event.transcript.details["rawTranscriptText"],
                    "details" to event.transcript.details,
                )
            ),
            source = event,
        )
    }

    private suspend fun handleTranscriptionFailed(event: AlertTranscriptionFailedEvent) {
        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "transcription-failed",
                createdAt = clock.instant(),
                details = mapOf("reason" to event.reason),
            ),
            source = event,
        )
    }

    private suspend fun handleAlertExpired(event: AlertExpiredEvent) {
        try {
            val existing = repository.findByAlertId(event.alertId)
            val updated = if (existing != null) {
                existing.copy(
                    state = "EXPIRED",
                    updatedAt = clock.instant(),
                    artifacts = existing.artifacts + StoredAlertArtifact(
                        artifactType = "alert-expired",
                        createdAt = clock.instant(),
                        details = mapOf("expiredAt" to event.expiredAt),
                    ),
                )
            } else {
                StoredAlertRecord(
                    alertId = event.alertId,
                    stationId = event.stationId,
                    state = "EXPIRED",
                    header = event.header,
                    updatedAt = clock.instant(),
                    artifacts = listOf(
                        StoredAlertArtifact(
                            artifactType = "alert-expired",
                            createdAt = clock.instant(),
                            details = mapOf("expiredAt" to event.expiredAt),
                        )
                    ),
                )
            }

            repository.upsert(updated)

            weatherRadioEventBus.publish(
                AlertStateStoredEvent(
                    stationId = event.stationId,
                    alertId = event.alertId,
                    state = "EXPIRED",
                    correlationId = event.correlationId,
                    causationId = event.eventId,
                )
            )
        } catch (ex: Exception) {
            storeFailed(
                event.stationId,
                event.alertId,
                "Failed to store alert-expired state: ${ex.message}",
                event,
            )
        }
    }

    private suspend fun handleAudioFileCreated(event: AlertAudioFileCreatedEvent) {
        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "audio-file",
                createdAt = clock.instant(),
                details = mapOf(
                    "filePath" to event.artifact.filePath,
                    "format" to event.artifact.format,
                    "sampleRateHz" to event.artifact.sampleRateHz,
                    "channelCount" to event.artifact.channelCount,
                    "frameCount" to event.artifact.frameCount,
                    "artifactCreatedAt" to event.artifact.createdAt.toString(),
                    "startedAt" to event.artifact.startedAt.toString(),
                    "completedAt" to event.artifact.completedAt.toString(),
                    "durationMillis" to event.artifact.durationMillis,
                    "byteLength" to event.artifact.byteLength,
                    "captureReason" to event.artifact.captureReason,
                    "wasPartial" to event.artifact.wasPartial,
                )
            ),
            source = event,
        )
    }

    private suspend fun handleAudioFileCreationFailed(event: AlertAudioFileCreationFailedEvent) {
        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "audio-file-failed",
                createdAt = clock.instant(),
                details = mapOf("reason" to event.reason),
            ),
            source = event,
        )
    }

    private suspend fun handleTranscriptFileCreated(event: AlertTranscriptFileCreatedEvent) {
        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "transcript-file",
                createdAt = clock.instant(),
                details = mapOf(
                    "textFilePath" to event.artifact.textFilePath,
                    "jsonFilePath" to event.artifact.jsonFilePath,
                    "artifactCreatedAt" to event.artifact.createdAt.toString(),
                )
            ),
            source = event,
        )
    }

    private suspend fun handleTranscriptFileCreationFailed(event: AlertTranscriptFileCreationFailedEvent) {
        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "transcript-file-failed",
                createdAt = clock.instant(),
                details = mapOf("reason" to event.reason),
            ),
            source = event,
        )
    }

    private suspend fun handleTranscriptionSkipped(event: AlertTranscriptionSkippedEvent) {
        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "transcription-skipped",
                createdAt = clock.instant(),
                details = mapOf(
                    "reason" to event.reason,
                ),
            ),
            source = event,
        )
    }

    private suspend fun handleAudioCaptureSkipped(event: AlertAudioCaptureSkippedEvent) {
        appendArtifact(
            stationId = event.stationId,
            alertId = event.alertId,
            artifact = StoredAlertArtifact(
                artifactType = "audio-capture-skipped",
                createdAt = clock.instant(),
                details = mapOf(
                    "reason" to event.reason,
                ),
            ),
            source = event,
        )
    }

    private suspend fun appendArtifact(
        stationId: String,
        alertId: String,
        artifact: StoredAlertArtifact,
        source: WeatherRadioEvent,
    ) {
        try {
            val existing = repository.findByAlertId(alertId)
            val updated = if (existing != null) {
                existing.copy(
                    updatedAt = clock.instant(),
                    artifacts = existing.artifacts + artifact,
                )
            } else {
                StoredAlertRecord(
                    alertId = alertId,
                    stationId = stationId,
                    state = "UNKNOWN",
                    updatedAt = clock.instant(),
                    artifacts = listOf(artifact),
                )
            }

            repository.upsert(updated)

            weatherRadioEventBus.publish(
                AlertArtifactStoredEvent(
                    stationId = stationId,
                    alertId = alertId,
                    artifactType = artifact.artifactType,
                    correlationId = source.correlationId,
                    causationId = source.eventId,
                )
            )
        } catch (ex: Exception) {
            storeFailed(
                stationId,
                alertId,
                "Failed to store artifact ${artifact.artifactType}: ${ex.message}",
                source,
            )
        }
    }

    private suspend fun storeFailed(
        stationId: String,
        alertId: String?,
        reason: String,
        source: WeatherRadioEvent,
    ) {
        if (datafill.debugLogging) {
            log.warn(
                "Alert store failure stationId={} alertId={} reason={}",
                stationId,
                alertId,
                reason,
            )
        }

        weatherRadioEventBus.publish(
            AlertStoreFailedEvent(
                stationId = stationId,
                alertId = alertId,
                reason = reason,
                correlationId = source.correlationId,
                causationId = source.eventId,
            )
        )
    }
}