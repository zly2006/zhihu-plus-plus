package com.github.zly2006.zhihu.v2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.v2.viewmodel.CollectionContentViewModel

@Composable
fun CollectionContentScreen(
    collectionId: String,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = viewModel { CollectionContentViewModel(collectionId) }
    val displayItems = viewModel.displayItems
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.refresh(context)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleItemIndex ->
                if (lastVisibleItemIndex == displayItems.size - 1 && !viewModel.isEnd && !viewModel.isLoading) {
                    viewModel.loadMore(context)
                }
            }
    }


    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(displayItems) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                onClick = {
                    item.navDestination?.let {
                        onNavigate(it)
                    }
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = item.title ?: "无标题", style = MaterialTheme.typography.titleMedium)
                    Text(text = item.details ?: "无详情", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            if (!viewModel.isEnd)
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
    }
}
