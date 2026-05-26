package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.question.QuestionScreenUiState
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.viewmodel.consumeDesktopPendingContentOpenFrom
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberQuestionScreenRuntime(): QuestionScreenRuntime {
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    val shareRuntime = rememberShareDialogRuntime()
    val store = DesktopAccountStore()
    val historyStorage = DesktopHistoryStorage()
    val contentFilterDatabase = remember {
        getContentFilterDatabase(desktopContentFilterDatabaseFile())
    }
    return remember(settings, userMessages, shareRuntime) {
        QuestionScreenRuntime(
            loadQuestion = { question ->
                addDesktopReadHistory(store, question.questionId.toString(), "question")
                val questionData = fetchDesktopQuestionDetail(store, question)
                if (questionData != null) {
                    val historyDestination = Question(question.questionId, questionData.title)
                    historyStorage.add(historyDestination)
                    ContentOpenEventSupport.recordOpenEvent(
                        database = contentFilterDatabase,
                        destination = question,
                        questionId = question.questionId,
                        openFrom = consumeDesktopPendingContentOpenFrom(question),
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
                openDesktopExternalUrl("https://www.zhihu.com/question/${question.questionId}/log")
            },
            handleShareAction = { question, onShowDialog ->
                handleShareAction(question, settings, shareRuntime, onShowDialog)
            },
            showShortMessage = { message -> userMessages.showShortMessage(message) },
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

private suspend fun fetchDesktopQuestionDetail(
    store: DesktopAccountStore,
    question: Question,
): DataHolder.Question? {
    val account = store.load()
    val apiUrl = "https://www.zhihu.com/api/v4/questions/${question.questionId}" +
        "?include=read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics"

    return runCatching {
        val jo = store.fetchAuthenticatedJson(apiUrl) {
            account.cookies["d_c0"]?.let { dc0 ->
                signZhihuFetchRequest(dc0 = dc0)
            }
        } ?: return@runCatching null
        val jojo = buildJsonObject {
            jo.entries.forEach { (key, value) ->
                if (key == "id") {
                    put(key, JsonPrimitive(value.jsonPrimitive.long))
                } else {
                    put(key, value)
                }
            }
        }
        ZhihuJson.decodeJson<DataHolder.Question>(jojo)
    }.getOrNull()
}

private fun openDesktopExternalUrl(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}

private suspend fun addDesktopReadHistory(
    store: DesktopAccountStore,
    contentToken: String,
    contentType: String,
) {
    store.addReadHistory(
        contentToken = contentToken,
        contentTypeName = contentType,
    )
}
