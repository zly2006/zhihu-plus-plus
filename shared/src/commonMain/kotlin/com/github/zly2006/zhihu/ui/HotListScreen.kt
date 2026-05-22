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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.shared.data.HotListFeed
import com.github.zly2006.zhihu.shared.platform.UserMessageDuration
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.HotListViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment

const val HOT_LIST_LIST_TAG = "hot_list_list"
const val HOT_LIST_REFRESH_BUTTON_TAG = "hot_list_refresh_button"

@Composable
fun HotListScreen(
    innerPadding: PaddingValues,
): Unit = HotListScreenContent(innerPadding, onTestRefreshClick = null, onTestLoadMore = null)

@Composable
fun HotListScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onTestRefreshClick: (() -> Unit)? = null,
    onTestLoadMore: (() -> Unit)? = null,
): Unit = HotListScreenContent(innerPadding, onTestRefreshClick, onTestLoadMore)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HotListScreenContent(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onTestRefreshClick: (() -> Unit)? = null,
    onTestLoadMore: (() -> Unit)? = null,
) {
    val viewModel: HotListViewModel = viewModel()
    val environment = rememberPaginationEnvironment(viewModel.allowGuestAccess)
    val userMessages = rememberUserMessageSink()
    val settings = rememberSettingsStore()

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

    Column {
        FeedPullToRefresh(viewModel, environment) {
            PaginatedList(
                items = viewModel.displayItems,
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
    }
}
