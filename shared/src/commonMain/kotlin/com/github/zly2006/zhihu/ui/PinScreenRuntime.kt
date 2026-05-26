package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.pin.PinLinkCardPreview
import com.github.zly2006.zhihu.shared.pin.PinScreenUiState
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent

data class PinLikeResult(
    val isLiked: Boolean,
    val likeCount: Int,
)

data class PinScreenRuntime(
    val loadPinDetail: suspend (Pin) -> PinScreenUiState,
    val toggleLike: (Pin, Boolean, (PinLikeResult) -> Unit) -> Unit,
    val handleShareAction: (Pin, () -> Unit) -> Unit,
    val fetchLinkCardPreview: suspend (DataHolder.Pin.ContentLinkCard) -> PinLinkCardPreview?,
    val openExternalUrl: (String) -> Unit,
)

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
