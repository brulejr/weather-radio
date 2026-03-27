package io.jrb.labs.weatherradio.domain

import java.time.Instant

data class WeatherReport(
    val regionName: String,
    val generatedAt: Instant,
    val radioStatus: RadioSignalStatus?,
    val authoritativeAlerts: List<WeatherAlert>,
    val transcribedForecast: String?,
    val latestTranscript: TranscriptSegment?,
    val station: WeatherStation?
)
