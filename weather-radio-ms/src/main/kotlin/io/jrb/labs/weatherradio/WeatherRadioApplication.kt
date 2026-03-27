package io.jrb.labs.weatherradio

import io.jrb.labs.weatherradio.config.WeatherRadioProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(WeatherRadioProperties::class)
class WeatherRadioApplication

fun main(args: Array<String>) {
    runApplication<WeatherRadioApplication>(*args)
}
