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

package io.jrb.labs.weatherradio.features.samedecoder

import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.metrics.FeatureMetricsFactory
import io.jrb.labs.weatherradio.events.WeatherRadioEventBus
import io.jrb.labs.weatherradio.features.FeatureDescriptors.CONFIG_PREFIX_SAME_DECODER
import io.jrb.labs.weatherradio.features.samedecoder.service.SameDecoderFeature
import io.jrb.labs.weatherradio.features.samedecoder.support.NoOpSameFrameDecoder
import io.jrb.labs.weatherradio.features.samedecoder.support.SameFrameDecoder
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
@ConfigurationPropertiesScan( basePackages = ["io.jrb.labs.weatherradio.features.samedecoder"])
@ConditionalOnProperty(prefix = CONFIG_PREFIX_SAME_DECODER, name = ["enabled"], havingValue = "true", matchIfMissing = true)
class SameDecoderConfiguration {

    @Bean
    fun sameFrameDecoder(): SameFrameDecoder = NoOpSameFrameDecoder()

    @Bean
    fun sameDecoderFeature(
        systemEventBus: SystemEventBus,
        weatherRadioEventBus: WeatherRadioEventBus,
        datafill: SameDecoderDatafill,
        decoder: SameFrameDecoder,
        clock: Clock,
        metricsFactory: FeatureMetricsFactory,
    ): SameDecoderFeature = SameDecoderFeature(
        systemEventBus = systemEventBus,
        weatherRadioEventBus = weatherRadioEventBus,
        datafill = datafill,
        decoder = decoder,
        clock = clock,
        metricsFactory = metricsFactory,
    )

    @Bean
    fun sameDecoderStartup(
        feature: SameDecoderFeature,
    ) = ApplicationRunner {
        feature.start()
    }

}