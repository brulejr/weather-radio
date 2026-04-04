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

package io.jrb.labs.weatherradio.features.audiocapture.service

import io.jrb.labs.commons.eventbus.EventBus.Subscription
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.weatherradio.domain.radio.AudioFrame
import io.jrb.labs.weatherradio.events.AlertAudioCaptureFailedEvent
import io.jrb.labs.weatherradio.events.AlertAudioCaptureSkippedEvent
import io.jrb.labs.weatherradio.events.AlertAudioCaptureStartedEvent
import io.jrb.labs.weatherradio.events.AlertAudioCapturedEvent
import io.jrb.labs.weatherradio.events.AlertAudioFileCreatedEvent
import io.jrb.labs.weatherradio.events.AlertAudioFileCreationFailedEvent
import io.jrb.labs.weatherradio.events.AlertRecordingRequestedEvent
import io.jrb.labs.weatherradio.events.AudioFrameAvailableEvent
import io.jrb.labs.weatherradio.events.FeatureHeartbeatEvent
import io.jrb.labs.weatherradio.events.WeatherRadioEventBus
import io.jrb.labs.weatherradio.features.audiocapture.AudioCaptureDatafill
import io.jrb.labs.weatherradio.features.audiocapture.model.AlertAudioCaptureRecord
import io.jrb.labs.weatherradio.features.audiocapture.port.AudioArtifactWriter
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AudioCaptureFeature(
    systemEventBus: SystemEventBus,
    private val weatherRadioEventBus: WeatherRadioEventBus,
    private val datafill: AudioCaptureDatafill,
    private val audioArtifactWriter: AudioArtifactWriter,
    private val clock: Clock,
) : ControllableService(systemEventBus) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val subscriptions = mutableListOf<Subscription>()
    private val recentFramesByStation = ConcurrentHashMap<String, MutableList<AudioFrame>>()
    private val activeCapturesByAlertId = ConcurrentHashMap<String, MutableList<AudioFrame>>()
    private val captureMetadataByAlertId = ConcurrentHashMap<String, CaptureMetadata>()

    override fun onStart() {
        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "audio-capture",
                status = "STARTING",
                details = mapOf(
                    "preRollMs" to datafill.preRollMs,
                    "postRollMs" to datafill.postRollMs,
                    "maxConcurrentCapturesPerStation" to datafill.maxConcurrentCapturesPerStation,
                    "writeWavFiles" to datafill.writeWavFiles,
                    "artifactDirectory" to datafill.artifactDirectory,
                ),
            )
        )

        subscriptions += weatherRadioEventBus.subscribe<AudioFrameAvailableEvent> { event ->
            handleAudioFrame(event)
        }

        subscriptions += weatherRadioEventBus.subscribe<AlertRecordingRequestedEvent> { event ->
            handleRecordingRequested(event)
        }

        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "audio-capture",
                status = "RUNNING",
            )
        )
    }

    override fun onStop() {
        subscriptions.forEach { it.cancel() }
        subscriptions.clear()
        recentFramesByStation.clear()
        activeCapturesByAlertId.clear()
        captureMetadataByAlertId.clear()

        weatherRadioEventBus.send(
            FeatureHeartbeatEvent(
                featureId = "audio-capture",
                status = "STOPPED",
                details = mapOf("stoppedAt" to clock.instant().toString()),
            )
        )
    }

    private suspend fun handleAudioFrame(event: AudioFrameAvailableEvent) {
        val stationFrames = recentFramesByStation.computeIfAbsent(event.stationId) { mutableListOf() }
        stationFrames += event.frame
        trimPreRollWindow(stationFrames)

        val activeEntries = captureMetadataByAlertId.values.filter { it.stationId == event.stationId }
        activeEntries.forEach { metadata ->
            val captureFrames = activeCapturesByAlertId[metadata.alertId] ?: return@forEach
            captureFrames += event.frame

            val elapsedMs = Duration.between(metadata.startedAt, event.frame.frameEnd).toMillis()
            if (elapsedMs >= datafill.postRollMs) {
                completeCapture(metadata)
            }
        }
    }

    private suspend fun handleRecordingRequested(event: AlertRecordingRequestedEvent) {
        if (captureMetadataByAlertId.containsKey(event.alertId)) {
            if (datafill.emitSkippedCaptureEvents) {
                weatherRadioEventBus.publish(
                    AlertAudioCaptureSkippedEvent(
                        stationId = event.stationId,
                        alertId = event.alertId,
                        reason = "Capture already active for alert",
                        correlationId = event.correlationId,
                        causationId = event.eventId,
                    )
                )
            }
            return
        }

        val activeCountForStation = captureMetadataByAlertId.values.count { it.stationId == event.stationId }
        if (activeCountForStation >= datafill.maxConcurrentCapturesPerStation) {
            weatherRadioEventBus.publish(
                AlertAudioCaptureFailedEvent(
                    stationId = event.stationId,
                    alertId = event.alertId,
                    reason = "Maximum concurrent captures reached for station",
                    correlationId = event.correlationId,
                    causationId = event.eventId,
                )
            )
            return
        }

        val preRollFrames = recentFramesByStation[event.stationId]
            ?.toList()
            ?.takeLastFramesWithin(datafill.preRollMs)
            ?: emptyList()

        activeCapturesByAlertId[event.alertId] = preRollFrames.toMutableList()
        captureMetadataByAlertId[event.alertId] = CaptureMetadata(
            alertId = event.alertId,
            stationId = event.stationId,
            startedAt = clock.instant(),
            reason = event.reason,
            preRollFrameCount = preRollFrames.size,
            correlationId = event.correlationId,
            causationId = event.eventId,
        )

        weatherRadioEventBus.publish(
            AlertAudioCaptureStartedEvent(
                stationId = event.stationId,
                alertId = event.alertId,
                reason = event.reason,
                correlationId = event.correlationId,
                causationId = event.eventId,
            )
        )

        if (datafill.debugLogging) {
            log.debug(
                "Started audio capture alertId={} stationId={} preRollFrameCount={}",
                event.alertId,
                event.stationId,
                preRollFrames.size,
            )
        }
    }

    private suspend fun completeCapture(metadata: CaptureMetadata) {
        val frames = activeCapturesByAlertId.remove(metadata.alertId)?.toList().orEmpty()
        captureMetadataByAlertId.remove(metadata.alertId)

        if (frames.isEmpty()) {
            weatherRadioEventBus.publish(
                AlertAudioCaptureFailedEvent(
                    stationId = metadata.stationId,
                    alertId = metadata.alertId,
                    reason = "No audio frames were captured",
                    correlationId = metadata.correlationId,
                    causationId = metadata.causationId,
                )
            )
            return
        }

        val firstFrame = frames.first()
        val completedAt = clock.instant()

        val wasPartial = frames.size < datafill.minimumCapturedFrames
        val record = AlertAudioCaptureRecord(
            alertId = metadata.alertId,
            stationId = metadata.stationId,
            startedAt = metadata.startedAt,
            completedAt = completedAt,
            frameCount = frames.size,
            sampleRateHz = firstFrame.sampleRateHz,
            channelCount = firstFrame.channelCount,
            frames = frames,
            captureReason = metadata.reason,
            preRollFrameCount = metadata.preRollFrameCount,
            durationMillis = Duration.between(metadata.startedAt, completedAt).toMillis(),
            wasPartial = wasPartial,
        )

        weatherRadioEventBus.publish(
            AlertAudioCapturedEvent(
                stationId = metadata.stationId,
                alertId = metadata.alertId,
                capture = record,
                correlationId = metadata.correlationId,
                causationId = metadata.causationId,
            )
        )

        if (datafill.writeWavFiles) {
            var lastError: Exception? = null
            repeat(datafill.audioFileWriteAttempts) {
                try {
                    val artifact = audioArtifactWriter.writeCapture(record)
                    weatherRadioEventBus.publish(
                        AlertAudioFileCreatedEvent(
                            stationId = metadata.stationId,
                            alertId = metadata.alertId,
                            artifact = artifact,
                            correlationId = metadata.correlationId,
                            causationId = metadata.causationId,
                        )
                    )
                    lastError = null
                    return
                } catch (ex: Exception) {
                    lastError = ex
                }
            }

            weatherRadioEventBus.publish(
                AlertAudioFileCreationFailedEvent(
                    stationId = metadata.stationId,
                    alertId = metadata.alertId,
                    reason = lastError?.message ?: "Unknown audio file creation failure",
                    correlationId = metadata.correlationId,
                    causationId = metadata.causationId,
                )
            )
        }

        if (datafill.debugLogging) {
            log.debug(
                "Completed audio capture alertId={} stationId={} frameCount={}",
                metadata.alertId,
                metadata.stationId,
                frames.size,
            )
        }
    }

    private fun trimPreRollWindow(frames: MutableList<AudioFrame>) {
        if (frames.isEmpty()) return
        val newestEnd = frames.last().frameEnd
        while (frames.isNotEmpty()) {
            val oldest = frames.first()
            val ageMs = Duration.between(oldest.frameStart, newestEnd).toMillis()
            if (ageMs <= datafill.preRollMs * 2) break
            frames.removeFirst()
        }
    }

    private fun List<AudioFrame>.takeLastFramesWithin(windowMs: Long): List<AudioFrame> {
        if (isEmpty()) return emptyList()
        val newestEnd = last().frameEnd
        return asReversed()
            .takeWhile { Duration.between(it.frameStart, newestEnd).toMillis() <= windowMs }
            .asReversed()
    }

    private data class CaptureMetadata(
        val alertId: String,
        val stationId: String,
        val startedAt: Instant,
        val reason: String,
        val preRollFrameCount: Int,
        val correlationId: UUID?,
        val causationId: UUID?,
    )
}