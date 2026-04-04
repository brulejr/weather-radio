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
import io.jrb.labs.weatherradio.features.alertstore.model.StoredAdminOperationRecord
import io.jrb.labs.weatherradio.features.alertstore.port.AlertStoreAdminRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/alerts/artifacts/prune")
@ConditionalOnProperty(
    prefix = CONFIG_PREFIX_ALERT_STORE,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class AlertArtifactPruneHistoryController(
    private val adminRepository: AlertStoreAdminRepository,
) {

    @GetMapping("/history")
    fun history(
        @RequestParam(defaultValue = "25") limit: Int,
    ): ResponseEntity<List<StoredAdminOperationRecord>> =
        ResponseEntity.ok(
            adminRepository.findRecent(
                category = "artifact-prune",
                limit = limit.coerceIn(1, 200),
            )
        )

    @GetMapping("/history/latest")
    fun latest(): ResponseEntity<StoredAdminOperationRecord> =
        adminRepository.findLatest(category = "artifact-prune")
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
}