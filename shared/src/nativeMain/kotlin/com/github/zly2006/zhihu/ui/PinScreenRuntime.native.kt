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
            handleShareAction = { _, _ -> error("Pin share not available on iOS yet") },
            fetchLinkCardPreview = { null },
            openExternalUrl = { error("Pin external URL not available on iOS yet") },
        )
    }
}

// TODO: iOS 想法 WebView 实现
@Composable
actual fun PinHtmlWebViewContent(html: String) = Unit

actual fun supportsPinHtmlWebView(): Boolean = false
