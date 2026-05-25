package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.addZhihuReadHistory
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.question.QuestionScreenUiState
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.ui.components.ShareDialogContent
import com.github.zly2006.zhihu.ui.components.getShareText
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

@Composable
actual fun rememberQuestionScreenRuntime(): QuestionScreenRuntime {
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    val store = DesktopAccountStore()
    val historyStorage = DesktopHistoryStorage()
    return remember(settings, userMessages) {
        QuestionScreenRuntime(
            loadQuestion = { question ->
                addDesktopReadHistory(store, question.questionId.toString(), "question")
                val questionData = fetchDesktopQuestionDetail(store, question)
                if (questionData != null) {
                    val historyDestination = Question(question.questionId, questionData.title)
                    historyStorage.add(historyDestination)
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
                handleDesktopShareAction(
                    shareText = getShareText(question),
                    settings = settings,
                    userMessages = userMessages,
                    onShowDialog = onShowDialog,
                )
            },
            showShortMessage = { message -> println(message) },
        )
    }
}

@Composable
actual fun QuestionDetailContent(
    questionId: Long,
    html: String,
) {
    RenderMarkdown(html)
}

@Composable
actual fun QuestionShareDialog(
    content: Question,
    shareText: String,
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()

    ShareDialogContent(
        showDialog = showDialog,
        onDismissRequest = onDismissRequest,
        onShareClick = {
            onDismissRequest()
            copyDesktopText(shareText)
            userMessages.showMessage("已复制分享文本")
        },
        onCopyClick = {
            onDismissRequest()
            copyDesktopText(shareText)
            userMessages.showMessage("已复制链接")
        },
        onSettingsClick = {
            onDismissRequest()
            navigator.onNavigate(Account.AppearanceSettings(setting = "shareAction"))
        },
    )
}

private fun copyDesktopText(text: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}

private fun handleDesktopShareAction(
    shareText: String?,
    settings: com.github.zly2006.zhihu.shared.platform.SettingsStore,
    userMessages: com.github.zly2006.zhihu.shared.platform.UserMessageSink,
    onShowDialog: () -> Unit,
) {
    when (settings.getString("shareActionMode", "ask")) {
        "copy" -> {
            if (shareText != null) {
                copyDesktopText(shareText)
                userMessages.showMessage("已复制链接")
            }
        }
        "share" -> {
            if (shareText != null) {
                copyDesktopText(shareText)
                userMessages.showMessage("已复制分享文本")
            }
        }
        else -> onShowDialog()
    }
}

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
    val account = store.load()
    val dc0 = account.cookies["d_c0"] ?: return
    store.createHttpClient(account.cookies).use { client ->
        addZhihuReadHistory(
            client = client,
            contentToken = contentToken,
            contentType = contentType,
            dc0 = dc0,
        )
    }
}
