package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.data.decodeQuestionContentDetail
import com.github.zly2006.zhihu.data.zhihuQuestionContentDetailUrl
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.desktop.signDesktopRequest
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.viewmodel.consumeDesktopPendingContentOpenFrom
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase

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
                store.addReadHistory(contentToken = question.questionId.toString(), contentTypeName = "question")
                val questionData = fetchDesktopQuestionDetail(store, question)
                if (questionData != null) {
                    val loadedData = loadedQuestionScreenData(question, questionData)
                    historyStorage.add(loadedData.historyDestination)
                    ContentOpenEventSupport.recordOpenEvent(
                        database = contentFilterDatabase,
                        destination = question,
                        questionId = question.questionId,
                        openFrom = consumeDesktopPendingContentOpenFrom(question),
                    )
                    loadedData
                } else {
                    null
                }
            },
            openLog = { question ->
                openDesktopExternalUrl(zhihuQuestionLogUrl(question))
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
    val apiUrl = zhihuQuestionContentDetailUrl(question)

    return runCatching {
        val jo = store.fetchAuthenticatedJson(apiUrl) {
            signDesktopRequest(account.cookies)
        } ?: return@runCatching null
        decodeQuestionContentDetail(jo)
    }.getOrNull()
}
