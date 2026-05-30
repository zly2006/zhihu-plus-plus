package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.viewmodel.feed.handleBlockByKeywords
import com.github.zly2006.zhihu.viewmodel.feed.handleBlockTopic
import com.github.zly2006.zhihu.viewmodel.feed.handleBlockUser
import com.github.zly2006.zhihu.viewmodel.filter.getBlocklistManager

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
actual fun rememberBlockUserConfirmRuntime(): BlockUserConfirmRuntime {
    val context = LocalContext.current
    return remember(context) {
        BlockUserConfirmRuntime(
            blockUser = { author ->
                getBlocklistManager(context).addBlockedUser(
                    userId = author.id,
                    userName = author.name,
                    urlToken = author.urlToken,
                    avatarUrl = author.avatarUrl,
                )
            },
        )
    }
}
