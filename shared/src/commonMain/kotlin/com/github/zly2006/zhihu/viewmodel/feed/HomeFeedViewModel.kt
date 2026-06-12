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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.viewmodel.ContentInteractionEnvironment
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.filter.ContentDetailProvider
import com.github.zly2006.zhihu.viewmodel.filter.extractTopicIds
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray

suspend fun resolveFeedBlockAuthorInfo(
    feedItem: FeedDisplayItem,
    contentDetailProvider: ContentDetailProvider?,
): Pair<String, String>? {
    feedItem.feed?.target?.author?.let { author ->
        return Pair(author.id, author.name)
    }

    return when (val content = resolveFeedBlockContentDetail(feedItem, contentDetailProvider)) {
        is DataHolder.Answer -> content.author.let { Pair(it.id, it.name) }
        is DataHolder.Article -> content.author.let { Pair(it.id, it.name) }
        is DataHolder.Question -> content.author.let { Pair(it.id, it.name) }
        is DataHolder.Pin -> content.author.let { Pair(it.id, it.name) }
        else -> null
    }
}

suspend fun resolveFeedKeywordBlockingContent(
    feedItem: FeedDisplayItem,
    contentDetailProvider: ContentDetailProvider?,
): Triple<String, String, String?>? {
    val title = feedItem.title
    val summary = feedItem.summary ?: feedItem.feed?.target?.excerpt ?: ""
    val content = feedItem.content ?: when (val fullContent = resolveFeedBlockContentDetail(feedItem, contentDetailProvider)) {
        is DataHolder.Answer -> fullContent.content
        is DataHolder.Article -> fullContent.content
        is DataHolder.Question -> fullContent.detail
        is DataHolder.Pin -> fullContent.contentHtml
        else -> null
    }

    return if (title.isNotEmpty() || summary.isNotEmpty() || content != null) {
        Triple(title, summary, content)
    } else {
        null
    }
}

fun removeFeedItemsByBlockedTopic(
    viewModel: BaseFeedViewModel,
    topicId: String,
) {
    viewModel.displayItems.removeAll { item ->
        val raw = item.raw
        raw != null && extractTopicIds(raw)?.any { topic -> topic == topicId } == true
    }
}

private suspend fun resolveFeedBlockContentDetail(
    feedItem: FeedDisplayItem,
    contentDetailProvider: ContentDetailProvider?,
): DataHolder.Content? {
    val raw = feedItem.raw
    if (raw != null && raw !is DataHolder.DummyContent) {
        return raw
    }

    val navDestination = feedItem.navDestination ?: return null
    return contentDetailProvider?.get(navDestination)
}

interface HomeFeedInteractionViewModel {
    suspend fun recordContentInteraction(environment: ContentInteractionEnvironment, feed: Feed)

    fun onUiContentClick(environment: ContentInteractionEnvironment, feed: Feed, item: FeedDisplayItem)
}

class HomeFeedViewModel :
    BaseFeedViewModel(),
    HomeFeedInteractionViewModel {
    private val reportedTouchedItems = hashSetOf<Pair<String, String>>()

    override val initialUrl: String
//        get() = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10"
        get() = "https://api.zhihu.com/topstory/recommend"

    init {
        allowGuestAccess = true
    }

    public override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        markItemsAsTouched(environment)
        super.fetchFeeds(environment)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun processResponse(environment: PaginationEnvironment, data: List<Feed>, rawData: JsonArray) {
        allData.addAll(data)
        debugData.addAll(rawData)

        viewModelScope.launch {
            val newItems = data
                .flatten()
                .map { feed -> createDisplayItem(environment, feed) }

            val filterResult = environment.applyHomeFeedFilters(newItems)
            if (!filterResult.reverseBlock) {
                withContext(Dispatchers.Main) {
                    addDisplayItems(filterResult.foregroundItems)
                }
            }

            val newDestinations = filterResult.foregroundItems.map { it.navDestination }.toSet()

            if (filterResult.reverseBlock) {
                addDisplayItems(filterResult.filteredItems)
            }

            // 移除被过滤的条目，并更新已保留条目的 raw 内容
            withContext(Dispatchers.Main) {
                displayItems.removeAll { item ->
                    if (item.navDestination !in newDestinations) return@removeAll false
                    val filteredVersion = filterResult.filteredItems.find { it.navDestination == item.navDestination }
                    item.raw = filteredVersion?.raw ?: item.raw
                    // remove if no filtered version exists, which means it was filtered out
                    filteredVersion == null
                }
            }
        }
    }

    /**
     * 记录用户与内容的交互行为
     * 应该在用户点击、点赞等操作时调用
     */
    override suspend fun recordContentInteraction(environment: ContentInteractionEnvironment, feed: Feed) {
        try {
            environment.recordContentInteraction(feed)
        } catch (e: Exception) {
            environment.handleFetchFailure("HomeFeedViewModel", e)
        }
    }

    /**
     * 记录用户点击内容
     * 在viewModelScope中运行，使用viewModelScope代替GlobalScope
     */
    override fun onUiContentClick(environment: ContentInteractionEnvironment, feed: Feed, item: FeedDisplayItem) {
        viewModelScope.launch(Dispatchers.Default) {
            environment.sendFeedReadStatus(feed)
            recordContentInteraction(environment, feed)
        }
    }

    private suspend fun markItemsAsTouched(
        environment: ContentInteractionEnvironment,
    ) {
        try {
            val currentTouchItems = displayItems
                .asSequence()
                .filterNot { it.isFiltered }
                .mapNotNull { it.feed?.target }
                .mapNotNull { target ->
                    when (target) {
                        is Feed.AnswerTarget -> "answer" to target.id.toString()
                        is Feed.ArticleTarget -> "article" to target.id.toString()
                        is Feed.PinTarget -> "pin" to target.id.toString()
                        else -> null
                    }
                }.toList()
            val untouchedItemSet = currentTouchItems - reportedTouchedItems

            if (untouchedItemSet.isNotEmpty()) {
                reportedTouchedItems.addAll(environment.markItemsAsTouched(untouchedItemSet.toSet()))
            }
        } catch (e: Exception) {
            environment.handleFetchFailure("FeedViewModel", e)
        }
    }

    override fun refresh(environment: PaginationEnvironment) {
        super.refresh(environment)
        reportedTouchedItems.clear()
    }
}
