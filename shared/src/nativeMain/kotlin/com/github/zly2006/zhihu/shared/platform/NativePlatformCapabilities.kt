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

package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.ui.noopSettingsStore
import com.github.zly2006.zhihu.ui.openIosUrl

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember { ::openIosUrl }

@Composable
actual fun rememberSystemUrlOpener(): (String) -> Unit = rememberExternalUrlOpener()

@Composable
actual fun rememberZhihuWebUrlOpener(): (String) -> Unit = rememberExternalUrlOpener()

@Composable
actual fun rememberImagePreviewOpener(): (String) -> Unit = rememberExternalUrlOpener()

@Composable
actual fun rememberImageGalleryOpener(): (List<String>, Int) -> Unit {
    val openExternalUrl = rememberExternalUrlOpener()
    return remember(openExternalUrl) {
        { urls, initialIndex ->
            urls.getOrNull(initialIndex)?.let(openExternalUrl)
        }
    }
}

@Composable
actual fun rememberImageSaver(): (String) -> Unit {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        { userMessages.showMessage("iOS 图片保存暂未实现") } // TODO: iOS 图片保存
    }
}

@Composable
actual fun rememberImageSharer(): (String) -> Unit {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        { userMessages.showMessage("iOS 图片分享暂未实现") } // TODO: iOS 图片分享
    }
}

@Composable
actual fun rememberPlainTextClipboard(): (label: String, text: String) -> Unit = remember {
    { _, text ->
        platform.UIKit.UIPasteboard.generalPasteboard.string = text
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit // TODO: iOS 返回手势处理

@Composable
actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
) = PlatformBackHandler(enabled = enabled, onBack = onBack)

@Composable
actual fun rememberSettingsStore(): SettingsStore = noopSettingsStore() // TODO: iOS 设置存储

actual fun Modifier.exportTestTagsForUiAutomation(): Modifier = this

@Composable
actual fun rememberIsLiteVariant(): Boolean = false // TODO: iOS 变体判断

@Composable
actual fun rememberUserMessageSink(): UserMessageSink = remember { UserMessageSink(showShortMessage = {}) } // TODO: iOS 用户消息提示
