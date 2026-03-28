/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.jrb.labs.weatherradio.features.ingestion.service

import io.jrb.labs.weatherradio.config.WeatherRadioProperties
import io.jrb.labs.weatherradio.domain.RadioSignalStatus
import io.jrb.labs.weatherradio.domain.SameEventType
import io.jrb.labs.weatherradio.domain.SameMessage
import io.jrb.labs.weatherradio.domain.TranscriptSegment
import io.jrb.labs.weatherradio.domain.WeatherStation
import io.jrb.labs.weatherradio.features.radio.service.RadioService
import io.jrb.labs.weatherradio.features.same.service.SameService
import io.jrb.labs.weatherradio.features.transcription.service.TranscriptionService
import jakarta.annotation.PostConstruct
import java.time.Clock
import java.time.Instant

class StubIngestionInitializer(
    private val properties: WeatherRadioProperties,
    private val sameService: SameService,
    private val radioService: RadioService,
    private val transcriptionService: TranscriptionService,
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

        radioService.updateRadioStatus(
            RadioSignalStatus(
                station = station,
                signalPresent = true,
                audioActive = true,
                lastSignalDetectedAt = now,
                snrEstimate = 18.7
            )
        )

        sameService.updateSameMessage(
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

        transcriptionService.updateTranscript(
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