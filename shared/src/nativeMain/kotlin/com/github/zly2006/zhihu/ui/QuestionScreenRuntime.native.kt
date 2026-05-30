package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable

@Composable
actual fun QuestionDetailWebViewContent(questionId: Long, html: String) = Unit // TODO: iOS 问题 WebView 实现

actual fun supportsQuestionDetailWebView(): Boolean = false
