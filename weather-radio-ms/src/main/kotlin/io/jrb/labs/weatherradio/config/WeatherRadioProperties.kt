package io.jrb.labs.weatherradio.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "weather-radio")
data class WeatherRadioProperties(
    val stationName: String = "NOAA Weather Radio",
    val stationCallSign: String = "KIG60",
    val regionName: String = "South Burlington, VT",
    val frequencyMHz: Double = 162.4
)
