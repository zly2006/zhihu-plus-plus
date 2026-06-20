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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.SearchResult
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.ZhihuPaging
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.jsonArray

const val ZHIHU_HOT_SEARCH_URL = "https://www.zhihu.com/api/v4/search/hot_search"
private const val SEARCH_VERTICAL_INFO = "0,0,0,0,0,0,0,0,0,0,0,0"

class SearchViewModel(
    val searchQuery: String,
    val restrictedMemberHashId: String = "",
) : BaseFeedViewModel() {
    var sortOption by mutableStateOf(SearchSortOption.Default)
        private set
    var contentType by mutableStateOf(SearchContentType.All)
        private set
    var timeRange by mutableStateOf(SearchTimeRange.All)
        private set

    val initialRequestUrl: String
        get() = initialUrl

    override val initialUrl: String
        get() = zhihuSearchUrl(searchQuery, sortOption, contentType, timeRange, restrictedMemberHashId)

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

    fun updateTimeRange(
        environment: PaginationEnvironment,
        range: SearchTimeRange,
    ) {
        if (timeRange == range) return
        timeRange = range
        refresh(environment)
    }

    private val filterPipeline = SearchFeedFilterPipeline()

    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val url = lastPaging?.next ?: initialUrl
            val jojo = environment.fetchJson(url, include)!!
            val jsonArray = jojo["data"]!!.jsonArray

            // Parse search results and convert to Feed objects
            val feeds = jsonArray.mapNotNull { element ->
                try {
                    val searchResult = ZhihuJson.decodeJson<SearchResult>(element)
                    searchResult.toFeed()
                } catch (e: Exception) {
                    environment.logDecodeFailure("SearchViewModel", element, e)
                    null
                }
            }

            processResponse(environment, feeds, jsonArray)

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
        super.processResponse(environment, filterPipeline.filter(data, environment), rawData)
    }
}

/**
 * 搜索结果使用更保守的过滤策略：只应用用户明确加入本地屏蔽列表的作者规则。
 *
 * 推荐流可以继续在内容详情、质量、广告、关键词和主题等阶段做更激进的筛选；搜索的目标是尽量完整展示
 * 命中内容，所以这里把搜索可复用的阶段收敛为“显式作者屏蔽”。后续如果要给搜索添加其它明确规则，
 * 应该作为新的阶段接到这个 pipeline，而不是复用推荐流的整套过滤。
 */
private class SearchFeedFilterPipeline {
    fun filter(
        feeds: List<Feed>,
        environment: PaginationEnvironment,
    ): List<Feed> {
        val blockedUserIds = environment.blockedUserIds()
        if (blockedUserIds.isEmpty()) return feeds

        return feeds.filterNot { feed ->
            feed.target?.author?.id in blockedUserIds
        }
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
        add("t" to "general")
        add("q" to query)
        add("correction" to "1")
        add("offset" to "0")
        add("limit" to "20")
        add("search_source" to if (hasActiveFilter) "Filter" else "Normal")
        add("show_all_topics" to "0")
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
