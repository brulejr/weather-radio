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

package io.jrb.labs.weatherradio.features.alertstore.api

data class AlertDetailResponse(
    val alertId: String,
    val stationId: String,
    val state: String,
    val eventCode: String? = null,
    val senderId: String? = null,
    val countyCodes: List<String> = emptyList(),
    val locallyRelevant: Boolean = false,
    val openedAt: String? = null,
    val updatedAt: String,
    val audio: AlertAudioSummary? = null,
    val transcription: AlertTranscriptionSummary? = null,
    val artifacts: List<AlertArtifactResponse> = emptyList(),
)

data class AlertAudioSummary(
    val captured: Boolean,
    val skipped: Boolean,
    val failed: Boolean,
    val poorQuality: Boolean,
    val fileCreated: Boolean,
    val qualityClassification: String? = null,
    val acceptableForTranscription: Boolean? = null,
    val durationMillis: Long? = null,
    val byteLength: Long? = null,
    val filePath: String? = null,
)

data class AlertTranscriptionSummary(
    val started: Boolean,
    val skipped: Boolean,
    val failed: Boolean,
    val lowConfidence: Boolean,
    val fallbackSelected: Boolean,
    val transcriptCreated: Boolean,
    val transcriptFileCreated: Boolean,
    val engineName: String? = null,
    val confidence: Double? = null,
    val confidenceAccepted: Boolean? = null,
    val sourceAudioQualityClassification: String? = null,
    val textFilePath: String? = null,
    val jsonFilePath: String? = null,
)