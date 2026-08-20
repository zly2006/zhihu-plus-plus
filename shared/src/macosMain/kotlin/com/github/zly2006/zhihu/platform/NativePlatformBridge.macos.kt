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
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.data.macosAppDataDirectoryPath
import com.github.zly2006.zhihu.data.macosBackgroundUiDebugDataDirectoryPath
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.AppKit.NSModalResponseOK
import platform.AppKit.NSOpenPanel
import platform.AppKit.NSPasteboard
import platform.AppKit.NSPasteboardTypeString
import platform.AppKit.NSWorkspace
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSURL

internal actual val nativeIsDesktop: Boolean = true

private val macosQrLoginRequests = MutableStateFlow(0)

internal val macosQrLoginRequestVersion: StateFlow<Int> = macosQrLoginRequests.asStateFlow()

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember {
    { url -> NSURL.URLWithString(url)?.let(NSWorkspace.sharedWorkspace::openURL) }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun copyNativePlainText(text: String) {
    NSPasteboard.generalPasteboard.apply {
        clearContents()
        setString(text, forType = NSPasteboardTypeString)
    }
}

internal actual fun requestNativeQrLogin() {
    macosQrLoginRequests.update { it + 1 }
}

internal actual fun nativeAccountFilePath(): String =
    macosBackgroundUiDebugDataDirectoryPath()?.let { "$it/account.json" }
        ?: "${NSHomeDirectory()}/.zhihu-plus-plus/account.json"

internal actual fun nativeAppPrivateDirectoryPath(): String =
    macosAppDataDirectoryPath()

internal actual fun nativeDownloadsDirectoryPath(): String =
    macosBackgroundUiDebugDataDirectoryPath()?.let { "$it/Downloads" }
        ?: "${NSHomeDirectory()}/Downloads"

@OptIn(ExperimentalForeignApi::class)
internal actual fun nativeChooseBlocklistImportFilePath(): String? {
    val panel = NSOpenPanel.openPanel()
    panel.title = "导入屏蔽规则"
    panel.canChooseFiles = true
    panel.canChooseDirectories = false
    panel.allowsMultipleSelection = false
    return if (panel.runModal() == NSModalResponseOK) panel.URL?.path else null
}

internal data class MacosUserMessage(
    val text: String,
    val duration: UserMessageDuration,
)

internal val macosUserMessages = Channel<MacosUserMessage>(capacity = Channel.UNLIMITED)

fun showMacosUserMessage(
    message: String,
    duration: UserMessageDuration = UserMessageDuration.Short,
) {
    println(message)
    check(macosUserMessages.trySend(MacosUserMessage(message, duration)).isSuccess) {
        "macOS user message queue is unavailable"
    }
}
