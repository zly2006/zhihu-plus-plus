package com.github.zly2006.zhihu.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.viewmodel.feed.handleBlockByKeywords
import com.github.zly2006.zhihu.viewmodel.filter.getBlocklistManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberFeedBlockActions(): FeedBlockActions {
    val context = LocalContext.current
    return remember(context) {
        FeedBlockActions(
            handleBlockUser = { viewModel, feedItem, onShowDialog ->
                viewModel.viewModelScope.launch {
                    val authorInfo = ensureAuthorInfo(context, feedItem)
                    if (authorInfo != null) {
                        onShowDialog(authorInfo)
                    } else {
                        Toast.makeText(context, "无法获取屏蔽用户所需的数据，请尝试进入内容详情页操作", Toast.LENGTH_LONG).show()
                    }
                }
            },
            handleBlockTopic = { viewModel, topicId, topicName ->
                viewModel.viewModelScope.launch {
                    try {
                        val blocklistManager = getBlocklistManager(context)
                        blocklistManager.addBlockedTopic(topicId, topicName)
                        Toast.makeText(context, "已屏蔽主题「$topicName」", Toast.LENGTH_SHORT).show()
                        viewModel.displayItems.removeAll {
                            val topics = when (val content = it.raw) {
                                is DataHolder.Answer -> content.question.topics
                                is DataHolder.Article -> content.topics
                                is DataHolder.Question -> content.topics
                                else -> null
                            }
                            topics?.any { topic -> topic.id == topicId } == true
                        }
                    } catch (e: Exception) {
                        val message = "屏蔽失败: ${e.message}"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
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
                    Toast.makeText(context, "已屏蔽用户：${author.name}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "屏蔽用户失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        },
    )
}

private suspend fun ensureAuthorInfo(
    context: Context,
    feedItem: FeedDisplayItem,
): Pair<String, String>? = withContext(Dispatchers.IO) {
    feedItem.feed?.target?.author?.let { author ->
        return@withContext Pair(author.id, author.name)
    }

    feedItem.navDestination?.let { navDest ->
        if (navDest is Article) {
            when (val fullContent = ContentDetailCache.getOrFetch(context, navDest)) {
                is DataHolder.Answer -> return@withContext fullContent.author.let { Pair(it.id, it.name) }
                is DataHolder.Article -> return@withContext fullContent.author.let { Pair(it.id, it.name) }
                else -> {}
            }
        }
    }

    null
}
