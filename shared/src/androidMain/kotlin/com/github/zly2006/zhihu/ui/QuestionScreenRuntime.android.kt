package com.github.zly2006.zhihu.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import org.jsoup.Jsoup

private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"

@Composable
actual fun rememberQuestionScreenRuntime(): QuestionScreenRuntime {
    val context = LocalContext.current
    val settings = rememberSettingsStore()
    val shareRuntime = rememberShareDialogRuntime()
    return remember(context, settings, shareRuntime) {
        val userMessages = androidUserMessageSink(context)
        QuestionScreenRuntime(
            openLog = { question ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = "https://www.zhihu.com/question/${question.questionId}/log".toUri()
                    setClassName(context, WEBVIEW_ACTIVITY_CLASS)
                }
                context.startActivity(intent)
            },
            handleShareAction = { question, onShowDialog ->
                handleShareAction(question, settings, shareRuntime, onShowDialog)
            },
            showShortMessage = { message ->
                userMessages.showShortMessage(message)
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
