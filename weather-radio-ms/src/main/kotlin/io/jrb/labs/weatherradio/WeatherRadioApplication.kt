package io.jrb.labs.weatherradio

import io.jrb.labs.commons.actuator.FeatureInfoContributor
import io.jrb.labs.commons.actuator.FeaturesInfoContributor
import io.jrb.labs.commons.eventbus.SystemEventBus
import io.jrb.labs.commons.eventbus.SystemEventLogger
import io.jrb.labs.commons.metrics.FeatureMetricsFactory
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.Clock

@SpringBootApplication
@ConfigurationPropertiesScan
class WeatherRadioApplication {

    @Bean
    fun systemEventBus(): SystemEventBus = SystemEventBus()

    @Bean
    fun systemEventLogger(systemEventBus: SystemEventBus): SystemEventLogger =
        SystemEventLogger(systemEventBus)

    @Bean
    fun featuresInfoContributor(contributors: List<FeatureInfoContributor>) =
        FeaturesInfoContributor(contributors)

    @Bean
    fun featureMetricsFactory(registry: MeterRegistry) =
        FeatureMetricsFactory(registry)

    @Bean
    fun clock(): Clock = Clock.systemUTC()

}

fun main(args: Array<String>) {
    runApplication<WeatherRadioApplication>(*args)
}