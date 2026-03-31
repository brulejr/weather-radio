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

package io.jrb.labs.weatherradio.features.ingestion.service

import io.jrb.labs.weatherradio.features.ingestion.IngestionDatafill
import io.jrb.labs.weatherradio.features.ingestion.model.AudioSegment
import java.io.InputStream
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.util.UUID

class AudioSegmentationService(
    private val datafill: IngestionDatafill,
    private val clock: Clock
) {
    suspend fun segmentStream(
        stationId: String,
        input: InputStream,
        onSegment: suspend (AudioSegment) -> Unit
    ) {
        Files.createDirectories(datafill.outputDir)

        val bytesPerSecond = datafill.sampleRateHz * 2
        val targetBytes = bytesPerSecond * datafill.segmentDuration.seconds.toInt()
        val buffer = ByteArray(targetBytes)

        while (true) {
            val startedAt = Instant.now(clock)
            var offset = 0

            while (offset < buffer.size) {
                val read = input.read(buffer, offset, buffer.size - offset)
                if (read < 0) return
                offset += read
            }

            val endedAt = Instant.now(clock)
            val segmentId = UUID.randomUUID().toString()
            val path = datafill.outputDir.resolve("$segmentId.pcm")

            Files.write(path, buffer)

            onSegment(
                AudioSegment(
                    stationId = stationId,
                    segmentId = segmentId,
                    audioFile = path,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    sampleRateHz = datafill.sampleRateHz,
                    channelCount = 1
                )
            )
        }
    }
}
