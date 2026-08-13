/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.flattenFeeds
import com.github.zly2006.zhihu.data.toDisplayItem
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Topic
import com.github.zly2006.zhihu.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val TOPIC_SCREEN_TAG = "topic_screen"
const val TOPIC_SHARE_BUTTON_TAG = "topic_share_button"

@Serializable
data class TopicDetail(
    val id: String,
    val name: String = "",
    val description: String = "",
    val avatarUrl: String? = null,
    val followersCount: Int = 0,
    val questionsCount: Int = 0,
    val isFollowing: Boolean = false,
)

enum class TopicFeedTab(
    val title: String,
    val endpoint: String,
) {
    Hot("热门", "top_activity"),
    Essence("精华", "essence"),
    Latest("最新", "timeline_activity"),
}

class TopicViewModel(
    private val topicId: String,
) : ViewModel() {
    var detail by mutableStateOf<TopicDetail?>(null)
        private set
    val items = mutableStateListOf<FeedDisplayItem>()
    var selectedTab by mutableStateOf(TopicFeedTab.Hot)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    private var nextUrl: String? = null
    private var isEnd = false

    suspend fun loadDetail(environment: PaginationEnvironment) {
        runCatching {
            environment.fetchJson(
                "https://www.zhihu.com/api/v4/topics/$topicId",
                "name,description,avatar_url,followers_count,questions_count,is_following",
            ) ?: error("话题详情响应为空")
        }.onSuccess { detail = ZhihuJson.decodeJson(TopicDetail.serializer(), it) }
            .onFailure { errorMessage = it.message }
    }

    fun selectTab(environment: PaginationEnvironment, tab: TopicFeedTab) {
        if (selectedTab == tab && items.isNotEmpty()) return
        selectedTab = tab
        items.clear()
        nextUrl = null
        isEnd = false
        loadMore(environment)
    }

    suspend fun loadMoreNow(environment: PaginationEnvironment) {
        if (isLoading || isEnd) return
        isLoading = true
        errorMessage = null
        runCatching {
            val url = nextUrl ?: topicFeedUrl(topicId, selectedTab)
            environment.fetchJson(url, "data[*].content,excerpt,target.author.badge_v2")
                ?: error("话题内容响应为空")
        }.onSuccess { json ->
            val feeds = (json["data"] as? JsonArray).orEmpty().mapNotNull { element ->
                runCatching { ZhihuJson.decodeJson<Feed>(element) }.getOrNull()
            }
            feeds.flattenFeeds().map(Feed::toDisplayItem).forEach { item ->
                if (items.none { it.stableKey == item.stableKey }) items += item
            }
            val paging = json["paging"]
            nextUrl = paging?.let { runCatching { it.jsonObject["next"]?.jsonPrimitive?.content }.getOrNull() }
            isEnd = paging?.let { runCatching { it.jsonObject["is_end"]?.jsonPrimitive?.content == "true" }.getOrDefault(false) }
                ?: true
        }.onFailure { errorMessage = it.message }
        isLoading = false
    }

    fun loadMore(environment: PaginationEnvironment) {
        if (!isLoading && !isEnd) viewModelScope.launch { loadMoreNow(environment) }
    }
}

fun topicFeedUrl(topicId: String, tab: TopicFeedTab): String =
    "https://www.zhihu.com/api/v4/topics/$topicId/feeds/${tab.endpoint}?limit=20&offset=0"

fun topicSearchUrl(query: String): String =
    "https://www.zhihu.com/api/v4/search_v3?gk_version=gz-gaokao&t=general&q=${query.encodeURLParameter(spaceToPlus = true)}" +
        "&correction=1&offset=0&limit=20&search_source=Normal&show_all_topics=1"

suspend fun searchTopics(environment: PaginationEnvironment, query: String): List<DataHolder.Topic> {
    if (query.isBlank()) return emptyList()
    val json = environment.fetchJson(topicSearchUrl(query.trim()), "data[*].object,type") ?: return emptyList()
    return (json["data"] as? JsonArray)
        .orEmpty()
        .mapNotNull { entry ->
            val obj = entry.jsonObject["object"]?.jsonObject ?: return@mapNotNull null
            if (obj["type"]?.jsonPrimitive?.content != "topic") return@mapNotNull null
            runCatching { ZhihuJson.decodeJson(DataHolder.Topic.serializer(), obj) }.getOrNull()
        }.distinctBy(DataHolder.Topic::id)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicScreen(topic: Topic) {
    val navigator = LocalNavigator.current
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val clipboard = rememberPlainTextClipboard()
    val messages = rememberUserMessageSink()
    val viewModel: TopicViewModel = viewModel(key = "topic_${topic.id}") { TopicViewModel(topic.id) }

    LaunchedEffect(topic.id) {
        viewModel.loadDetail(environment)
        viewModel.loadMoreNow(environment)
    }

    Scaffold(
        modifier = Modifier.testTag(TOPIC_SCREEN_TAG),
        topBar = {
            TopAppBar(
                title = { Text(viewModel.detail?.name?.ifBlank { topic.name } ?: topic.name.ifBlank { "话题" }) },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.testTag(TOPIC_SHARE_BUTTON_TAG),
                        onClick = {
                            clipboard("topic-link", "https://www.zhihu.com/topic/${topic.id}")
                            messages.showShortMessage("话题链接已复制")
                        },
                    ) { Icon(Icons.Default.Share, contentDescription = "分享") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                TopicHeader(viewModel.detail, topic)
                PrimaryTabRow(selectedTabIndex = viewModel.selectedTab.ordinal) {
                    TopicFeedTab.entries.forEach { tab ->
                        Tab(
                            selected = viewModel.selectedTab == tab,
                            onClick = { viewModel.selectTab(environment, tab) },
                            text = { Text(tab.title) },
                            modifier = Modifier.testTag("topic_tab_${tab.name.lowercase()}"),
                        )
                    }
                }
            }
            items(viewModel.items, key = FeedDisplayItem::stableKey) { item ->
                FeedCard(item = item, modifier = Modifier.testTag("topic_feed_${item.stableKey}"))
            }
            item {
                when {
                    viewModel.isLoading -> Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                    viewModel.errorMessage != null -> Text(
                        "加载失败：${viewModel.errorMessage}",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                    viewModel.items.isEmpty() -> Text("暂无内容", modifier = Modifier.padding(24.dp))
                    else -> TextButton(
                        onClick = { viewModel.loadMore(environment) },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    ) { Text("加载更多") }
                }
            }
        }
    }
}

@Composable
private fun TopicHeader(detail: TopicDetail?, destination: Topic) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = detail?.avatarUrl,
                contentDescription = "话题头像",
                modifier = Modifier.size(64.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(detail?.name?.ifBlank { destination.name } ?: destination.name, style = MaterialTheme.typography.headlineSmall)
                Text("${detail?.followersCount ?: 0} 关注 · ${detail?.questionsCount ?: 0} 问题")
            }
        }
        detail?.description?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Text(
            if (detail?.isFollowing == true) "已关注（当前只读）" else "关注状态只读",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
