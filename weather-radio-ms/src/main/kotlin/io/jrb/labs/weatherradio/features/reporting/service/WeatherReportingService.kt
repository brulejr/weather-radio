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
import io.jrb.labs.weatherradio.features.reporting.ReportingDatafill
import io.jrb.labs.weatherradio.features.reporting.cache.ReportingCache
import io.jrb.labs.weatherradio.features.reporting.cache.ReportingFreshness
import java.time.Clock
import java.time.Instant

class WeatherReportingService(
    private val datafill: ReportingDatafill,
    private val reportingCache: ReportingCache,
    private val clock: Clock
) {

    fun currentReport(): WeatherReport {
        val now = Instant.now(clock)
        val cached = reportingCache.currentWeatherReport()

        return when {
            cached != null && ReportingFreshness.isFresh(cached, now, datafill.weatherReportTtl) ->
                cached.value.copy(generatedAt = now)

            else ->
                WeatherReport(
                    regionName = "Unknown Region",
                    generatedAt = now,
                    radioStatus = null,
                    authoritativeAlerts = emptyList(),
                    transcribedForecast = null,
                    latestTranscript = null,
                    station = null
                )
        }
    }

}
