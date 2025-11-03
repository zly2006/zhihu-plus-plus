package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.CollectionsViewModel

@Composable
fun CollectionScreen(
    urlToken: String,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = viewModel {
        CollectionsViewModel(urlToken)
    }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (viewModel.allData.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    PaginatedList(
        items = viewModel.allData,
        onLoadMore = { viewModel.loadMore(context) },
        isEnd = { viewModel.isEnd },
        listState = listState,
        modifier = Modifier.fillMaxSize().padding(16.dp),
        footer = ProgressIndicatorFooter,
        topContent = {
            item(0) {
                Text(
                    text = "我的收藏",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth(),
                )
            }
        },
    ) { collection ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            onClick = {
                onNavigate(NavDestination.CollectionContent(collection.id))
            },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = collection.title, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
