package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember {
    { url -> openDesktopExternalUrl(url) }
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
        runCatching { copyDesktopPlainText(text) }
    }
}
