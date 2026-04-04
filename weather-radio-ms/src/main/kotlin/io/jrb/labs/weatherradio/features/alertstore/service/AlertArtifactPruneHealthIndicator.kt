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

package io.jrb.labs.weatherradio.features.alertstore.service

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator

class AlertArtifactPruneHealthIndicator(
    private val statusService: AlertArtifactPruneStatusService,
) : HealthIndicator {

    override fun health(): Health {
        val snapshot = statusService.snapshot()
        val lastRun = snapshot.lastRun

        if (!snapshot.pruningEnabled) {
            return Health.up()
                .withDetail("pruningEnabled", false)
                .withDetail("reason", "Artifact pruning disabled")
                .build()
        }

        if (lastRun == null) {
            return Health.unknown()
                .withDetail("pruningEnabled", true)
                .withDetail("reason", "No prune run recorded yet")
                .build()
        }

        return if (lastRun.success) {
            Health.up()
                .withDetail("pruningEnabled", true)
                .withDetail("lastRunSource", lastRun.source)
                .withDetail("startedAt", lastRun.startedAt)
                .withDetail("completedAt", lastRun.completedAt)
                .withDetail("result", lastRun.result)
                .build()
        } else {
            Health.down()
                .withDetail("pruningEnabled", true)
                .withDetail("lastRunSource", lastRun.source)
                .withDetail("startedAt", lastRun.startedAt)
                .withDetail("completedAt", lastRun.completedAt)
                .withDetail("error", lastRun.error)
                .build()
        }
    }
}