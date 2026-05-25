package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.viewmodel.feed.handleBlockByKeywords
import com.github.zly2006.zhihu.viewmodel.feed.handleBlockTopic
import com.github.zly2006.zhihu.viewmodel.feed.handleBlockUser
import com.github.zly2006.zhihu.viewmodel.filter.getBlocklistManager
import kotlinx.coroutines.launch

@Composable
actual fun rememberFeedBlockActions(): FeedBlockActions {
    val context = LocalContext.current
    return remember(context) {
        FeedBlockActions(
            handleBlockUser = { viewModel, feedItem, onShowDialog ->
                viewModel.handleBlockUser(context, feedItem, onShowDialog)
            },
            handleBlockTopic = { viewModel, topicId, topicName ->
                viewModel.handleBlockTopic(context, topicId, topicName)
            },
            handleBlockByKeywords = { viewModel, feedItem, onShowDialog ->
                viewModel.handleBlockByKeywords(context, feedItem, onShowDialog)
            },
        )
    }
}

@Composable
actual fun BlockUserConfirmDialog(
    showDialog: Boolean,
    userToBlock: Pair<String, String>?,
    displayItems: List<FeedDisplayItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    BlockUserConfirmDialogContent(
        showDialog = showDialog,
        userToBlock = userToBlock,
        displayItems = displayItems,
        onDismiss = onDismiss,
        onConfirmBlock = { author ->
            coroutineScope.launch {
                try {
                    val blocklistManager = getBlocklistManager(context)
                    blocklistManager.addBlockedUser(
                        userId = author.id,
                        userName = author.name,
                        urlToken = author.urlToken,
                        avatarUrl = author.avatarUrl,
                    )
                    onConfirm()
                    userMessages.showShortMessage("已屏蔽用户：${author.name}")
                } catch (e: Exception) {
                    Log.e("FeedBlockActions", "Failed to block user", e)
                    userMessages.showShortMessage("屏蔽用户失败: ${e.message}")
                }
            }
        },
    )
}
