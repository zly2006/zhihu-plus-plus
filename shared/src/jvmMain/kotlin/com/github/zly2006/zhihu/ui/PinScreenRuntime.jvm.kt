package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.addZhihuReadHistory
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.pin.PinLinkCardPreview
import com.github.zly2006.zhihu.shared.pin.PinScreenUiState
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.ui.components.ShareDialogContent
import com.github.zly2006.zhihu.ui.components.getShareText
import com.github.zly2006.zhihu.viewmodel.DesktopArticleViewModelRuntime
import io.ktor.http.HttpMethod
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime {
    val scope = rememberCoroutineScope()
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    val store = DesktopAccountStore()
    val historyStorage = DesktopHistoryStorage()
    return remember(scope, settings, userMessages) {
        PinScreenRuntime(
            loadPinDetail = { pin ->
                addDesktopReadHistory(store, pin.id.toString(), "pin")
                val content = fetchDesktopPinDetail(store, pin)
                if (content == null) {
                    PinScreenUiState(isLoading = false, errorMessage = "无法加载想法详情")
                } else {
                    historyStorage.add(pin)
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
                handleDesktopShareAction(
                    shareText = getShareText(pin),
                    settings = settings,
                    userMessages = userMessages,
                    onShowDialog = onShowDialog,
                )
            },
            fetchLinkCardPreview = { linkCard ->
                fetchDesktopLinkCardPreview(store, userMessages, linkCard)
            },
            openExternalUrl = ::openDesktopExternalUrl,
        )
    }
}

@Composable
actual fun PinHtmlContent(html: String) {
    Spacer(Modifier.height(10.dp))
    RenderMarkdown(
        html = html,
        selectable = true,
        enableScroll = false,
    )
}

@Composable
actual fun PinShareDialog(
    content: Pin,
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

private suspend fun fetchDesktopPinDetail(
    store: DesktopAccountStore,
    pin: Pin,
): DataHolder.Pin? {
    val account = store.load()
    val endpoint = "https://www.zhihu.com/api/v4/pins/${pin.id}"
    return runCatching {
        val json = store.fetchAuthenticatedJson(endpoint) {
            account.cookies["d_c0"]?.let { dc0 ->
                signZhihuFetchRequest(dc0 = dc0)
            }
        } ?: return@runCatching null
        ZhihuJson.decodeJson<DataHolder.Pin>(json)
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
            account.cookies["d_c0"]?.let { dc0 ->
                signZhihuFetchRequest(dc0 = dc0)
            }
        }
    }.getOrNull()
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

private suspend fun fetchDesktopLinkCardPreview(
    store: DesktopAccountStore,
    userMessages: UserMessageSink,
    linkCard: DataHolder.Pin.ContentLinkCard,
): PinLinkCardPreview? {
    val destination = resolveLinkCardDestination(linkCard) ?: return null
    return when (destination) {
        is Article -> {
            when (val detail = DesktopArticleViewModelRuntime(store, userMessages).getContentDetail(destination)) {
                is DataHolder.Article -> PinLinkCardPreview(
                    title = compactTitle(detail.title),
                    preview = compactPreview(detail.excerpt.ifBlank { detail.content }),
                )
                is DataHolder.Answer -> PinLinkCardPreview(
                    title = compactTitle(detail.question.title),
                    preview = compactPreview(detail.excerpt.ifBlank { detail.content }),
                )
                else -> null
            }
        }
        is Question -> {
            fetchDesktopQuestionDetail(store, destination)?.let { detail ->
                PinLinkCardPreview(
                    title = compactTitle(detail.title),
                    preview = compactPreview(detail.detail),
                )
            }
        }
        is Pin -> {
            fetchDesktopPinDetail(store, destination)?.let { detail ->
                PinLinkCardPreview(
                    title = "${detail.author.name} 的想法",
                    preview = compactPreview(detail.contentHtml),
                )
            }
        }
        else -> null
    }
}

private fun kotlinx.serialization.json.JsonObject?.booleanCompat(vararg keys: String): Boolean {
    if (this == null) return false
    return keys.firstNotNullOfOrNull { key ->
        get(key)?.jsonPrimitive?.booleanOrNull
    } ?: false
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
