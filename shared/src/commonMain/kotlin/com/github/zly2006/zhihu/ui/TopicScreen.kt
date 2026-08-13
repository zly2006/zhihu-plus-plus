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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
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
import com.github.zly2006.zhihu.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Topic
import com.github.zly2006.zhihu.navigation.WritePin
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.ui.components.ShareDialog
import com.github.zly2006.zhihu.ui.components.getShareText
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareActionExecutor
import com.github.zly2006.zhihu.util.raiseForStatus
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.deleteSigned
import com.github.zly2006.zhihu.viewmodel.postSigned
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import io.ktor.http.Url
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val TOPIC_SCREEN_TAG = "topic_screen"
const val TOPIC_SHARE_BUTTON_TAG = "topic_share_button"
const val TOPIC_FOLLOW_BUTTON_TAG = "topic_follow_button"
const val TOPIC_WRITE_PIN_BUTTON_TAG = "topic_write_pin_button"
const val TOPIC_RELATED_TAG = "topic_related"

@Serializable
data class TopicDetail(
    val id: String,
    val name: String = "",
    val introduction: String = "",
    val avatarUrl: String? = null,
    val followersCount: Int = 0,
    val questionsCount: Int = 0,
    val isFollowing: Boolean = false,
)

@Serializable
data class TopicBestAnswerer(
    val member: DataHolder.People,
    val answerVotes: Int = 0,
    val totalVotes: Int = 0,
    val answerCount: Int = 0,
    val articleCount: Int = 0,
)

enum class TopicFeedTab(
    val title: String,
) {
    Discussion("讨论"),
    Ideas("想法"),
    Unanswered("待回答"),
}

enum class TopicDiscussionSort(
    val title: String,
) {
    Essence("精华"),
    Hot("热度"),
    Timeline("时间"),
}

enum class TopicIdeasSort(
    val title: String,
    val endpoint: String,
) {
    Hot("最热", "pin-hot"),
    Latest("最新", "pin-new"),
}

@Serializable
data class TopicPinFeed(
    val type: String,
    val target: TopicPinTarget,
)

@Serializable
data class TopicPinTarget(
    val id: JsonPrimitive,
    val type: String,
    val url: String = "",
    val author: TopicPinAuthor,
    val title: String = "",
    val excerpt: String = "",
    val content: String = "",
    val plainContent: String = "",
    val counter: TopicPinCounter = TopicPinCounter(),
)

@Serializable
data class TopicPinAuthor(
    val avatarUrl: String = "",
    val name: String = "",
)

@Serializable
data class TopicPinCounter(
    val applaud: Int = 0,
    val comment: Int = 0,
    val favorite: Int = 0,
    val forward: Int = 0,
    val pv: Int = 0,
)

class TopicViewModel(
    private val topicId: String,
    initialDetail: TopicDetail? = null,
) : ViewModel() {
    var detail by mutableStateOf(initialDetail)
        private set
    val items = mutableStateListOf<FeedDisplayItem>()
    val parentTopics = mutableStateListOf<DataHolder.Topic>()
    val childTopics = mutableStateListOf<DataHolder.Topic>()
    val bestAnswerers = mutableStateListOf<TopicBestAnswerer>()
    var selectedTab by mutableStateOf(TopicFeedTab.Discussion)
        private set
    var discussionSort by mutableStateOf(TopicDiscussionSort.Hot)
        private set
    var ideasSort by mutableStateOf(TopicIdeasSort.Hot)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var detailErrorMessage by mutableStateOf<String?>(null)
        private set
    private var nextUrl: String? = null
    var isEnd by mutableStateOf(false)
        private set
    private var requestGeneration = 0L
    private var loadJob: Job? = null
    var isFollowingChanging by mutableStateOf(false)
        private set

    suspend fun loadDetail(environment: PaginationEnvironment) {
        detailErrorMessage = null
        runCatching {
            environment.fetchJson(
                "https://www.zhihu.com/api/v4/topics/$topicId",
                "name,introduction,avatar_url,followers_count,questions_count,is_following",
            ) ?: error("话题详情响应为空")
        }.onSuccess { detail = ZhihuJson.decodeJson(TopicDetail.serializer(), it) }
            .onFailure {
                if (it is CancellationException) throw it
                detailErrorMessage = it.message
            }
    }

    suspend fun loadSupportingContent(environment: PaginationEnvironment) {
        suspend fun loadTopics(url: String, destination: MutableList<DataHolder.Topic>) {
            val json = environment.fetchJson(url, "") ?: return
            destination.clear()
            destination += (json["data"] as? JsonArray).orEmpty().mapNotNull {
                runCatching { ZhihuJson.decodeJson(DataHolder.Topic.serializer(), it) }.getOrNull()
            }
        }
        runCatching { loadTopics("https://www.zhihu.com/api/v3/topics/$topicId/parent", parentTopics) }
            .onFailure { if (it is CancellationException) throw it }
        runCatching { loadTopics("https://www.zhihu.com/api/v3/topics/$topicId/children?limit=10&offset=0", childTopics) }
            .onFailure { if (it is CancellationException) throw it }
        runCatching {
            val json = environment.fetchJson(
                "https://www.zhihu.com/api/v4/topics/$topicId/best_answerers?limit=3",
                "data[*].member,answer_votes,total_votes,answer_count,article_count",
            ) ?: return@runCatching
            bestAnswerers.clear()
            bestAnswerers += (json["data"] as? JsonArray).orEmpty().mapNotNull {
                runCatching { ZhihuJson.decodeJson(TopicBestAnswerer.serializer(), it) }.getOrNull()
            }
        }.onFailure { if (it is CancellationException) throw it }
    }

    fun selectTab(environment: PaginationEnvironment, tab: TopicFeedTab) {
        if (selectedTab == tab && items.isNotEmpty()) return
        selectedTab = tab
        items.clear()
        nextUrl = null
        isEnd = false
        requestGeneration++
        loadJob?.cancel()
        loadMore(environment)
    }

    fun selectDiscussionSort(environment: PaginationEnvironment, sort: TopicDiscussionSort) {
        if (discussionSort == sort) return
        discussionSort = sort
        items.clear()
        nextUrl = null
        isEnd = false
        requestGeneration++
        loadJob?.cancel()
        loadMore(environment)
    }

    fun selectIdeasSort(environment: PaginationEnvironment, sort: TopicIdeasSort) {
        if (ideasSort == sort) return
        ideasSort = sort
        items.clear()
        nextUrl = null
        isEnd = false
        requestGeneration++
        loadJob?.cancel()
        loadMore(environment)
    }

    suspend fun loadMoreNow(environment: PaginationEnvironment) {
        if (isEnd) return
        val generation = requestGeneration
        isLoading = true
        errorMessage = null
        runCatching {
            val url = nextUrl ?: topicFeedUrl(topicId, selectedTab, discussionSort, ideasSort)
            environment.fetchJson(url, "data[*].content,excerpt,target.author.badge_v2")
                ?: error("话题内容响应为空")
        }.onSuccess { json ->
            if (generation != requestGeneration) return@onSuccess
            val loadedItems = if (selectedTab == TopicFeedTab.Ideas) {
                decodeTopicPinFeeds(json)
            } else {
                val feeds = (json["data"] as? JsonArray).orEmpty().mapNotNull { element ->
                    runCatching { ZhihuJson.decodeJson<Feed>(element) }.getOrNull()
                }
                feeds.flattenFeeds().map { it.toDisplayItem(enableQualityFilter = false) }
            }
            loadedItems.forEach { item ->
                if (items.none { it.stableKey == item.stableKey }) items += item
            }
            val paging = json["paging"]
            val rawNext = paging
                ?.let { runCatching { it.jsonObject["next"]?.jsonPrimitive?.content }.getOrNull() }
            nextUrl = rawNext?.let(::normalizeTopicPagingUrl)
            isEnd = if (rawNext != null && nextUrl == null) {
                errorMessage = "服务端返回了不受信任的分页地址，已停止加载"
                true
            } else {
                paging?.let { runCatching { it.jsonObject["is_end"]?.jsonPrimitive?.content == "true" }.getOrDefault(false) }
                    ?: true
            }
        }.onFailure { if (generation == requestGeneration) errorMessage = it.message }
        if (generation == requestGeneration) isLoading = false
    }

    fun loadMore(environment: PaginationEnvironment) {
        if (isEnd || errorMessage != null || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch { loadMoreNow(environment) }
    }

    fun retry(environment: PaginationEnvironment) {
        errorMessage = null
        loadMore(environment)
    }

    fun initializeSection(section: String) {
        selectedTab = when (section) {
            "unanswered" -> TopicFeedTab.Unanswered
            else -> TopicFeedTab.Discussion
        }
        discussionSort = if (section == "top-answers") TopicDiscussionSort.Essence else TopicDiscussionSort.Hot
    }

    suspend fun setFollowing(environment: ZhihuApiEnvironment, following: Boolean): Result<Unit> {
        val current = detail ?: return Result.failure(IllegalStateException("话题详情尚未加载"))
        if (isFollowingChanging || current.isFollowing == following) return Result.success(Unit)
        isFollowingChanging = true
        detail = current.copy(
            isFollowing = following,
            followersCount = (current.followersCount + if (following) 1 else -1).coerceAtLeast(0),
        )
        return runCatching {
            val endpoint = "https://www.zhihu.com/api/v4/topics/$topicId/followers"
            if (following) environment.postSigned(endpoint) else environment.deleteSigned(endpoint)
        }.mapCatching { response ->
            response.raiseForStatus()
            Unit
        }.onFailure {
            detail = current
        }.also {
            isFollowingChanging = false
        }
    }
}

fun topicFeedUrl(
    topicId: String,
    tab: TopicFeedTab,
    discussionSort: TopicDiscussionSort = TopicDiscussionSort.Hot,
    ideasSort: TopicIdeasSort = TopicIdeasSort.Hot,
): String = when (tab) {
    TopicFeedTab.Discussion -> {
        when (discussionSort) {
            TopicDiscussionSort.Essence -> "https://www.zhihu.com/api/v5.1/topics/$topicId/feeds/essence/v2?limit=20&offset=0"
            TopicDiscussionSort.Hot -> "https://www.zhihu.com/api/v4/topics/$topicId/feeds/top_activity?limit=20&offset=0"
            TopicDiscussionSort.Timeline -> "https://www.zhihu.com/api/v4/topics/$topicId/feeds/timeline_activity?limit=20&offset=0"
        }
    }
    TopicFeedTab.Ideas -> "https://api.zhihu.com/v5.1/topics/$topicId/feeds/${ideasSort.endpoint}?offset=0&limit=10"
    TopicFeedTab.Unanswered -> "https://www.zhihu.com/api/v4/topics/$topicId/unanswered_questions?limit=20&offset=0"
}

fun decodeTopicPinFeeds(json: kotlinx.serialization.json.JsonObject): List<FeedDisplayItem> =
    (json["data"] as? JsonArray).orEmpty().mapNotNull { element ->
        runCatching { ZhihuJson.decodeJson<TopicPinFeed>(element) }.getOrNull()?.target?.let { target ->
            val pinId = target.id.content.toLongOrNull() ?: return@let null
            FeedDisplayItem(
                title = target.title.ifBlank { "想法" },
                summary = target.plainContent.ifBlank { target.excerpt.ifBlank { target.content } }.takeIf(String::isNotBlank),
                details = "想法 · ${target.counter.applaud} 赞 · ${target.counter.comment} 评论",
                feed = null,
                navDestinationJson = com.github.zly2006.zhihu.navigation
                    .Pin(pinId)
                    .toFeedDisplayItemNavDestinationJson(),
                avatarSrc = target.author.avatarUrl,
                authorName = target.author.name,
            )
        }
    }

fun topicSearchUrl(query: String): String =
    "https://www.zhihu.com/api/v4/search_v3?t=topic&q=${query.encodeURLParameter(spaceToPlus = true)}&offset=0&limit=20"

fun normalizeTopicPagingUrl(rawUrl: String): String? {
    val url = Url(rawUrl)
    if (url.host == "www.zhihu.com" &&
        url.encodedPath.startsWith("/api/v4/") ||
        url.host == "www.zhihu.com" &&
        url.encodedPath.startsWith("/api/v5.1/")
    ) {
        return "https://www.zhihu.com${url.encodedPath}" + url.encodedQuery
            .takeIf(String::isNotEmpty)
            ?.let { "?$it" }
            .orEmpty()
    }
    if (url.host == "api.zhihu.com" && url.encodedPath.startsWith("/v5.1/topics/")) {
        return "https://api.zhihu.com${url.encodedPath}" + url.encodedQuery
            .takeIf(String::isNotEmpty)
            ?.let { "?$it" }
            .orEmpty()
    }
    if (url.host != "172.16.201.121" || url.port != 80) return null
    return buildString {
        append("https://www.zhihu.com/api/v4")
        append(url.encodedPath)
        if (url.encodedQuery.isNotEmpty()) append('?').append(url.encodedQuery)
    }
}

suspend fun searchTopics(environment: PaginationEnvironment, query: String): List<DataHolder.Topic> {
    if (query.isBlank()) return emptyList()
    val json = environment.fetchJson(topicSearchUrl(query.trim()), "data[*].object,type") ?: return emptyList()
    return decodeTopicSearchResults(json)
}

fun decodeTopicSearchResults(json: kotlinx.serialization.json.JsonObject): List<DataHolder.Topic> =
    (json["data"] as? JsonArray)
        .orEmpty()
        .mapNotNull { entry ->
            val obj = entry.jsonObject["object"]?.jsonObject ?: return@mapNotNull null
            if (obj["type"]?.jsonPrimitive?.content != "topic") return@mapNotNull null
            runCatching { ZhihuJson.decodeJson(DataHolder.Topic.serializer(), obj) }
                .getOrNull()
                ?.let { topic ->
                    topic.copy(name = topic.name.replace("<em>", "").replace("</em>", ""))
                }
        }.distinctBy(DataHolder.Topic::id)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicScreen(topic: Topic) {
    val navigator = LocalNavigator.current
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val messages = rememberUserMessageSink()
    val settings = rememberSettingsStore()
    val executeShareAction = rememberShareActionExecutor()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showShareDialog by androidx.compose.runtime.remember { mutableStateOf(false) }
    var isIntroductionExpanded by rememberSaveable(topic.id) { mutableStateOf(false) }
    var isSupportingContentExpanded by rememberSaveable(topic.id) { mutableStateOf(false) }
    val viewModel: TopicViewModel = viewModel(key = "topic_${topic.id}") { TopicViewModel(topic.id) }

    LaunchedEffect(topic.id) {
        viewModel.initializeSection(topic.section)
        launch { viewModel.loadDetail(environment) }
        launch { viewModel.loadSupportingContent(environment) }
        launch { viewModel.loadMoreNow(environment) }
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
                            val loadedTopic = topic.copy(name = viewModel.detail?.name ?: topic.name)
                            handleShareAction(loadedTopic, settings, executeShareAction) { showShareDialog = true }
                        },
                    ) { Icon(Icons.Default.Share, contentDescription = "分享") }
                },
            )
        },
    ) { padding ->
        PaginatedList(
            items = viewModel.items,
            onLoadMore = { viewModel.loadMore(environment) },
            isEnd = { viewModel.isEnd },
            key = FeedDisplayItem::stableKey,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            footer = { listState ->
                if (viewModel.errorMessage == null) {
                    ProgressIndicatorFooter(listState)
                } else {
                    TextButton(
                        onClick = { viewModel.retry(environment) },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    ) { Text("加载失败，点击重试") }
                }
            },
            topContent = {
                item {
                    TopicHeader(
                        detail = viewModel.detail,
                        detailErrorMessage = viewModel.detailErrorMessage,
                        destination = topic,
                        isIntroductionExpanded = isIntroductionExpanded,
                        onIntroductionExpandedChange = { isIntroductionExpanded = it },
                        isFollowingChanging = viewModel.isFollowingChanging,
                        onFollowingChange = { following ->
                            scope.launch {
                                viewModel.setFollowing(environment, following).onFailure {
                                    messages.showShortMessage("${if (following) "关注" else "取消关注"}失败：${it.message}")
                                }
                            }
                        },
                    )
                    Button(
                        onClick = {
                            navigator.onNavigate(
                                WritePin(
                                    topicId = topic.id,
                                    topicName = viewModel.detail?.name?.ifBlank { topic.name } ?: topic.name,
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag(TOPIC_WRITE_PIN_BUTTON_TAG),
                    ) { Text("发想法") }
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
                    if (viewModel.selectedTab == TopicFeedTab.Discussion) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TopicDiscussionSort.entries.forEach { sort ->
                                androidx.compose.material3.FilterChip(
                                    selected = viewModel.discussionSort == sort,
                                    onClick = { viewModel.selectDiscussionSort(environment, sort) },
                                    label = { Text(sort.title) },
                                )
                            }
                        }
                    }
                    if (viewModel.selectedTab == TopicFeedTab.Ideas) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TopicIdeasSort.entries.forEach { sort ->
                                androidx.compose.material3.FilterChip(
                                    selected = viewModel.ideasSort == sort,
                                    onClick = { viewModel.selectIdeasSort(environment, sort) },
                                    label = { Text(sort.title) },
                                )
                            }
                        }
                    }
                    if (viewModel.parentTopics.isNotEmpty() || viewModel.childTopics.isNotEmpty() || viewModel.bestAnswerers.isNotEmpty()) {
                        TextButton(
                            onClick = { isSupportingContentExpanded = !isSupportingContentExpanded },
                            modifier = Modifier.fillMaxWidth().testTag(TOPIC_RELATED_TAG),
                        ) {
                            Text(if (isSupportingContentExpanded) "收起相关话题与答主" else "展开相关话题与答主")
                        }
                        if (isSupportingContentExpanded) {
                            TopicSupportingContent(
                                parentTopics = viewModel.parentTopics,
                                childTopics = viewModel.childTopics,
                                bestAnswerers = viewModel.bestAnswerers,
                            )
                        }
                    }
                }
            },
        ) { item ->
            FeedCard(item = item, modifier = Modifier.testTag("topic_feed_${item.stableKey}"))
        }
    }
    val loadedTopic = topic.copy(name = viewModel.detail?.name ?: topic.name)
    getShareText(loadedTopic)?.let { shareText ->
        ShareDialog(
            content = loadedTopic,
            shareText = shareText,
            showDialog = showShareDialog,
            onDismissRequest = { showShareDialog = false },
        )
    }
}

@Composable
private fun TopicHeader(
    detail: TopicDetail?,
    detailErrorMessage: String?,
    destination: Topic,
    isIntroductionExpanded: Boolean,
    onIntroductionExpandedChange: (Boolean) -> Unit,
    isFollowingChanging: Boolean,
    onFollowingChange: (Boolean) -> Unit,
) {
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
                when {
                    detail != null -> Text("${detail.followersCount} 关注 · ${detail.questionsCount} 问题")
                    detailErrorMessage != null -> Text("话题信息加载失败", color = MaterialTheme.colorScheme.error)
                    else -> Text("正在加载话题信息…")
                }
            }
        }
        detail?.introduction?.takeIf(String::isNotBlank)?.let { introduction ->
            Text(
                text = introduction,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isIntroductionExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = { onIntroductionExpandedChange(!isIntroductionExpanded) }) {
                Text(if (isIntroductionExpanded) "收起简介" else "展开简介")
            }
        }
        detail?.let { loaded ->
            Button(
                onClick = { onFollowingChange(!loaded.isFollowing) },
                enabled = !isFollowingChanging,
                modifier = Modifier.testTag(TOPIC_FOLLOW_BUTTON_TAG),
            ) {
                if (isFollowingChanging) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (loaded.isFollowing) "已关注" else "关注话题")
                }
            }
        }
    }
}

@Composable
private fun TopicSupportingContent(
    parentTopics: List<DataHolder.Topic>,
    childTopics: List<DataHolder.Topic>,
    bestAnswerers: List<TopicBestAnswerer>,
) {
    val navigator = LocalNavigator.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (parentTopics.isNotEmpty()) {
            Text("父话题", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                parentTopics.forEach { parent ->
                    TextButton(onClick = { navigator.onNavigate(Topic(parent.id, parent.name)) }) { Text(parent.name) }
                }
            }
        }
        if (childTopics.isNotEmpty()) {
            Text("子话题", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                childTopics.forEach { child ->
                    TextButton(
                        onClick = { navigator.onNavigate(Topic(child.id, child.name)) },
                    ) { Text(child.name) }
                }
            }
        }
        if (bestAnswerers.isNotEmpty()) {
            Text("优秀答主", style = MaterialTheme.typography.titleMedium)
            bestAnswerers.forEach { item ->
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        navigator.onNavigate(
                            Person(
                                id = item.member.id,
                                urlToken = item.member.urlToken.orEmpty(),
                                name = item.member.name,
                            ),
                        )
                    },
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(item.member.name)
                        Text(
                            "${item.answerCount} 回答 · ${item.articleCount} 文章 · ${item.answerVotes} 回答赞同",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
