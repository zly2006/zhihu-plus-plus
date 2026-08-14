/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.flattenFeeds
import com.github.zly2006.zhihu.data.toDisplayItem
import com.github.zly2006.zhihu.platform.UserMessageSink
import com.github.zly2006.zhihu.viewmodel.FeedDisplayEnvironment
import com.github.zly2006.zhihu.viewmodel.HomeFeedFilterResult
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel
import com.github.zly2006.zhihu.viewmodel.filter.BlockedTopic
import com.github.zly2006.zhihu.viewmodel.filter.ContentDetailProvider
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.getOrFetchContentDetail
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

abstract class BaseFeedViewModel : PaginationViewModel<Feed>(typeOf<Feed>()) {
    var displayItems = mutableStateListOf<FeedDisplayItem>()
    internal var latestLoadedDisplayItems = mutableStateOf<List<FeedDisplayItem>>(emptyList())
    var isPullToRefresh by mutableStateOf(false)
        protected set

    override fun processResponse(environment: PaginationEnvironment, data: List<Feed>, rawData: JsonArray) {
        super.processResponse(environment, data, rawData)
        val loadedItems = data.flattenFeeds().map { createDisplayItem(environment, it) }
        addDisplayItems(loadedItems)
        latestLoadedDisplayItems.value = loadedItems
    }

    override fun refresh(environment: PaginationEnvironment) {
        displayItems.clear()
        super.refresh(environment)
    }

    suspend fun pullToRefresh(environment: PaginationEnvironment) {
        if (isLoading) return
        isPullToRefresh = true
        try {
            displayItems.clear()
            errorMessage = null
            debugData.clear()
            allData.clear()
            lastPaging = null // 重置 lastPaging
            isLoading = true
            try {
                fetchFeeds(environment)
            } catch (e: Exception) {
                errorHandle(e)
            }
            isLoading = false
        } finally {
            isPullToRefresh = false
        }
    }

    open fun createDisplayItem(environment: FeedDisplayEnvironment, feed: Feed): FeedDisplayItem {
        val settings = environment.feedDisplaySettings()
        return feed.toDisplayItem(
            enableQualityFilter = settings.enableQualityFilter,
            reverseBlock = settings.reverseBlock,
        )
    }

    fun addDisplayItems(newItems: List<FeedDisplayItem>) {
        newItems.forEach {
            if (displayItems.none { existing -> existing.stableKey == it.stableKey }) {
                displayItems.add(it)
            }
        }
    }

    fun handleBlockUser(
        environment: PaginationEnvironment,
        userMessages: UserMessageSink,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<String, String>) -> Unit,
    ) {
        viewModelScope.launch {
            val authorInfo = resolveFeedBlockAuthorInfo(
                feedItem,
                ContentDetailProvider(environment::getOrFetchContentDetail),
            )
            if (authorInfo != null) {
                onShowDialog(authorInfo)
            } else {
                userMessages.showLongMessage("无法获取屏蔽用户所需的数据，请尝试进入内容详情页操作")
            }
        }
    }

    fun handleBlockQuestionAuthor(
        environment: PaginationEnvironment,
        userMessages: UserMessageSink,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<String, String>) -> Unit,
    ) {
        viewModelScope.launch {
            val authorInfo = resolveFeedQuestionAuthorInfo(
                feedItem,
                ContentDetailProvider(environment::getOrFetchContentDetail),
            )
            if (authorInfo != null) {
                onShowDialog(authorInfo)
            } else {
                userMessages.showLongMessage("当前条目没有可用的提问者数据，无法屏蔽提问者")
            }
        }
    }

    fun handleBlockByKeywords(
        environment: PaginationEnvironment,
        userMessages: UserMessageSink,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<FeedDisplayItem, Triple<String, String, String?>>) -> Unit,
    ) {
        viewModelScope.launch {
            val contentInfo = resolveFeedKeywordBlockingContent(
                feedItem,
                ContentDetailProvider(environment::getOrFetchContentDetail),
            )
            if (contentInfo != null) {
                onShowDialog(feedItem to contentInfo)
            } else {
                userMessages.showLongMessage("无法获取关键词屏蔽所需的数据，请尝试进入内容详情页操作")
            }
        }
    }

    fun handleBlockTopic(
        userMessages: UserMessageSink,
        topicId: String,
        topicName: String,
    ) {
        viewModelScope.launch {
            try {
                getContentFilterDatabase()
                    .blockedTopicDao()
                    .insertTopic(BlockedTopic(topicId = topicId, topicName = topicName))
                userMessages.showShortMessage("已屏蔽主题「$topicName」")
                removeFeedItemsByBlockedTopic(this@BaseFeedViewModel, topicId)
            } catch (e: Exception) {
                userMessages.showShortMessage("屏蔽失败: ${e.message}")
            }
        }
    }
}

/**
 * Merges the final home-feed filter result back into the list that was already shown optimistically.
 *
 * Only items from [HomeFeedFilterResult.foregroundItems] are touched, so older or unrelated cards in the
 * list keep their current state. A foreground item is removed when it is absent from
 * [HomeFeedFilterResult.filteredItems], and replaced when the final filter pipeline returns a matching item
 * with the same [FeedDisplayItem.stableKey]. This lets delayed quality/content filters swap an already
 * rendered card with an `已屏蔽` placeholder while preserving existing raw content if the replacement has not
 * loaded one. Reverse-block mode is intentionally ignored because it renders filtered items directly.
 */
internal fun MutableList<FeedDisplayItem>.replaceHomeFeedItemsWithFilteredResult(filterResult: HomeFeedFilterResult) {
    if (filterResult.reverseBlock) return

    val foregroundKeys = filterResult.foregroundItems.map { it.stableKey }.toSet()
    val filteredItemsByKey = filterResult.filteredItems.associateBy { it.stableKey }
    var index = 0
    while (index < size) {
        val item = this[index]
        if (item.stableKey !in foregroundKeys) {
            index++
            continue
        }

        val filteredVersion = filteredItemsByKey[item.stableKey]
        if (filteredVersion == null) {
            removeAt(index)
        } else {
            this[index] = filteredVersion.copy(raw = filteredVersion.raw ?: item.raw)
            index++
        }
    }
}
