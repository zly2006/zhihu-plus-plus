package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO: iOS 问题页面完整实现
@Composable
actual fun rememberQuestionScreenRuntime(): QuestionScreenRuntime = remember {
    QuestionScreenRuntime(
        openLog = { error("Question log not available on iOS yet") },
        handleShareAction = { _, _ -> error("Question share not available on iOS yet") },
    )
}

// TODO: iOS 问题 WebView 实现
@Composable
actual fun QuestionDetailWebViewContent(questionId: Long, html: String) = Unit

actual fun supportsQuestionDetailWebView(): Boolean = false
