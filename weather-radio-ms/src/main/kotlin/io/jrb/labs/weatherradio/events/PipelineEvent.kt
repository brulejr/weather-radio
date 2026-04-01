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

package io.jrb.labs.weatherradio.events

import io.jrb.labs.commons.eventbus.Event
import io.jrb.labs.weatherradio.domain.RadioSignalStatus
import io.jrb.labs.weatherradio.domain.SameMessage
import io.jrb.labs.weatherradio.domain.TranscriptSegment
import io.jrb.labs.weatherradio.domain.WeatherReport
import io.jrb.labs.weatherradio.features.ingestion.model.AudioSegment

sealed class PipelineEvent : Event {

    data class AudioSegmentDetected(
        val stationId: String,
        val segmentId: String,
        val audioPath: String
    ) : PipelineEvent()

    data class AudioSegmentAnalyzed(
        val stationId: String,
        val segmentId: String,
        val audioPath: String,
        val contentHint: AudioSegment.ContentHint,
        val rmsLevel: Double,
        val peakLevel: Int,
        val zeroCrossingRate: Double
    ) : PipelineEvent()

    data class SameMessageDecoded(
        val stationId: String,
        val same: SameMessage
    ) : PipelineEvent()

    data class TranscriptProduced(
        val stationId: String,
        val transcript: TranscriptSegment
    ) : PipelineEvent()

    data class RadioStatusUpdated(
        val status: RadioSignalStatus
    ) : PipelineEvent()

    data class WeatherReportUpdated(
        val report: WeatherReport
    ) : PipelineEvent()

}