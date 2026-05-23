package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.addZhihuReadHistory
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.pin.PinLinkCardPreview
import com.github.zly2006.zhihu.shared.pin.PinScreenUiState
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.viewmodel.DesktopArticleViewModelRuntime
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime = remember {
    val store = DesktopAccountStore()
    PinScreenRuntime(
        loadPinDetail = { pin ->
            addDesktopReadHistory(store, pin.id.toString(), "pin")
            val content = fetchDesktopPinDetail(store, pin)
            if (content == null) {
                PinScreenUiState(isLoading = false, errorMessage = "无法加载想法详情")
            } else {
                PinScreenUiState(
                    isLoading = false,
                    pinContent = content,
                    isLiked = content.virtuals.booleanCompat("isLiked", "is_liked"),
                    likeCount = content.likeCount,
                )
            }
        },
        toggleLike = { _, _, _ -> },
        handleShareAction = { _, onShowDialog -> onShowDialog() },
        fetchLinkCardPreview = { linkCard ->
            fetchDesktopLinkCardPreview(store, linkCard)
        },
        openExternalUrl = ::openDesktopExternalUrl,
    )
}

@Composable
actual fun PinHtmlContent(html: String) {
    RenderMarkdown(html)
}

@Composable
actual fun PinCommentsSheet(
    showComments: Boolean,
    onDismiss: () -> Unit,
    content: Pin,
) {
}

@Composable
actual fun PinShareDialog(
    content: Pin,
    shareText: String,
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
}

private suspend fun fetchDesktopPinDetail(
    store: DesktopAccountStore,
    pin: Pin,
): DataHolder.Pin? {
    val account = store.load()
    val endpoint = "https://www.zhihu.com/api/v4/pins/${pin.id}"
    return runCatching {
        store.createHttpClient(account.cookies).use { client ->
            val json = client
                .get(endpoint) {
                    account.cookies["d_c0"]?.let { dc0 ->
                        signZhihuFetchRequest(dc0 = dc0)
                    }
                }.body<kotlinx.serialization.json.JsonObject>()
            ZhihuJson.decodeJson<DataHolder.Pin>(json)
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

private suspend fun fetchDesktopLinkCardPreview(
    store: DesktopAccountStore,
    linkCard: DataHolder.Pin.ContentLinkCard,
): PinLinkCardPreview? {
    val destination = resolveLinkCardDestination(linkCard) ?: return null
    return when (destination) {
        is Article -> {
            when (val detail = DesktopArticleViewModelRuntime(store).getContentDetail(destination)) {
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
