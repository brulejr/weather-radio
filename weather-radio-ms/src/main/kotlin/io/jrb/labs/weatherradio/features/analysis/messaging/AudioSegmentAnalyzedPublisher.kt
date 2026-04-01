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

package io.jrb.labs.weatherradio.features.analysis.messaging

import io.jrb.labs.weatherradio.events.PipelineEvent
import io.jrb.labs.weatherradio.events.PipelineEventBus
import io.jrb.labs.weatherradio.features.analysis.model.AudioSegmentAnalysis

class AudioSegmentAnalyzedPublisher(
    private val eventBus: PipelineEventBus
) {
    suspend fun publish(
        stationId: String,
        segmentId: String,
        audioPath: String,
        analysis: AudioSegmentAnalysis
    ) {
        eventBus.publish(
            PipelineEvent.AudioSegmentAnalyzed(
                stationId = stationId,
                segmentId = segmentId,
                audioPath = audioPath,
                contentHint = analysis.contentHint,
                rmsLevel = analysis.rmsLevel,
                peakLevel = analysis.peakLevel,
                zeroCrossingRate = analysis.zeroCrossingRate,
                rmsVariance = analysis.rmsVariance,
                suspiciousWindowCount = analysis.suspiciousWindowCount,
                totalWindowCount = analysis.totalWindowCount,
                suspiciousOffsetsMs = analysis.suspiciousOffsetsMs
            )
        )
    }
}