/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 *
 * 结构照抄 KernelSU SuperUserMiuix（GPL-3.0）：
 *   - 头像放 TopAppBar 的 actions（右上角）
 *   - SearchBarFake 放 TopAppBar 的 bottomContent，用 onGloballyPositioned 上报 offsetY
 *   - SearchPager 展开时真框从 offsetY 位置开始动画 → 假框/真框位置一致
 *   - TopAppBarAnim 包裹 TopAppBar，提供消失回弹（alpha 切换）
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.shared.data.fetchZhihuUnreadNotificationCount
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.SEARCH_HISTORY_MAX_SIZE
import com.github.zly2006.zhihu.ui.components.AutoHideTopBar
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.rememberFeedBlockActions
import com.github.zly2006.zhihu.ui.loadSearchHistory
import com.github.zly2006.zhihu.ui.miuix.components.MiuixAccountSheet
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.ui.miuix.components.MiuixSearchSuggestions
import com.github.zly2006.zhihu.ui.miuix.components.SearchBarFake
import com.github.zly2006.zhihu.ui.miuix.components.SearchBox
import com.github.zly2006.zhihu.ui.miuix.components.SearchPager
import com.github.zly2006.zhihu.ui.miuix.components.SearchStatus
import com.github.zly2006.zhihu.ui.rememberHomeScreenRuntime
import com.github.zly2006.zhihu.ui.saveSearchHistory
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedInteractionViewModel
import com.github.zly2006.zhihu.viewmodel.feed.SearchViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixHomeScreen(
    scrollToTopTrigger: Int = 0,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val navigator = LocalNavigator.current
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = true)
    val settings = rememberSettingsStore()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val duo3HomeAccount = remember { settings.getBoolean("duo3_home_account", false) }

    val currentRecommendationMode = RecommendationMode.entries.find {
        it.key == settings.getString("recommendationMode", RecommendationMode.MIXED.key)
    } ?: RecommendationMode.MIXED
    val runtime = rememberHomeScreenRuntime(currentRecommendationMode)
    val feedBlockActions = rememberFeedBlockActions()
    val viewModel: BaseFeedViewModel = runtime.viewModel

    val listState = rememberLazyListState()
    var searchStatus by remember { mutableStateOf(SearchStatus(label = "搜索知乎")) }
    val showAccountSheet = remember { mutableStateOf(false) }
    var unreadCount by remember { mutableIntStateOf(0) }

    // 屏蔽相关 state（沿用 HomeScreen 逻辑）
    var showBlockUserDialog by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showBlockByKeywordsDialog by remember { mutableStateOf(false) }
    var feedToBlockByKeywords by remember { mutableStateOf<Pair<String, String?>?>(null) }

    LaunchedEffect(currentRecommendationMode, runtime.account.isLoggedIn) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(paginationEnvironment)
        }
    }

    // 拉取未读通知数（与 M3 HomeScreen 行为一致）
    LaunchedEffect(Unit) {
        try {
            unreadCount = fetchZhihuUnreadNotificationCount(paginationEnvironment.httpClient()) {
                paginationEnvironment.configureSignedRequest(this)
            }
        } catch (_: Exception) {
        }
    }

    // 点搜索框展开时立即把 feed 滚回顶部，避免展开动画期间背景文字造成模糊穿帮
    LaunchedEffect(searchStatus.current) {
        if (searchStatus.isAnimatingExpand() && listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    // ── 同页搜索 ──
    // 防抖后的查询词：searchText 停止变化 350ms 后才真正搜索，避免每打一个字就请求
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchStatus.searchText) {
        val q = searchStatus.searchText.trim()
        if (q.isEmpty()) {
            debouncedQuery = ""
        } else {
            kotlinx.coroutines.delay(350)
            debouncedQuery = q
        }
    }
    // query 变化时重建 SearchViewModel（SearchViewModel 的 query 是构造参数，不可变）
    val searchViewModel = remember(debouncedQuery) {
        if (debouncedQuery.isEmpty()) {
            null
        } else {
            SearchViewModel(debouncedQuery)
        }
    }
    val searchListState = rememberLazyListState()
    // 触发搜索 + 驱动 resultStatus
    LaunchedEffect(searchViewModel) {
        val vm = searchViewModel
        if (vm == null) {
            searchStatus = searchStatus.copy(resultStatus = SearchStatus.ResultStatus.DEFAULT)
        } else {
            searchStatus = searchStatus.copy(resultStatus = SearchStatus.ResultStatus.LOAD)
            vm.refresh(paginationEnvironment)
        }
    }
    // 搜索结果加载完成后切换 SHOW / EMPTY
    val showSearchHistory = remember { settings.getBoolean("showSearchHistory", true) }
    LaunchedEffect(searchViewModel?.displayItems?.size, searchViewModel?.isLoading) {
        val vm = searchViewModel ?: return@LaunchedEffect
        if (!vm.isLoading) {
            val hasResult = vm.displayItems.isNotEmpty()
            // 出结果即写入历史；defaultResult 重挂载会自动重读
            if (hasResult && showSearchHistory && debouncedQuery.isNotBlank()) {
                val history = loadSearchHistory(settings).toMutableList()
                history.remove(debouncedQuery)
                history.add(0, debouncedQuery)
                while (history.size > SEARCH_HISTORY_MAX_SIZE) history.removeAt(history.lastIndex)
                saveSearchHistory(settings, history)
            }
            searchStatus = searchStatus.copy(
                resultStatus = if (hasResult) SearchStatus.ResultStatus.SHOW else SearchStatus.ResultStatus.EMPTY,
            )
        }
    }

    val blurEnabled = remember { settings.getBoolean("blurEnabled", true) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    // 外层 Box：让 SearchPager 覆盖整个屏幕（从真正的屏幕顶部算），
    // 而不是被困在 Scaffold 内容区（已被 topBar 推下去，会导致不靠顶 + 返回闪现）
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                AutoHideTopBar {
                    // TopAppBarAnim：消失回弹（alpha 切换 + 背景层）
                    searchStatus.TopAppBarAnim(
                        modifier = Modifier.installerMiuixBlurEffect(backdrop),
                        backgroundColor = backdrop.getMiuixAppBarColor(),
                    ) {
                        TopAppBar(
                            color = backdrop.getMiuixAppBarColor(),
                            title = "主页",
                            scrollBehavior = scrollBehavior,
                            // 头像移到右上角 actions
                            actions = {
                                // duo3_home_account 开启时显示头像（弹账号面板），关闭时显示通知入口（与 M3 HomeScreen 对齐）
                                Box(modifier = Modifier.padding(end = 8.dp)) {
                                    IconButton(
                                        onClick = {
                                            if (duo3HomeAccount) {
                                                showAccountSheet.value = true
                                            } else {
                                                navigator.onNavigate(Notification)
                                            }
                                        },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        if (duo3HomeAccount) {
                                            val avatarUrl = runtime.account.avatarUrl
                                            if (avatarUrl != null) {
                                                AsyncImage(
                                                    model = avatarUrl,
                                                    contentDescription = "账号",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .border(0.5.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                                                        .clip(CircleShape),
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.AccountCircle,
                                                    contentDescription = "账号",
                                                    tint = MiuixTheme.colorScheme.onBackground,
                                                    modifier = Modifier.size(32.dp),
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "通知",
                                                tint = MiuixTheme.colorScheme.onBackground,
                                                modifier = Modifier.size(28.dp),
                                            )
                                        }
                                    }
                                    if (unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MiuixTheme.colorScheme.error),
                                        )
                                    }
                                }
                            },
                            // SearchBarFake 放 bottomContent，上报 offsetY 供真框对齐
                            bottomContent = {
                                Box(
                                    modifier = Modifier
                                        .alpha(if (searchStatus.isCollapsed()) 1f else 0f)
                                        .onGloballyPositioned { coordinates ->
                                            // 只在折叠态上报 offsetY，锁定假框真实位置；
                                            // 展开/收起期间布局会变，不能更新，否则收起会弹到错误位置
                                            if (searchStatus.isCollapsed()) {
                                                with(density) {
                                                    val newOffsetY = coordinates.positionInWindow().y.toDp()
                                                    if (searchStatus.offsetY != newOffsetY) {
                                                        searchStatus = searchStatus.copy(offsetY = newOffsetY)
                                                    }
                                                }
                                            }
                                        }.then(
                                            if (searchStatus.isCollapsed()) {
                                                Modifier.pointerInput(Unit) {
                                                    detectTapGestures {
                                                        searchStatus = searchStatus.copy(current = SearchStatus.Status.EXPANDING)
                                                    }
                                                }
                                            } else {
                                                Modifier
                                            },
                                        ),
                                ) {
                                    SearchBarFake(
                                        label = searchStatus.label,
                                        searchBarTopPadding = 0.dp,
                                        onClick = {
                                            searchStatus = searchStatus.copy(current = SearchStatus.Status.EXPANDING)
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
            },
        ) { padding ->
            // Scaffold 内容区：feed + 下拉刷新
            searchStatus.SearchBox {
                PullToRefresh(
                    isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
                    onRefresh = { coroutineScope.launch { viewModel.pullToRefresh(paginationEnvironment) } },
                    contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
                    refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
                ) {
                    Box(
                        modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
                    ) {
                        PaginatedList(
                            items = viewModel.displayItems,
                            listState = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .overScrollVertical()
                                .scrollEndHaptic()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = PaddingValues(
                                top = padding.calculateTopPadding() + 6.dp,
                                bottom = innerPadding.calculateBottomPadding() + 12.dp,
                            ),
                            onLoadMore = { viewModel.loadMore(paginationEnvironment) },
                            key = { item -> item.stableKey },
                        ) { item ->
                            MiuixFeedCard(
                                item = item,
                                onLike = { runtime.recordLocalItemFeedback(it, 1.0) },
                                onDislike = { runtime.recordLocalItemFeedback(it, -1.0) },
                                onBlockUser = { feedItem ->
                                    feedBlockActions.handleBlockUser(viewModel, feedItem) { authorInfo ->
                                        userToBlock = authorInfo
                                        showBlockUserDialog = true
                                    }
                                },
                                onBlockByKeywords = { feedItem ->
                                    feedBlockActions.handleBlockByKeywords(viewModel, feedItem) { (_, contentInfo) ->
                                        feedToBlockByKeywords = contentInfo.first to contentInfo.second
                                        showBlockByKeywordsDialog = true
                                    }
                                },
                                onBlockTopic = { topicId, topicName ->
                                    feedBlockActions.handleBlockTopic(viewModel, topicId, topicName)
                                },
                                onClick = {
                                    // 默认跳转逻辑：本地内容回调 + navDestination
                                    val feed = this.feed
                                    if (feed != null) {
                                        (viewModel as HomeFeedInteractionViewModel)
                                            .onUiContentClick(paginationEnvironment, feed, this)
                                    } else if (this.localContentId != null) {
                                        runtime.recordLocalItemOpened(this)
                                    }
                                    this.navDestination?.let { navigator.onNavigate(it) }
                                },
                            )
                        }
                    }
                }
            }
        }

        // 搜索浮层：在 Scaffold 外层、覆盖全屏（从屏幕真正顶部算坐标），
        // 这样 topPadding = systemBarsPadding + 5.dp 能真正靠到屏幕顶部
        searchStatus.SearchPager(
            onSearchStatusChange = { searchStatus = it },
            // 纯间距，不含 statusBar —— statusBar 高度已由 SearchPager 内部的 topPadding 处理，
            // 这里再加 statusBar 会双重 padding，导致收起时搜索框偏下
            searchBarTopPadding = 12.dp,
            defaultResult = {
                // 重挂载时自动从 prefs 重读历史（搜索完成会写入），无需共享 state
                MiuixSearchSuggestions(
                    onQueryClick = { q -> searchStatus = searchStatus.copy(searchText = q) },
                )
            },
            loadingResult = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    top.yukonga.miuix.kmp.basic
                        .CircularProgressIndicator()
                }
            },
            emptyResult = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有找到相关结果", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            },
            result = {
                val vm = searchViewModel
                if (vm != null) {
                    PaginatedList(
                        items = vm.displayItems,
                        listState = searchListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 6.dp,
                            bottom = innerPadding.calculateBottomPadding() + 12.dp,
                        ),
                        onLoadMore = { vm.loadMore(paginationEnvironment) },
                        key = { item -> item.stableKey },
                    ) { item ->
                        MiuixFeedCard(
                            item = item,
                            onClick = {
                                this.navDestination?.let { navigator.onNavigate(it) }
                            },
                        )
                    }
                }
            },
        )
    } // 外层 Box 结束

    // 屏蔽用户确认对话框（与 HomeScreen 同签名）
    com.github.zly2006.zhihu.ui.components.BlockUserConfirmDialog(
        showDialog = showBlockUserDialog,
        userToBlock = userToBlock,
        displayItems = viewModel.displayItems,
        onDismiss = {
            showBlockUserDialog = false
            userToBlock = null
        },
        onConfirm = {
            viewModel.refresh(paginationEnvironment)
            showBlockUserDialog = false
            userToBlock = null
        },
    )

    // 按关键词屏蔽对话框
    feedToBlockByKeywords?.let { (title, excerpt) ->
        com.github.zly2006.zhihu.ui.components.BlockByKeywordsDialog(
            showDialog = showBlockByKeywordsDialog,
            feedTitle = title,
            feedExcerpt = excerpt,
            onDismiss = {
                showBlockByKeywordsDialog = false
                feedToBlockByKeywords = null
            },
            onConfirm = {
                viewModel.refresh(paginationEnvironment)
                showBlockByKeywordsDialog = false
                feedToBlockByKeywords = null
            },
        )
    }

    MiuixAccountSheet(
        show = showAccountSheet.value,
        unreadCount = unreadCount,
        onDismiss = { showAccountSheet.value = false },
    )
}
