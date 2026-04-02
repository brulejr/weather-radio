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

import io.jrb.labs.weatherradio.events.AlertAudioCapturedEvent
import io.jrb.labs.weatherradio.features.transcription.TranscriptionDatafill
import io.jrb.labs.weatherradio.features.transcription.model.AlertTranscriptRecord
import io.jrb.labs.weatherradio.features.transcription.port.AudioTranscriber
import java.time.Clock

class SyntheticAudioTranscriber(
    private val datafill: TranscriptionDatafill,
    private val clock: Clock,
) : AudioTranscriber {

    override fun transcribe(event: AlertAudioCapturedEvent): AlertTranscriptRecord {
        val baseText = buildString {
            append("Synthetic bulletin transcript. ")
            append("Station ")
            append(event.stationId)
            append(". ")
            append("Captured ")
            append(event.capture.frameCount)
            append(" frames at ")
            append(event.capture.sampleRateHz)
            append(" Hz.")
        }

        val details = if (datafill.includeDebugTranscriptDetails) {
            mapOf(
                "channelCount" to event.capture.channelCount,
                "startedAt" to event.capture.startedAt.toString(),
                "completedAt" to event.capture.completedAt.toString(),
            )
        } else {
            emptyMap()
        }

        return AlertTranscriptRecord(
            alertId = event.alertId,
            stationId = event.stationId,
            transcriptText = baseText,
            confidence = 0.90,
            createdAt = clock.instant(),
            engineName = "synthetic-audio-transcriber",
            details = details,
        )
    }
}