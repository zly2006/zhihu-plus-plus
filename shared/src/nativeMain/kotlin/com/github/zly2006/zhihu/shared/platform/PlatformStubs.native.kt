package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.ui.noopSettingsStore

// TODO: iOS 返回手势处理
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

// TODO: iOS 屏幕尺寸获取
@Composable
actual fun rememberScreenSizeDp(): ScreenSizeDp = ScreenSizeDp(width = 0f, height = 0f)

@Composable
actual fun rememberSettingsStore(): SettingsStore = noopSettingsStore()

// TODO: iOS 变体判断
@Composable
actual fun rememberIsLiteVariant(): Boolean = false

@Composable
actual fun rememberUserMessageSink(): UserMessageSink = remember {
    UserMessageSink(showShortMessage = {}, showLongMessage = {})
}
