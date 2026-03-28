package io.jrb.labs.weatherradio.events

import io.jrb.labs.commons.eventbus.Event

sealed class WeatherPipelineEvent : Event {
    data class AudioSegmentDetected(
        val stationId: String,
        val segmentId: String,
        val audioPath: String
    ) : WeatherPipelineEvent()

    data class SameMessageDecoded(
        val stationId: String,
        val same: io.jrb.labs.weatherradio.domain.SameMessage
    ) : WeatherPipelineEvent()

    data class TranscriptProduced(
        val stationId: String,
        val transcript: io.jrb.labs.weatherradio.domain.TranscriptSegment
    ) : WeatherPipelineEvent()

    data class RadioStatusUpdated(
        val status: io.jrb.labs.weatherradio.domain.RadioSignalStatus
    ) : WeatherPipelineEvent()

    data class WeatherReportUpdated(
        val report: io.jrb.labs.weatherradio.domain.WeatherReport
    ) : WeatherPipelineEvent()
}