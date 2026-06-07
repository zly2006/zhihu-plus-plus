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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.viewmodel.feed.HistoryViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment

@Composable
fun LegacyLocalHistoryScreen(
    innerPadding: PaddingValues,
) {
    val viewModel: HistoryViewModel = viewModel { HistoryViewModel() }
    val environment = rememberPaginationEnvironment(allowGuestAccess = true)

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(environment)
        }
    }

    FeedPullToRefresh(viewModel, environment) {
        PaginatedList(
            modifier = Modifier.padding(innerPadding),
            items = viewModel.displayItems,
            onLoadMore = { /* 不需要加载更多 */ },
            isEnd = { true }, // 始终为 true，因为没有更多数据需要加载。
        ) { item ->
            FeedCard(
                item,
            )
        }
    }
}
