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

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.github.zly2006.zhihu.viewmodel.local

import androidx.room.Room
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory

private val macosLocalContentDatabase by lazy {
    val dataDirectory = "${NSHomeDirectory()}/.zhihu-plus"
    NSFileManager.defaultManager.createDirectoryAtPath(
        dataDirectory,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    buildLocalContentDatabase(
        Room.databaseBuilder<LocalContentDatabase>(
            name = "$dataDirectory/local-content.db",
        ),
    )
}

internal actual fun nativeLocalContentDatabase(): LocalContentDatabase? = macosLocalContentDatabase
