package io.jrb.labs.weatherradio.domain

import java.time.Instant

data class SameMessage(
    val rawHeader: String,
    val originator: String,
    val eventType: SameEventType,
    val fipsCodes: List<String>,
    val purgeDurationMinutes: Int?,
    val issuedAt: Instant?,
    val stationCallSign: String?,
    val receivedAt: Instant
)
