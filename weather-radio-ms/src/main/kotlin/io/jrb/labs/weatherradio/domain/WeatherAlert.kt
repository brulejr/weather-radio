package io.jrb.labs.weatherradio.domain

import java.time.Instant

data class WeatherAlert(
    val eventCode: SameEventType,
    val headline: String,
    val severity: AlertSeverity,
    val affectedFipsCodes: List<String>,
    val issuedAt: Instant?,
    val expiresAt: Instant?,
    val source: AlertSource
) {
    enum class AlertSource {
        SAME,
        TRANSCRIPT
    }
}
