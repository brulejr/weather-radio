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

import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.service.ControllableService
import io.jrb.labs.weatherradio.domain.RadioSignalStatus
import io.jrb.labs.weatherradio.domain.SameEventType
import io.jrb.labs.weatherradio.domain.SameMessage
import io.jrb.labs.weatherradio.domain.TranscriptSegment
import io.jrb.labs.weatherradio.domain.WeatherStation
import io.jrb.labs.weatherradio.events.PipelineEvent
import io.jrb.labs.weatherradio.events.PipelineEventBus
import io.jrb.labs.weatherradio.features.ingestion.IngestionDatafill
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import java.time.Clock
import java.time.Instant

class StubIngestionInitializer(
    private val datafill: IngestionDatafill,
    private val eventBus: PipelineEventBus,
    private val clock: Clock,
    systemEventBus: SystemEventBus
) : ControllableService(systemEventBus) {

    @EventListener(ApplicationReadyEvent::class)
    fun initialize() {
        val now = Instant.now(clock)

        val station = WeatherStation(
            callSign = datafill.stationCallSign,
            frequencyMHz = datafill.frequencyMHz,
            name = datafill.stationName,
            regionName = datafill.regionName
        )

        publishRadioStatus(now, station)
        publishSame(now)
        publishTranscript(now)
    }

    private fun publishRadioStatus(now: Instant, station: WeatherStation) {
        eventBus.send(
            PipelineEvent.RadioStatusUpdated(
                RadioSignalStatus(
                    station = station,
                    signalPresent = true,
                    audioActive = true,
                    lastSignalDetectedAt = now,
                    snrEstimate = 18.7
                )
            )
        )
    }

    private fun publishSame(now: Instant) {
        eventBus.send(
            PipelineEvent.SameMessageDecoded(
                stationId = datafill.stationCallSign,
                same = SameMessage(
                    rawHeader = "ZCZC-WXR-TOR-050007,086183+0030-086183-KIG60/NWS-",
                    originator = "WXR",
                    eventType = SameEventType.TOR,
                    fipsCodes = listOf("050007", "086183"),
                    purgeDurationMinutes = 30,
                    issuedAt = now,
                    stationCallSign = datafill.stationCallSign,
                    receivedAt = now
                )
            )
        )
    }

    private fun publishTranscript(now: Instant) {
        eventBus.send(
            PipelineEvent.TranscriptProduced(
                stationId = datafill.stationCallSign,
                transcript = TranscriptSegment(
                    text = "National Weather Service Burlington Vermont. Tornado warning in effect until 4:30 PM.",
                    startedAt = now.minusSeconds(20),
                    endedAt = now.minusSeconds(2),
                    confidence = 0.86,
                    kind = TranscriptSegment.SegmentKind.HAZARD_BULLETIN
                )
            )
        )
    }

}