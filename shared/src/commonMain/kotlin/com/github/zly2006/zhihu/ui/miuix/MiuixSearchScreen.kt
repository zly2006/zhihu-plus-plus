/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.officialBadge
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.navigation.Topic
import com.github.zly2006.zhihu.platform.UserMessageDuration
import com.github.zly2006.zhihu.platform.UserMessageSink
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.reading.RegisterReadingQueueSource
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.SEARCH_HISTORY_MAX_SIZE
import com.github.zly2006.zhihu.ui.components.AuthorBadge
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.ui.formatTopicCount
import com.github.zly2006.zhihu.ui.loadSearchHistory
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.miuix.components.MiuixListLoadingIndicator
import com.github.zly2006.zhihu.ui.miuix.components.MiuixSearchFilterSheet
import com.github.zly2006.zhihu.ui.miuix.components.MiuixSearchSuggestions
import com.github.zly2006.zhihu.ui.saveSearchHistory
import com.github.zly2006.zhihu.util.parseEmphasizedHtmlTextWithTheme
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.SearchEntity
import com.github.zly2006.zhihu.viewmodel.feed.SearchTab
import com.github.zly2006.zhihu.viewmodel.feed.SearchViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 搜索结果页的 miuix 版本（对齐 M3 SearchScreen）。
 *
 * 顶栏内嵌搜索输入框（miuix TopAppBar 标题仅支持字符串，故自绘顶栏）+ 返回 + 筛选；空查询时复用
 * [MiuixSearchSuggestions] 展示历史/热搜（成员搜索则显示提示），有查询时分页展示结果。筛选（排序/内容
 * 类型/时间范围）走底部弹层，复用 [SearchViewModel] 的过滤逻辑。
 */
@Composable
fun MiuixSearchScreen(search: Search) {
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()
    val settings = rememberSettingsStore()
    val viewModel = viewModel { SearchViewModel(search.query, search.restrictedMemberHashId) }
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val keyboard = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)

    // 朗读队列来源：把当前查询与筛选一起编进 id，换筛选后自动换一条队列（对齐 M3 SearchScreen）。
    val readingQueueSourceId = buildString {
        append("search:")
        append(search.restrictedMemberHashId)
        append(':')
        append(viewModel.filters.sort.name)
        append(':')
        append(viewModel.filters.contentType.name)
        append(':')
        append(viewModel.searchTab.name)
        append(':')
        append(viewModel.filters.timeRange.name)
        append(':')
        append(search.query)
    }
    RegisterReadingQueueSource(
        sourceId = readingQueueSourceId,
        items = viewModel.displayItems,
    )

    var searchText by remember { mutableStateOf(search.query) }
    var showFilter by remember { mutableStateOf(false) }
    val isMember = search.isRestrictedToMember
    val memberName = search.restrictedMemberName.ifBlank { "TA" }
    val placeholder = if (isMember) "搜索 $memberName 的创作" else "搜索内容"
    val showSearchHistory = rememberSettingBoolean("showSearchHistory", true, settings)

    fun submitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        if (showSearchHistory && !isMember) {
            val history = loadSearchHistory(settings).toMutableList()
            history.remove(trimmed)
            history.add(0, trimmed)
            while (history.size > SEARCH_HISTORY_MAX_SIZE) history.removeAt(history.lastIndex)
            saveSearchHistory(settings, history)
        }
        navigator.onNavigate(search.copy(query = trimmed))
    }

    LaunchedEffect(search.query) {
        if (search.query.isNotEmpty() && viewModel.displayItems.isEmpty()) {
            viewModel.refresh(environment)
        }
    }
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { userMessages.showMessage(it, UserMessageDuration.Long) }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .installerMiuixBlurEffect(backdrop)
                    .background(backdrop.getMiuixAppBarColor())
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                    // 搜索输入框
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Search, "搜索", tint = MiuixTheme.colorScheme.onSurfaceSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MiuixTheme.textStyles.body1.copy(color = MiuixTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboard?.hide()
                                submitSearch(searchText)
                            }),
                            decorationBox = { inner ->
                                if (searchText.isEmpty()) {
                                    Text(placeholder, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                                }
                                inner()
                            },
                        )
                        if (searchText.isNotEmpty()) {
                            Icon(
                                Icons.Default.Clear,
                                "清除",
                                tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.size(18.dp).clickable { searchText = "" },
                            )
                        }
                    }
                    // 筛选只作用于全站结果；用户/话题 tab 没有排序与内容类型可选（对齐 M3）。
                    if (viewModel.searchTab == SearchTab.General) {
                        IconButton(onClick = { showFilter = true }, enabled = search.query.isNotEmpty()) {
                            Icon(Icons.Default.FilterList, "筛选", tint = MiuixTheme.colorScheme.onBackground)
                        }
                    }
                }
                if (search.query.isNotEmpty() && !isMember) {
                    TabRowWithContour(
                        tabs = SearchTab.entries.map { it.label },
                        selectedTabIndex = viewModel.searchTab.ordinal,
                        onTabSelected = { index ->
                            viewModel.selectTab(environment, SearchTab.entries[index])
                        },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp)
                            .testTag(SEARCH_TAB_ROW_TAG),
                    )
                }
            }
        },
    ) { padding ->
        if (search.query.isEmpty()) {
            // 输入态：非成员搜索显示历史/热搜，成员搜索显示提示。
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (isMember) {
                    Text(
                        "输入关键词搜索 $memberName 的创作",
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    MiuixSearchSuggestions(onQueryClick = { submitSearch(it) })
                }
            }
        } else if (viewModel.searchTab != SearchTab.General) {
            // 用户/话题 tab：结果是实体而不是信息流条目，各自有头像、简介和独立跳转目标。
            MiuixSearchEntityList(
                viewModel = viewModel,
                environment = environment,
                userMessages = userMessages,
                backdrop = backdrop,
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 6.dp,
                    bottom = padding.calculateBottomPadding() + 12.dp,
                ),
            )
        } else {
            PullToRefresh(
                isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
                onRefresh = { coroutineScope.launch { viewModel.pullToRefresh(environment) } },
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
                refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
            ) {
                Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
                    MiuixListLoadingIndicator(
                        isLoading = viewModel.isLoading,
                        isEmpty = viewModel.displayItems.isEmpty(),
                        isPullToRefresh = viewModel.isPullToRefresh,
                    )
                    PaginatedList(
                        items = viewModel.displayItems,
                        listState = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .overScrollVertical()
                            .scrollEndHaptic(),
                        contentPadding = PaddingValues(
                            top = padding.calculateTopPadding() + 6.dp,
                            bottom = padding.calculateBottomPadding() + 12.dp,
                        ),
                        onLoadMore = { viewModel.loadMore(environment) },
                        footer = ProgressIndicatorFooter,
                        key = { it.stableKey },
                        topContent = {
                            if (isMember) {
                                item {
                                    Text(
                                        "以下结果来自 $memberName 的创作",
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        },
                    ) { item ->
                        MiuixFeedCard(item = item, readingQueueSourceId = readingQueueSourceId)
                    }
                }
            }
        }
    }

    // 筛选弹层：排序 / 内容类型 / 时间范围（与首页内联搜索共用）。
    MiuixSearchFilterSheet(
        show = showFilter,
        onDismiss = { showFilter = false },
        viewModel = viewModel,
        environment = environment,
    )
}

/**
 * 用户 / 话题搜索结果列表（对齐 M3 [com.github.zly2006.zhihu.ui.SearchScreen] 的非「全站」分支）。
 *
 * 这两个 tab 返回的是 [SearchEntity] 而不是信息流条目：用户行点进个人主页，话题行点进话题页并带关注按钮。
 */
@Composable
private fun MiuixSearchEntityList(
    viewModel: SearchViewModel,
    environment: PaginationEnvironment,
    userMessages: UserMessageSink,
    backdrop: LayerBackdrop?,
    contentPadding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(listState) {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: -1
            lastVisibleIndex >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore, viewModel.isLoading, viewModel.isEnd) {
        if (shouldLoadMore && !viewModel.isLoading && !viewModel.isEnd && viewModel.errorMessage == null) {
            viewModel.loadMore(environment)
        }
    }

    Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
        MiuixListLoadingIndicator(
            isLoading = viewModel.isLoading,
            isEmpty = viewModel.entities.isEmpty(),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .scrollEndHaptic()
                .testTag(SEARCH_ENTITY_LIST_TAG),
            contentPadding = contentPadding,
        ) {
            items(viewModel.entities, key = SearchEntity::id) { result ->
                when (result) {
                    is SearchEntity.Topic -> MiuixSearchTopicRow(
                        result = result,
                        changing = result.topic.id in viewModel.changingTopicIds,
                        onClick = { navigator.onNavigate(Topic(result.topic.id, result.topic.name)) },
                        onToggleFollow = {
                            coroutineScope.launch {
                                viewModel
                                    .setTopicFollowing(environment, result.topic.id, !result.isFollowing)
                                    .onFailure { userMessages.showShortMessage("关注操作失败：${it.message}") }
                            }
                        },
                    )
                    is SearchEntity.Person -> {
                        val person = result.person
                        val plainName = person.name.replace("<em>", "").replace("</em>", "")
                        MiuixSearchPersonRow(person) {
                            navigator.onNavigate(Person(person.id, person.urlToken.orEmpty(), plainName))
                        }
                    }
                    is SearchEntity.Content -> Unit
                }
            }
            if (viewModel.errorMessage != null) {
                item(key = "retry") {
                    TextButton(
                        text = "加载失败：${viewModel.errorMessage}，点击重试",
                        modifier = Modifier.fillMaxWidth().testTag(SEARCH_RETRY_BUTTON_TAG),
                        onClick = { viewModel.retry(environment) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixSearchTopicRow(
    result: SearchEntity.Topic,
    changing: Boolean,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit,
) {
    val topic = result.topic
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("search_topic_result_${topic.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = topic.avatarUrl,
            contentDescription = "${topic.name}的话题头像",
            modifier = Modifier.size(48.dp).clip(CircleShape),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(topic.name, fontWeight = FontWeight.Bold, color = MiuixTheme.colorScheme.onSurface)
            result.excerpt.takeIf(String::isNotBlank)?.let {
                Text(
                    it,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${formatTopicCount(result.visitCount.toString())} 浏览 · ${formatTopicCount(result.discussCount.toString())} 讨论",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        TextButton(
            text = if (result.isFollowing) "已关注" else "关注",
            enabled = !changing,
            onClick = onToggleFollow,
            modifier = Modifier.testTag("search_topic_follow_${topic.id}"),
        )
    }
}

@Composable
private fun MiuixSearchPersonRow(
    person: DataHolder.People,
    onClick: () -> Unit,
) {
    val plainName = person.name.replace("<em>", "").replace("</em>", "")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("search_people_result_${person.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = person.avatarUrl,
            contentDescription = "${plainName}的头像",
            modifier = Modifier.size(48.dp).clip(CircleShape),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = parseEmphasizedHtmlTextWithTheme(person.name),
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                val badge = person.badgeV2.officialBadge()
                if (badge?.isUsefulInList == true) {
                    Spacer(Modifier.width(4.dp))
                    AuthorBadge(badge, compact = true)
                }
            }
            person.headline.takeIf(String::isNotEmpty)?.let {
                Text(
                    text = it,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${person.followerCount} 粉丝 · ${person.answerCount} 回答",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

private const val SEARCH_TAB_ROW_TAG = "search_tab_row"
private const val SEARCH_ENTITY_LIST_TAG = "search_entity_list"
private const val SEARCH_RETRY_BUTTON_TAG = "search_retry_button"
