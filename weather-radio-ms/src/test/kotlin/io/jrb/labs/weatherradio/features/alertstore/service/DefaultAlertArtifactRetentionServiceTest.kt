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
import io.jrb.labs.weatherradio.features.alertstore.model.StoredAlertArtifact
import io.jrb.labs.weatherradio.features.alertstore.model.StoredAlertRecord
import io.jrb.labs.weatherradio.features.alertstore.port.AlertStoreRepository
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant

class DefaultAlertArtifactRetentionService(
    private val datafill: AlertStoreDatafill,
    private val repository: AlertStoreRepository,
    private val clock: Clock,
) : AlertArtifactRetentionService {

    private val log = LoggerFactory.getLogger(javaClass)

    private val prunableArtifactTypes = setOf(
        "audio-file",
        "transcript-file",
    )

    override suspend fun pruneArtifacts(now: Instant): ArtifactPruneResult {
        if (!datafill.artifactPruningEnabled) {
            return ArtifactPruneResult(
                alertsScanned = 0,
                alertsEligible = 0,
                alertsPruned = 0,
                artifactsRemoved = 0,
                filesDeleted = 0,
                filesMissing = 0,
                filesRejected = 0,
                dryRun = datafill.artifactPruningDryRun,
            )
        }

        val records = repository.findRecent(datafill.artifactPruningScanLimit)

        var alertsEligible = 0
        var alertsPruned = 0
        var artifactsRemoved = 0
        var filesDeleted = 0
        var filesMissing = 0
        var filesRejected = 0

        records.forEach { record ->
            if (!isEligible(record, now)) {
                return@forEach
            }

            alertsEligible++

            val artifactsToRemove = prunableArtifacts(record)
            if (artifactsToRemove.isEmpty()) {
                return@forEach
            }

            val fileDeleteResult = deleteArtifactFiles(artifactsToRemove)
            val prunedRecord = pruneRecord(record)
            val removedCount = record.artifacts.size - prunedRecord.artifacts.size
            if (removedCount <= 0) {
                return@forEach
            }

            alertsPruned++
            artifactsRemoved += removedCount
            filesDeleted += fileDeleteResult.deleted
            filesMissing += fileDeleteResult.missing
            filesRejected += fileDeleteResult.rejected

            if (datafill.debugLogging) {
                log.info(
                    "Artifact pruning candidate alertId={} state={} removedArtifacts={} filesDeleted={} filesMissing={} filesRejected={} dryRun={} deleteArtifactFiles={}",
                    record.alertId,
                    record.state,
                    removedCount,
                    fileDeleteResult.deleted,
                    fileDeleteResult.missing,
                    fileDeleteResult.rejected,
                    datafill.artifactPruningDryRun,
                    datafill.deleteArtifactFiles,
                )
            }

            if (!datafill.artifactPruningDryRun) {
                repository.upsert(
                    prunedRecord.copy(
                        updatedAt = clock.instant()
                    )
                )
            }
        }

        return ArtifactPruneResult(
            alertsScanned = records.size,
            alertsEligible = alertsEligible,
            alertsPruned = alertsPruned,
            artifactsRemoved = artifactsRemoved,
            filesDeleted = filesDeleted,
            filesMissing = filesMissing,
            filesRejected = filesRejected,
            dryRun = datafill.artifactPruningDryRun,
        )
    }

    private fun isEligible(record: StoredAlertRecord, now: Instant): Boolean {
        val age = Duration.between(record.updatedAt, now)

        return when (record.state) {
            "EXPIRED" -> age >= Duration.ofHours(datafill.pruneExpiredAlertsAfterHours)
            "IGNORED" -> age >= Duration.ofHours(datafill.pruneIgnoredAlertsAfterHours)
            else -> false
        }
    }

    private fun pruneRecord(record: StoredAlertRecord): StoredAlertRecord =
        record.copy(
            artifacts = record.artifacts.filterNot(::shouldPruneArtifact)
        )

    private fun prunableArtifacts(record: StoredAlertRecord): List<StoredAlertArtifact> =
        record.artifacts.filter(::shouldPruneArtifact)

    private fun shouldPruneArtifact(artifact: StoredAlertArtifact): Boolean =
        artifact.artifactType in prunableArtifactTypes

    private fun deleteArtifactFiles(artifacts: List<StoredAlertArtifact>): FileDeleteResult {
        var deleted = 0
        var missing = 0
        var rejected = 0

        artifacts.forEach { artifact ->
            artifactPaths(artifact).forEach { rawPath ->
                val path = resolveAllowedPath(rawPath)
                if (path == null) {
                    rejected++
                    return@forEach
                }

                if (!Files.exists(path)) {
                    missing++
                    return@forEach
                }

                if (!Files.isRegularFile(path)) {
                    rejected++
                    return@forEach
                }

                if (!datafill.artifactPruningDryRun && datafill.deleteArtifactFiles) {
                    runCatching { Files.deleteIfExists(path) }
                        .onFailure {
                            if (datafill.debugLogging) {
                                log.warn(
                                    "Failed to delete artifact file path={} reason={}",
                                    path,
                                    it.message,
                                )
                            }
                        }
                        .getOrNull()
                }

                deleted++
            }
        }

        return FileDeleteResult(
            deleted = deleted,
            missing = missing,
            rejected = rejected,
        )
    }

    private fun artifactPaths(artifact: StoredAlertArtifact): List<String> =
        when (artifact.artifactType) {
            "audio-file" -> listOfNotNull(artifact.details["filePath"] as? String)
            "transcript-file" -> listOfNotNull(
                artifact.details["textFilePath"] as? String,
                artifact.details["jsonFilePath"] as? String,
            )
            else -> emptyList()
        }

    private fun resolveAllowedPath(rawPath: String?): Path? {
        if (rawPath.isNullOrBlank()) return null

        val resolved = runCatching {
            Path.of(rawPath).normalize().toAbsolutePath()
        }.getOrNull() ?: return null

        val roots = datafill.allowedRoots.mapNotNull {
            runCatching { Path.of(it).normalize().toAbsolutePath() }.getOrNull()
        }

        if (roots.isEmpty()) {
            return resolved
        }

        return resolved.takeIf { candidate ->
            roots.any { root -> candidate.startsWith(root) }
        }
    }

    private data class FileDeleteResult(
        val deleted: Int,
        val missing: Int,
        val rejected: Int,
    )
}