package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedPullToRefresh(
    viewModel: BaseFeedViewModel,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = viewModel.isPullToRefresh,
        onRefresh = { viewModel.pullToRefresh(context) },
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.Companion.align(Alignment.Companion.TopCenter),
                isRefreshing = viewModel.isPullToRefresh,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                state = state
            )
        },
        state = state,
        modifier = Modifier.Companion.fillMaxSize(),
        content = content
    )
}
