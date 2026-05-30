package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberExternalUrlOpener(): (String) -> Unit

@Composable
expect fun rememberSystemUrlOpener(): (String) -> Unit

@Composable
expect fun rememberZhihuWebUrlOpener(): (String) -> Unit

@Composable
expect fun rememberImagePreviewOpener(): (String) -> Unit

@Composable
expect fun rememberPlainTextClipboard(): (label: String, text: String) -> Unit
