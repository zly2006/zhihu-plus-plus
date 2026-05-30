package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime

@Composable
actual fun rememberQuestionScreenRuntime(): QuestionScreenRuntime {
    val settings = rememberSettingsStore()
    val shareRuntime = rememberShareDialogRuntime()
    return remember(settings, shareRuntime) {
        QuestionScreenRuntime(
            openLog = { question ->
                openDesktopExternalUrl("https://www.zhihu.com/question/${question.questionId}/log")
            },
            handleShareAction = { question, onShowDialog ->
                handleShareAction(question, settings, shareRuntime, onShowDialog)
            },
        )
    }
}

@Composable
actual fun QuestionDetailWebViewContent(
    questionId: Long,
    html: String,
) {
}

actual fun supportsQuestionDetailWebView(): Boolean = false
