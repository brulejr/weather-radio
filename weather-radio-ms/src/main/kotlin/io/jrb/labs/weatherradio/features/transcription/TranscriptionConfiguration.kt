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

package io.jrb.labs.weatherradio.features.transcription

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.weatherradio.events.WeatherRadioEventBus
import io.jrb.labs.weatherradio.features.FeatureDescriptors.CONFIG_PREFIX_TRANSCRIPTION
import io.jrb.labs.weatherradio.features.transcription.port.AudioFileTranscriber
import io.jrb.labs.weatherradio.features.transcription.port.TranscriptArtifactWriter
import io.jrb.labs.weatherradio.features.transcription.port.TranscriptNormalizer
import io.jrb.labs.weatherradio.features.transcription.service.TranscriptionFeature
import io.jrb.labs.weatherradio.features.transcription.support.DefaultTranscriptNormalizer
import io.jrb.labs.weatherradio.features.transcription.support.SyntheticAudioFileTranscriber
import io.jrb.labs.weatherradio.features.transcription.support.TextTranscriptArtifactWriter
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Paths
import java.time.Clock

@Configuration
@ConfigurationPropertiesScan(basePackages = ["io.jrb.labs.weatherradio.features.transcription"])
@ConditionalOnProperty(
    prefix = CONFIG_PREFIX_TRANSCRIPTION,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class TranscriptionConfiguration {

    @Bean
    fun audioFileTranscriber(
        datafill: TranscriptionDatafill,
        clock: Clock,
    ): AudioFileTranscriber = SyntheticAudioFileTranscriber(
        datafill = datafill,
        clock = clock,
    )

    @Bean
    fun transcriptArtifactWriter(
        datafill: TranscriptionDatafill,
        objectMapper: ObjectMapper,
        clock: Clock,
    ): TranscriptArtifactWriter = TextTranscriptArtifactWriter(
        artifactRoot = Paths.get(datafill.artifactDirectory),
        objectMapper = objectMapper,
        clock = clock,
    )

    @Bean
    fun transcriptionFeature(
        systemEventBus: SystemEventBus,
        weatherRadioEventBus: WeatherRadioEventBus,
        datafill: TranscriptionDatafill,
        audioFileTranscriber: AudioFileTranscriber,
        transcriptArtifactWriter: TranscriptArtifactWriter,
        transcriptNormalizer: TranscriptNormalizer,
        clock: Clock,
    ): TranscriptionFeature = TranscriptionFeature(
        systemEventBus = systemEventBus,
        weatherRadioEventBus = weatherRadioEventBus,
        datafill = datafill,
        audioFileTranscriber = audioFileTranscriber,
        transcriptArtifactWriter = transcriptArtifactWriter,
        transcriptNormalizer = transcriptNormalizer,
        clock = clock,
    )

    @Bean
    fun transcriptionStartup(
        feature: TranscriptionFeature,
    ) = ApplicationRunner {
        feature.start()
    }

    @Bean
    fun transcriptNormalizer(): TranscriptNormalizer = DefaultTranscriptNormalizer()

}