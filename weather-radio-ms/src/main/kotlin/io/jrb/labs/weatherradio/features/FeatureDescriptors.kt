package io.jrb.labs.weatherradio.features

import io.jrb.labs.commons.feature.FeatureDescriptor

object FeatureDescriptors {

    const val CONFIG_PREFIX_ANALYSIS = "application.analysis"
    const val CONFIG_PREFIX_INGESTION = "application.ingestion"
    const val CONFIG_PREFIX_RADIO = "application.radio"
    const val CONFIG_PREFIX_SAME = "application.same"
    const val CONFIG_PREFIX_TRANSCRIPTION = "application.transcription"
    const val CONFIG_PREFIX_FUSION = "application.fusion"
    const val CONFIG_PREFIX_REPORTING = "application.reporting"

    val ANALYSIS = FeatureDescriptor(
        application = "weather-radio",
        featureId = "analysis",
        displayName = "Audio Analysis",
        description = "Analyzes PCM segments and produces content hints for downstream decoding.",
        configPrefix = CONFIG_PREFIX_ANALYSIS
    )

    val INGESTION = FeatureDescriptor(
        application = "weather-radio",
        featureId = "ingestion",
        displayName = "Audio Ingestion",
        description = "Receives SDR or audio input and creates pipeline segments.",
        configPrefix = CONFIG_PREFIX_INGESTION
    )

    val RADIO = FeatureDescriptor(
        application = "weather-radio",
        featureId = "radio",
        displayName = "Radio Status",
        description = "Tracks weather station signal and audio state.",
        configPrefix = CONFIG_PREFIX_RADIO
    )

    val SAME = FeatureDescriptor(
        application = "weather-radio",
        featureId = "same",
        displayName = "SAME Decoder",
        description = "Detects and parses SAME alert bursts.",
        configPrefix = CONFIG_PREFIX_SAME
    )

    val TRANSCRIPTION = FeatureDescriptor(
        application = "weather-radio",
        featureId = "transcription",
        displayName = "Transcription",
        description = "Transcribes spoken weather radio forecast content.",
        configPrefix = CONFIG_PREFIX_TRANSCRIPTION
    )

    val FUSION = FeatureDescriptor(
        application = "weather-radio",
        featureId = "fusion",
        displayName = "Fusion",
        description = "Combines SAME alerts, transcripts, and signal state into a unified report.",
        configPrefix = CONFIG_PREFIX_FUSION
    )

    val REPORTING = FeatureDescriptor(
        application = "weather-radio",
        featureId = "reporting",
        displayName = "Reporting",
        description = "Publishes REST-ready weather reports.",
        configPrefix = CONFIG_PREFIX_REPORTING
    )

    val ALL = listOf(INGESTION, RADIO, SAME, TRANSCRIPTION, FUSION, REPORTING)
}