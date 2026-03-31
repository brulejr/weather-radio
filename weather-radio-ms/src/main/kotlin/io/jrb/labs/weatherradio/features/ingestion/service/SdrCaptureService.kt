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
import io.jrb.labs.weatherradio.domain.WeatherStation
import io.jrb.labs.weatherradio.events.PipelineEvent
import io.jrb.labs.weatherradio.events.PipelineEventBus
import io.jrb.labs.weatherradio.features.ingestion.IngestionDatafill
import io.jrb.labs.weatherradio.features.ingestion.messaging.AudioSegmentPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import java.time.Clock
import java.time.Instant

class SdrCaptureService(
    private val datafill: IngestionDatafill,
    private val clock: Clock,
    private val eventBus: PipelineEventBus,
    private val audioSegmentPublisher: AudioSegmentPublisher,
    private val radioReceiverFactory: RadioReceiverFactory,
    systemEventBus: SystemEventBus
) : ControllableService(systemEventBus) {

    private var scope: CoroutineScope? = null
    private var captureJob: Job? = null
    private var receiver: RadioReceiver? = null

    override fun onStart() {
        val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = serviceScope

        captureJob = serviceScope.launch {
            while (isActive) {
                try {
                    val selectedReceiver = radioReceiverFactory.create(datafill)
                    receiver = selectedReceiver

                    val input = selectedReceiver.start()
                    publishRadioStatus(signalPresent = true, audioActive = true)

                    val segmenter = AudioSegmentationService(datafill, clock)
                    segmenter.segmentStream(datafill.stationCallSign, input) { segment ->
                        audioSegmentPublisher.publish(segment)
                        publishRadioStatus(signalPresent = true, audioActive = true)
                    }

                    publishRadioStatus(signalPresent = false, audioActive = false)
                } catch (_: Exception) {
                    publishRadioStatus(signalPresent = false, audioActive = false)
                    delay(datafill.reconnectDelay)
                } finally {
                    receiver?.stop()
                    receiver = null
                }
            }
        }
    }

    override fun onStop() = runBlocking {
        receiver?.stop()
        receiver = null
        scope?.cancel()
        scope = null
        publishRadioStatus(signalPresent = false, audioActive = false)
    }

    private suspend fun publishRadioStatus(signalPresent: Boolean, audioActive: Boolean) {
        val now = Instant.now(clock)
        val station = WeatherStation(
            callSign = datafill.stationCallSign,
            frequencyMHz = datafill.frequencyMHz,
            name = datafill.stationName,
            regionName = datafill.regionName
        )

        eventBus.publish(
            PipelineEvent.RadioStatusUpdated(
                RadioSignalStatus(
                    station = station,
                    signalPresent = signalPresent,
                    audioActive = audioActive,
                    lastSignalDetectedAt = now,
                    snrEstimate = null
                )
            )
        )
    }

}