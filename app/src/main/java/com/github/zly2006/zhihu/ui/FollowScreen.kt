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

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.ui.components.BlockUserConfirmDialog
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.FollowRecommendViewModel
import com.github.zly2006.zhihu.viewmodel.feed.FollowViewModel
import com.github.zly2006.zhihu.viewmodel.feed.RecentMomentsViewModel
import kotlinx.coroutines.launch

class FollowScreenData : ViewModel() {
    var selectedTabIndex by mutableIntStateOf(0)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FollowScreen(
    scrollToTopTrigger: Int = 0,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val viewModel = viewModel<FollowScreenData>()
    val titles = listOf("推荐", "动态")
    val pagerState = rememberPagerState(pageCount = { titles.size })
    val coroutineScope = rememberCoroutineScope()

    // 同步PagerState和ViewModel的selectedTabIndex
    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectedTabIndex = pagerState.currentPage
    }

    LaunchedEffect(viewModel.selectedTabIndex) {
        if (pagerState.currentPage != viewModel.selectedTabIndex) {
            pagerState.animateScrollToPage(viewModel.selectedTabIndex)
        }
    }

    Column(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
        PrimaryTabRow(
            selectedTabIndex = viewModel.selectedTabIndex,
            modifier = Modifier.padding(
                top = innerPadding.calculateTopPadding(),
            ),
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = viewModel.selectedTabIndex == index,
                    onClick = {
                        viewModel.selectedTabIndex = index
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> FollowRecommendScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == 0,
                )

                1 -> FollowDynamicScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == 1,
                )
            }
        }
    }
}

@Composable
fun FollowingUsersRow() {
    val context = LocalActivity.current as MainActivity
    val navigator = LocalNavigator.current
    val viewModel: RecentMomentsViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.load(context)
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(viewModel.users) { user ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
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

@Composable
fun FollowRecommendScreen(
    scrollToTopTrigger: Int = 0,
    isActive: Boolean = true,
) {
    val context = LocalActivity.current as MainActivity
    val viewModel: FollowRecommendViewModel by context.viewModels()
    val preferences = remember {
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }
    val showRefreshFab = remember { preferences.getBoolean("showRefreshFab", true) }
    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }

    LaunchedEffect(scrollToTopTrigger, isActive) {
        val action = topLevelReselectAction(
            triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
            isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
        )
        if (isActive) {
            when (action) {
                TopLevelReselectAction.Refresh -> viewModel.refresh(context)
                TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
                null -> {}
            }
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // 屏蔽用户确认对话框
    var showBlockUserDialog by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column {
        FeedPullToRefresh(viewModel) {
            PaginatedList(
                items = viewModel.displayItems,
                listState = listState,
                topContent = {
                    item {
                        FollowingUsersRow()
                    }
                },
                onLoadMore = { viewModel.loadMore(context) },
                footer = ProgressIndicatorFooter,
            ) { item ->
                FeedCard(
                    item,
                    onBlockUser = { feedItem ->
                        viewModel.handleBlockUser(context, feedItem) { authorInfo ->
                            userToBlock = authorInfo
                            showBlockUserDialog = true
                        }
                    },
                    onBlockTopic = { topicId, topicName ->
                        viewModel.handleBlockTopic(context, topicId, topicName)
                    },
                )
            }

            if (showRefreshFab) {
                DraggableRefreshButton(
                    onClick = {
                        viewModel.refresh(context)
                    },
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            }
        }

        // 屏蔽用户确认对话框
        BlockUserConfirmDialog(
            showDialog = showBlockUserDialog,
            userToBlock = userToBlock,
            displayItems = viewModel.displayItems,
            context = context,
            onDismiss = {
                showBlockUserDialog = false
                userToBlock = null
            },
            onConfirm = {
                viewModel.refresh(context)
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
) {
    val context = LocalActivity.current as MainActivity
    val viewModel: FollowViewModel by context.viewModels()
    val preferences = remember {
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }
    val showRefreshFab = remember { preferences.getBoolean("showRefreshFab", true) }
    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }

    LaunchedEffect(scrollToTopTrigger, isActive) {
        val action = topLevelReselectAction(
            triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
            isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
        )
        if (isActive) {
            when (action) {
                TopLevelReselectAction.Refresh -> viewModel.refresh(context)
                TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
                null -> {}
            }
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // 屏蔽用户确认对话框
    var showBlockUserDialog by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column {
        FeedPullToRefresh(viewModel) {
            PaginatedList(
                items = viewModel.displayItems,
                listState = listState,
                onLoadMore = { viewModel.loadMore(context) },
                topContent = {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                },
                footer = ProgressIndicatorFooter,
            ) { item ->
                FeedCard(
                    item,
                    onLike = {
                        Toast.makeText(context, "收到喜欢，功能正在优化", Toast.LENGTH_SHORT).show()
                    },
                    onDislike = {
                        Toast.makeText(context, "收到反馈，功能正在优化", Toast.LENGTH_SHORT).show()
                    },
                    onBlockUser = { feedItem ->
                        viewModel.handleBlockUser(context, feedItem) { authorInfo ->
                            userToBlock = authorInfo
                            showBlockUserDialog = true
                        }
                    },
                    onBlockTopic = { topicId, topicName ->
                        viewModel.handleBlockTopic(context, topicId, topicName)
                    },
                )
            }

            if (showRefreshFab) {
                DraggableRefreshButton(
                    onClick = {
                        viewModel.refresh(context)
                    },
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            }
        }

        // 屏蔽用户确认对话框
        BlockUserConfirmDialog(
            showDialog = showBlockUserDialog,
            userToBlock = userToBlock,
            displayItems = viewModel.displayItems,
            context = context,
            onDismiss = {
                showBlockUserDialog = false
                userToBlock = null
            },
            onConfirm = {
                viewModel.refresh(context)
                showBlockUserDialog = false
                userToBlock = null
            },
        )
    }
}
