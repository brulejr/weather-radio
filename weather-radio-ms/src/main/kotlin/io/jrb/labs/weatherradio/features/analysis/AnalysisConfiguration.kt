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

package io.jrb.labs.weatherradio.features.analysis

import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.weatherradio.events.PipelineEventBus
import io.jrb.labs.weatherradio.features.FeatureDescriptors.CONFIG_PREFIX_ANALYSIS
import io.jrb.labs.weatherradio.features.analysis.messaging.AudioSegmentAnalyzedLogger
import io.jrb.labs.weatherradio.features.analysis.messaging.AudioSegmentAnalyzedPublisher
import io.jrb.labs.weatherradio.features.analysis.messaging.AudioSegmentDetectedAnalysisConsumer
import io.jrb.labs.weatherradio.features.analysis.service.AudioSegmentAnalyzer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationPropertiesScan( basePackages = ["io.jrb.labs.weatherradio.features.analysis"])
@ConditionalOnProperty(prefix = CONFIG_PREFIX_ANALYSIS, name = ["enabled"], havingValue = "true", matchIfMissing = true)
class AnalysisConfiguration {

    @Bean
    fun audioSegmentDetectedAnalysisConsumer(
        eventBus: PipelineEventBus,
        systemEventBus: SystemEventBus,
        analyzer: AudioSegmentAnalyzer,
        publisher: AudioSegmentAnalyzedPublisher
    ) = AudioSegmentDetectedAnalysisConsumer(eventBus, systemEventBus, analyzer, publisher)

    @Bean
    fun audioSegmentAnalyzedLogger(eventBus: PipelineEventBus, systemEventBus: SystemEventBus) =
        AudioSegmentAnalyzedLogger(eventBus, systemEventBus)

    @Bean
    fun audioSegmentAnalyzedPublisher(eventBus: PipelineEventBus) = AudioSegmentAnalyzedPublisher(eventBus)

    @Bean
    fun audioSegmentAnalyzer(datafill: AnalysisDatafill) = AudioSegmentAnalyzer(datafill)

    @Bean
    fun analysisInfoContributor(datafill: AnalysisDatafill) = AnalysisInfoContributor(datafill)

}