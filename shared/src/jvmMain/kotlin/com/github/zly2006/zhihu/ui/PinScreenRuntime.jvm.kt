package com.github.zly2006.zhihu.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.shared.pin.PinScreenUiState

@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime = PinScreenRuntime(
    loadPinDetail = { PinScreenUiState(isLoading = false, errorMessage = "暂时无法在桌面端加载想法详情") },
    toggleLike = { _, _, _ -> },
    handleShareAction = { _, onShowDialog -> onShowDialog() },
    fetchLinkCardPreview = { null },
    openExternalUrl = {},
)

@Composable
actual fun PinHtmlContent(html: String) {
    Text(compactPreview(html, maxLength = Int.MAX_VALUE))
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
