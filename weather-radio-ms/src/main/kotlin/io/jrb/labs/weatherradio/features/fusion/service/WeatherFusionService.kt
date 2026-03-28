package io.jrb.labs.weatherradio.features.fusion.service

import io.jrb.labs.weatherradio.domain.AlertSeverity
import io.jrb.labs.weatherradio.domain.SameEventType
import io.jrb.labs.weatherradio.domain.SameMessage
import io.jrb.labs.weatherradio.domain.WeatherAlert

class WeatherFusionService {

    fun toAlert(message: SameMessage): WeatherAlert {
        return WeatherAlert(
            eventCode = message.eventType,
            headline = headlineFor(message.eventType),
            severity = severityFor(message.eventType),
            affectedFipsCodes = message.fipsCodes,
            issuedAt = message.issuedAt,
            expiresAt = message.issuedAt?.plusSeconds((message.purgeDurationMinutes ?: 0) * 60L),
            source = WeatherAlert.AlertSource.SAME
        )
    }

    private fun headlineFor(eventType: SameEventType): String =
        when (eventType) {
            SameEventType.TOR -> "Tornado Warning"
            SameEventType.SVR -> "Severe Thunderstorm Warning"
            SameEventType.FFW -> "Flash Flood Warning"
            SameEventType.FLS -> "Flood Statement"
            SameEventType.WIN -> "Winter Weather Advisory/Warning"
            SameEventType.BZW -> "Blizzard Warning"
            SameEventType.HWW -> "High Wind Warning"
            SameEventType.RWT -> "Required Weekly Test"
            SameEventType.RMT -> "Required Monthly Test"
            SameEventType.UNKNOWN -> "Unknown Weather Alert"
        }

    private fun severityFor(eventType: SameEventType): AlertSeverity =
        when (eventType) {
            SameEventType.TOR,
            SameEventType.SVR,
            SameEventType.FFW,
            SameEventType.BZW,
            SameEventType.HWW -> AlertSeverity.WARNING

            SameEventType.WIN,
            SameEventType.FLS -> AlertSeverity.ADVISORY

            SameEventType.RWT,
            SameEventType.RMT -> AlertSeverity.INFO

            SameEventType.UNKNOWN -> AlertSeverity.UNKNOWN
        }
}
