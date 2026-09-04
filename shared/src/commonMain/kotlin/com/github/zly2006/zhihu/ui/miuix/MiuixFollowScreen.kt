/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.platform.UserMessageDuration
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.reading.RegisterReadingQueueSource
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.FOLLOWING_USERS_ROW_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_DYNAMIC_LIST_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_DYNAMIC_REFRESH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_RECOMMEND_LIST_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_RECOMMEND_REFRESH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_SCREEN_PAGER_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_SCREEN_TAB_ROW_TAG
import com.github.zly2006.zhihu.ui.FollowScreenData
import com.github.zly2006.zhihu.ui.TopLevelReselectAction
import com.github.zly2006.zhihu.ui.components.AutoHideTopBar
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedAuthorBlockConfirmDialog
import com.github.zly2006.zhihu.ui.components.FeedAuthorBlockRequest
import com.github.zly2006.zhihu.ui.components.FeedAuthorBlockType
import com.github.zly2006.zhihu.ui.components.NoOpPagerNestedScrollConnection
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.rememberNestedHorizontalPagerConnection
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.ui.topLevelReselectAction
import com.github.zly2006.zhihu.viewmodel.feed.FollowRecommendViewModel
import com.github.zly2006.zhihu.viewmodel.feed.FollowViewModel
import com.github.zly2006.zhihu.viewmodel.feed.RecentMomentsViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 关注页的 miuix 版本，对标 M3 [com.github.zly2006.zhihu.ui.FollowScreen]。
 *
 * tab 状态由页面自己通过 [FollowScreenData] 持有；父 pager 只传进来用于手势协商，
 * 这样在「推荐」左边缘或「动态」右边缘继续横滑才会交还给主壳切 tab。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiuixFollowTopLevelPage(
    scrollToTopTrigger: Int = 0,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    parentPagerState: PagerState,
) {
    val settings = rememberSettingsStore()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = viewModel { FollowScreenData() }
    val pagerState = rememberPagerState(
        initialPage = viewModel.selectedTabIndex,
        pageCount = { 2 },
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectedTabIndex = pagerState.currentPage
    }

    Scaffold(
        topBar = {
            AutoHideTopBar {
                Column(
                    modifier = Modifier
                        .installerMiuixBlurEffect(backdrop),
                ) {
                    TopAppBar(
                        color = backdrop.getMiuixAppBarColor(),
                        title = "关注",
                        scrollBehavior = scrollBehavior,
                    )
                    MiuixFollowTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        onTabSelected = { index ->
                            viewModel.selectedTabIndex = index
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .nestedScroll(
                    rememberNestedHorizontalPagerConnection(
                        parentState = parentPagerState,
                        childState = pagerState,
                    ),
                ).testTag(FOLLOW_SCREEN_PAGER_TAG),
            pageNestedScrollConnection = NoOpPagerNestedScrollConnection,
        ) { page ->
            when (page) {
                0 -> MiuixFollowRecommendScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == 0,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    contentTopPadding = padding.calculateTopPadding(),
                )
                1 -> MiuixFollowDynamicScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == 1,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    contentTopPadding = padding.calculateTopPadding(),
                )
            }
        }
    }
}

/**
 * 「推荐 / 动态」分段控件。
 *
 * 用 [TabRowWithContour] 而不是 [TabRow] 外面手工套一个圆角 Box：后者的选中块会一直画到容器边缘，
 * 圆角半径又和外框不一致，导致外框四角的描边被选中块盖住。带 contour 的版本自带 5dp 内缩，
 * 外框圆角 = 选中块圆角 + 内缩量，两者同心。
 */
@Composable
private fun MiuixFollowTabRow(selectedTabIndex: Int, onTabSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    TabRowWithContour(
        tabs = listOf("推荐", "动态"),
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        modifier = modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .testTag(FOLLOW_SCREEN_TAB_ROW_TAG),
    )
}

@Composable
fun MiuixFollowingUsersRow() {
    val navigator = LocalNavigator.current
    val viewModel: RecentMomentsViewModel = viewModel()
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)

    LaunchedEffect(Unit) { viewModel.load(environment) }

    when {
        viewModel.errorMessage != null -> {
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text(viewModel.errorMessage!!, color = MiuixTheme.colorScheme.onSurfaceSecondary)
            }
        }
        viewModel.users.isNotEmpty() -> {
            LazyRow(
                modifier = Modifier.testTag(FOLLOWING_USERS_ROW_TAG),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(viewModel.users) { user ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .testTag("following_users_item_${user.actor.id}")
                            .clickable {
                                navigator.onNavigate(
                                    Person(
                                        id = user.actor.id,
                                        urlToken = user.actor.urlToken,
                                        name = user.actor.name,
                                        jumpTo = "动态",
                                    ),
                                )
                            }.padding(vertical = 4.dp),
                    ) {
                        Box {
                            AsyncImage(
                                model = user.actor.avatarUrl,
                                contentDescription = user.actor.name,
                                modifier = Modifier.size(56.dp).clip(CircleShape),
                            )
                            if (user.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .then(
                                            Modifier.background(MiuixTheme.colorScheme.error),
                                        ),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = user.actor.name,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.size(width = 60.dp, height = 18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiuixFollowRecommendScreen(
    scrollToTopTrigger: Int = 0,
    isActive: Boolean = true,
    backdrop: LayerBackdrop? = null,
    scrollBehavior: ScrollBehavior? = null,
    contentTopPadding: Dp = 0.dp,
) {
    val viewModel: FollowRecommendViewModel = viewModel { FollowRecommendViewModel() }
    val readingQueueSourceId = "follow:recommend"
    if (isActive) {
        RegisterReadingQueueSource(
            sourceId = readingQueueSourceId,
            items = viewModel.displayItems,
        )
    }
    val environment = rememberPaginationEnvironment(allowGuestAccess = viewModel.allowGuestAccess)
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    val showRefreshFab = rememberSettingBoolean("showRefreshFab", true, settings)
    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }

    LaunchedEffect(scrollToTopTrigger, isActive) {
        val action = topLevelReselectAction(
            triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
            isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
        )
        if (isActive) {
            when (action) {
                TopLevelReselectAction.Refresh -> viewModel.refresh(environment)
                TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
                null -> {}
            }
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(environment)
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            userMessages.showMessage(it, UserMessageDuration.Long)
        }
    }

    var feedAuthorBlockRequest by remember { mutableStateOf<FeedAuthorBlockRequest?>(null) }

    val scope = rememberCoroutineScope()

    Column {
        PullToRefresh(
            isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
            onRefresh = { scope.launch { viewModel.pullToRefresh(environment) } },
            contentPadding = PaddingValues(top = contentTopPadding + 6.dp),
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
        ) {
            Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
                PaginatedList(
                    items = viewModel.displayItems,
                    listState = listState,
                    modifier = Modifier
                        .fillMaxHeight()
                        .overScrollVertical()
                        .scrollEndHaptic()
                        .then(if (scrollBehavior != null) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)
                        .testTag(FOLLOW_RECOMMEND_LIST_TAG),
                    contentPadding = PaddingValues(top = contentTopPadding + 6.dp),
                    topContent = { item { MiuixFollowingUsersRow() } },
                    onLoadMore = { viewModel.loadMore(environment) },
                ) { item ->
                    MiuixFeedCard(
                        item = item,
                        modifier = Modifier.testTag("follow_recommend_item_${item.stableKey}"),
                        readingQueueSourceId = readingQueueSourceId,
                        onBlockUser = { feedItem ->
                            viewModel.handleBlockUser(environment, userMessages, feedItem) { authorInfo ->
                                feedAuthorBlockRequest = FeedAuthorBlockRequest(
                                    type = FeedAuthorBlockType.CONTENT_AUTHOR,
                                    userId = authorInfo.first,
                                    userName = authorInfo.second,
                                )
                            }
                        },
                        onBlockTopic = { topicId, topicName ->
                            viewModel.handleBlockTopic(userMessages, topicId, topicName)
                        },
                    )
                }
            }
            if (showRefreshFab) {
                DraggableRefreshButton(
                    modifier = Modifier.testTag(FOLLOW_RECOMMEND_REFRESH_BUTTON_TAG),
                    onClick = { viewModel.refresh(environment) },
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            }
        }

        FeedAuthorBlockConfirmDialog(
            request = feedAuthorBlockRequest,
            displayItems = viewModel.displayItems,
            onDismiss = {
                feedAuthorBlockRequest = null
            },
            onConfirm = {
                viewModel.refresh(environment)
                feedAuthorBlockRequest = null
            },
        )
    }
}

@Composable
fun MiuixFollowDynamicScreen(
    scrollToTopTrigger: Int = 0,
    isActive: Boolean = true,
    backdrop: LayerBackdrop? = null,
    scrollBehavior: ScrollBehavior? = null,
    contentTopPadding: Dp = 0.dp,
) {
    val viewModel: FollowViewModel = viewModel { FollowViewModel() }
    val readingQueueSourceId = "follow:dynamic"
    if (isActive) {
        RegisterReadingQueueSource(
            sourceId = readingQueueSourceId,
            items = viewModel.displayItems,
        )
    }
    val environment = rememberPaginationEnvironment(allowGuestAccess = viewModel.allowGuestAccess)
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    val showRefreshFab = rememberSettingBoolean("showRefreshFab", true, settings)
    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }

    LaunchedEffect(scrollToTopTrigger, isActive) {
        val action = topLevelReselectAction(
            triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
            isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
        )
        if (isActive) {
            when (action) {
                TopLevelReselectAction.Refresh -> viewModel.refresh(environment)
                TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
                null -> {}
            }
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(environment)
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            userMessages.showMessage(it, UserMessageDuration.Long)
        }
    }

    var feedAuthorBlockRequest by remember { mutableStateOf<FeedAuthorBlockRequest?>(null) }

    val scope = rememberCoroutineScope()

    Column {
        PullToRefresh(
            isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
            onRefresh = { scope.launch { viewModel.pullToRefresh(environment) } },
            contentPadding = PaddingValues(top = contentTopPadding + 6.dp),
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
        ) {
            Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
                PaginatedList(
                    items = viewModel.displayItems,
                    listState = listState,
                    modifier = Modifier
                        .fillMaxHeight()
                        .overScrollVertical()
                        .scrollEndHaptic()
                        .then(if (scrollBehavior != null) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)
                        .testTag(FOLLOW_DYNAMIC_LIST_TAG),
                    contentPadding = PaddingValues(top = contentTopPadding + 6.dp),
                    topContent = { item { Spacer(modifier = Modifier.height(8.dp)) } },
                    onLoadMore = { viewModel.loadMore(environment) },
                ) { item ->
                    MiuixFeedCard(
                        item = item,
                        modifier = Modifier.testTag("follow_dynamic_item_${item.stableKey}"),
                        readingQueueSourceId = readingQueueSourceId,
                        showSourceLabel = true,
                        onLike = { userMessages.showShortMessage("收到喜欢，功能正在优化") },
                        onDislike = { userMessages.showShortMessage("收到反馈，功能正在优化") },
                        onBlockUser = { feedItem ->
                            viewModel.handleBlockUser(environment, userMessages, feedItem) { authorInfo ->
                                feedAuthorBlockRequest = FeedAuthorBlockRequest(
                                    type = FeedAuthorBlockType.CONTENT_AUTHOR,
                                    userId = authorInfo.first,
                                    userName = authorInfo.second,
                                )
                            }
                        },
                        onBlockTopic = { topicId, topicName ->
                            viewModel.handleBlockTopic(userMessages, topicId, topicName)
                        },
                    )
                }
            }
            if (showRefreshFab) {
                DraggableRefreshButton(
                    modifier = Modifier.testTag(FOLLOW_DYNAMIC_REFRESH_BUTTON_TAG),
                    onClick = { viewModel.refresh(environment) },
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            }
        }

        FeedAuthorBlockConfirmDialog(
            request = feedAuthorBlockRequest,
            displayItems = viewModel.displayItems,
            onDismiss = {
                feedAuthorBlockRequest = null
            },
            onConfirm = {
                viewModel.refresh(environment)
                feedAuthorBlockRequest = null
            },
        )
    }
}
