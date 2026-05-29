package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink

// TODO: iOS 想法页面完整实现
@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        PinScreenRuntime(
            loadPinDetail = { error("Pin detail not available on iOS yet") },
            toggleLike = { _, _, _ -> error("Pin like not available on iOS yet") },
            handleShareAction = { _, _ -> },
            fetchLinkCardPreview = { null },
            openExternalUrl = { openIosUrl(it) },
        )
    }
}

// TODO: iOS 想法 WebView 实现
@Composable
actual fun PinHtmlWebViewContent(html: String) = Unit

actual fun supportsPinHtmlWebView(): Boolean = false
