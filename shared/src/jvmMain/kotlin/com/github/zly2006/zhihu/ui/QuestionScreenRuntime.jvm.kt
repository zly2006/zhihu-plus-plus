package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable

@Composable
actual fun QuestionDetailWebViewContent(
    questionId: Long,
    html: String,
) = Unit // TODO: desktop question WebView

actual fun supportsQuestionDetailWebView(): Boolean = false
