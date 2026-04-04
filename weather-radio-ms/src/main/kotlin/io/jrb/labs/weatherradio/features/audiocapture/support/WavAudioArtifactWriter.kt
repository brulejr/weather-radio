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

package io.jrb.labs.weatherradio.features.audiocapture.support

import io.jrb.labs.weatherradio.features.audiocapture.model.AlertAudioCaptureRecord
import io.jrb.labs.weatherradio.features.audiocapture.model.AlertAudioFileArtifact
import io.jrb.labs.weatherradio.features.audiocapture.port.AudioArtifactWriter
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import kotlin.io.path.outputStream
import kotlin.math.roundToInt

class WavAudioArtifactWriter(
    private val artifactRoot: Path,
    private val clock: Clock,
) : AudioArtifactWriter {

    override fun writeCapture(capture: AlertAudioCaptureRecord): AlertAudioFileArtifact {
        val alertDir = artifactRoot.resolve(capture.alertId)
        Files.createDirectories(alertDir)

        val outputPath = alertDir.resolve("audio.wav")
        val pcm = toPcm16(capture)
        writeWav(
            outputPath = outputPath,
            pcmData = pcm,
            sampleRateHz = capture.sampleRateHz,
            channelCount = capture.channelCount,
        )

        return AlertAudioFileArtifact(
            alertId = capture.alertId,
            stationId = capture.stationId,
            filePath = outputPath.toAbsolutePath().toString(),
            format = "wav",
            sampleRateHz = capture.sampleRateHz,
            channelCount = capture.channelCount,
            frameCount = capture.frameCount,
            createdAt = clock.instant(),
            startedAt = capture.startedAt,
            completedAt = capture.completedAt,
            durationMillis = capture.durationMillis,
            byteLength = Files.size(outputPath),
            captureReason = capture.captureReason,
            wasPartial = capture.wasPartial,
            qualityMetrics = capture.qualityMetrics,
            qualityClassification = capture.qualityClassification,
            acceptableForTranscription = capture.acceptableForTranscription,
        )
    }

    private fun toPcm16(capture: AlertAudioCaptureRecord): ByteArray {
        val sampleCount = capture.frames.sumOf { it.samples.size }
        val bytes = ByteArray(sampleCount * 2)
        var offset = 0

        capture.frames.forEach { frame ->
            frame.samples.forEach { sample ->
                val clamped = sample.coerceIn(-1.0f, 1.0f)
                val pcm = (clamped * Short.MAX_VALUE).roundToInt().toShort()
                bytes[offset++] = (pcm.toInt() and 0xFF).toByte()
                bytes[offset++] = ((pcm.toInt() shr 8) and 0xFF).toByte()
            }
        }

        return bytes
    }

    private fun writeWav(
        outputPath: Path,
        pcmData: ByteArray,
        sampleRateHz: Int,
        channelCount: Int,
    ) {
        val bitsPerSample = 16
        val byteRate = sampleRateHz * channelCount * bitsPerSample / 8
        val blockAlign = channelCount * bitsPerSample / 8
        val subchunk2Size = pcmData.size
        val chunkSize = 36 + subchunk2Size

        DataOutputStream(BufferedOutputStream(outputPath.outputStream())).use { out ->
            out.writeBytes("RIFF")
            out.writeIntLE(chunkSize)
            out.writeBytes("WAVE")

            out.writeBytes("fmt ")
            out.writeIntLE(16)
            out.writeShortLE(1.toShort())
            out.writeShortLE(channelCount.toShort())
            out.writeIntLE(sampleRateHz)
            out.writeIntLE(byteRate)
            out.writeShortLE(blockAlign.toShort())
            out.writeShortLE(bitsPerSample.toShort())

            out.writeBytes("data")
            out.writeIntLE(subchunk2Size)
            out.write(pcmData)
        }
    }

    private fun DataOutputStream.writeIntLE(value: Int) {
        writeByte(value and 0xFF)
        writeByte((value ushr 8) and 0xFF)
        writeByte((value ushr 16) and 0xFF)
        writeByte((value ushr 24) and 0xFF)
    }

    private fun DataOutputStream.writeShortLE(value: Short) {
        val intValue = value.toInt()
        writeByte(intValue and 0xFF)
        writeByte((intValue ushr 8) and 0xFF)
    }
}