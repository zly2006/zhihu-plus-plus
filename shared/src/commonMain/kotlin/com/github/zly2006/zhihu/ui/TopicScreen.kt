/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.flattenFeeds
import com.github.zly2006.zhihu.data.toDisplayItem
import com.github.zly2006.zhihu.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.navigation.LocalNavigator
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

const val TOPIC_SCREEN_TAG = "topic_screen"
const val TOPIC_SHARE_BUTTON_TAG = "topic_share_button"
const val TOPIC_FOLLOW_BUTTON_TAG = "topic_follow_button"
const val TOPIC_WRITE_PIN_BUTTON_TAG = "topic_write_pin_button"
const val TOPIC_INTRODUCTION_TOGGLE_TAG = "topic_introduction_toggle"
const val TOPIC_RETRY_BUTTON_TAG = "topic_retry_button"

@Serializable
data class TopicDetail(
    val id: String,
    val name: String = "",
    val excerpt: String = "",
    val avatarUrl: String? = null,
    val followersCount: Int = 0,
    val questionsCount: Int = 0,
    val isFollowing: Boolean = false,
    val topicId: Long? = null,
    val totalPv: String = "",
    val discussCount: String = "",
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
    Hot("最热"),
    Timeline("最新"),
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

@Serializable
private data class TopicPaging(
    val isEnd: Boolean = true,
    val next: String? = null,
)

class TopicViewModel(
    private val topicId: String,
    initialDetail: TopicDetail? = null,
) : ViewModel() {
    var detail by mutableStateOf(initialDetail)
        private set
    val items = mutableStateListOf<FeedDisplayItem>()
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
                "https://www.zhihu.com/api/v5.1/topics/$topicId",
                "name,excerpt,avatar_url,followers_count,questions_count,is_following,topic_id,total_pv,discuss_count",
            ) ?: error("话题详情响应为空")
        }.onSuccess { detail = ZhihuJson.decodeJson(TopicDetail.serializer(), it) }
            .onFailure {
                if (it is CancellationException) throw it
                detailErrorMessage = it.message
            }
    }

    fun selectTab(environment: PaginationEnvironment, tab: TopicFeedTab) {
        if (selectedTab == tab && items.isNotEmpty()) return
        loadJob?.cancel()
        loadJob = null
        isLoading = false
        selectedTab = tab
        items.clear()
        errorMessage = null
        nextUrl = null
        isEnd = false
        requestGeneration++
        loadMore(environment)
    }

    fun selectDiscussionSort(environment: PaginationEnvironment, sort: TopicDiscussionSort) {
        if (discussionSort == sort) return
        loadJob?.cancel()
        loadJob = null
        isLoading = false
        discussionSort = sort
        items.clear()
        errorMessage = null
        nextUrl = null
        isEnd = false
        requestGeneration++
        loadMore(environment)
    }

    fun selectIdeasSort(environment: PaginationEnvironment, sort: TopicIdeasSort) {
        if (ideasSort == sort) return
        loadJob?.cancel()
        loadJob = null
        isLoading = false
        ideasSort = sort
        items.clear()
        errorMessage = null
        nextUrl = null
        isEnd = false
        requestGeneration++
        loadMore(environment)
    }

    private suspend fun loadMoreNow(environment: PaginationEnvironment) {
        if (isEnd) return
        val generation = requestGeneration
        isLoading = true
        errorMessage = null
        try {
            val url = nextUrl ?: topicFeedUrl(topicId, selectedTab, discussionSort, ideasSort)
            val json = environment.fetchJson(url, "data[*].content,excerpt,target.author.badge_v2")
                ?: error("话题内容响应为空")
            if (generation != requestGeneration) return
            val responseItems = (json["data"] as? JsonArray).orEmpty()
            val loadedItems = if (selectedTab == TopicFeedTab.Ideas) {
                decodeTopicPinFeeds(json)
            } else {
                val feeds = responseItems.mapNotNull { element ->
                    runCatching { ZhihuJson.decodeJson<Feed>(element) }.getOrNull()
                }
                feeds.flattenFeeds().map { it.toDisplayItem(enableQualityFilter = false) }
            }
            if (responseItems.isNotEmpty() && loadedItems.isEmpty()) {
                error("话题内容解码失败：服务端返回 ${responseItems.size} 项，但没有可显示内容")
            }
            loadedItems.forEach { item ->
                if (items.none { it.stableKey == item.stableKey }) items += item
            }
            val paging = json["paging"]?.let { ZhihuJson.decodeJson<TopicPaging>(it) }
            val rawNext = paging?.next
            nextUrl = rawNext?.let(::normalizeTopicPagingUrl)
            isEnd = if (rawNext != null && nextUrl == null) {
                errorMessage = "服务端返回了不受信任的分页地址，已停止加载"
                true
            } else {
                paging?.isEnd ?: true
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            if (generation == requestGeneration) {
                errorMessage = error.message ?: error::class.simpleName ?: "未知错误"
            }
        } finally {
            if (generation == requestGeneration) isLoading = false
        }
    }

    fun loadMore(environment: PaginationEnvironment) {
        if (isEnd || isLoading || errorMessage != null || loadJob?.isActive == true) return
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
        discussionSort = when (section) {
            "top-answers" -> TopicDiscussionSort.Essence
            "newest" -> TopicDiscussionSort.Timeline
            else -> TopicDiscussionSort.Hot
        }
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
            TopicDiscussionSort.Essence -> "https://www.zhihu.com/api/v5.1/topics/$topicId/feeds/top_activity/v2?limit=20&offset=0"
            TopicDiscussionSort.Hot -> "https://www.zhihu.com/api/v5.1/topics/$topicId/feeds/essence/v2?limit=20&offset=0"
            TopicDiscussionSort.Timeline -> "https://www.zhihu.com/api/v5.1/topics/$topicId/feeds/timeline_activity/v2?limit=20&offset=0"
        }
    }
    TopicFeedTab.Ideas -> "https://www.zhihu.com/api/v5.1/topics/$topicId/feeds/${ideasSort.endpoint}?offset=0&limit=10"
    TopicFeedTab.Unanswered -> "https://www.zhihu.com/api/v5.1/topics/$topicId/feeds/top_question/v2?limit=20&offset=0"
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

fun normalizeTopicPagingUrl(rawUrl: String): String? {
    val url = runCatching { Url(rawUrl) }.getOrNull() ?: return null
    if (url.host == "www.zhihu.com" &&
        (url.encodedPath.startsWith("/api/v4/") || url.encodedPath.startsWith("/api/v5.1/"))
    ) {
        return "https://www.zhihu.com${url.encodedPath}" + url.encodedQuery
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
    val viewModel: TopicViewModel = viewModel(key = "topic_${topic.id}_${topic.section}") { TopicViewModel(topic.id) }

    LaunchedEffect(topic.id, topic.section) {
        viewModel.initializeSection(topic.section)
        launch { viewModel.loadDetail(environment) }
        viewModel.loadMore(environment)
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
                        modifier = Modifier.fillMaxWidth().padding(8.dp).testTag(TOPIC_RETRY_BUTTON_TAG),
                    ) { Text("加载失败：${viewModel.errorMessage}，点击重试") }
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
                        onRetryDetail = { scope.launch { viewModel.loadDetail(environment) } },
                        onFollowingChange = { following ->
                            scope.launch {
                                viewModel.setFollowing(environment, following).onFailure {
                                    messages.showShortMessage("${if (following) "关注" else "取消关注"}失败：${it.message}")
                                }
                            }
                        },
                        onWritePin = {
                            navigator.onNavigate(
                                WritePin(
                                    topicName = viewModel.detail?.name?.ifBlank { topic.name } ?: topic.name,
                                    publishTopicId = viewModel.detail
                                        ?.topicId
                                        ?.toString()
                                        .orEmpty(),
                                ),
                            )
                        },
                    )
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
                                    modifier = Modifier.testTag("topic_discussion_sort_${sort.name.lowercase()}"),
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
                                    modifier = Modifier.testTag("topic_ideas_sort_${sort.name.lowercase()}"),
                                )
                            }
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
    onRetryDetail: () -> Unit,
    onFollowingChange: (Boolean) -> Unit,
    onWritePin: () -> Unit,
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
                    detail != null -> Text(
                        listOfNotNull(
                            detail.totalPv.takeIf(String::isNotBlank)?.let { "${formatTopicCount(it)} 浏览" },
                            detail.discussCount.takeIf(String::isNotBlank)?.let { "${formatTopicCount(it)} 讨论" },
                            "${detail.followersCount} 关注",
                            "${detail.questionsCount} 问题",
                        ).joinToString(" · "),
                    )
                    detailErrorMessage != null -> TextButton(onClick = onRetryDetail) {
                        Text("话题信息加载失败：$detailErrorMessage，点击重试", color = MaterialTheme.colorScheme.error)
                    }
                    else -> Text("正在加载话题信息…")
                }
            }
        }
        detail?.excerpt?.takeIf(String::isNotBlank)?.let { introduction ->
            TopicIntroduction(
                introduction = introduction,
                isExpanded = isIntroductionExpanded,
                onExpandedChange = onIntroductionExpandedChange,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { detail?.let { onFollowingChange(!it.isFollowing) } },
                enabled = detail != null && !isFollowingChanging,
                modifier =
                    Modifier
                        .weight(3f)
                        .testTag(TOPIC_FOLLOW_BUTTON_TAG)
                        .semantics { selected = detail?.isFollowing == true },
            ) {
                if (isFollowingChanging) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (detail?.isFollowing == true) "已关注" else "关注话题", maxLines = 1)
                }
            }
            FilledTonalButton(
                onClick = onWritePin,
                enabled = detail?.topicId != null,
                modifier = Modifier.weight(2f).testTag(TOPIC_WRITE_PIN_BUTTON_TAG),
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) {
                Text("发想法", maxLines = 1)
            }
        }
    }
}

@Composable
private fun TopicIntroduction(
    introduction: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    var canCollapse by remember(introduction) { mutableStateOf(false) }
    val surfaceColor = MaterialTheme.colorScheme.surface
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clipToBounds()
                .animateContentSize(
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                ),
    ) {
        Text(
            text = introduction,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Clip,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (canCollapse) Modifier.padding(bottom = 56.dp) else Modifier),
            onTextLayout = { result ->
                canCollapse = result.lineCount > 3 || result.hasVisualOverflow
            },
        )
        if (canCollapse && !isExpanded) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(88.dp)
                        .blur(12.dp)
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        surfaceColor.copy(alpha = 0.7f),
                                        surfaceColor,
                                    ),
                                ),
                        ),
            )
        }
        if (canCollapse) {
            TextButton(
                onClick = { onExpandedChange(!isExpanded) },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = 4.dp)
                        .padding(end = 4.dp)
                        .testTag(TOPIC_INTRODUCTION_TOGGLE_TAG),
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
                Spacer(Modifier.width(4.dp))
                Text(if (isExpanded) "收起简介" else "展开简介")
            }
        }
    }
}

internal fun formatTopicCount(raw: String): String {
    val value = raw.toLongOrNull() ?: return raw

    fun scaled(divisor: Double, unit: String): String {
        val number = (value / divisor * 10).toLong() / 10.0
        val text = if (number % 1.0 == 0.0) number.toLong().toString() else number.toString()
        return "$text $unit"
    }
    return when {
        value >= 100_000_000 -> scaled(100_000_000.0, "亿")
        value >= 10_000 -> scaled(10_000.0, "万")
        else -> value.toString()
    }
}
