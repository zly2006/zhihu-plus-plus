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
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.GroupFeed
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.data.toDisplayItem
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

abstract class BaseFeedViewModel : PaginationViewModel<Feed>(typeOf<Feed>()) {
    var displayItems = mutableStateListOf<FeedDisplayItem>()
    var isPullToRefresh by mutableStateOf(false)
        protected set

    override fun processResponse(context: Context, data: List<Feed>, rawData: JsonArray) {
        super.processResponse(context, data, rawData)
        addDisplayItems(data.flatten().map { createDisplayItem(context, it) })
    }

    override fun refresh(context: Context) {
        displayItems.clear()
        super.refresh(context)
    }

    suspend fun pullToRefresh(context: Context) {
        isPullToRefresh = true
        displayItems.clear()
        if (isLoading) return
        errorMessage = null
        debugData.clear()
        allData.clear()
        lastPaging = null // 重置 lastPaging
        isLoading = true
        try {
            fetchFeeds(context)
        } catch (e: Exception) {
            errorHandle(e)
        }
        isLoading = false
        isPullToRefresh = false
    }

    open fun createDisplayItem(context: Context, feed: Feed): FeedDisplayItem {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return feed.toDisplayItem(
            enableQualityFilter = preferences.getBoolean("enableQualityFilter", true),
            reverseBlock = preferences.getBoolean("reverseBlock", false),
        )
    }

    fun addDisplayItems(newItems: List<FeedDisplayItem>) {
        newItems.forEach {
            if (displayItems.none { existing -> existing.stableKey == it.stableKey }) {
                displayItems.add(it)
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun List<Feed>.flatten() = flatMap {
        (it as? GroupFeed)?.list ?: listOf(it)
    }

    /**
     * 确保能获取作者信息，优先从 feed.target.author，失败时 lazy 加载完整内容
     */
    private suspend fun ensureAuthorInfo(
        context: Context,
        feedItem: FeedDisplayItem,
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        // 优先从 feed.target.author 获取
        feedItem.feed?.target?.author?.let { author ->
            return@withContext Pair(author.id, author.name)
        }

        feedItem.navDestination?.let { navDest ->
            if (navDest is Article) {
                when (val fullContent = ContentDetailCache.getOrFetch(context, navDest)) {
                    is DataHolder.Answer -> {
                        return@withContext fullContent.author.let { Pair(it.id, it.name) }
                    }
                    is DataHolder.Article -> {
                        return@withContext fullContent.author.let { Pair(it.id, it.name) }
                    }
                    else -> {}
                }
            }
        }

        // 无法获取作者信息
        null
    }

    /**
     * 确保能获取关键词屏蔽所需的内容（标题+摘要+正文），优先从 feed.target，失败时 lazy 加载
     */
    private suspend fun ensureContentForKeywordBlocking(
        context: Context,
        feedItem: FeedDisplayItem,
    ): Triple<String, String, String?>? = withContext(Dispatchers.IO) {
        // 优先使用已有的数据
        val title = feedItem.title
        val summary = feedItem.summary ?: feedItem.feed?.target?.excerpt ?: ""
        var content = feedItem.content

        // 如果 content 为空，尝试 lazy 加载
        if (content == null) {
            feedItem.navDestination?.let { navDest ->
                val fullContent = ContentDetailCache.getOrFetch(context, navDest)
                content = when (fullContent) {
                    is DataHolder.Answer -> fullContent.content
                    is DataHolder.Article -> fullContent.content
                    is DataHolder.Question -> fullContent.detail
                    else -> null
                }
            }
        }

        if (title.isNotEmpty() || summary.isNotEmpty() || content != null) {
            Triple(title, summary, content)
        } else {
            null
        }
    }

    /**
     * 处理用户屏蔽（包含数据获取和屏蔽逻辑）
     */
    fun handleBlockUser(
        context: Context,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<String, String>) -> Unit,
    ) {
        viewModelScope.launch {
            val authorInfo = ensureAuthorInfo(context, feedItem)
            if (authorInfo != null) {
                onShowDialog(authorInfo)
            } else {
                Toast.makeText(context, "无法获取屏蔽用户所需的数据，请尝试进入内容详情页操作", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 处理关键词屏蔽（包含数据获取和屏蔽逻辑）
     */
    fun handleBlockByKeywords(
        context: Context,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<FeedDisplayItem, Triple<String, String, String?>>) -> Unit,
    ) {
        viewModelScope.launch {
            val contentInfo = ensureContentForKeywordBlocking(context, feedItem)
            if (contentInfo != null) {
                onShowDialog(feedItem to contentInfo)
            } else {
                Toast.makeText(context, "无法获取关键词屏蔽所需的数据，请尝试进入内容详情页操作", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 屏蔽主题
     */
    fun handleBlockTopic(
        context: Context,
        topicId: String,
        topicName: String,
    ) {
        viewModelScope.launch {
            try {
                val blocklistManager = BlocklistManager.getInstance(context)
                blocklistManager.addBlockedTopic(topicId, topicName)
                Toast.makeText(context, "已屏蔽主题「$topicName」", Toast.LENGTH_SHORT).show()
                displayItems.removeAll {
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
    }
}
