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

import io.jrb.labs.weatherradio.events.AlertAudioFileCreatedEvent
import io.jrb.labs.weatherradio.features.transcription.TranscriptionDatafill
import io.jrb.labs.weatherradio.features.transcription.model.AlertTranscriptRecord
import io.jrb.labs.weatherradio.features.transcription.port.AudioFileTranscriber
import java.time.Clock
import java.nio.file.Files
import java.nio.file.Path

class SyntheticAudioFileTranscriber(
    private val datafill: TranscriptionDatafill,
    private val clock: Clock,
    private val engineName: String = "synthetic-audio-file-transcriber"
) : AudioFileTranscriber {

    override fun transcribe(event: AlertAudioFileCreatedEvent): AlertTranscriptRecord {
        val path = Path.of(event.artifact.filePath)
        val fileSize = Files.size(path)

        val text = buildString {
            append("Synthetic file-based bulletin transcript. ")
            append("Station ")
            append(event.stationId)
            append(". ")
            append("Format ")
            append(event.artifact.format)
            append(". ")
            append("Sample rate ")
            append(event.artifact.sampleRateHz)
            append(" Hz. ")
            append("File size ")
            append(fileSize)
            append(" bytes.")
        }

        val details = if (datafill.includeDebugTranscriptDetails) {
            mapOf(
                "filePath" to event.artifact.filePath,
                "fileSize" to fileSize,
                "frameCount" to event.artifact.frameCount,
                "channelCount" to event.artifact.channelCount,
            )
        } else {
            emptyMap()
        }

        return AlertTranscriptRecord(
            alertId = event.alertId,
            stationId = event.stationId,
            transcriptText = text,
            confidence = 0.90,
            createdAt = clock.instant(),
            engineName = engineName,
            details = details,
        )
    }
}