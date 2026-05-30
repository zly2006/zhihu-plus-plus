package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime

@Composable
actual fun rememberQuestionScreenRuntime(): QuestionScreenRuntime {
    val settings = rememberSettingsStore()
    val shareRuntime = rememberShareDialogRuntime()
    return remember(settings, shareRuntime) {
        QuestionScreenRuntime(
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
) = Unit // TODO: desktop question WebView

actual fun supportsQuestionDetailWebView(): Boolean = false
