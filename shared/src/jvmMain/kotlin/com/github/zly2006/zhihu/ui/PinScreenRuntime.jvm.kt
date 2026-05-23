package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.pin.PinScreenUiState
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime = remember {
    val store = DesktopAccountStore()
    PinScreenRuntime(
        loadPinDetail = { pin ->
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
        fetchLinkCardPreview = { null },
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
