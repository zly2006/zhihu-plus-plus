package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.pin.PinLinkCardPreview
import com.github.zly2006.zhihu.shared.pin.PinScreenUiState
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

data class PinLikeResult(
    val isLiked: Boolean,
    val likeCount: Int,
)

data class PinScreenRuntime(
    val loadPinDetail: suspend (Pin) -> PinScreenUiState,
    val handleShareAction: (Pin, () -> Unit) -> Unit,
    val fetchLinkCardPreview: suspend (DataHolder.Pin.ContentLinkCard) -> PinLinkCardPreview?,
    val openExternalUrl: (String) -> Unit,
)

internal suspend fun fetchPinLinkCardPreview(
    linkCard: DataHolder.Pin.ContentLinkCard,
    fetchDetail: suspend (NavDestination) -> DataHolder.Content?,
): PinLinkCardPreview? {
    val destination = resolveLinkCardDestination(linkCard) ?: return null
    return when (destination) {
        is Article -> {
            when (val detail = fetchDetail(destination)) {
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
            (fetchDetail(destination) as? DataHolder.Question)?.let { detail ->
                PinLinkCardPreview(
                    title = compactTitle(detail.title),
                    preview = compactPreview(detail.detail),
                )
            }
        }
        is Pin -> {
            (fetchDetail(destination) as? DataHolder.Pin)?.let { detail ->
                PinLinkCardPreview(
                    title = "${detail.author.name} 的想法",
                    preview = compactPreview(detail.contentHtml),
                )
            }
        }
        else -> null
    }
}

internal fun JsonObject?.booleanCompat(vararg keys: String): Boolean {
    if (this == null) return false
    return keys.firstNotNullOfOrNull { key ->
        get(key)?.jsonPrimitive?.booleanOrNull
    } ?: false
}

@Composable
expect fun rememberPinScreenRuntime(): PinScreenRuntime

@Composable
fun PinHtmlContent(html: String) {
    if (rememberSettingsStore().getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false) &&
        supportsPinHtmlWebView()
    ) {
        PinHtmlWebViewContent(html)
    } else {
        Spacer(Modifier.height(10.dp))
        RenderMarkdown(
            html = html,
            modifier = Modifier.questionSelectionWorkaround(),
            selectable = true,
            enableScroll = false,
        )
    }
}

expect fun supportsPinHtmlWebView(): Boolean

@Composable
expect fun PinHtmlWebViewContent(html: String)

@Composable
fun PinCommentsSheet(
    showComments: Boolean,
    onDismiss: () -> Unit,
    content: Pin,
) {
    CommentScreenComponent(
        showComments = showComments,
        onDismiss = onDismiss,
        content = content,
    )
}
