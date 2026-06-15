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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.shared.data.HotListFeed
import com.github.zly2006.zhihu.shared.platform.UserMessageDuration
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.ui.TopLevelReselectAction
import com.github.zly2006.zhihu.shared.ui.topLevelReselectAction
import com.github.zly2006.zhihu.ui.components.BlockUserConfirmDialog
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.HotListViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment

const val HOT_LIST_LIST_TAG = "hot_list_list"
const val HOT_LIST_REFRESH_BUTTON_TAG = "hot_list_refresh_button"

/**
 * 热榜页面。
 *
 * 页面主体是知乎热榜分页列表，支持下拉刷新、加载更多和刷新 FAB。它既可以作为主 tab 页使用，
 * 也可以在测试中通过 [onTestRefreshClick]、[onTestLoadMore] 控制分页行为，因此新增交互时要保留测试注入路径。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotListScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    scrollToTopTrigger: Int = 0,
    isActive: Boolean = true,
    onTestRefreshClick: (() -> Unit)? = null,
    onTestLoadMore: (() -> Unit)? = null,
) {
    val viewModel: HotListViewModel = viewModel { HotListViewModel() }
    val environment = rememberPaginationEnvironment(viewModel.allowGuestAccess)
    val userMessages = rememberUserMessageSink()
    val settings = rememberSettingsStore()
    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(environment)
        }
    }

    LaunchedEffect(scrollToTopTrigger, isActive) {
        val action = topLevelReselectAction(
            triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
            isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
        )
        if (isActive) {
            when (action) {
                TopLevelReselectAction.Refresh -> onTestRefreshClick?.invoke() ?: viewModel.refresh(environment)
                TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
                null -> {}
            }
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            userMessages.showMessage(it, UserMessageDuration.Long)
        }
    }

    // 屏蔽用户确认弹窗。
    var showBlockUserDialog by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column {
        FeedPullToRefresh(viewModel, environment) {
            PaginatedList(
                items = viewModel.displayItems,
                listState = listState,
                onLoadMore = { onTestLoadMore?.invoke() ?: viewModel.loadMore(environment) },
                modifier = Modifier
                    .padding(innerPadding)
                    .testTag(HOT_LIST_LIST_TAG),
                isEnd = { viewModel.isEnd },
                footer = ProgressIndicatorFooter,
            ) { item ->
                FeedCard(
                    item,
                    thumbnailUrl = (item.feed as? HotListFeed)?.children?.firstOrNull()?.thumbnail,
                )
            }

            val showRefreshFab = remember { settings.getBoolean("showRefreshFab", true) }
            if (showRefreshFab) {
                DraggableRefreshButton(
                    modifier = Modifier.testTag(HOT_LIST_REFRESH_BUTTON_TAG),
                    onClick = {
                        onTestRefreshClick?.invoke() ?: viewModel.refresh(environment)
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

        // 屏蔽用户确认弹窗。
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
