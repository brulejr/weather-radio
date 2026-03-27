package io.jrb.labs.weatherradio.support

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockProvider {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
