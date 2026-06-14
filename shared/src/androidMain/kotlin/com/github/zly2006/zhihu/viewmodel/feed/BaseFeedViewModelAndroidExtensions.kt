/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.getOrFetch
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.viewmodel.filter.ContentDetailProvider
import com.github.zly2006.zhihu.viewmodel.filter.getBlocklistManager
import com.github.zly2006.zhihu.viewmodel.paginationEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun BaseFeedViewModel.pullToRefresh(context: Context) {
    pullToRefresh(paginationEnvironment(context))
}

/**
 * 处理用户屏蔽（包含数据获取和屏蔽逻辑）
 */
fun BaseFeedViewModel.handleBlockUser(
    context: Context,
    feedItem: FeedDisplayItem,
    onShowDialog: (Pair<String, String>) -> Unit,
) {
    val userMessages = androidUserMessageSink(context)
    viewModelScope.launch {
        val authorInfo = withContext(Dispatchers.IO) {
            resolveFeedBlockAuthorInfo(feedItem, androidContentDetailProvider(context))
        }
        if (authorInfo != null) {
            onShowDialog(authorInfo)
        } else {
            userMessages.showLongMessage("无法获取屏蔽用户所需的数据，请尝试进入内容详情页操作")
        }
    }
}

/**
 * 处理关键词屏蔽（包含数据获取和屏蔽逻辑）
 */
fun BaseFeedViewModel.handleBlockByKeywords(
    context: Context,
    feedItem: FeedDisplayItem,
    onShowDialog: (Pair<FeedDisplayItem, Triple<String, String, String?>>) -> Unit,
) {
    val userMessages = androidUserMessageSink(context)
    viewModelScope.launch {
        val contentInfo = withContext(Dispatchers.IO) {
            resolveFeedKeywordBlockingContent(feedItem, androidContentDetailProvider(context))
        }
        if (contentInfo != null) {
            onShowDialog(feedItem to contentInfo)
        } else {
            userMessages.showLongMessage("无法获取关键词屏蔽所需的数据，请尝试进入内容详情页操作")
        }
    }
}

/**
 * 屏蔽主题
 */
fun BaseFeedViewModel.handleBlockTopic(
    context: Context,
    topicId: String,
    topicName: String,
) {
    val userMessages = androidUserMessageSink(context)
    viewModelScope.launch {
        try {
            val blocklistManager = getBlocklistManager(context)
            blocklistManager.addBlockedTopic(topicId, topicName)
            userMessages.showShortMessage("已屏蔽主题「$topicName」")
            removeFeedItemsByBlockedTopic(this@handleBlockTopic, topicId)
        } catch (e: Exception) {
            val message = "屏蔽失败: ${e.message}"
            userMessages.showShortMessage(message)
        }
    }
}

private fun androidContentDetailProvider(context: Context): ContentDetailProvider =
    ContentDetailProvider { destination ->
        ContentDetailCache.getOrFetch(context, destination)
    }
