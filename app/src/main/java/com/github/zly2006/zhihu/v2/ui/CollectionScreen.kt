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
import com.github.zly2006.zhihu.v2.viewmodel.CollectionsViewModel

@Composable
fun CollectionScreen() {
    val context = LocalContext.current
    val viewModel = viewModel<CollectionsViewModel>()
    val collections = viewModel.allData
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.refresh(context)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleItemIndex ->
                if (lastVisibleItemIndex == collections.size - 1 && !viewModel.isEnd && !viewModel.isLoading) {
                    viewModel.loadMore(context)
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(0) {
            Text(
                text = "我的收藏",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
            )
        }
        items(collections) { collection ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                onClick = {

                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = collection.title, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        item {
            if (!viewModel.isEnd)
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
    }
}
