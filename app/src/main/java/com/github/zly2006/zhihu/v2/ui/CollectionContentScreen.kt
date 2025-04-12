package com.github.zly2006.zhihu.v2.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.v2.ui.components.FeedCard
import com.github.zly2006.zhihu.v2.ui.components.PaginatedList
import com.github.zly2006.zhihu.v2.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.v2.viewmodel.CollectionContentViewModel
import java.util.*

@Composable
fun CollectionContentScreen(
    collectionId: String,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = viewModel { CollectionContentViewModel(collectionId) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (viewModel.allData.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    PaginatedList(
        items = viewModel.displayItems,
        onLoadMore = { viewModel.loadMore(context) },
        isEnd = { viewModel.isEnd },
        listState = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        footer = ProgressIndicatorFooter,
        topContent = {
            item(0) {
                Text(
                    text = viewModel.title,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                )
                Text(
                    listOfNotNull(
                        "${viewModel.collection?.item_count} 条收藏",
                        "${viewModel.collection?.like_count} 个赞同",
                        "${viewModel.collection?.comment_count} 条评论",
                        viewModel.collection?.updated_time?.let { "${YMDHMS.format(Date(it * 1000))} 更新" }
                    ).fastJoinToString(" · "),
                )
            }
        },
        itemContent = { item ->
            FeedCard(
                item,
                Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                navDestination?.let { onNavigate(it) }
            }
        }
    )
}
