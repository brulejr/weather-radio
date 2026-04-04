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

import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration

class AlertArtifactPruneRunner(
    private val retentionService: AlertArtifactRetentionService,
    private val statusService: AlertArtifactPruneStatusService,
    private val metrics: AlertArtifactPruneMetrics,
    private val clock: Clock,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun run(source: String): ArtifactPruneResult {
        val startedAt = statusService.recordStart(source)
        val startInstant = clock.instant()

        return runCatching {
            retentionService.pruneArtifacts()
        }.onSuccess { result ->
            val durationMillis = Duration.between(startInstant, clock.instant()).toMillis()
            statusService.recordSuccess(source, startedAt, result)
            metrics.recordSuccess(source, result, durationMillis)

            log.info(
                "Artifact prune run source={} alertsScanned={} alertsEligible={} alertsPruned={} artifactsRemoved={} filesDeleted={} filesMissing={} filesRejected={} dryRun={} durationMillis={}",
                source,
                result.alertsScanned,
                result.alertsEligible,
                result.alertsPruned,
                result.artifactsRemoved,
                result.filesDeleted,
                result.filesMissing,
                result.filesRejected,
                result.dryRun,
                durationMillis,
            )
        }.onFailure { error ->
            val durationMillis = Duration.between(startInstant, clock.instant()).toMillis()
            statusService.recordFailure(source, startedAt, error)
            metrics.recordFailure(source, durationMillis, error)

            log.warn(
                "Artifact prune run failed source={} durationMillis={} reason={}",
                source,
                durationMillis,
                error.message,
            )
        }.getOrThrow()
    }
}