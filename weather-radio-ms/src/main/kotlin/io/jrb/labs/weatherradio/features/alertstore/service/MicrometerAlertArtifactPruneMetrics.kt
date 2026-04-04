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

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit

class MicrometerAlertArtifactPruneMetrics(
    private val registry: MeterRegistry,
) : AlertArtifactPruneMetrics {

    override fun recordSuccess(source: String, result: ArtifactPruneResult, durationMillis: Long) {
        registry.counter("weatherradio.alertstore.prune.runs", "source", source, "outcome", "success").increment()
        registry.counter("weatherradio.alertstore.prune.alerts_pruned", "source", source).increment(result.alertsPruned.toDouble())
        registry.counter("weatherradio.alertstore.prune.artifacts_removed", "source", source).increment(result.artifactsRemoved.toDouble())
        registry.counter("weatherradio.alertstore.prune.files_deleted", "source", source).increment(result.filesDeleted.toDouble())
        registry.counter("weatherradio.alertstore.prune.files_missing", "source", source).increment(result.filesMissing.toDouble())
        registry.counter("weatherradio.alertstore.prune.files_rejected", "source", source).increment(result.filesRejected.toDouble())

        Timer.builder("weatherradio.alertstore.prune.duration")
            .tag("source", source)
            .tag("outcome", "success")
            .register(registry)
            .record(durationMillis, TimeUnit.MILLISECONDS)
    }

    override fun recordFailure(source: String, durationMillis: Long, throwable: Throwable) {
        registry.counter("weatherradio.alertstore.prune.runs", "source", source, "outcome", "failure").increment()

        Timer.builder("weatherradio.alertstore.prune.duration")
            .tag("source", source)
            .tag("outcome", "failure")
            .register(registry)
            .record(durationMillis, TimeUnit.MILLISECONDS)
    }
}