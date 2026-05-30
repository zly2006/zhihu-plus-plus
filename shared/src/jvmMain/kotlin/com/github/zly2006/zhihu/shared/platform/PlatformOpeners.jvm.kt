package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember {
    { url ->
        runCatching {
            openDesktopExternalUrl(url)
        }
    }
}

@Composable
actual fun rememberZhihuWebUrlOpener(): (String) -> Unit = rememberExternalUrlOpener()

@Composable
actual fun rememberImagePreviewOpener(): (String) -> Unit = rememberExternalUrlOpener()
