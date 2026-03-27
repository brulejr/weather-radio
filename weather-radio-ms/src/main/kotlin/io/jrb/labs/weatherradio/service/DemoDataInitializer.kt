package io.jrb.labs.weatherradio.service

import io.jrb.labs.weatherradio.config.WeatherRadioProperties
import io.jrb.labs.weatherradio.domain.RadioSignalStatus
import io.jrb.labs.weatherradio.domain.SameEventType
import io.jrb.labs.weatherradio.domain.SameMessage
import io.jrb.labs.weatherradio.domain.TranscriptSegment
import io.jrb.labs.weatherradio.domain.WeatherStation
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class DemoDataInitializer(
    private val properties: WeatherRadioProperties,
    private val repository: InMemoryRadioStateRepository,
    private val clock: Clock
) {

    @PostConstruct
    fun init() {
        val now = Instant.now(clock)

        val station = WeatherStation(
            callSign = properties.stationCallSign,
            frequencyMHz = properties.frequencyMHz,
            name = properties.stationName,
            regionName = properties.regionName
        )

        repository.updateRadioStatus(
            RadioSignalStatus(
                station = station,
                signalPresent = true,
                audioActive = true,
                lastSignalDetectedAt = now,
                snrEstimate = 18.7
            )
        )

        repository.updateSameMessage(
            SameMessage(
                rawHeader = "ZCZC-WXR-TOR-050007,086183+0030-086183-KIG60/NWS-",
                originator = "WXR",
                eventType = SameEventType.TOR,
                fipsCodes = listOf("050007", "086183"),
                purgeDurationMinutes = 30,
                issuedAt = now,
                stationCallSign = properties.stationCallSign,
                receivedAt = now
            )
        )

        repository.updateTranscript(
            TranscriptSegment(
                text = "National Weather Service Burlington Vermont. Tornado warning in effect until 4:30 PM.",
                startedAt = now.minusSeconds(20),
                endedAt = now.minusSeconds(2),
                confidence = 0.86,
                kind = TranscriptSegment.SegmentKind.HAZARD_BULLETIN
            )
        )
    }
}
