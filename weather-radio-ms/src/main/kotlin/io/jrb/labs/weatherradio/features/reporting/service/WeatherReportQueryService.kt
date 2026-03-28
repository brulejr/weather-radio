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

package io.jrb.labs.weatherradio.features.reporting.service

import io.jrb.labs.weatherradio.domain.WeatherReport
import io.jrb.labs.weatherradio.features.fusion.service.WeatherFusionService
import io.jrb.labs.weatherradio.features.radio.service.RadioService
import io.jrb.labs.weatherradio.features.same.service.SameService
import io.jrb.labs.weatherradio.features.transcription.service.TranscriptionService
import java.time.Clock
import java.time.Instant

class WeatherReportQueryService(
    private val radioService: RadioService,
    private val sameService: SameService,
    private val transcriptionService: TranscriptionService,
    private val fusionService: WeatherFusionService,
    private val clock: Clock
) : WeatherReportService {

    override fun currentReport(): WeatherReport {
        val radioStatus = radioService.radioStatus()
        val sameMessage = sameService.latestSameMessage()
        val latestTranscript = transcriptionService.latestTranscript()

        val alerts = buildList {
            if (sameMessage != null) {
                add(fusionService.toAlert(sameMessage))
            }
        }

        return WeatherReport(
            regionName = radioStatus?.station?.regionName ?: "Unknown Region",
            generatedAt = Instant.now(clock),
            radioStatus = radioStatus,
            authoritativeAlerts = alerts,
            transcribedForecast = latestTranscript?.text,
            latestTranscript = latestTranscript,
            station = radioStatus?.station
        )
    }
}