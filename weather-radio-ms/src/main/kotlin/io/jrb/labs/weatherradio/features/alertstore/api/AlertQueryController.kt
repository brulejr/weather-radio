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

package io.jrb.labs.weatherradio.features.alertstore.api

import io.jrb.labs.weatherradio.features.FeatureDescriptors.CONFIG_PREFIX_ALERT_STORE
import io.jrb.labs.weatherradio.features.alertstore.model.StoredAlertRecord
import io.jrb.labs.weatherradio.features.alertstore.port.AlertStoreRepository
import io.jrb.labs.weatherradio.features.alertstore.service.AlertArtifactLookupService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/alerts")
@ConditionalOnProperty(prefix = CONFIG_PREFIX_ALERT_STORE, name = ["enabled"], havingValue = "true", matchIfMissing = true)
class AlertQueryController(
    private val repository: AlertStoreRepository,
    private val artifactLookupService: AlertArtifactLookupService
) {

    @GetMapping
    fun listAlerts(
        @RequestParam(name = "limit", defaultValue = "50") limit: Int,
        @RequestParam(name = "state", required = false) state: String?,
    ): List<AlertSummaryResponse> {
        val normalizedState = state
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.uppercase()

        return repository.findRecent(limit.coerceAtLeast(1))
            .asSequence()
            .filter { record ->
                normalizedState == null || record.state.uppercase() == normalizedState
            }
            .map { it.toSummaryResponse() }
            .toList()
    }

    @GetMapping("/{alertId}")
    fun getAlert(
        @PathVariable alertId: String,
    ): ResponseEntity<AlertDetailResponse> {
        val record = repository.findByAlertId(alertId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(record.toDetailResponse())
    }

    private fun StoredAlertRecord.toSummaryResponse(): AlertSummaryResponse =
        AlertSummaryResponse(
            alertId = alertId,
            stationId = stationId,
            state = state,
            eventCode = header?.eventCode,
            senderId = header?.senderId,
            countyCodes = header?.countyCodes.orEmpty(),
            locallyRelevant = locallyRelevant,
            openedAt = openedAt?.toString(),
            updatedAt = updatedAt.toString(),
        )

    private fun StoredAlertRecord.toDetailResponse(): AlertDetailResponse {
        val artifactResponses = artifactLookupService.listArtifacts(alertId).orEmpty()

        val audioCaptured = artifacts.lastOrNull { it.artifactType == "audio-captured" }
        val audioSkipped = artifacts.lastOrNull { it.artifactType == "audio-capture-skipped" }
        val audioFailed = artifacts.lastOrNull { it.artifactType == "audio-capture-failed" }
        val audioPoorQuality = artifacts.lastOrNull { it.artifactType == "audio-capture-poor-quality" }
        val audioFile = artifacts.lastOrNull { it.artifactType == "audio-file" }

        val transcriptionStarted = artifacts.lastOrNull { it.artifactType == "transcription-started" }
        val transcriptionSkipped = artifacts.lastOrNull { it.artifactType == "transcription-skipped" }
        val transcriptionFailed = artifacts.lastOrNull { it.artifactType == "transcription-failed" }
        val transcriptionLowConfidence = artifacts.lastOrNull { it.artifactType == "transcription-low-confidence" }
        val transcriptionFallbackSelected = artifacts.lastOrNull { it.artifactType == "transcription-fallback-selected" }
        val transcriptCreated = artifacts.lastOrNull { it.artifactType == "transcript-created" }
        val transcriptFile = artifacts.lastOrNull { it.artifactType == "transcript-file" }

        return AlertDetailResponse(
            alertId = alertId,
            stationId = stationId,
            state = state,
            eventCode = header?.eventCode,
            senderId = header?.senderId,
            countyCodes = header?.countyCodes.orEmpty(),
            locallyRelevant = locallyRelevant ?: false,
            openedAt = openedAt?.toString(),
            updatedAt = updatedAt.toString(),
            audio = AlertAudioSummary(
                captured = audioCaptured != null,
                skipped = audioSkipped != null,
                failed = audioFailed != null,
                poorQuality = audioPoorQuality != null,
                fileCreated = audioFile != null,
                qualityClassification = audioFile?.details?.get("qualityClassification") as? String
                    ?: audioCaptured?.details?.get("qualityClassification") as? String
                    ?: audioPoorQuality?.details?.get("classification") as? String,
                acceptableForTranscription = audioFile?.details?.get("acceptableForTranscription") as? Boolean
                    ?: audioCaptured?.details?.get("acceptableForTranscription") as? Boolean,
                durationMillis = (audioFile?.details?.get("durationMillis") as? Number)?.toLong()
                    ?: (audioCaptured?.details?.get("durationMillis") as? Number)?.toLong(),
                byteLength = (audioFile?.details?.get("byteLength") as? Number)?.toLong(),
                filePath = audioFile?.details?.get("filePath") as? String,
            ),
            transcription = AlertTranscriptionSummary(
                started = transcriptionStarted != null,
                skipped = transcriptionSkipped != null,
                failed = transcriptionFailed != null,
                lowConfidence = transcriptionLowConfidence != null,
                fallbackSelected = transcriptionFallbackSelected != null,
                transcriptCreated = transcriptCreated != null,
                transcriptFileCreated = transcriptFile != null,
                engineName = transcriptCreated?.details?.get("engineName") as? String,
                confidence = (transcriptCreated?.details?.get("confidence") as? Number)?.toDouble(),
                confidenceAccepted = transcriptCreated?.details?.get("confidenceAccepted") as? Boolean,
                sourceAudioQualityClassification = transcriptCreated?.details?.get("sourceAudioQualityClassification") as? String,
                textFilePath = transcriptFile?.details?.get("textFilePath") as? String,
                jsonFilePath = transcriptFile?.details?.get("jsonFilePath") as? String,
            ),
            artifacts = artifactResponses,
        )
    }

}