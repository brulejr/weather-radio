package io.jrb.labs.weatherradio.web

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class WeatherControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `report endpoint returns seeded report`() {
        mockMvc.get("/api/weather/report")
            .andExpect {
                status { isOk() }
                jsonPath("$.regionName") { value("South Burlington, VT") }
                jsonPath("$.station.callSign") { value("KIG60") }
                jsonPath("$.authoritativeAlerts[0].headline") { value("Tornado Warning") }
            }
    }
}
