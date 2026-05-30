package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberQuestionScreenRuntime(): QuestionScreenRuntime { // TODO: iOS 问题页面完整实现
    return remember {
        QuestionScreenRuntime(
            handleShareAction = { _, _ -> error("Question share not available on iOS yet") },
        )
    }
}

@Composable
actual fun QuestionDetailWebViewContent(questionId: Long, html: String) = Unit // TODO: iOS 问题 WebView 实现

actual fun supportsQuestionDetailWebView(): Boolean = false
