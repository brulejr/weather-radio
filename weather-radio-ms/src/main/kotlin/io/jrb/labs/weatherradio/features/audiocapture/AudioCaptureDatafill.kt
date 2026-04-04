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

package io.jrb.labs.weatherradio.features.audiocapture

import io.jrb.labs.weatherradio.features.FeatureDescriptors.CONFIG_PREFIX_AUDIO_CAPTURE
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = CONFIG_PREFIX_AUDIO_CAPTURE)
data class AudioCaptureDatafill(

    val enabled: Boolean = true,

    @field:Min(0)
    val preRollMs: Long = 5000,

    @field:Min(1000)
    val postRollMs: Long = 15000,

    @field:Min(1)
    val maxConcurrentCapturesPerStation: Int = 1,

    val writeWavFiles: Boolean = true,

    @field:NotBlank
    val artifactDirectory: String = "./data/artifacts",

    val debugLogging: Boolean = false,

    val minimumCapturedFrames: Int = 1,

    val emitSkippedCaptureEvents: Boolean = true,

    val audioFileWriteAttempts: Int = 2,

    val minimumRmsAmplitude: Double = 0.01,

    val maximumSilenceFraction: Double = 0.95,

    val maximumClippedFraction: Double = 0.05,

    val silenceAmplitudeThreshold: Double = 0.002,

    val clipAmplitudeThreshold: Double = 0.98,

    val emitPoorQualityCaptureEvents: Boolean = true,

    val allowPoorQualityTranscription: Boolean = true,

)