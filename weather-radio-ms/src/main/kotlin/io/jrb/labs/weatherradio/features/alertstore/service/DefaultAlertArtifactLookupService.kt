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
import io.jrb.labs.weatherradio.features.alertstore.api.AlertArtifactResponse
import io.jrb.labs.weatherradio.features.alertstore.port.AlertStoreRepository
import org.springframework.http.MediaType
import java.nio.file.Files
import java.nio.file.Path

class DefaultAlertArtifactLookupService(
    private val datafill: AlertStoreDatafill,
    private val repository: AlertStoreRepository
) : AlertArtifactLookupService {

    override fun listArtifacts(alertId: String): List<AlertArtifactResponse>? {
        val record = repository.findByAlertId(alertId) ?: return null
        return record.artifacts.map { artifact ->
            val resolved = when (artifact.artifactType) {
                "audio-file" -> resolvePath(artifact.details["filePath"] as? String)
                "transcript-file" -> resolvePath(artifact.details["textFilePath"] as? String)
                else -> null
            }

            val contentType = when (artifact.artifactType) {
                "audio-file" -> "audio/wav"
                "transcript-file" -> "text/plain"
                else -> null
            }

            val downloadUrl = when (artifact.artifactType) {
                "audio-file" -> "/api/alerts/$alertId/artifacts/audio"
                "transcript-file" -> "/api/alerts/$alertId/artifacts/transcript.txt"
                else -> null
            }

            AlertArtifactResponse(
                artifactType = artifact.artifactType,
                createdAt = artifact.createdAt.toString(),
                exists = resolved?.let { Files.exists(it) } ?: false,
                contentType = contentType,
                sizeBytes = resolved
                    ?.takeIf { Files.exists(it) }
                    ?.let { Files.size(it) },
                downloadUrl = downloadUrl,
                details = when (artifact.artifactType) {
                    "transcript-file" -> artifact.details + mapOf(
                        "textDownloadUrl" to "/api/alerts/$alertId/artifacts/transcript.txt",
                        "jsonDownloadUrl" to "/api/alerts/$alertId/artifacts/transcript.json",
                    )
                    else -> artifact.details
                }
            )
        }
    }

    override fun resolveAudio(alertId: String): ResolvedArtifactFile? =
        resolveLatest(alertId, "audio-file", "filePath", MediaType.parseMediaType("audio/wav"), "audio.wav")

    override fun resolveTranscriptText(alertId: String): ResolvedArtifactFile? =
        resolveLatest(alertId, "transcript-file", "textFilePath", MediaType.TEXT_PLAIN, "transcript.txt")

    override fun resolveTranscriptJson(alertId: String): ResolvedArtifactFile? =
        resolveLatest(alertId, "transcript-file", "jsonFilePath", MediaType.APPLICATION_JSON, "transcript.json")

    private fun resolveLatest(
        alertId: String,
        artifactType: String,
        detailKey: String,
        contentType: MediaType,
        downloadName: String,
    ): ResolvedArtifactFile? {
        val record = repository.findByAlertId(alertId) ?: return null
        val rawPath = record.artifacts
            .lastOrNull { it.artifactType == artifactType }
            ?.details
            ?.get(detailKey) as? String
            ?: return null

        val path = resolvePath(rawPath) ?: return null
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return null
        }

        return ResolvedArtifactFile(
            path = path,
            contentType = contentType,
            downloadName = downloadName,
        )
    }

    private fun resolvePath(rawPath: String?): Path? {
        if (rawPath.isNullOrBlank()) return null

        val resolved = runCatching { Path.of(rawPath).normalize().toAbsolutePath() }.getOrNull()
            ?: return null

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

}