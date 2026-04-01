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

package io.jrb.labs.weatherradio.features.same.service

import io.jrb.labs.weatherradio.events.PipelineEvent
import io.jrb.labs.weatherradio.features.same.SameDatafill
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class SameCandidateDetector(
    private val datafill: SameDatafill
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun inspect(event: PipelineEvent.AudioSegmentAnalyzed) {
        val audioPath = Path.of(event.audioPath)

        if (!Files.exists(audioPath)) {
            logger.warn(
                "SAME candidate skipped because audio file does not exist: stationId={}, segmentId={}, audioPath={}",
                event.stationId,
                event.segmentId,
                event.audioPath
            )
            return
        }

        if (event.suspiciousWindowCount < datafill.minSuspiciousWindows) {
            logger.debug(
                "SAME candidate rejected: stationId={}, segmentId={}, suspiciousWindowCount={}, minRequired={}",
                event.stationId,
                event.segmentId,
                event.suspiciousWindowCount,
                datafill.minSuspiciousWindows
            )
            return
        }

        logger.info(
            "SAME candidate accepted: stationId={}, segmentId={}, hint={}, suspicious={}/{}, offsets={}, audioPath={}",
            event.stationId,
            event.segmentId,
            event.contentHint,
            event.suspiciousWindowCount,
            event.totalWindowCount,
            event.suspiciousOffsetsMs,
            event.audioPath
        )

        if (datafill.saveCandidateDebugSnippets) {
            saveDebugSnippets(audioPath, event)
        }
    }

    private fun saveDebugSnippets(
        audioPath: Path,
        event: PipelineEvent.AudioSegmentAnalyzed
    ) {
        Files.createDirectories(datafill.candidateOutputDir)

        val bytes = Files.readAllBytes(audioPath)
        val bytesPerSample = 2
        val sampleRate = 22050
        val samplesPerMs = sampleRate / 1000.0
        val snippetHalfWindowMs = datafill.candidateWindowMs / 2

        event.suspiciousOffsetsMs.forEachIndexed { index, offsetMs ->
            val startSample = ((offsetMs - snippetHalfWindowMs).coerceAtLeast(0) * samplesPerMs).toInt()
            val endSample = ((offsetMs + snippetHalfWindowMs) * samplesPerMs).toInt()

            val startByte = (startSample * bytesPerSample).coerceAtLeast(0)
            val endByte = (endSample * bytesPerSample).coerceAtMost(bytes.size)

            if (startByte >= endByte) return@forEachIndexed

            val snippet = bytes.copyOfRange(startByte, endByte)
            val out = datafill.candidateOutputDir.resolve("${event.segmentId}-candidate-$index.pcm")
            Files.write(out, snippet)

            logger.info(
                "Saved SAME candidate snippet: stationId={}, segmentId={}, offsetMs={}, snippetPath={}",
                event.stationId,
                event.segmentId,
                offsetMs,
                out
            )
        }
    }
}