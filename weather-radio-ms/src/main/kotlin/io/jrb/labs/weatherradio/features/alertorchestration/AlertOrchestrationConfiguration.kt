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

package io.jrb.labs.weatherradio.features.alertorchestration

import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.weatherradio.events.WeatherRadioEventBus
import io.jrb.labs.weatherradio.features.FeatureDescriptors.CONFIG_PREFIX_ALERT_ORCHESTRATION
import io.jrb.labs.weatherradio.features.alertorchestration.service.AlertOrchestrationFeature
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
@ConfigurationPropertiesScan(basePackages = ["io.jrb.labs.weatherradio.features.alertorchestration"])
@ConditionalOnProperty(
    prefix = CONFIG_PREFIX_ALERT_ORCHESTRATION,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class AlertOrchestrationConfiguration {

    @Bean
    fun alertOrchestrationFeature(
        systemEventBus: SystemEventBus,
        weatherRadioEventBus: WeatherRadioEventBus,
        datafill: AlertOrchestrationDatafill,
        clock: Clock,
    ): AlertOrchestrationFeature = AlertOrchestrationFeature(
        systemEventBus = systemEventBus,
        weatherRadioEventBus = weatherRadioEventBus,
        datafill = datafill,
        clock = clock,
    )

    @Bean
    fun alertOrchestrationStartup(
        feature: AlertOrchestrationFeature,
    ) = ApplicationRunner {
        feature.start()
    }

}