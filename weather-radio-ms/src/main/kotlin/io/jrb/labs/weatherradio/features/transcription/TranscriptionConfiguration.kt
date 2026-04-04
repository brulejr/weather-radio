package io.jrb.labs.weatherradio.features.transcription

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.weatherradio.events.WeatherRadioEventBus
import io.jrb.labs.weatherradio.features.FeatureDescriptors.CONFIG_PREFIX_TRANSCRIPTION
import io.jrb.labs.weatherradio.features.transcription.port.AudioFileTranscriber
import io.jrb.labs.weatherradio.features.transcription.port.TranscriptArtifactWriter
import io.jrb.labs.weatherradio.features.transcription.port.TranscriptNormalizer
import io.jrb.labs.weatherradio.features.transcription.port.TranscriptionDecisionPolicy
import io.jrb.labs.weatherradio.features.transcription.service.TranscriptionFeature
import io.jrb.labs.weatherradio.features.transcription.support.DefaultTranscriptNormalizer
import io.jrb.labs.weatherradio.features.transcription.support.DefaultTranscriptionDecisionPolicy
import io.jrb.labs.weatherradio.features.transcription.support.SyntheticAudioFileTranscriber
import io.jrb.labs.weatherradio.features.transcription.support.TextTranscriptArtifactWriter
import org.springframework.beans.factory.annotation.Qualifier
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

    @Bean("primaryAudioFileTranscriber")
    fun primaryAudioFileTranscriber(
        datafill: TranscriptionDatafill,
        clock: Clock,
    ): AudioFileTranscriber = SyntheticAudioFileTranscriber(
        datafill = datafill,
        clock = clock,
        engineName = datafill.primaryEngineName,
    )

    @Bean("fallbackAudioFileTranscriber")
    fun fallbackAudioFileTranscriber(
        datafill: TranscriptionDatafill,
        clock: Clock,
    ): AudioFileTranscriber = SyntheticAudioFileTranscriber(
        datafill = datafill,
        clock = clock,
        engineName = datafill.fallbackEngineName,
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
    fun transcriptNormalizer(): TranscriptNormalizer =
        DefaultTranscriptNormalizer()

    @Bean
    fun transcriptionDecisionPolicy(
        datafill: TranscriptionDatafill,
    ): TranscriptionDecisionPolicy =
        DefaultTranscriptionDecisionPolicy(datafill)

    @Bean
    fun transcriptionFeature(
        systemEventBus: SystemEventBus,
        weatherRadioEventBus: WeatherRadioEventBus,
        datafill: TranscriptionDatafill,
        @Qualifier("primaryAudioFileTranscriber")
        audioFileTranscriber: AudioFileTranscriber,
        @Qualifier("fallbackAudioFileTranscriber")
        fallbackAudioFileTranscriber: AudioFileTranscriber,
        transcriptArtifactWriter: TranscriptArtifactWriter,
        transcriptNormalizer: TranscriptNormalizer,
        transcriptionDecisionPolicy: TranscriptionDecisionPolicy,
        clock: Clock,
    ): TranscriptionFeature = TranscriptionFeature(
        systemEventBus = systemEventBus,
        weatherRadioEventBus = weatherRadioEventBus,
        datafill = datafill,
        audioFileTranscriber = audioFileTranscriber,
        fallbackAudioFileTranscriber = fallbackAudioFileTranscriber,
        transcriptArtifactWriter = transcriptArtifactWriter,
        transcriptNormalizer = transcriptNormalizer,
        transcriptionDecisionPolicy = transcriptionDecisionPolicy,
        clock = clock,
    )

    @Bean
    fun transcriptionStartup(
        feature: TranscriptionFeature,
    ) = ApplicationRunner { feature.start() }
}