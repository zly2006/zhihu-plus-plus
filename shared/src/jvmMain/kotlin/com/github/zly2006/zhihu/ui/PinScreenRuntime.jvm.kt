package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.github.zly2006.zhihu.data.decodePinContentDetail
import com.github.zly2006.zhihu.data.decodeQuestionContentDetail
import com.github.zly2006.zhihu.data.zhihuPinContentDetailUrl
import com.github.zly2006.zhihu.data.zhihuQuestionContentDetailUrl
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.desktop.signDesktopRequest
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
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl as browseDesktopExternalUrl

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
                addDesktopReadHistory(store, pin.id.toString(), "pin")
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
                        val endpoint = zhihuPinVotersUpUrl(pin)
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
): DataHolder.Pin? {
    val account = store.load()
    val endpoint = zhihuPinContentDetailUrl(pin)
    return runCatching {
        val json = store.fetchAuthenticatedJson(endpoint) {
            signDesktopRequest(account.cookies)
        } ?: return@runCatching null
        decodePinContentDetail(json)
    }.getOrNull()
}

private suspend fun fetchDesktopPinLike(
    store: DesktopAccountStore,
    endpoint: String,
    method: HttpMethod,
): JsonObject? {
    val account = store.load()
    return runCatching {
        store.fetchAuthenticatedJson(endpoint) {
            this.method = method
            signDesktopRequest(account.cookies)
        }
    }.getOrNull()
}

internal suspend fun fetchDesktopQuestionDetailForFeedBlock(
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

private fun openDesktopExternalUrl(url: String) {
    runCatching {
        browseDesktopExternalUrl(url)
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
