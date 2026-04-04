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

import io.jrb.labs.weatherradio.features.alertstore.AlertStoreDatafill
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

class AlertArtifactPruneStatusService(
    private val datafill: AlertStoreDatafill,
    private val clock: Clock,
) {

    private val lastRunRef = AtomicReference<ArtifactPruneRunSummary?>(null)

    fun snapshot(): ArtifactPruneStatus =
        ArtifactPruneStatus(
            schedulerEnabled = datafill.artifactPruningSchedulerEnabled,
            runOnStartup = datafill.artifactPruningRunOnStartup,
            intervalMillis = datafill.artifactPruningIntervalMillis,
            pruningEnabled = datafill.artifactPruningEnabled,
            dryRun = datafill.artifactPruningDryRun,
            deleteArtifactFiles = datafill.deleteArtifactFiles,
            lastRun = lastRunRef.get(),
        )

    fun recordStart(source: String): Instant {
        val startedAt = clock.instant()
        lastRunRef.set(
            ArtifactPruneRunSummary(
                source = source,
                startedAt = startedAt.toString(),
                completedAt = null,
                success = false,
                result = null,
                error = null,
            )
        )
        return startedAt
    }

    fun recordSuccess(source: String, startedAt: Instant, result: ArtifactPruneResult) {
        lastRunRef.set(
            ArtifactPruneRunSummary(
                source = source,
                startedAt = startedAt.toString(),
                completedAt = clock.instant().toString(),
                success = true,
                result = result,
                error = null,
            )
        )
    }

    fun recordFailure(source: String, startedAt: Instant, throwable: Throwable) {
        lastRunRef.set(
            ArtifactPruneRunSummary(
                source = source,
                startedAt = startedAt.toString(),
                completedAt = clock.instant().toString(),
                success = false,
                result = null,
                error = throwable.message ?: throwable::class.java.simpleName,
            )
        )
    }
}