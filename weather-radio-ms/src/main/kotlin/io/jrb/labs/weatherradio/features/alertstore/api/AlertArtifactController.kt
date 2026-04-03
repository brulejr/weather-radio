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

package io.jrb.labs.weatherradio.features.alertstore.api

import io.jrb.labs.weatherradio.features.alertstore.port.AlertStoreRepository
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.nio.file.Path

@RestController
@RequestMapping("/api/alerts/{alertId}/artifacts")
class AlertArtifactController(
    private val repository: AlertStoreRepository,
) {

    @GetMapping
    fun listArtifacts(
        @PathVariable alertId: String,
    ): ResponseEntity<List<Map<String, Any?>>> {
        val record = repository.findByAlertId(alertId)
            ?: return ResponseEntity.notFound().build()

        val body = record.artifacts.map {
            mapOf(
                "artifactType" to it.artifactType,
                "createdAt" to it.createdAt.toString(),
                "details" to it.details,
            )
        }

        return ResponseEntity.ok(body)
    }

    @GetMapping("/audio")
    fun getAudioArtifact(
        @PathVariable alertId: String,
    ): ResponseEntity<Resource> =
        serveArtifactFile(
            alertId = alertId,
            artifactType = "audio-file",
            detailKey = "filePath",
            contentType = MediaType.parseMediaType("audio/wav"),
            downloadName = "audio.wav",
        )

    @GetMapping("/transcript")
    fun getTranscriptArtifact(
        @PathVariable alertId: String,
    ): ResponseEntity<Resource> =
        serveArtifactFile(
            alertId = alertId,
            artifactType = "transcript-file",
            detailKey = "textFilePath",
            contentType = MediaType.TEXT_PLAIN,
            downloadName = "transcript.txt",
        )

    private fun serveArtifactFile(
        alertId: String,
        artifactType: String,
        detailKey: String,
        contentType: MediaType,
        downloadName: String,
    ): ResponseEntity<Resource> {
        val record = repository.findByAlertId(alertId)
            ?: return ResponseEntity.notFound().build()

        val pathValue = record.artifacts
            .lastOrNull { it.artifactType == artifactType }
            ?.details
            ?.get(detailKey) as? String
            ?: return ResponseEntity.notFound().build()

        val path = Path.of(pathValue)
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build()
        }

        val resource = FileSystemResource(path)

        return ResponseEntity.ok()
            .contentType(contentType)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline().filename(downloadName).build().toString()
            )
            .body(resource)
    }

}