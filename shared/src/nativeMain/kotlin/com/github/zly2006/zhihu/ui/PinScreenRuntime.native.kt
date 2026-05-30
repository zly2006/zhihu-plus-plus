package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime =
    remember {
        // TODO: iOS 想法页面完整实现
        PinScreenRuntime(
            fetchLinkCardPreview = { null },
        )
    }

@Composable
actual fun PinHtmlWebViewContent(html: String) = Unit // TODO: iOS 想法 WebView 实现

actual fun supportsPinHtmlWebView(): Boolean = false
