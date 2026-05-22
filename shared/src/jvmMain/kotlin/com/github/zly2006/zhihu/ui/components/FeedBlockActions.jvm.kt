package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem

@Composable
actual fun rememberFeedBlockActions(): FeedBlockActions = FeedBlockActions(
    handleBlockUser = { _, _, _ -> },
    handleBlockTopic = { _, _, _ -> },
)

@Composable
actual fun BlockUserConfirmDialog(
    showDialog: Boolean,
    userToBlock: Pair<String, String>?,
    displayItems: List<FeedDisplayItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
}
