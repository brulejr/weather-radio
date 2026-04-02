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

package io.jrb.labs.weatherradio.features

import io.jrb.labs.commons.feature.FeatureDescriptor

object FeatureDescriptors {

    const val CONFIG_PREFIX_RADIO_INPUT = "application.radio-input"
    const val CONFIG_PREFIX_AUDIO_BUFFER = "application.audio-buffer"
    const val CONFIG_PREFIX_SAME_DECODER = "application.same-decoder"
    const val CONFIG_PREFIX_ALERT_ORCHESTRATION = "application.alert-orchestration"
    const val CONFIG_PREFIX_AUDIO_CAPTURE = "application.audio-capture"
    const val CONFIG_PREFIX_TRANSCRIPTION = "application.transcription"
    const val CONFIG_PREFIX_ALERT_STORE = "application.alert-store"
    const val CONFIG_PREFIX_NOTIFICATION_OUTPUT = "application.notification-output"
    const val CONFIG_PREFIX_OBSERVABILITY = "application.observability"

    val RADIO_INPUT = FeatureDescriptor(
        application = "weather-radio",
        featureId = "radio-input",
        displayName = "Radio Input",
        description = "Tunes radio sources and publishes normalized audio frames.",
        configPrefix = CONFIG_PREFIX_RADIO_INPUT,
    )

    val AUDIO_BUFFER = FeatureDescriptor(
        application = "weather-radio",
        featureId = "audio-buffer",
        displayName = "Audio Buffer",
        description = "Maintains rolling pre-roll audio for later capture.",
        configPrefix = CONFIG_PREFIX_AUDIO_BUFFER,
    )

    val SAME_DECODER = FeatureDescriptor(
        application = "weather-radio",
        featureId = "same-decoder",
        displayName = "SAME Decoder",
        description = "Detects and decodes SAME headers from weather radio audio.",
        configPrefix = CONFIG_PREFIX_SAME_DECODER,
    )

    val ALERT_ORCHESTRATION = FeatureDescriptor(
        application = "weather-radio",
        featureId = "alert-orchestration",
        displayName = "Alert Orchestration",
        description = "Owns alert lifecycle, deduplication, and local relevance decisions.",
        configPrefix = CONFIG_PREFIX_ALERT_ORCHESTRATION,
    )

    val AUDIO_CAPTURE = FeatureDescriptor(
        application = "weather-radio",
        featureId = "audio-capture",
        displayName = "Audio Capture",
        description = "Captures pre-roll and spoken bulletin audio around alert activity.",
        configPrefix = CONFIG_PREFIX_AUDIO_CAPTURE,
    )

    val TRANSCRIPTION = FeatureDescriptor(
        application = "weather-radio",
        featureId = "transcription",
        displayName = "Transcription",
        description = "Transcribes captured spoken bulletin audio using local engines.",
        configPrefix = CONFIG_PREFIX_TRANSCRIPTION,
    )

    val ALERT_STORE = FeatureDescriptor(
        application = "weather-radio",
        featureId = "alert-store",
        displayName = "Alert Store",
        description = "Persists alerts, artifacts, and derived state.",
        configPrefix = CONFIG_PREFIX_ALERT_STORE,
    )

    val NOTIFICATION_OUTPUT = FeatureDescriptor(
        application = "weather-radio",
        featureId = "notification-output",
        displayName = "Notification Output",
        description = "Publishes alerts to downstream outputs such as REST, MQTT, and local sinks.",
        configPrefix = CONFIG_PREFIX_NOTIFICATION_OUTPUT,
    )

    val OBSERVABILITY = FeatureDescriptor(
        application = "weather-radio",
        featureId = "observability",
        displayName = "Observability",
        description = "Tracks pipeline health, counters, timing, and anomalies.",
        configPrefix = CONFIG_PREFIX_OBSERVABILITY,
    )

    val ALL = listOf(
        RADIO_INPUT,
        AUDIO_BUFFER,
        SAME_DECODER,
        ALERT_ORCHESTRATION,
        AUDIO_CAPTURE,
        TRANSCRIPTION,
        ALERT_STORE,
        NOTIFICATION_OUTPUT,
        OBSERVABILITY,
    )

}