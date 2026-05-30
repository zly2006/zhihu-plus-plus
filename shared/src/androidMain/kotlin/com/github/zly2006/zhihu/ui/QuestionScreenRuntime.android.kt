package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import org.jsoup.Jsoup

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
) {
    WebviewComp {
        it.loadZhihu(
            "https://www.zhihu.com/question/$questionId",
            Jsoup.parse(html),
        )
    }
}

actual fun supportsQuestionDetailWebView(): Boolean = true
