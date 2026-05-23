package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.question.QuestionScreenUiState
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberQuestionScreenRuntime(): QuestionScreenRuntime = remember {
    val store = DesktopAccountStore()
    QuestionScreenRuntime(
        loadQuestion = { question ->
            val questionData = fetchDesktopQuestionDetail(store, question)
            if (questionData != null) {
                val historyDestination = Question(question.questionId, questionData.title)
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
        handleShareAction = { _, onShowDialog -> onShowDialog() },
        showShortMessage = { message -> println(message) },
    )
}

@Composable
actual fun QuestionDetailContent(
    questionId: Long,
    html: String,
) {
    RenderMarkdown(html)
}

@Composable
actual fun QuestionCommentsSheet(
    showComments: Boolean,
    onDismiss: () -> Unit,
    content: Question,
) {
}

@Composable
actual fun QuestionShareDialog(
    content: Question,
    shareText: String,
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
}

private suspend fun fetchDesktopQuestionDetail(
    store: DesktopAccountStore,
    question: Question,
): DataHolder.Question? {
    val account = store.load()
    val apiUrl = "https://www.zhihu.com/api/v4/questions/${question.questionId}" +
        "?include=read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics"

    return runCatching {
        store.createHttpClient(account.cookies).use { client ->
            val jo = client
                .get(apiUrl) {
                    account.cookies["d_c0"]?.let { dc0 ->
                        signZhihuFetchRequest(dc0 = dc0)
                    }
                }.body<JsonObject>()
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
        }
    }.getOrNull()
}

private fun openDesktopExternalUrl(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}
