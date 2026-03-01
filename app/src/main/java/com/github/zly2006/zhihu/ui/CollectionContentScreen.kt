package com.github.zly2006.zhihu.ui

import androidx.activity.viewModels
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
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.navigator.CollectionAnswerNavigator
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.CollectionContentViewModel
import java.util.Date

@Composable
fun CollectionContentScreen(
    collectionId: String,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel = viewModel { CollectionContentViewModel(collectionId) }
    val listState = rememberLazyListState()
    val sharedData = if (context is MainActivity) {
        val sd by context.viewModels<ArticleViewModel.ArticlesSharedData>()
        sd
    } else {
        null
    }

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
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth(),
                )
                Text(
                    listOfNotNull(
                        "${viewModel.collection?.itemCount} 条收藏",
                        "${viewModel.collection?.likeCount} 个赞同",
                        "${viewModel.collection?.commentCount} 条评论",
                        viewModel.collection?.updatedTime?.let { "${YMDHMS.format(Date(it * 1000))} 更新" },
                    ).fastJoinToString(" · "),
                )
            }
        },
    ) { item ->
        FeedCard(
            item,
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            val dest = navDestination
            if (dest is Article && dest.type == ArticleType.Answer && sharedData != null) {
                val idx = viewModel.displayItems.indexOf(item)
                val nextItems = if (idx >= 0) viewModel.allData.drop(idx + 1) else emptyList()
                val prevItems = if (idx > 0) viewModel.allData.take(idx).reversed() else emptyList()
                sharedData.pendingNavigator = CollectionAnswerNavigator(
                    collectionId = collectionId,
                    collectionTitle = viewModel.title,
                    initialNextItems = nextItems,
                    initialPreviousItems = prevItems,
                )
            }
            dest?.let { navigator.onNavigate(it) }
        }
    }
}
