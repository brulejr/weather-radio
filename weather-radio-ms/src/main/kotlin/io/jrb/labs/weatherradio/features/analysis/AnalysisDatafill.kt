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

package io.jrb.labs.weatherradio.features.analysis

import io.jrb.labs.weatherradio.features.FeatureDescriptors.CONFIG_PREFIX_ANALYSIS
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = CONFIG_PREFIX_ANALYSIS)
data class AnalysisDatafill(
    val enabled: Boolean = true,
    val lowRmsThreshold: Double = 600.0,
    val voiceRmsThreshold: Double = 1800.0,
    val toneZeroCrossingThreshold: Double = 0.20,
    val sameZeroCrossingThreshold: Double = 0.14,
    val voiceRmsVarianceThreshold: Double = 250_000.0,
    val windowSizeSamples: Int = 1024
)