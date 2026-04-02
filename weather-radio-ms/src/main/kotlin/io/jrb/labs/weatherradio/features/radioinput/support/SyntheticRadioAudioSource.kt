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
package io.jrb.labs.weatherradio.features.radioinput.support

import io.jrb.labs.weatherradio.domain.radio.AudioFrame
import io.jrb.labs.weatherradio.domain.radio.RadioStation
import io.jrb.labs.weatherradio.features.radioinput.RadioInputDatafill
import io.jrb.labs.weatherradio.features.radioinput.port.RadioAudioSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class SyntheticRadioAudioSource(
    private val datafill: RadioInputDatafill,
    private val clock: Clock,
) : RadioAudioSource {

    override fun stream(station: RadioStation): Flow<AudioFrame> = flow {
        val sampleRate = datafill.sampleRateHz
        val intervalMs = datafill.publishIntervalMs
        val samplesPerFrame = ((sampleRate * intervalMs) / 1000L).toInt().coerceAtLeast(1)
        val channelCount = datafill.channelCount

        var phase = 0.0
        var frameCounter = 0L
        val baseFrequencyHz = 1050.0

        while (true) {
            val frameStart = Instant.now(clock)
            val samples = FloatArray(samplesPerFrame * channelCount)

            for (sampleIndex in 0 until samplesPerFrame) {
                val signal = sin(phase) * 0.15
                val noise = Random.nextDouble(-0.01, 0.01)
                val value = (signal + noise).toFloat()

                val baseOffset = sampleIndex * channelCount
                for (channel in 0 until channelCount) {
                    samples[baseOffset + channel] = value
                }

                phase += 2.0 * PI * baseFrequencyHz / sampleRate
                if (phase > 2.0 * PI) {
                    phase -= 2.0 * PI
                }
            }

            frameCounter++

            if (datafill.injectSyntheticSame &&
                frameCounter % datafill.syntheticSameIntervalFrames.toLong() == 0L
            ) {
                injectSyntheticSameMarker(samples, channelCount)
            }

            val frameEnd = Instant.now(clock)

            emit(
                AudioFrame(
                    stationId = station.stationId,
                    sampleRateHz = sampleRate,
                    channelCount = channelCount,
                    samples = samples,
                    frameStart = frameStart,
                    frameEnd = frameEnd,
                )
            )

            delay(intervalMs)
        }
    }

    private fun injectSyntheticSameMarker(
        samples: FloatArray,
        channelCount: Int,
    ) {
        if (samples.isEmpty()) return

        val sampleFrames = samples.size / channelCount
        if (sampleFrames < 12) return

        val runLengthFrames = maxOf(8, minOf(120, sampleFrames / 8))
        var frameOffset = 0

        repeat(3) {
            frameOffset = writeRun(
                samples = samples,
                channelCount = channelCount,
                startFrameOffset = frameOffset,
                runLengthFrames = runLengthFrames,
                value = 0.95f,
            )

            frameOffset = writeRun(
                samples = samples,
                channelCount = channelCount,
                startFrameOffset = frameOffset,
                runLengthFrames = runLengthFrames,
                value = -0.95f,
            )
        }
    }

    private fun writeRun(
        samples: FloatArray,
        channelCount: Int,
        startFrameOffset: Int,
        runLengthFrames: Int,
        value: Float,
    ): Int {
        val totalFrames = samples.size / channelCount
        val endFrameExclusive = minOf(startFrameOffset + runLengthFrames, totalFrames)

        for (frame in startFrameOffset until endFrameExclusive) {
            val baseOffset = frame * channelCount
            for (channel in 0 until channelCount) {
                samples[baseOffset + channel] = value
            }
        }

        return endFrameExclusive
    }
}