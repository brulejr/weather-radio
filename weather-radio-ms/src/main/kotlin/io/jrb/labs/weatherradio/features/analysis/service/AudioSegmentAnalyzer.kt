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

package io.jrb.labs.weatherradio.features.analysis.service

import io.jrb.labs.weatherradio.features.analysis.AnalysisDatafill
import io.jrb.labs.weatherradio.features.analysis.model.AudioSegmentAnalysis
import io.jrb.labs.weatherradio.features.ingestion.model.AudioSegment
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class AudioSegmentAnalyzer(
    private val datafill: AnalysisDatafill
) {

    fun analyze(path: Path): AudioSegmentAnalysis {
        val bytes = Files.readAllBytes(path)
        if (bytes.size < 4) {
            return AudioSegmentAnalysis(
                contentHint = AudioSegment.ContentHint.UNKNOWN,
                rmsLevel = 0.0,
                peakLevel = 0,
                zeroCrossingRate = 0.0,
                rmsVariance = 0.0
            )
        }

        val samples = ShortArray(bytes.size / 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        var i = 0
        while (buffer.remaining() >= 2) {
            samples[i++] = buffer.short
        }

        val actualSamples = samples.copyOf(i)
        if (actualSamples.isEmpty()) {
            return AudioSegmentAnalysis(
                contentHint = AudioSegment.ContentHint.UNKNOWN,
                rmsLevel = 0.0,
                peakLevel = 0,
                zeroCrossingRate = 0.0,
                rmsVariance = 0.0
            )
        }

        var sumSquares = 0.0
        var peak = 0
        var zeroCrossings = 0

        for (idx in actualSamples.indices) {
            val sample = actualSamples[idx].toInt()
            val magnitude = abs(sample)

            if (magnitude > peak) {
                peak = magnitude
            }

            sumSquares += sample.toDouble() * sample.toDouble()

            if (idx > 0) {
                val prev = actualSamples[idx - 1].toInt()
                if ((prev < 0 && sample >= 0) || (prev >= 0 && sample < 0)) {
                    zeroCrossings++
                }
            }
        }

        val rms = sqrt(sumSquares / actualSamples.size)
        val zcr = zeroCrossings.toDouble() / actualSamples.size.toDouble()
        val rmsVariance = computeWindowedRmsVariance(actualSamples, datafill.windowSizeSamples)

        val hint = classify(rms, peak, zcr, rmsVariance)

        return AudioSegmentAnalysis(
            contentHint = hint,
            rmsLevel = (rms * 100.0).roundToInt() / 100.0,
            peakLevel = peak,
            zeroCrossingRate = (zcr * 10000.0).roundToInt() / 10000.0,
            rmsVariance = (rmsVariance * 100.0).roundToInt() / 100.0
        )
    }

    private fun computeWindowedRmsVariance(
        samples: ShortArray,
        windowSize: Int
    ): Double {
        if (samples.isEmpty() || windowSize <= 0) {
            return 0.0
        }

        val windowRmsValues = mutableListOf<Double>()
        var start = 0

        while (start < samples.size) {
            val end = minOf(start + windowSize, samples.size)
            val window = samples.sliceArray(start until end)

            var sumSquares = 0.0
            for (sample in window) {
                val value = sample.toDouble()
                sumSquares += value * value
            }

            val rms = sqrt(sumSquares / window.size)
            windowRmsValues += rms
            start = end
        }

        if (windowRmsValues.size < 2) {
            return 0.0
        }

        val mean = windowRmsValues.average()
        val variance = windowRmsValues
            .map { value -> (value - mean) * (value - mean) }
            .average()

        return variance
    }

    private fun classify(
        rms: Double,
        peak: Int,
        zcr: Double,
        rmsVariance: Double
    ): AudioSegment.ContentHint {
        if (rms < datafill.lowRmsThreshold || peak < 1000) {
            return AudioSegment.ContentHint.UNKNOWN
        }

        // Speech usually has a changing envelope even when riding on hiss/noise.
        if (rms >= datafill.voiceRmsThreshold &&
            rmsVariance >= datafill.voiceRmsVarianceThreshold
        ) {
            return AudioSegment.ContentHint.VOICE
        }

        // Tone bursts are usually more stable in energy.
        if (zcr >= datafill.toneZeroCrossingThreshold &&
            rms >= datafill.voiceRmsThreshold &&
            rmsVariance < datafill.voiceRmsVarianceThreshold
        ) {
            return AudioSegment.ContentHint.TONE_BURST
        }

        if (zcr in datafill.sameZeroCrossingThreshold..datafill.toneZeroCrossingThreshold &&
            rms >= datafill.voiceRmsThreshold
        ) {
            return AudioSegment.ContentHint.POSSIBLE_SAME
        }

        if (rms >= datafill.voiceRmsThreshold) {
            return AudioSegment.ContentHint.VOICE
        }

        return AudioSegment.ContentHint.UNKNOWN
    }

}