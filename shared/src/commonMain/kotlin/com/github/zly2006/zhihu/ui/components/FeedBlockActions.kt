package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel

data class FeedBlockActions(
    val handleBlockUser: (
        viewModel: BaseFeedViewModel,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<String, String>) -> Unit,
    ) -> Unit,
    val handleBlockTopic: (
        viewModel: BaseFeedViewModel,
        topicId: String,
        topicName: String,
    ) -> Unit,
    val handleBlockByKeywords: (
        viewModel: BaseFeedViewModel,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<FeedDisplayItem, Triple<String, String, String?>>) -> Unit,
    ) -> Unit,
)

@Composable
expect fun rememberFeedBlockActions(): FeedBlockActions

@Composable
expect fun BlockUserConfirmDialog(
    showDialog: Boolean,
    userToBlock: Pair<String, String>?,
    displayItems: List<FeedDisplayItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
)

@Composable
expect fun BlockByKeywordsDialog(
    showDialog: Boolean,
    feedTitle: String,
    feedExcerpt: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
)
