/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.withReadingQueueSource
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.json.Json

/** One synchronized owner for each history file; page environments never keep writable copies. */
class LocalHistoryStore private constructor(
    private val file: Path,
) {
    private val lock = reentrantLock()
    private val entries = linkedMapOf<NavDestination, NavDestination>()

    init {
        if (SystemFileSystem.exists(file)) {
            runCatching {
                SystemFileSystem.source(file).buffered().use { Json.decodeFromString<List<NavDestination>>(it.readString()) }
            }.getOrDefault(emptyList()).forEach { entry ->
                val normalized = entry.withReadingQueueSource(null)
                entries[normalized] = normalized
            }
        }
    }

    val history: List<NavDestination>
        get() = lock.withLock { entries.values.reversed() }

    fun add(destination: NavDestination) = lock.withLock {
        val entry = destination.withReadingQueueSource(null)
        entries.remove(entry)
        entries[entry] = entry
        while (entries.size > 1000) entries.remove(entries.keys.first())
        save()
    }

    fun clear() = lock.withLock {
        entries.clear()
        save()
    }

    private fun save() {
        file.parent?.let { SystemFileSystem.createDirectories(it) }
        val temporary = Path("$file.tmp")
        SystemFileSystem.sink(temporary).buffered().use { it.writeString(Json.encodeToString(entries.values.toList())) }
        SystemFileSystem.atomicMove(temporary, file)
    }

    companion object {
        private val stores = mutableMapOf<String, LocalHistoryStore>()
        private val storesLock = reentrantLock()

        fun forFile(filePath: String): LocalHistoryStore = storesLock.withLock {
            stores.getOrPut(filePath) { LocalHistoryStore(Path(filePath)) }
        }
    }
}
