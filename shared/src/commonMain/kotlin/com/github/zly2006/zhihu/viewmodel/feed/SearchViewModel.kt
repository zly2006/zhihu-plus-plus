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
import com.github.zly2006.zhihu.data.CommonFeed
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.util.raiseForStatus
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.deleteSigned
import com.github.zly2006.zhihu.viewmodel.postSigned
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

const val ZHIHU_HOT_SEARCH_URL = "https://www.zhihu.com/api/v4/search/hot_search"
private const val SEARCH_VERTICAL_INFO = "0,0,0,0,0,0,0,0,0,0,0,0"

open class SearchViewModel(
    val searchQuery: String,
    private val restrictedMemberHashId: String = "",
) : BaseFeedViewModel() {
    val entities = mutableStateListOf<SearchEntity>()
    private var pendingGeneralEntities = emptyList<PendingGeneralEntity>()
    val changingTopicIds = mutableStateListOf<String>()
    var filters by mutableStateOf(SearchFilters())
        private set
    var searchTab by mutableStateOf(SearchTab.General)
        private set

    override val initialUrl: String
        get() = zhihuSearchUrl(searchQuery, searchTab, filters, restrictedMemberHashId)

    override val include = "data[*].highlight,object,type"

    fun selectTab(
        environment: PaginationEnvironment,
        tab: SearchTab,
    ) {
        if (searchTab == tab) return
        searchTab = tab
        refresh(environment)
    }

    fun updateFilters(
        environment: PaginationEnvironment,
        newFilters: SearchFilters,
    ) {
        if (filters == newFilters) return
        filters = newFilters
        refresh(environment)
    }

    suspend fun setTopicFollowing(
        environment: PaginationEnvironment,
        topicId: String,
        following: Boolean,
    ): Result<Unit> {
        val index = entities.indexOfFirst { it is SearchEntity.Topic && it.id == topicId }
        val previous = entities.getOrNull(index) as? SearchEntity.Topic
            ?: return Result.success(Unit)
        if (topicId in changingTopicIds || previous.isFollowing == following) return Result.success(Unit)

        changingTopicIds += topicId
        entities[index] = previous.copy(isFollowing = following)
        return runCatching {
            val endpoint = "https://www.zhihu.com/api/v4/topics/$topicId/followers"
            val response = if (following) environment.postSigned(endpoint) else environment.deleteSigned(endpoint)
            response.raiseForStatus()
            Unit
        }.onFailure {
            val currentIndex = entities.indexOfFirst { it.id == topicId }
            if (currentIndex >= 0) entities[currentIndex] = previous
        }.also {
            changingTopicIds -= topicId
        }
    }

    override fun refresh(environment: PaginationEnvironment) {
        entities.clear()
        super.refresh(environment)
    }

    fun retry(environment: PaginationEnvironment) {
        errorMessage = null
        loadMore(environment)
    }

    override fun decodePage(
        environment: PaginationEnvironment,
        rawData: JsonArray,
    ): List<Feed> {
        val existingIds = entities.mapTo(mutableSetOf(), SearchEntity::id)
        var decodedTopicCount = 0
        val indexedEntries = rawData.withIndex().sortedBy { (responseOrder, element) ->
            (element as? JsonObject)
                ?.get("index")
                ?.jsonPrimitive
                ?.content
                ?.toIntOrNull() ?: responseOrder
        }
        if (searchTab == SearchTab.General) {
            pendingGeneralEntities = indexedEntries.mapNotNull { (_, element) ->
                val entry = element as? JsonObject ?: return@mapNotNull null
                if (entry["type"]?.jsonPrimitive?.content != "search_result") return@mapNotNull null
                val content = entry["object"] as? JsonObject ?: return@mapNotNull null
                try {
                    if (content["type"]?.jsonPrimitive?.content == "people") {
                        PendingGeneralEntity.Person(ZhihuJson.decodeJson<DataHolder.People>(content))
                    } else {
                        PendingGeneralEntity.Content(
                            CommonFeed(
                                id = content["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                                verb = "SEARCH_RESULT",
                                target = ZhihuJson.decodeJson<Feed.Target>(content),
                            ),
                        )
                    }
                } catch (e: Exception) {
                    environment.logDecodeFailure("SearchViewModel", element, e)
                    null
                }
            }
            return pendingGeneralEntities.mapNotNull { (it as? PendingGeneralEntity.Content)?.feed }
        }
        val feeds = indexedEntries.mapNotNull { (_, element) ->
            val entry = element as? JsonObject ?: return@mapNotNull null
            if (entry["type"]?.jsonPrimitive?.content != "search_result") return@mapNotNull null
            val content = entry["object"] as? JsonObject ?: return@mapNotNull null
            try {
                when (searchTab) {
                    SearchTab.General -> error("General 搜索已在保序分支解码")
                    SearchTab.People -> {
                        val person = ZhihuJson.decodeJson<DataHolder.People>(content)
                        if (existingIds.add(person.id)) entities += SearchEntity.Person(person)
                        null
                    }
                    SearchTab.Topic -> {
                        val topic = ZhihuJson.decodeJson<TopicSearchObject>(content)
                        if (topic.type != "topic") return@mapNotNull null
                        decodedTopicCount++
                        if (existingIds.add(topic.id)) {
                            entities += SearchEntity.Topic(
                                topic = DataHolder.Topic(
                                    id = topic.id,
                                    type = topic.type,
                                    url = topic.url,
                                    name = topic.name.replace("<em>", "").replace("</em>", ""),
                                    avatarUrl = topic.avatarUrl,
                                    topicType = topic.topicType,
                                ),
                                excerpt = topic.excerpt.replace("<em>", "").replace("</em>", ""),
                                visitCount = topic.visitCount,
                                discussCount = topic.topAnswerCount,
                                isFollowing = topic.isFollowing,
                            )
                        }
                        null
                    }
                }
            } catch (e: Exception) {
                environment.logDecodeFailure("SearchViewModel", element, e)
                null
            }
        }
        if (searchTab == SearchTab.Topic && rawData.isNotEmpty() && decodedTopicCount == 0) {
            error("服务端返回了 ${rawData.size} 条话题搜索结果，但均无法解码")
        }
        return feeds
    }

    override fun processResponse(
        environment: PaginationEnvironment,
        data: List<Feed>,
        rawData: JsonArray,
    ) {
        val blockedUserIds = environment.blockedUserIds()
        super.processResponse(
            environment,
            data.filterNot { it.target?.author?.id in blockedUserIds },
            rawData,
        )
        if (searchTab != SearchTab.General) return
        if (isPullToRefresh) entities.clear()
        val loadedContent = latestLoadedDisplayItems.value.associateBy(FeedDisplayItem::stableKey)
        val existingIds = entities.mapTo(mutableSetOf(), SearchEntity::id)
        pendingGeneralEntities.forEach { pending ->
            val entity = when (pending) {
                is PendingGeneralEntity.Person -> SearchEntity.Person(pending.person)
                is PendingGeneralEntity.Content -> createDisplayItem(environment, pending.feed)
                    .stableKey
                    .let(loadedContent::get)
                    ?.let { SearchEntity.Content(it) }
            }
            if (entity != null && existingIds.add(entity.id)) entities += entity
        }
    }
}

private sealed interface PendingGeneralEntity {
    data class Person(
        val person: DataHolder.People,
    ) : PendingGeneralEntity

    data class Content(
        val feed: Feed,
    ) : PendingGeneralEntity
}

sealed interface SearchEntity {
    val id: String

    data class Person(
        val person: DataHolder.People,
    ) : SearchEntity {
        override val id = person.id
    }

    data class Content(
        val item: FeedDisplayItem,
    ) : SearchEntity {
        override val id = item.stableKey
    }

    data class Topic(
        val topic: DataHolder.Topic,
        val excerpt: String,
        val visitCount: Long,
        val discussCount: Long,
        val isFollowing: Boolean,
    ) : SearchEntity {
        override val id = topic.id
    }
}

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

data class SearchFilters(
    val sort: SearchSortOption = SearchSortOption.Default,
    val contentType: SearchContentType = SearchContentType.All,
    val timeRange: SearchTimeRange = SearchTimeRange.All,
)

enum class SearchSortOption(
    val label: String,
    val parameter: String,
) {
    Default("综合排序", ""),
    Latest("最新发布", "created_time"),
    MostVoted("最多赞同", "upvoted_count"),
}

enum class SearchContentType(
    val label: String,
    val parameter: String,
) {
    All("全部内容", ""),
    Answer("回答", "answer"),
    Article("文章", "article"),
    Video("视频", "zvideo"),
}

enum class SearchTab(
    val label: String,
    val parameter: String,
) {
    General("全站", "general"),
    People("用户", "people"),
    Topic("话题", "topic"),
}

enum class SearchTimeRange(
    val label: String,
    val parameter: String,
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
    filters: SearchFilters = SearchFilters(),
    restrictedMemberHashId: String = "",
): String {
    val activeFilters = if (searchTab == SearchTab.General) filters else SearchFilters()
    val params = buildList {
        add("gk_version" to "gz-gaokao")
        add("t" to searchTab.parameter)
        add("q" to query)
        add("correction" to "1")
        add("offset" to "0")
        add("limit" to "20")
        add("search_source" to if (activeFilters == SearchFilters()) "Normal" else "Filter")
        add("show_all_topics" to if (searchTab == SearchTab.Topic) "1" else "0")
        if (restrictedMemberHashId.isNotBlank()) {
            add("filter_fields" to "")
            add("lc_idx" to "0")
            add("restricted_scene" to "member")
            add("restricted_field" to "member_hash_id")
            add("restricted_value" to restrictedMemberHashId)
        }
        activeFilters.contentType.parameter.takeIf(String::isNotEmpty)?.let {
            add("vertical" to it)
            add("vertical_info" to SEARCH_VERTICAL_INFO)
        }
        activeFilters.sort.parameter
            .takeIf(String::isNotEmpty)
            ?.let { add("sort" to it) }
        activeFilters.timeRange.parameter
            .takeIf(String::isNotEmpty)
            ?.let { add("time_interval" to it) }
    }.joinToString("&") { (key, value) ->
        "$key=${value.encodeURLParameter(spaceToPlus = true)}"
    }
    return "https://www.zhihu.com/api/v4/search_v3?$params"
}
