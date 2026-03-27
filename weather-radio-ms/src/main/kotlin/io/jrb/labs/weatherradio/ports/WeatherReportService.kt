package io.jrb.labs.weatherradio.ports

import io.jrb.labs.weatherradio.domain.WeatherReport

interface WeatherReportService {
    fun currentReport(): WeatherReport
}
