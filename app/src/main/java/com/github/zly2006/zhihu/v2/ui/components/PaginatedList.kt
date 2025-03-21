@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.v2.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

val ProgressIndicatorFooter: @Composable (() -> Unit)? = {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun <T> PaginatedList(
    items: List<T>,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    isEnd: () -> Boolean = { false },
    footer: @Composable (() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 3
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && items.isNotEmpty()) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(items) { item ->
            itemContent(item)
        }
        
        if (items.isNotEmpty()) {
            item {
                if (isEnd()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "已经到底啦",
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    footer?.invoke()
                }
            }
        }
    }
}
