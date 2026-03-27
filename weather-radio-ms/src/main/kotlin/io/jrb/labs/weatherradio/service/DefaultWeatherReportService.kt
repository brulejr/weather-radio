package io.jrb.labs.weatherradio.service

import io.jrb.labs.weatherradio.domain.WeatherReport
import io.jrb.labs.weatherradio.ports.WeatherReportService
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class DefaultWeatherReportService(
    private val repository: InMemoryRadioStateRepository,
    private val fusionService: WeatherFusionService,
    private val clock: Clock
) : WeatherReportService {

    override fun currentReport(): WeatherReport {
        val radioStatus = repository.radioStatus()
        val sameMessage = repository.latestSameMessage()
        val latestTranscript = repository.latestTranscript()

        val alerts = buildList {
            if (sameMessage != null) {
                add(fusionService.toAlert(sameMessage))
            }
        }

        return WeatherReport(
            regionName = radioStatus?.station?.regionName ?: "Unknown Region",
            generatedAt = Instant.now(clock),
            radioStatus = radioStatus,
            authoritativeAlerts = alerts,
            transcribedForecast = latestTranscript?.text,
            latestTranscript = latestTranscript,
            station = radioStatus?.station
        )
    }
}
