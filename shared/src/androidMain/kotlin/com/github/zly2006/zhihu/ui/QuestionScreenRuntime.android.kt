package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.ui.components.WebviewComp
import org.jsoup.Jsoup

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
