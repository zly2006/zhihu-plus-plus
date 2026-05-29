package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.github.zly2006.zhihu.data.decodePinContentDetail
import com.github.zly2006.zhihu.data.decodeQuestionContentDetail
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.desktop.signedFetchJson
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.pin.PinLinkCardPreview
import com.github.zly2006.zhihu.shared.pin.PinScreenUiState
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.viewmodel.DesktopArticleViewModelRuntime
import com.github.zly2006.zhihu.viewmodel.consumeDesktopPendingContentOpenFrom
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import io.ktor.http.HttpMethod
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime {
    val scope = rememberCoroutineScope()
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    val shareRuntime = rememberShareDialogRuntime()
    val store = DesktopAccountStore()
    val historyStorage = DesktopHistoryStorage()
    val contentFilterDatabase = remember {
        getContentFilterDatabase(desktopContentFilterDatabaseFile())
    }
    return remember(scope, settings, userMessages, shareRuntime) {
        PinScreenRuntime(
            loadPinDetail = { pin ->
                store.addReadHistory(contentToken = pin.id.toString(), contentTypeName = "pin")
                val content = fetchDesktopPinDetail(store, pin)
                if (content == null) {
                    PinScreenUiState(isLoading = false, errorMessage = "无法加载想法详情")
                } else {
                    historyStorage.add(pin)
                    ContentOpenEventSupport.recordOpenEvent(
                        database = contentFilterDatabase,
                        destination = pin,
                        openFrom = consumeDesktopPendingContentOpenFrom(pin),
                    )
                    PinScreenUiState(
                        isLoading = false,
                        pinContent = content,
                        isLiked = content.virtuals.booleanCompat("isLiked", "is_liked"),
                        likeCount = content.likeCount,
                    )
                }
            },
            toggleLike = { pin, isLiked, onResult ->
                scope.launch {
                    try {
                        val method = if (isLiked) HttpMethod.Delete else HttpMethod.Post
                        val endpoint = "https://www.zhihu.com/api/v4/pins/${pin.id}/voters/up"
                        val jojo = fetchDesktopPinLike(store, endpoint, method) ?: return@launch
                        onResult(
                            PinLikeResult(
                                isLiked = !isLiked,
                                likeCount = jojo["liked_count"]?.jsonPrimitive?.intOrNull ?: -1,
                            ),
                        )
                    } catch (e: Exception) {
                        Log.e("PinScreenRuntime", "Failed to toggle pin like", e)
                    }
                }
            },
            handleShareAction = { pin, onShowDialog ->
                handleShareAction(pin, settings, shareRuntime, onShowDialog)
            },
            fetchLinkCardPreview = { linkCard ->
                fetchDesktopLinkCardPreview(store, userMessages, linkCard)
            },
            openExternalUrl = ::openDesktopExternalUrl,
        )
    }
}

@Composable
actual fun PinHtmlWebViewContent(html: String) {
}

actual fun supportsPinHtmlWebView(): Boolean = false

internal suspend fun fetchDesktopPinDetail(
    store: DesktopAccountStore,
    pin: Pin,
): DataHolder.Pin? = runCatching {
    val json = store.signedFetchJson("https://www.zhihu.com/api/v4/pins/${pin.id}")
        ?: return@runCatching null
    decodePinContentDetail(json)
}.getOrNull()

private suspend fun fetchDesktopPinLike(
    store: DesktopAccountStore,
    endpoint: String,
    method: HttpMethod,
): JsonObject? = runCatching {
    store.signedFetchJson(endpoint) {
        this.method = method
    }
}.getOrNull()

internal suspend fun fetchDesktopQuestionDetailForFeedBlock(
    store: DesktopAccountStore,
    question: Question,
): DataHolder.Question? = runCatching {
    val jo = store.signedFetchJson("https://www.zhihu.com/api/v4/questions/${question.questionId}?include=read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics")
        ?: return@runCatching null
    decodeQuestionContentDetail(jo)
}.getOrNull()

private suspend fun fetchDesktopLinkCardPreview(
    store: DesktopAccountStore,
    userMessages: UserMessageSink,
    linkCard: DataHolder.Pin.ContentLinkCard,
): PinLinkCardPreview? = fetchPinLinkCardPreview(linkCard) { destination ->
    when (destination) {
        is Article -> {
            DesktopArticleViewModelRuntime(store, userMessages).getContentDetail(destination)
        }
        is Question -> {
            fetchDesktopQuestionDetailForFeedBlock(store, destination)
        }
        is Pin -> {
            fetchDesktopPinDetail(store, destination)
        }
        else -> null
    }
}
