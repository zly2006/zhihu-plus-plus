package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink

// TODO: iOS 问题页面完整实现
@Composable
actual fun rememberQuestionScreenRuntime(): QuestionScreenRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        QuestionScreenRuntime(
            openLog = { error("Question log not available on iOS yet") },
            handleShareAction = { _, _ -> error("Question share not available on iOS yet") },
            showShortMessage = { userMessages.showShortMessage(it) },
        )
    }
}

// TODO: iOS 问题 WebView 实现
@Composable
actual fun QuestionDetailWebViewContent(questionId: Long, html: String) = Unit

actual fun supportsQuestionDetailWebView(): Boolean = false
