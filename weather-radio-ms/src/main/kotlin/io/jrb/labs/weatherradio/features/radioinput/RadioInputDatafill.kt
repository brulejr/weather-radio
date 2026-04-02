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

package io.jrb.labs.weatherradio.features.radioinput

import io.jrb.labs.weatherradio.features.FeatureDescriptors.CONFIG_PREFIX_RADIO_INPUT
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = CONFIG_PREFIX_RADIO_INPUT)
data class RadioInputDatafill(
    val enabled: Boolean = true,

    val mode: Mode = Mode.SYNTHETIC,

    @field:Min(1)
    val publishIntervalMs: Long = 250,

    @field:Min(1000)
    val sampleRateHz: Int = 22_050,

    @field:Min(1)
    val channelCount: Int = 1,

    @field:Valid
    @field:NotEmpty
    val stations: List<StationProperties> = listOf(
        StationProperties(
            stationId = "noaa-default",
            displayName = "NOAA Default",
            frequencyMHz = 162.550,
            countyCodes = emptyList(),
        )
    ),
) {
    enum class Mode {
        SYNTHETIC,
        RTL_SDR,
    }

    data class StationProperties(
        @field:NotBlank
        val stationId: String,

        @field:NotBlank
        val displayName: String,

        @field:DecimalMin("162.400")
        @field:DecimalMax("162.550")
        val frequencyMHz: Double,

        val enabled: Boolean = true,

        val countyCodes: List<String> = emptyList(),
    )
}