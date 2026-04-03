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

import io.jrb.labs.weatherradio.features.FeatureDescriptors.CONFIG_PREFIX_ALERT_STORE
import io.jrb.labs.weatherradio.features.alertstore.service.AlertArtifactLookupService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/alerts/{alertId}/artifacts")
@ConditionalOnProperty(
    prefix = CONFIG_PREFIX_ALERT_STORE,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class AlertArtifactController(
    private val lookupService: AlertArtifactLookupService,
) {

    @GetMapping
    fun listArtifacts(
        @PathVariable alertId: String,
    ): ResponseEntity<List<AlertArtifactResponse>> {
        val artifacts = lookupService.listArtifacts(alertId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(artifacts)
    }

    @GetMapping("/audio")
    fun getAudioArtifact(
        @PathVariable alertId: String,
    ): ResponseEntity<Resource> =
        serve(lookupService.resolveAudio(alertId))

    @GetMapping("/transcript.txt")
    fun getTranscriptTextArtifact(
        @PathVariable alertId: String,
    ): ResponseEntity<Resource> =
        serve(lookupService.resolveTranscriptText(alertId))

    @GetMapping("/transcript.json")
    fun getTranscriptJsonArtifact(
        @PathVariable alertId: String,
    ): ResponseEntity<Resource> =
        serve(lookupService.resolveTranscriptJson(alertId))

    @GetMapping("/transcript")
    fun getTranscriptArtifactAlias(
        @PathVariable alertId: String,
    ): ResponseEntity<Resource> =
        serve(lookupService.resolveTranscriptText(alertId))

    private fun serve(
        resolved: io.jrb.labs.weatherradio.features.alertstore.service.ResolvedArtifactFile?
    ): ResponseEntity<Resource> {
        val file = resolved ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(file.contentType)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline().filename(file.downloadName).build().toString()
            )
            .body(FileSystemResource(file.path))
    }

}