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

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.platform.UserMessageDuration
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.ui.TopLevelReselectAction
import com.github.zly2006.zhihu.shared.ui.topLevelReselectAction
import com.github.zly2006.zhihu.theme.LocalThemeStyle
import com.github.zly2006.zhihu.theme.ThemeStyle
import com.github.zly2006.zhihu.ui.components.AutoHideTopBar
import com.github.zly2006.zhihu.ui.components.BlockUserConfirmDialog
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.rememberFeedBlockActions
import com.github.zly2006.zhihu.ui.miuix.MiuixFollowingUsersRow
import com.github.zly2006.zhihu.viewmodel.feed.FollowRecommendViewModel
import com.github.zly2006.zhihu.viewmodel.feed.FollowViewModel
import com.github.zly2006.zhihu.viewmodel.feed.RecentMomentsViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

class FollowScreenData : ViewModel() {
    var selectedTabIndex by mutableIntStateOf(0)
}

const val FOLLOW_SCREEN_TAB_ROW_TAG = "follow_screen_tab_row"
const val FOLLOW_SCREEN_PAGER_TAG = "follow_screen_pager"
const val FOLLOWING_USERS_ROW_TAG = "following_users_row"
const val FOLLOW_RECOMMEND_LIST_TAG = "follow_recommend_list"
const val FOLLOW_RECOMMEND_REFRESH_BUTTON_TAG = "follow_recommend_refresh_button"
const val FOLLOW_DYNAMIC_LIST_TAG = "follow_dynamic_list"
const val FOLLOW_DYNAMIC_REFRESH_BUTTON_TAG = "follow_dynamic_refresh_button"

fun followScreenTabTag(index: Int) = "follow_screen_tab_$index"

fun followingUserItemTag(userId: String) = "following_users_item_$userId"

fun followRecommendItemTag(stableKey: String) = "follow_recommend_item_$stableKey"

fun followDynamicItemTag(stableKey: String) = "follow_dynamic_item_$stableKey"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FollowScreen(
    scrollToTopTrigger: Int,
    innerPadding: PaddingValues,
): Unit = FollowScreenContent(
    scrollToTopTrigger = scrollToTopTrigger,
    innerPadding = innerPadding,
    onTestRecommendRefreshClick = null,
    onTestRecommendLoadMore = null,
    onTestDynamicRefreshClick = null,
    onTestDynamicLoadMore = null,
)

@Composable
fun FollowScreen(
    scrollToTopTrigger: Int = 0,
    innerPadding: PaddingValues,
    onTestRecommendRefreshClick: (() -> Unit)?,
    onTestRecommendLoadMore: (() -> Unit)?,
    onTestDynamicRefreshClick: (() -> Unit)?,
    onTestDynamicLoadMore: (() -> Unit)?,
): Unit = FollowScreenContent(
    scrollToTopTrigger = scrollToTopTrigger,
    innerPadding = innerPadding,
    onTestRecommendRefreshClick = onTestRecommendRefreshClick,
    onTestRecommendLoadMore = onTestRecommendLoadMore,
    onTestDynamicRefreshClick = onTestDynamicRefreshClick,
    onTestDynamicLoadMore = onTestDynamicLoadMore,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FollowScreenContent(
    scrollToTopTrigger: Int = 0,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onTestRecommendRefreshClick: (() -> Unit)? = null,
    onTestRecommendLoadMore: (() -> Unit)? = null,
    onTestDynamicRefreshClick: (() -> Unit)? = null,
    onTestDynamicLoadMore: (() -> Unit)? = null,
) {
    val viewModel = viewModel { FollowScreenData() }
    val titles = listOf("推荐", "动态")
    val pagerState = rememberPagerState(pageCount = { titles.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectedTabIndex = pagerState.currentPage
    }

    LaunchedEffect(viewModel.selectedTabIndex) {
        if (pagerState.currentPage != viewModel.selectedTabIndex) {
            pagerState.animateScrollToPage(viewModel.selectedTabIndex)
        }
    }

    Column(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
        FollowTabRow(
            selectedTabIndex = viewModel.selectedTabIndex,
            onTabSelected = { index ->
                viewModel.selectedTabIndex = index
                coroutineScope.launch {
                    pagerState.animateScrollToPage(index)
                }
            },
            modifier = Modifier
                .padding(
                    top = innerPadding.calculateTopPadding(),
                ),
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .testTag(FOLLOW_SCREEN_PAGER_TAG),
        ) { page ->
            when (page) {
                0 -> FollowRecommendScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == 0,
                    onTestRefreshClick = onTestRecommendRefreshClick,
                    onTestLoadMore = onTestRecommendLoadMore,
                )

                1 -> FollowDynamicScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == 1,
                    onTestRefreshClick = onTestDynamicRefreshClick,
                    onTestLoadMore = onTestDynamicLoadMore,
                )
            }
        }
    }
}

@Composable
fun FollowTopLevelPage(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    scrollToTopTrigger: Int,
    innerPadding: PaddingValues,
    isActive: Boolean,
) {
    Column(
        modifier = Modifier
            .padding(bottom = innerPadding.calculateBottomPadding())
            .then(if (isActive) Modifier else Modifier.clearAndSetSemantics {}),
    ) {
        AutoHideTopBar {
            FollowTabRow(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected,
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            )
        }
        when (selectedTabIndex) {
            0 -> FollowRecommendScreen(
                scrollToTopTrigger = scrollToTopTrigger,
                isActive = isActive,
            )
            1 -> FollowDynamicScreen(
                scrollToTopTrigger = scrollToTopTrigger,
                isActive = isActive,
            )
        }
    }
}

@Composable
private fun FollowTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titles = listOf("推荐", "动态")
    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier.testTag(FOLLOW_SCREEN_TAB_ROW_TAG),
    ) {
        titles.forEachIndexed { index, title ->
            Tab(
                modifier = Modifier.testTag(followScreenTabTag(index)),
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
fun FollowingUsersRow() {
    val navigator = LocalNavigator.current
    val viewModel: RecentMomentsViewModel = viewModel { RecentMomentsViewModel() }
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)

    LaunchedEffect(Unit) {
        viewModel.load(environment)
    }

    when {
        viewModel.errorMessage != null -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = viewModel.errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                            .testTag(followingUserItemTag(user.actor.id))
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
                        BadgedBox(
                            badge = {
                                if (user.unreadCount > 0) {
                                    Badge()
                                }
                            },
                        ) {
                            AsyncImage(
                                model = user.actor.avatarUrl,
                                contentDescription = user.actor.name,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                            )
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

// 关注页卡片按主题分流：miuix 用 MiuixFeedCard，M3 用 FeedCard（避免 miuix 卡片在 M3 主题下取色反色）
@Composable
private fun ThemedFeedCard(
    item: FeedDisplayItem,
    modifier: Modifier = Modifier,
    onLike: ((FeedDisplayItem) -> Unit)? = null,
    onDislike: ((FeedDisplayItem) -> Unit)? = null,
    onBlockUser: ((FeedDisplayItem) -> Unit)? = null,
    onBlockTopic: ((topicId: String, topicName: String) -> Unit)? = null,
) {
    if (LocalThemeStyle.current == ThemeStyle.Miuix) {
        com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard(
            item = item,
            modifier = modifier,
            onLike = onLike,
            onDislike = onDislike,
            onBlockUser = onBlockUser,
            onBlockTopic = onBlockTopic,
        )
    } else {
        FeedCard(
            item = item,
            modifier = modifier,
            onLike = onLike,
            onDislike = onDislike,
            onBlockUser = onBlockUser,
            onBlockTopic = onBlockTopic,
        )
    }
}

@Composable
fun FollowRecommendScreen(
    scrollToTopTrigger: Int = 0,
    isActive: Boolean = true,
    backdrop: LayerBackdrop? = null,
    scrollBehavior: ScrollBehavior? = null,
    contentTopPadding: Dp = 0.dp,
    onTestRefreshClick: (() -> Unit)? = null,
    onTestLoadMore: (() -> Unit)? = null,
) {
    val viewModel: FollowRecommendViewModel = viewModel { FollowRecommendViewModel() }
    val environment = rememberPaginationEnvironment(allowGuestAccess = viewModel.allowGuestAccess)
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    val feedBlockActions = rememberFeedBlockActions()
    val showRefreshFab = remember { settings.getBoolean("showRefreshFab", true) }
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

    var showBlockUserDialog by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) }

    val scope = rememberCoroutineScope()

    Column {
        run {
            // 统一 miuix 路径：backdrop=null 时只是不模糊，卡片仍是 MiuixFeedCard
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
                        topContent = {
                            item {
                                if (LocalThemeStyle.current == ThemeStyle.Miuix) MiuixFollowingUsersRow() else FollowingUsersRow()
                            }
                        },
                        onLoadMore = { onTestLoadMore?.invoke() ?: viewModel.loadMore(environment) },
                    ) { item ->
                        ThemedFeedCard(
                            item = item,
                            modifier = Modifier.testTag(followRecommendItemTag(item.stableKey)),
                            onBlockUser = { feedItem ->
                                feedBlockActions.handleBlockUser(viewModel, feedItem) { authorInfo ->
                                    userToBlock = authorInfo
                                    showBlockUserDialog = true
                                }
                            },
                            onBlockTopic = { topicId, topicName ->
                                feedBlockActions.handleBlockTopic(viewModel, topicId, topicName)
                            },
                        )
                    }
                }
                if (showRefreshFab) {
                    DraggableRefreshButton(
                        modifier = Modifier.testTag(FOLLOW_RECOMMEND_REFRESH_BUTTON_TAG),
                        onClick = { onTestRefreshClick?.invoke() ?: viewModel.refresh(environment) },
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                }
            }
        }

        BlockUserConfirmDialog(
            showDialog = showBlockUserDialog,
            userToBlock = userToBlock,
            displayItems = viewModel.displayItems,
            onDismiss = {
                showBlockUserDialog = false
                userToBlock = null
            },
            onConfirm = {
                viewModel.refresh(environment)
                showBlockUserDialog = false
                userToBlock = null
            },
        )
    }
}

@Composable
fun FollowDynamicScreen(
    scrollToTopTrigger: Int = 0,
    isActive: Boolean = true,
    backdrop: LayerBackdrop? = null,
    scrollBehavior: ScrollBehavior? = null,
    contentTopPadding: Dp = 0.dp,
    onTestRefreshClick: (() -> Unit)? = null,
    onTestLoadMore: (() -> Unit)? = null,
) {
    val viewModel: FollowViewModel = viewModel { FollowViewModel() }
    val environment = rememberPaginationEnvironment(allowGuestAccess = viewModel.allowGuestAccess)
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    val feedBlockActions = rememberFeedBlockActions()
    val showRefreshFab = remember { settings.getBoolean("showRefreshFab", true) }
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

    var showBlockUserDialog by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) }

    val scope = rememberCoroutineScope()

    Column {
        run {
            // 统一 miuix 路径：backdrop=null 时只是不模糊，卡片仍是 MiuixFeedCard
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
                        onLoadMore = { onTestLoadMore?.invoke() ?: viewModel.loadMore(environment) },
                    ) { item ->
                        ThemedFeedCard(
                            item = item,
                            modifier = Modifier.testTag(followDynamicItemTag(item.stableKey)),
                            onLike = { userMessages.showShortMessage("收到喜欢，功能正在优化") },
                            onDislike = { userMessages.showShortMessage("收到反馈，功能正在优化") },
                            onBlockUser = { feedItem ->
                                feedBlockActions.handleBlockUser(viewModel, feedItem) { authorInfo ->
                                    userToBlock = authorInfo
                                    showBlockUserDialog = true
                                }
                            },
                            onBlockTopic = { topicId, topicName ->
                                feedBlockActions.handleBlockTopic(viewModel, topicId, topicName)
                            },
                        )
                    }
                }
                if (showRefreshFab) {
                    DraggableRefreshButton(
                        modifier = Modifier.testTag(FOLLOW_DYNAMIC_REFRESH_BUTTON_TAG),
                        onClick = { onTestRefreshClick?.invoke() ?: viewModel.refresh(environment) },
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                }
            }
        }

        BlockUserConfirmDialog(
            showDialog = showBlockUserDialog,
            userToBlock = userToBlock,
            displayItems = viewModel.displayItems,
            onDismiss = {
                showBlockUserDialog = false
                userToBlock = null
            },
            onConfirm = {
                viewModel.refresh(environment)
                showBlockUserDialog = false
                userToBlock = null
            },
        )
    }
}
