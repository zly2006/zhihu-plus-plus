package com.github.zly2006.zhihu.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.getContentDetail
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.zhihuQuestionUrl
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.question.QuestionScreenUiState
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
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
            loadQuestion = { question ->
                AccountData.addReadHistory(context, question.questionId.toString(), "question")
                val questionData = DataHolder.getContentDetail(context, question)
                if (questionData != null) {
                    val historyDestination = Question(question.questionId, questionData.title)
                    val articleHost = context.articleHost()
                    articleHost?.postHistoryDestination(historyDestination)
                    ContentOpenEventSupport.recordOpenEvent(
                        database = getContentFilterDatabase(context),
                        destination = question,
                        questionId = question.questionId,
                        openFrom = articleHost?.consumePendingContentOpenFrom(question) ?: ContentOpenFrom.UNKNOWN,
                    )
                    LoadedQuestionScreenData(
                        uiState = QuestionScreenUiState(
                            questionContent = questionData.detail,
                            answerCount = questionData.answerCount,
                            visitCount = questionData.visitCount,
                            commentCount = questionData.commentCount,
                            followerCount = questionData.followerCount,
                            title = questionData.title,
                            isFollowing = questionData.relationship.isFollowing,
                        ),
                        historyDestination = historyDestination,
                    )
                } else {
                    null
                }
            },
            openLog = { question ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = zhihuQuestionLogUrl(question).toUri()
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
            zhihuQuestionUrl(questionId),
            Jsoup.parse(html),
        )
    }
}

actual fun supportsQuestionDetailWebView(): Boolean = true
