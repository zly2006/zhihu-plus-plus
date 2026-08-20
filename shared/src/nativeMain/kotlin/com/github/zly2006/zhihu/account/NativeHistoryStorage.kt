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

@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.github.zly2006.zhihu.account

import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.platform.nativeAppPrivateDirectoryPath
import kotlinx.serialization.json.Json
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

internal class NativeHistoryStorage {
    private val filePath = "${nativeAppPrivateDirectoryPath()}/history.json"
    private val historyMap = linkedMapOf<NavDestination, NavDestination>()
    val history: List<NavDestination>
        get() = historyMap.values.reversed()

    init {
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(filePath)) {
            val data = fileManager.contentsAtPath(filePath)
            val text = data?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString() }
            runCatching {
                Json.decodeFromString<List<NavDestination>>(text.orEmpty())
            }.getOrDefault(emptyList()).forEach { historyMap[it] = it }
        }
    }

    fun add(destination: NavDestination) {
        historyMap.remove(destination)
        historyMap[destination] = destination
        while (historyMap.size > 1000) {
            historyMap.remove(historyMap.keys.first())
        }
        save()
    }

    fun clearAndSave() {
        historyMap.clear()
        save()
    }

    private fun save() {
        val parentDirectory = filePath.substringBeforeLast('/')
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(parentDirectory)) {
            fileManager.createDirectoryAtPath(
                parentDirectory,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }
        val text = Json.encodeToString(historyMap.values.toList())
        val data = NSString.create(string = text).dataUsingEncoding(NSUTF8StringEncoding)
        fileManager.createFileAtPath(filePath, contents = data, attributes = null)
    }
}
