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
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.PeopleSearchResult
import com.github.zly2006.zhihu.data.SearchResult
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.ZhihuPaging
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.util.raiseForStatus
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.deleteSigned
import com.github.zly2006.zhihu.viewmodel.postSigned
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

const val ZHIHU_HOT_SEARCH_URL = "https://www.zhihu.com/api/v4/search/hot_search"
private const val SEARCH_VERTICAL_INFO = "0,0,0,0,0,0,0,0,0,0,0,0"

open class SearchViewModel(
    val searchQuery: String,
    val restrictedMemberHashId: String = "",
) : BaseFeedViewModel() {
    val peopleResults = mutableStateListOf<PeopleSearchResult>()
    val topicResults = mutableStateListOf<TopicSearchResult>()
    val changingTopicIds = mutableStateListOf<String>()
    var sortOption by mutableStateOf(SearchSortOption.Default)
        private set
    var contentType by mutableStateOf(SearchContentType.All)
        private set
    var searchTab by mutableStateOf(SearchTab.General)
        private set
    var timeRange by mutableStateOf(SearchTimeRange.All)
        private set

    val initialRequestUrl: String
        get() = initialUrl

    override val initialUrl: String
        get() = zhihuSearchUrl(searchQuery, searchTab, sortOption, contentType, timeRange, restrictedMemberHashId)

    // Override include to request necessary fields for search results
    override val include = "data[*].highlight,object,type"

    fun updateSortOption(
        environment: PaginationEnvironment,
        option: SearchSortOption,
    ) {
        if (sortOption == option) return
        sortOption = option
        refresh(environment)
    }

    fun updateContentType(
        environment: PaginationEnvironment,
        type: SearchContentType,
    ) {
        if (contentType == type) return
        contentType = type
        refresh(environment)
    }

    fun updateSearchTab(
        environment: PaginationEnvironment,
        tab: SearchTab,
    ) {
        if (searchTab == tab) return
        searchTab = tab
        refresh(environment)
    }

    fun updateTimeRange(
        environment: PaginationEnvironment,
        range: SearchTimeRange,
    ) {
        if (timeRange == range) return
        timeRange = range
        refresh(environment)
    }

    suspend fun setTopicFollowing(
        environment: PaginationEnvironment,
        topicId: String,
        following: Boolean,
    ): Result<Unit> {
        val index = topicResults.indexOfFirst { it.topic.id == topicId }
        if (index < 0 || topicId in changingTopicIds || topicResults[index].isFollowing == following) {
            return Result.success(Unit)
        }
        val previous = topicResults[index]
        changingTopicIds += topicId
        topicResults[index] = previous.copy(isFollowing = following)
        return runCatching {
            val endpoint = "https://www.zhihu.com/api/v4/topics/$topicId/followers"
            val response = if (following) environment.postSigned(endpoint) else environment.deleteSigned(endpoint)
            response.raiseForStatus()
            Unit
        }.onFailure {
            val currentIndex = topicResults.indexOfFirst { it.topic.id == topicId }
            if (currentIndex >= 0) topicResults[currentIndex] = previous
        }.also {
            changingTopicIds -= topicId
        }
    }

    override fun refresh(environment: PaginationEnvironment) {
        peopleResults.clear()
        topicResults.clear()
        super.refresh(environment)
    }

    fun retry(environment: PaginationEnvironment) {
        errorMessage = null
        loadMore(environment)
    }

    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val url = lastPaging?.next ?: initialUrl
            val jojo = environment.fetchJson(url, include) ?: error("搜索响应为空")
            val jsonArray = jojo["data"] as? JsonArray ?: error("搜索响应缺少 data 列表")

            // Parse search results and convert to Feed objects
            val results = jsonArray.mapNotNull { element ->
                if (searchTab == SearchTab.Topic) return@mapNotNull null
                try {
                    ZhihuJson.decodeJson<SearchResult>(element)
                } catch (e: Exception) {
                    environment.logDecodeFailure("SearchViewModel", element, e)
                    null
                }
            }
            val feeds = results.mapNotNull(SearchResult::toFeed)
            val existingPeopleIds = peopleResults.mapTo(mutableSetOf()) { it.people.id }
            results.mapNotNull(SearchResult::people).forEach { result ->
                if (existingPeopleIds.add(result.people.id)) peopleResults.add(result)
            }
            if (searchTab == SearchTab.Topic) {
                val existingTopicIds = topicResults.mapTo(mutableSetOf()) { it.topic.id }
                val decodedTopics = jsonArray.mapNotNull { element ->
                    decodeTopicSearchResult(element).also { result ->
                        if (result == null) {
                            environment.logDecodeFailure(
                                "SearchViewModel",
                                element,
                                IllegalArgumentException("话题搜索结果缺少可用的话题对象"),
                            )
                        }
                    }
                }
                if (jsonArray.isNotEmpty() && decodedTopics.isEmpty()) {
                    error("服务端返回了 ${jsonArray.size} 条话题搜索结果，但均无法解码")
                }
                decodedTopics.forEach { result ->
                    if (existingTopicIds.add(result.topic.id)) topicResults.add(result)
                }
            }

            if (searchTab == SearchTab.Topic) {
                debugData.addAll(jsonArray)
            } else {
                processResponse(environment, feeds, jsonArray)
            }

            // Handle pagination
            if ("paging" in jojo) {
                lastPaging = ZhihuJson.decodeJson<ZhihuPaging>(jojo["paging"]!!)
            }
        } catch (e: Exception) {
            environment.handleFetchFailure("SearchViewModel", e)
            throw e
        } finally {
            isLoading = false
        }
    }

    override fun processResponse(
        environment: PaginationEnvironment,
        data: List<Feed>,
        rawData: JsonArray,
    ) {
        val blockedUserIds = environment.blockedUserIds()
        // 进行搜索filter逻辑。目前仅支持作者。
        val filtered = if (blockedUserIds.isEmpty()) {
            data
        } else {
            data.filterNot { feed ->
                feed.target?.author?.id in blockedUserIds
            }
        }
        super.processResponse(environment, filtered, rawData)
    }
}

enum class SearchSortOption(
    val label: String,
    val value: String,
) {
    Default("综合排序", ""),
    Latest("最新发布", "created_time"),
    MostVoted("最多赞同", "upvoted_count"),
}

enum class SearchContentType(
    val label: String,
    val value: String,
) {
    All("全部内容", ""),
    Answer("回答", "answer"),
    Article("文章", "article"),
    Video("视频", "zvideo"),
}

enum class SearchTab(
    val label: String,
) {
    General("全站"),
    People("用户"),
    Topic("话题"),
}

data class TopicSearchResult(
    val topic: DataHolder.Topic,
    val excerpt: String,
    val visitCount: Long,
    val discussCount: Long,
    val isFollowing: Boolean,
)

@Serializable
private data class TopicSearchObject(
    val id: String,
    val type: String,
    val url: String,
    val name: String,
    val avatarUrl: String? = null,
    val topicType: String? = null,
    val excerpt: String = "",
    val visitCount: Long = 0,
    val topAnswerCount: Long = 0,
    val isFollowing: Boolean = false,
)

fun decodeTopicSearchResult(element: JsonElement): TopicSearchResult? {
    val entry = element as? JsonObject ?: return null
    val objectJson = entry["object"] as? JsonObject ?: return null
    val decoded = runCatching { ZhihuJson.decodeJson<TopicSearchObject>(objectJson) }.getOrNull() ?: return null
    if (decoded.type != "topic") return null
    return TopicSearchResult(
        topic = DataHolder.Topic(
            id = decoded.id,
            type = decoded.type,
            url = decoded.url,
            name = decoded.name.replace("<em>", "").replace("</em>", ""),
            avatarUrl = decoded.avatarUrl,
            topicType = decoded.topicType,
        ),
        excerpt = decoded.excerpt.replace("<em>", "").replace("</em>", ""),
        visitCount = decoded.visitCount,
        discussCount = decoded.topAnswerCount,
        isFollowing = decoded.isFollowing,
    )
}

enum class SearchTimeRange(
    val label: String,
    val value: String,
) {
    All("不限时间", ""),
    Day("一天内", "a_day"),
    Week("一周内", "a_week"),
    Month("一个月内", "a_month"),
    ThreeMonths("三个月内", "three_months"),
    HalfYear("半年内", "half_a_year"),
    Year("一年内", "a_year"),
}

fun zhihuSearchUrl(
    query: String,
    searchTab: SearchTab = SearchTab.General,
    sortOption: SearchSortOption = SearchSortOption.Default,
    contentType: SearchContentType = SearchContentType.All,
    timeRange: SearchTimeRange = SearchTimeRange.All,
    restrictedMemberHashId: String = "",
): String {
    val hasActiveFilter = sortOption != SearchSortOption.Default ||
        contentType != SearchContentType.All ||
        timeRange != SearchTimeRange.All
    val params = buildList {
        add("gk_version" to "gz-gaokao")
        add(
            "t" to when (searchTab) {
                SearchTab.People -> "people"
                SearchTab.Topic -> "topic"
                SearchTab.General -> "general"
            },
        )
        add("q" to query)
        add("correction" to "1")
        add("offset" to "0")
        add("limit" to "20")
        add("search_source" to if (hasActiveFilter) "Filter" else "Normal")
        add("show_all_topics" to if (searchTab == SearchTab.Topic) "1" else "0")
        if (restrictedMemberHashId.isNotBlank()) {
            add("filter_fields" to "")
            add("lc_idx" to "0")
            add("restricted_scene" to "member")
            add("restricted_field" to "member_hash_id")
            add("restricted_value" to restrictedMemberHashId)
        }
        if (contentType.value.isNotEmpty()) {
            add("vertical" to contentType.value)
            add("vertical_info" to SEARCH_VERTICAL_INFO)
        }
        if (sortOption.value.isNotEmpty()) {
            add("sort" to sortOption.value)
        }
        if (timeRange.value.isNotEmpty()) {
            add("time_interval" to timeRange.value)
        }
    }.joinToString("&") { (key, value) ->
        "$key=${value.encodeURLParameter(spaceToPlus = true)}"
    }
    return "https://www.zhihu.com/api/v4/search_v3?$params"
}
