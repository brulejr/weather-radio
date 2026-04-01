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

    private data class WindowMetrics(
        val offsetMs: Long,
        val rms: Double,
        val peak: Int,
        val zcr: Double
    )

    fun analyze(path: Path): AudioSegmentAnalysis {
        val bytes = Files.readAllBytes(path)
        if (bytes.size < 4) {
            return AudioSegmentAnalysis(
                contentHint = AudioSegment.ContentHint.UNKNOWN,
                rmsLevel = 0.0,
                peakLevel = 0,
                zeroCrossingRate = 0.0,
                rmsVariance = 0.0,
                suspiciousWindowCount = 0,
                totalWindowCount = 0,
                suspiciousOffsetsMs = emptyList()
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
                rmsVariance = 0.0,
                suspiciousWindowCount = 0,
                totalWindowCount = 0,
                suspiciousOffsetsMs = emptyList()
            )
        }

        val rms = computeRms(actualSamples)
        val peak = computePeak(actualSamples)
        val zcr = computeZeroCrossingRate(actualSamples)
        val rmsVariance = computeWindowedRmsVariance(actualSamples, datafill.windowSizeSamples)

        val windows = analyzeWindows(actualSamples)
        val suspiciousOffsets = findSuspiciousOffsets(windows)

        val hint = classify(
            rms = rms,
            peak = peak,
            zcr = zcr,
            rmsVariance = rmsVariance,
            suspiciousWindowCount = suspiciousOffsets.size,
            totalWindowCount = windows.size
        )

        return AudioSegmentAnalysis(
            contentHint = hint,
            rmsLevel = (rms * 100.0).roundToInt() / 100.0,
            peakLevel = peak,
            zeroCrossingRate = (zcr * 10000.0).roundToInt() / 10000.0,
            rmsVariance = (rmsVariance * 100.0).roundToInt() / 100.0,
            suspiciousWindowCount = suspiciousOffsets.size,
            totalWindowCount = windows.size,
            suspiciousOffsetsMs = suspiciousOffsets
        )
    }

    private fun analyzeWindows(samples: ShortArray): List<WindowMetrics> {
        val samplesPerWindow = maxOf(1, (22050 * datafill.analysisWindowMs) / 1000)

        val windows = mutableListOf<WindowMetrics>()
        var start = 0
        var offsetMs = 0L

        while (start < samples.size) {
            val end = minOf(start + samplesPerWindow, samples.size)
            val window = samples.sliceArray(start until end)

            windows += WindowMetrics(
                offsetMs = offsetMs,
                rms = computeRms(window),
                peak = computePeak(window),
                zcr = computeZeroCrossingRate(window)
            )

            start = end
            offsetMs += datafill.analysisWindowMs.toLong()
        }

        return windows
    }

    private fun findSuspiciousOffsets(windows: List<WindowMetrics>): List<Long> {
        return windows
            .filter { window ->
                window.rms >= datafill.voiceRmsThreshold &&
                        window.zcr >= datafill.suspiciousWindowZcrThreshold
            }
            .map { it.offsetMs }
    }

    private fun classify(
        rms: Double,
        peak: Int,
        zcr: Double,
        rmsVariance: Double,
        suspiciousWindowCount: Int,
        totalWindowCount: Int
    ): AudioSegment.ContentHint {
        if (rms < datafill.lowRmsThreshold || peak < 1000) {
            return AudioSegment.ContentHint.UNKNOWN
        }

        if (suspiciousWindowCount >= datafill.suspiciousClusterMinCount) {
            return AudioSegment.ContentHint.POSSIBLE_SAME
        }

        if (rms >= datafill.voiceRmsThreshold &&
            rmsVariance >= datafill.voiceRmsVarianceThreshold
        ) {
            return AudioSegment.ContentHint.VOICE
        }

        if (totalWindowCount > 0 &&
            suspiciousWindowCount.toDouble() / totalWindowCount.toDouble() > 0.6 &&
            zcr >= datafill.toneZeroCrossingThreshold
        ) {
            return AudioSegment.ContentHint.TONE_BURST
        }

        if (rms >= datafill.voiceRmsThreshold) {
            return AudioSegment.ContentHint.VOICE
        }

        return AudioSegment.ContentHint.UNKNOWN
    }

    private fun computeRms(samples: ShortArray): Double {
        var sumSquares = 0.0
        for (sample in samples) {
            val value = sample.toDouble()
            sumSquares += value * value
        }
        return sqrt(sumSquares / samples.size)
    }

    private fun computePeak(samples: ShortArray): Int {
        var peak = 0
        for (sample in samples) {
            val magnitude = abs(sample.toInt())
            if (magnitude > peak) {
                peak = magnitude
            }
        }
        return peak
    }

    private fun computeZeroCrossingRate(samples: ShortArray): Double {
        if (samples.size < 2) {
            return 0.0
        }

        var zeroCrossings = 0
        for (idx in 1 until samples.size) {
            val prev = samples[idx - 1].toInt()
            val sample = samples[idx].toInt()
            if ((prev < 0 && sample >= 0) || (prev >= 0 && sample < 0)) {
                zeroCrossings++
            }
        }

        return zeroCrossings.toDouble() / samples.size.toDouble()
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
            windowRmsValues += computeRms(window)
            start = end
        }

        if (windowRmsValues.size < 2) {
            return 0.0
        }

        val mean = windowRmsValues.average()
        return windowRmsValues
            .map { value -> (value - mean) * (value - mean) }
            .average()
    }
}