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

package io.jrb.labs.weatherradio.features.transcription.support

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.weatherradio.features.transcription.model.AlertTranscriptFileArtifact
import io.jrb.labs.weatherradio.features.transcription.model.AlertTranscriptRecord
import io.jrb.labs.weatherradio.features.transcription.port.TranscriptArtifactWriter
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import kotlin.io.path.writeText

class TextTranscriptArtifactWriter(
    private val artifactRoot: Path,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) : TranscriptArtifactWriter {

    override fun writeTranscript(transcript: AlertTranscriptRecord): AlertTranscriptFileArtifact {
        val alertDir = artifactRoot.resolve(transcript.alertId)
        Files.createDirectories(alertDir)

        val textPath = alertDir.resolve("transcript.txt")
        val jsonPath = alertDir.resolve("transcript.json")

        textPath.writeText(transcript.transcriptText)

        val jsonPayload = mapOf(
            "alertId" to transcript.alertId,
            "stationId" to transcript.stationId,
            "transcriptText" to transcript.transcriptText,
            "confidence" to transcript.confidence,
            "createdAt" to transcript.createdAt.toString(),
            "engineName" to transcript.engineName,
            "language" to transcript.details["language"],
            "rawTextLength" to transcript.details["rawTextLength"],
            "normalizedTextLength" to transcript.details["normalizedTextLength"],
            "wasNormalized" to transcript.details["wasNormalized"],
            "rawTextPreserved" to transcript.details["rawTextPreserved"],
            "rawTranscriptText" to transcript.details["rawTranscriptText"],
            "details" to transcript.details,
        )
        jsonPath.writeText(
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonPayload)
        )

        return AlertTranscriptFileArtifact(
            alertId = transcript.alertId,
            stationId = transcript.stationId,
            textFilePath = textPath.toAbsolutePath().toString(),
            jsonFilePath = jsonPath.toAbsolutePath().toString(),
            createdAt = clock.instant(),
        )
    }
}