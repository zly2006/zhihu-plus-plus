package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.pin.PinLinkCardPreview
import com.github.zly2006.zhihu.shared.pin.PinScreenUiState
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
expect fun PinHtmlContent(html: String)

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
