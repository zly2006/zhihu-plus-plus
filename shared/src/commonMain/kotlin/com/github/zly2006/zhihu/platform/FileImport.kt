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

package com.github.zly2006.zhihu.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray

@Composable
internal expect fun rememberFilePicker(
    title: String,
    mimeTypes: List<String>,
    extensions: List<String>,
    onPicked: (() -> Pair<String, RawSource>) -> Unit,
): () -> Unit

/** The picker supplies a lazy source so reading and closing always happen on the worker. */
@Composable
internal fun rememberFileImporter(
    title: String,
    mimeTypes: List<String> = listOf("*/*"),
    extensions: List<String> = emptyList(),
    maxBytes: Int? = null,
    userMessages: UserMessageSink = rememberUserMessageSink(),
    onImported: suspend (String, ByteArray) -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val currentOnImported by rememberUpdatedState(onImported)
    return rememberFilePicker(title, mimeTypes, extensions) { openFile ->
        scope.launch {
            try {
                val (name, bytes) = withContext(Dispatchers.Default) {
                    val (name, rawSource) = openFile()
                    name to rawSource.buffered().use { source ->
                        if (maxBytes != null) {
                            require(!source.request(maxBytes.toLong() + 1)) { "文件大小不能超过 ${maxBytes / 1024 / 1024} MB" }
                        }
                        source.readByteArray()
                    }
                }
                currentOnImported(name, bytes)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                userMessages.showLongMessage("导入失败：${error.message ?: "读取文件失败"}")
            }
        }
    }
}
