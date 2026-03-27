package io.jrb.labs.weatherradio.domain

data class WeatherStation(
    val callSign: String,
    val frequencyMHz: Double,
    val name: String,
    val regionName: String
)
