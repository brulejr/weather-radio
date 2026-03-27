package io.jrb.labs.weatherradio.domain

import java.time.Instant

data class TranscriptSegment(
    val text: String,
    val startedAt: Instant,
    val endedAt: Instant,
    val confidence: Double?,
    val kind: SegmentKind
) {
    enum class SegmentKind {
        UNKNOWN,
        STATION_ID,
        FORECAST,
        CURRENT_CONDITIONS,
        HAZARD_BULLETIN
    }
}
