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

package io.jrb.labs.weatherradio.features.audiocapture.support

import io.jrb.labs.weatherradio.domain.radio.AudioFrame
import io.jrb.labs.weatherradio.features.audiocapture.AudioCaptureDatafill
import io.jrb.labs.weatherradio.features.audiocapture.model.AlertAudioCaptureRecord
import io.jrb.labs.weatherradio.features.audiocapture.model.AudioQualityMetrics
import io.jrb.labs.weatherradio.features.audiocapture.port.AudioQualityAssessment
import io.jrb.labs.weatherradio.features.audiocapture.port.AudioQualityAssessor
import kotlin.math.abs
import kotlin.math.sqrt

class DefaultAudioQualityAssessor(
    private val datafill: AudioCaptureDatafill,
) : AudioQualityAssessor {

    override fun assess(capture: AlertAudioCaptureRecord): AudioQualityAssessment {
        val samples = capture.frames.flatMap { it.samplesAsDoubles() }
        if (samples.isEmpty()) {
            val metrics = AudioQualityMetrics(
                peakAmplitude = 0.0,
                rmsAmplitude = 0.0,
                silenceFraction = 1.0,
                clippedFraction = 0.0,
            )
            return AudioQualityAssessment(
                metrics = metrics,
                classification = "mostly-silent",
                acceptableForTranscription = false,
                reasons = listOf("No audio samples available"),
            )
        }

        val peak = samples.maxOf { abs(it) }
        val rms = sqrt(samples.map { it * it }.average())
        val silenceFraction = samples.count { abs(it) < datafill.silenceAmplitudeThreshold }.toDouble() / samples.size
        val clippedFraction = samples.count { abs(it) >= datafill.clipAmplitudeThreshold }.toDouble() / samples.size

        val reasons = mutableListOf<String>()
        var classification = "good"
        var acceptable = true

        if (rms < datafill.minimumRmsAmplitude) {
            classification = "weak"
            acceptable = false
            reasons += "RMS amplitude below threshold"
        }
        if (silenceFraction > datafill.maximumSilenceFraction) {
            classification = "mostly-silent"
            acceptable = false
            reasons += "Silence fraction above threshold"
        }
        if (clippedFraction > datafill.maximumClippedFraction) {
            classification = "clipped"
            acceptable = false
            reasons += "Clipped fraction above threshold"
        }

        return AudioQualityAssessment(
            metrics = AudioQualityMetrics(
                peakAmplitude = peak,
                rmsAmplitude = rms,
                silenceFraction = silenceFraction,
                clippedFraction = clippedFraction,
            ),
            classification = classification,
            acceptableForTranscription = acceptable,
            reasons = reasons,
        )
    }

    private fun AudioFrame.samplesAsDoubles(): List<Double> =
        samples.map { it.toDouble() / 32768.0 }
}