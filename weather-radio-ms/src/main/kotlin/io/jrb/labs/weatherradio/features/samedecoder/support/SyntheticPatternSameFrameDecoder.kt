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

package io.jrb.labs.weatherradio.features.samedecoder.support

import io.jrb.labs.weatherradio.features.samedecoder.model.SameDecodeAttempt
import io.jrb.labs.weatherradio.features.samedecoder.model.SameDecodeResult
import io.jrb.labs.weatherradio.features.samedecoder.model.SameHeader

class SyntheticPatternSameFrameDecoder : SameFrameDecoder {

    override fun decode(attempt: SameDecodeAttempt): SameDecodeResult {
        val samples = attempt.frames.flatMap { it.samples.asList() }
        if (samples.isEmpty()) {
            return SameDecodeResult.NoSignal
        }

        val positiveRuns = countRuns(samples) { it > 0.85f }
        val negativeRuns = countRuns(samples) { it < -0.85f }

        return if (positiveRuns >= 3 && negativeRuns >= 3) {
            SameDecodeResult.Decoded(
                header = SameHeader(
                    rawHeader = "ZCZC-WXR-RWT-050007+0030-0010100-KXXX/NWS-",
                    originator = "WXR",
                    eventCode = "RWT",
                    countyCodes = listOf("05007"),
                    purgeTime = "0030",
                    senderId = "KXXX/NWS",
                    issuedAt = null,
                ),
                confidence = 0.95,
                burstCount = 3,
            )
        } else {
            SameDecodeResult.NoSignal
        }
    }

    private fun countRuns(
        samples: List<Float>,
        predicate: (Float) -> Boolean,
    ): Int {
        var runs = 0
        var inRun = false

        for (sample in samples) {
            if (predicate(sample)) {
                if (!inRun) {
                    runs++
                    inRun = true
                }
            } else {
                inRun = false
            }
        }

        return runs
    }
}