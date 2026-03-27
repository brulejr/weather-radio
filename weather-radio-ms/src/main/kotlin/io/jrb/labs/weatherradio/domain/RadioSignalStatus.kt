package io.jrb.labs.weatherradio.domain

import java.time.Instant

data class RadioSignalStatus(
    val station: WeatherStation,
    val signalPresent: Boolean,
    val audioActive: Boolean,
    val lastSignalDetectedAt: Instant?,
    val snrEstimate: Double? = null
)
