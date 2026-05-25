/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Volatile
private var blocklistManager: BlocklistManager? = null

fun getBlocklistManager(context: Context): BlocklistManager =
    blocklistManager ?: synchronized(BlocklistManager::class) {
        blocklistManager ?: getContentFilterDatabase(context.applicationContext)
            .createBlocklistManager()
            .also {
                blocklistManager = it
            }
    }

suspend fun BlocklistManager.exportAllBlocklistToJson(context: Context): File = withContext(Dispatchers.IO) {
    val dir = context.getExternalFilesDir(null) ?: context.filesDir
    val file = File(dir, "zhihupp_blocklist.json")
    file.writeText(exportAllBlocklistToJsonText())
    file
}

suspend fun BlocklistManager.importAllBlocklistFromJson(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val text = context.contentResolver
        .openInputStream(uri)
        ?.bufferedReader()
        ?.readText()
        ?: return@withContext "读取文件失败"
    importAllBlocklistFromJsonText(text)
}
