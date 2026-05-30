package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.rememberZhihuWebUrlOpener

@Composable
actual fun rememberQuestionScreenRuntime(): QuestionScreenRuntime { // TODO: iOS 问题页面完整实现
    val openZhihuWebUrl = rememberZhihuWebUrlOpener()
    return remember(openZhihuWebUrl) {
        QuestionScreenRuntime(
            openLog = { question ->
                openZhihuWebUrl("https://www.zhihu.com/question/${question.questionId}/log")
            },
            handleShareAction = { _, _ -> error("Question share not available on iOS yet") },
        )
    }
}

@Composable
actual fun QuestionDetailWebViewContent(questionId: Long, html: String) = Unit // TODO: iOS 问题 WebView 实现

actual fun supportsQuestionDetailWebView(): Boolean = false
