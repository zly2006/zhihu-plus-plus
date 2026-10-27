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

package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.github.zly2006.zhihu.markdown.MAX_MATH_FONT_BYTES
import com.github.zly2006.zhihu.platform.nativeChooseImportFilePath
import com.github.zly2006.zhihu.platform.nativeIsDesktop
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

@Composable
internal actual fun rememberMathFontPicker(onPicked: (String, ByteArray) -> Unit): (() -> Unit)? {
    val scope = rememberCoroutineScope()
    val messages = rememberUserMessageSink()
    if (!nativeIsDesktop) return null
    return {
        val selected = nativeChooseImportFilePath("导入公式字体")
        if (selected != null) {
            scope.launch {
                try {
                    val bytes = withContext(Dispatchers.Default) {
                        val file = Path(selected)
                        require((SystemFileSystem.metadataOrNull(file)?.size ?: 0) in 1..MAX_MATH_FONT_BYTES.toLong()) { "字体文件不能超过 20 MB" }
                        SystemFileSystem.source(file).buffered().use { it.readByteArray() }
                    }
                    onPicked(selected.substringAfterLast('/'), bytes)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    messages.showLongMessage(error.message ?: "读取字体失败")
                }
            }
        }
    }
}
