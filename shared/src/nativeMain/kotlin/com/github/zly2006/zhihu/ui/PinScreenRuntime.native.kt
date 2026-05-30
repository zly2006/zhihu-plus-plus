package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO: iOS 想法页面完整实现
@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime =
    remember {
        PinScreenRuntime(
            handleShareAction = { _, _ -> error("Pin share not available on iOS yet") },
            fetchLinkCardPreview = { null },
        )
    }

@Composable
actual fun PinHtmlWebViewContent(html: String) = Unit // TODO: iOS 想法 WebView 实现

actual fun supportsPinHtmlWebView(): Boolean = false
