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

package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Room
import java.io.File

fun desktopContentFilterDatabaseFile(): File =
    File(File(System.getProperty("user.home"), ".zhihu-plus"), "content-filter.db")

private val desktopContentFilterDatabase by lazy {
    getContentFilterDatabase(desktopContentFilterDatabaseFile().also { it.parentFile?.mkdirs() })
}

actual fun getContentFilterDatabase(): ContentFilterDatabase = desktopContentFilterDatabase

fun getContentFilterDatabase(databaseFile: File): ContentFilterDatabase =
    buildContentFilterDatabase(
        Room.databaseBuilder<ContentFilterDatabase>(
            name = databaseFile.absolutePath,
        ),
    )
