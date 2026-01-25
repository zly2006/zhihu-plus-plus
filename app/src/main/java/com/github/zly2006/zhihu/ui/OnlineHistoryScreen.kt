package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.History
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.viewmodel.feed.OnlineHistoryViewModel

@Composable
fun OnlineHistoryScreen(
    onNavigate: (NavDestination) -> Unit,
) {
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
            onLoadMore = { viewModel.loadMore(context) },
            isEnd = { viewModel.isEnd },
            topContent = {
                item(0) {
                    Row {
                        Text("您正在查看在线阅读历史，点击查看")
                        Button(
                            onClick = {
                                onNavigate(History)
                            },
                        ) {
                            Text("本地历史记录（老版本）")
                        }
                    }
                }
            },
        ) { item ->
            FeedCard(item) {
                item.navDestination?.let {
                    onNavigate(it)
                }
            }
        }
    }
}
