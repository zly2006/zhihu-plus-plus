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

package com.github.zly2006.zhihu.shared.platform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.ui.noopSettingsStore
import com.github.zly2006.zhihu.ui.openIosUrl

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember {
    ::openIosUrl
}

@Composable
actual fun rememberSystemUrlOpener(): (String) -> Unit = rememberExternalUrlOpener()

@Composable
actual fun rememberZhihuWebUrlOpener(): (String) -> Unit = rememberExternalUrlOpener()

@Composable
actual fun rememberImagePreviewOpener(): (String) -> Unit = rememberExternalUrlOpener()

@Composable
actual fun rememberPlainTextClipboard(): (label: String, text: String) -> Unit = remember {
    { _, text ->
        platform.UIKit.UIPasteboard.generalPasteboard.string = text
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit // TODO: iOS 返回手势处理

@Composable
actual fun rememberScreenSizeDp(): ScreenSizeDp = ScreenSizeDp(width = 0f, height = 0f) // TODO: iOS 屏幕尺寸获取

@Composable
actual fun rememberSettingsStore(): SettingsStore = noopSettingsStore() // TODO: iOS 设置存储

@Composable
actual fun rememberIsLiteVariant(): Boolean = false // TODO: iOS 变体判断

@Composable
actual fun rememberUserMessageSink(): UserMessageSink = remember {
    UserMessageSink(showShortMessage = {})
} // TODO: iOS 用户消息提示
