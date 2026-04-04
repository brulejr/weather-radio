/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Jon Brule
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

package io.jrb.labs.weatherradio.features.alertstore.support

import com.fasterxml.jackson.databind.ObjectMapper
import io.jrb.labs.weatherradio.features.alertstore.model.StoredAdminOperationRecord
import io.jrb.labs.weatherradio.features.alertstore.port.AlertStoreAdminRepository
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentLinkedDeque

class FileAlertStoreAdminRepository(
    private val objectMapper: ObjectMapper,
    private val filePath: Path,
    private val maxEntries: Int,
) : AlertStoreAdminRepository {

    private val records = ConcurrentLinkedDeque<StoredAdminOperationRecord>()

    init {
        Files.createDirectories(filePath.parent)
        if (!Files.exists(filePath)) {
            Files.createFile(filePath)
        }
        loadExisting()
    }

    override fun append(record: StoredAdminOperationRecord) {
        val json = objectMapper.writeValueAsString(record)
        Files.writeString(
            filePath,
            json + System.lineSeparator(),
            StandardCharsets.UTF_8,
            StandardOpenOption.APPEND
        )

        records.addFirst(record)
        trimToMaxEntries()
    }

    override fun findRecent(category: String?, limit: Int): List<StoredAdminOperationRecord> =
        records.asSequence()
            .filter { category == null || it.category == category }
            .take(limit)
            .toList()

    override fun findLatest(category: String?): StoredAdminOperationRecord? =
        records.firstOrNull { category == null || it.category == category }

    private fun loadExisting() {
        Files.readAllLines(filePath, StandardCharsets.UTF_8)
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .mapNotNull { line ->
                runCatching {
                    objectMapper.readValue(line, StoredAdminOperationRecord::class.java)
                }.getOrNull()
            }
            .forEach { records.addFirst(it) }

        val newestFirst = records.toList().sortedByDescending { it.startedAt }
        records.clear()
        newestFirst.forEach { records.addLast(it) }
        trimToMaxEntries()
    }

    private fun trimToMaxEntries() {
        while (records.size > maxEntries) {
            records.pollLast()
        }
    }
}