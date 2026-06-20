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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.shared.data.HotListFeed
import com.github.zly2006.zhihu.shared.platform.UserMessageDuration
import com.github.zly2006.zhihu.shared.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.ui.TopLevelReselectAction
import com.github.zly2006.zhihu.shared.ui.topLevelReselectAction
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeStyle
import com.github.zly2006.zhihu.ui.components.BlockUserConfirmDialog
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.HotListViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

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
    backdrop: LayerBackdrop? = null,
    scrollBehavior: ScrollBehavior? = null,
    contentTopPadding: Dp = 0.dp,
) {
    val viewModel: HotListViewModel = viewModel { HotListViewModel() }
    val environment = rememberPaginationEnvironment(viewModel.allowGuestAccess)
    val userMessages = rememberUserMessageSink()
    val settings = rememberSettingsStore()
    val scope = rememberCoroutineScope()
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
        // 按主题分流，而非 backdrop 是否为空：miuix 主题下关闭模糊或低版本系统不支持
        // RenderEffect 时 backdrop 也为 null，但仍应渲染 miuix 内容样式。
        if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
            // miuix path
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
                        onLoadMore = { onTestLoadMore?.invoke() ?: viewModel.loadMore(environment) },
                        modifier = Modifier
                            .fillMaxHeight()
                            .overScrollVertical()
                            .scrollEndHaptic()
                            .then(if (scrollBehavior != null) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)
                            .testTag(HOT_LIST_LIST_TAG),
                        contentPadding = PaddingValues(top = contentTopPadding + 6.dp),
                        isEnd = { viewModel.isEnd },
                    ) { item ->
                        com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard(
                            item = item,
                            thumbnailUrl = (item.feed as? HotListFeed)?.children?.firstOrNull()?.thumbnail,
                            // 热榜标题即正文，放宽到 4 行；同时抬高 maxHeight 给多行标题留空间，避免卡片裁掉底部。
                            titleMaxLines = 4,
                            maxHeight = 320.dp,
                        )
                    }
                }
                val showRefreshFab = rememberSettingBoolean("showRefreshFab", true, settings)
                if (showRefreshFab) {
                    DraggableRefreshButton(
                        modifier = Modifier.testTag(HOT_LIST_REFRESH_BUTTON_TAG),
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
        } else {
            // M3 path
            FeedPullToRefresh(viewModel) {
                PaginatedList(
                    items = viewModel.displayItems,
                    listState = listState,
                    onLoadMore = { onTestLoadMore?.invoke() ?: viewModel.loadMore(environment) },
                    modifier = Modifier.padding(innerPadding).testTag(HOT_LIST_LIST_TAG),
                    isEnd = { viewModel.isEnd },
                    footer = ProgressIndicatorFooter,
                ) { item ->
                    FeedCard(
                        item,
                        thumbnailUrl = (item.feed as? HotListFeed)?.children?.firstOrNull()?.thumbnail,
                    )
                }

                val showRefreshFab = rememberSettingBoolean("showRefreshFab", true, settings)
                if (showRefreshFab) {
                    DraggableRefreshButton(
                        modifier = Modifier.testTag(HOT_LIST_REFRESH_BUTTON_TAG),
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
