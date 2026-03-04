package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.History
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.viewmodel.feed.OnlineHistoryViewModel

@Composable
fun OnlineHistoryScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    val navigator = LocalNavigator.current
    val viewModel: OnlineHistoryViewModel = viewModel()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    FeedPullToRefresh(viewModel) {
        PaginatedList(
            items = viewModel.displayItems,
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
            onLoadMore = { viewModel.loadMore(context) },
            isEnd = { viewModel.isEnd },
            topContent = {
                item(0) {
                    Row(modifier = androidx.compose.ui.Modifier.padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())) {
                        FilledTonalButton(
                            onClick = {
                                navigator.onNavigate(History)
                            },
                        ) {
                            Text("本地历史记录（老版本）")
                        }
                    }
                }
            },
        ) { item ->
            FeedCard(
                item,
            ) {
                item.navDestination?.let {
                    navigator.onNavigate(it)
                }
            }
        }
    }
}
