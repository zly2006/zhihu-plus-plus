package com.github.zly2006.zhihu.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.getContentDetail
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
import com.github.zly2006.zhihu.shared.pin.PinLinkCardPreview
import com.github.zly2006.zhihu.shared.pin.PinScreenUiState
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup

@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime {
    val context = LocalContext.current
    val settings = rememberSettingsStore()
    val shareRuntime = rememberShareDialogRuntime()
    return remember(context, settings, shareRuntime) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val userMessages = androidUserMessageSink(context)
        PinScreenRuntime(
            loadPinDetail = { pin ->
                AccountData.addReadHistory(context, pin.id.toString(), "pin")
                val content = DataHolder.getContentDetail(context, pin)
                if (content == null) {
                    PinScreenUiState(isLoading = false, errorMessage = "无法加载想法详情")
                } else {
                    ContentOpenEventSupport.recordOpenEvent(
                        database = getContentFilterDatabase(context),
                        destination = pin,
                        openFrom = context.articleHost()?.consumePendingContentOpenFrom(pin) ?: ContentOpenFrom.UNKNOWN,
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
                        val jojo = AccountData.fetch(context, endpoint) {
                            this.method = method
                            signFetchRequest()
                        } ?: return@launch
                        onResult(
                            PinLikeResult(
                                isLiked = !isLiked,
                                likeCount = jojo["liked_count"]?.jsonPrimitive?.intOrNull ?: -1,
                            ),
                        )
                    } catch (e: Exception) {
                        userMessages.showShortMessage("操作失败: ${e.message}")
                    }
                }
            },
            handleShareAction = { pin, onShowDialog ->
                handleShareAction(pin, settings, shareRuntime, onShowDialog)
            },
            fetchLinkCardPreview = { linkCard ->
                fetchAndroidLinkCardPreview(context, linkCard)
            },
            openExternalUrl = { url ->
                luoTianYiUrlLauncher(context, url.toUri())
            },
        )
    }
}

@Composable
actual fun PinHtmlWebViewContent(html: String) {
    WebviewComp {
        it.isVerticalScrollBarEnabled = false
        it.setupUpWebviewClient()
        it.loadZhihu(
            "https://www.zhihu.com",
            Jsoup.parse(html),
        )
    }
}

actual fun supportsPinHtmlWebView(): Boolean = true

private suspend fun fetchAndroidLinkCardPreview(
    context: Context,
    linkCard: DataHolder.Pin.ContentLinkCard,
): PinLinkCardPreview? {
    val destination = resolveLinkCardDestination(linkCard) ?: return null
    return when (destination) {
        is Article -> {
            when (val detail = DataHolder.getContentDetail(context, destination)) {
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
            DataHolder.getContentDetail(context, destination)?.let { detail ->
                PinLinkCardPreview(
                    title = compactTitle(detail.title),
                    preview = compactPreview(detail.detail),
                )
            }
        }
        is Pin -> {
            DataHolder.getContentDetail(context, destination)?.let { detail ->
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
