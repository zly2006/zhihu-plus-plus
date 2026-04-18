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

import android.content.Context.MODE_PRIVATE
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.viewModels
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.HotListFeed
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.ui.components.BlockUserConfirmDialog
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.HotListViewModel

const val HOT_LIST_LIST_TAG = "hot_list_list"
const val HOT_LIST_REFRESH_BUTTON_TAG = "hot_list_refresh_button"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotListScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onTestRefreshClick: (() -> Unit)? = null,
    onTestLoadMore: (() -> Unit)? = null,
) {
    val navigator = LocalNavigator.current
    val context = LocalActivity.current as MainActivity
    val viewModel: HotListViewModel by context.viewModels()
    val preferences = remember {
        context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE)
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

    // Block user confirm dialog
    var showBlockUserDialog by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column {
        FeedPullToRefresh(viewModel) {
            PaginatedList(
                items = viewModel.displayItems,
                onLoadMore = { onTestLoadMore?.invoke() ?: viewModel.loadMore(context) },
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

            val showRefreshFab = remember { preferences.getBoolean("showRefreshFab", true) }
            if (showRefreshFab) {
                DraggableRefreshButton(
                    modifier = Modifier.testTag(HOT_LIST_REFRESH_BUTTON_TAG),
                    onClick = {
                        onTestRefreshClick?.invoke() ?: viewModel.refresh(context)
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

        // Block user confirm dialog
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
