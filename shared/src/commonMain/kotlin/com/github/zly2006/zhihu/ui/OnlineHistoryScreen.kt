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

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.shared.platform.PlatformBackHandler
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.ui.TopLevelReselectAction
import com.github.zly2006.zhihu.shared.ui.topLevelReselectAction
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.viewmodel.feed.OnlineHistoryViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch

const val ONLINE_HISTORY_OVERFLOW_TAG = "online_history_overflow"

/**
 * 在线浏览历史页面。
 *
 * 页面展示知乎账号侧的浏览历史，提供刷新、分页加载和清空历史入口。它既可能作为底部栏 tab 出现，也可能从账号页快捷入口独立 push。
 * 当前实现本身不绘制返回箭头，底栏显隐由外层 route 决定；如果包装层需要独立返回入口，应由包装层显式传入对应状态。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineHistoryScreen(
    scrollToTopTrigger: Int = 0,
    isActive: Boolean = true,
) {
    val navigator = LocalNavigator.current
    val viewModel: OnlineHistoryViewModel = viewModel { OnlineHistoryViewModel() }
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = false)
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(paginationEnvironment)
        }
    }

    LaunchedEffect(scrollToTopTrigger, isActive) {
        val action = topLevelReselectAction(
            triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
            isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
        )
        if (isActive) {
            when (action) {
                TopLevelReselectAction.Refresh -> viewModel.refresh(paginationEnvironment)
                TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
                null -> {}
            }
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    PlatformBackHandler(enabled = showClearHistoryDialog) {
        showClearHistoryDialog = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                actions = {
                    var showActionsMenu by remember { mutableStateOf(false) }
                    PlatformBackHandler(enabled = showActionsMenu) {
                        showActionsMenu = false
                    }
                    IconButton(
                        modifier = Modifier.testTag(ONLINE_HISTORY_OVERFLOW_TAG),
                        onClick = { showActionsMenu = true },
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "更多选项",
                        )

                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("查看本地历史记录") },
                                onClick = {
                                    showActionsMenu = false
                                    navigator.onNavigate(History)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("清除历史记录") },
                                onClick = {
                                    showActionsMenu = false
                                    showClearHistoryDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("确认清除历史记录") },
                text = { Text("此操作会清除当前账号的在线和本地的全部历史记录。") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearHistoryDialog = false
                        coroutineScope.launch {
                            paginationEnvironment.clearAllHistory()
                            viewModel.displayItems.clear()
                            userMessages.showShortMessage("已清除所有历史记录")
                        }
                    }) {
                        Text("确认")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("我再想想")
                    }
                },
            )
        }
        FeedPullToRefresh(viewModel, paginationEnvironment, padding = innerPadding) {
            PaginatedList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("online_history_list"),
                items = viewModel.displayItems,
                listState = listState,
                onLoadMore = { viewModel.loadMore(paginationEnvironment) },
                isEnd = { viewModel.isEnd },
            ) { item ->
                FeedCard(
                    item,
                )
            }
        }
    }
}
