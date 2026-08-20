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

package com.github.zly2006.zhihu.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.github.zly2006.zhihu.account.NativeAccountStore
import com.github.zly2006.zhihu.data.toCookieHeaderString
import com.github.zly2006.zhihu.platform.nativeAppPrivateDirectoryPath
import com.hrm.latex.renderer.font.MathFont
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithBytes

private const val FONT_VERSION = "1"
private val LM_MATH_URLS = listOf(
    "https://mirrors.ustc.edu.cn/CTAN/fonts/lm-math/opentype/latinmodern-math.otf",
    "https://mirrors.tuna.tsinghua.edu.cn/CTAN/fonts/lm-math/opentype/latinmodern-math.otf",
)

@Composable
actual fun rememberMarkdownMathFont(): MathFont? {
    val store = remember { NativeAccountStore() }
    var mathFont by remember { mutableStateOf<MathFont?>(null) }

    LaunchedEffect(store) {
        mathFont = runCatching { loadNativeMathFont(store) }.getOrNull()
    }

    return mathFont
}

@Composable
actual fun rememberMarkdownImageRequestHeaders(): MarkdownImageRequestHeaders {
    val store = remember { NativeAccountStore() }
    val session = remember(store) { store.load() }
    return MarkdownImageRequestHeaders(
        cookieHeader = session.cookies.toCookieHeaderString(),
        userAgent = session.userAgent,
    )
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun loadNativeMathFont(store: NativeAccountStore): MathFont {
    val fontFilePath = "${nativeAppPrivateDirectoryPath()}/latex-fonts/v$FONT_VERSION/latinmodern-math.otf"
    val fontBytes = NSFileManager.defaultManager
        .contentsAtPath(fontFilePath)
        ?.toByteArray()
        ?.takeIf(::isOpenTypeFont)
        ?: downloadNativeMathFont(store, fontFilePath)
    val font = Font(
        identity = "Latin Modern Math",
        data = fontBytes,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
    )
    return MathFont.OTF(fontBytes, FontFamily(font))
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private suspend fun downloadNativeMathFont(store: NativeAccountStore, fontFilePath: String): ByteArray {
    var lastError: Exception? = null
    for (url in LM_MATH_URLS) {
        try {
            val bytes = store.httpClient().get(url).body<ByteArray>()
            if (!isOpenTypeFont(bytes)) continue
            val parentDirectory = fontFilePath.substringBeforeLast('/')
            val fileManager = NSFileManager.defaultManager
            fileManager.createDirectoryAtPath(
                parentDirectory,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
            bytes.usePinned { pinned ->
                fileManager.createFileAtPath(
                    fontFilePath,
                    contents = NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong()),
                    attributes = null,
                )
            }
            return bytes
        } catch (error: Exception) {
            lastError = error
        }
    }
    throw lastError ?: IllegalStateException("Failed to download Latin Modern Math")
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray =
    bytes?.reinterpret<ByteVar>()?.readBytes(length.toInt()) ?: ByteArray(0)

private fun isOpenTypeFont(bytes: ByteArray): Boolean =
    bytes.size > 4 &&
        bytes[0] == 0x4F.toByte() &&
        bytes[1] == 0x54.toByte() &&
        bytes[2] == 0x54.toByte() &&
        bytes[3] == 0x4F.toByte()
