package io.jrb.labs.weatherradio.web

import io.jrb.labs.weatherradio.domain.RadioSignalStatus
import io.jrb.labs.weatherradio.domain.SameMessage
import io.jrb.labs.weatherradio.domain.TranscriptSegment
import io.jrb.labs.weatherradio.domain.WeatherReport
import io.jrb.labs.weatherradio.ports.WeatherReportService
import io.jrb.labs.weatherradio.service.InMemoryRadioStateRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/weather")
class WeatherController(
    private val weatherReportService: WeatherReportService,
    private val repository: InMemoryRadioStateRepository
) {

    @GetMapping("/report")
    fun report(): WeatherReport = weatherReportService.currentReport()

    @GetMapping("/radio/status")
    fun radioStatus(): RadioSignalStatus? = repository.radioStatus()

    @GetMapping("/radio/latest-same")
    fun latestSame(): SameMessage? = repository.latestSameMessage()

    @GetMapping("/radio/latest-transcript")
    fun latestTranscript(): TranscriptSegment? = repository.latestTranscript()
}
