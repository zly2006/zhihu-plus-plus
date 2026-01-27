package com.github.zly2006.zhihu.ui

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.ui.components.BlockUserConfirmDialog
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.HotListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotListScreen(
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalActivity.current as MainActivity
    val viewModel: HotListViewModel by context.viewModels()

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // Block user confirm dialog
    var showBlockUserDialog by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column {
        FeedPullToRefresh(viewModel) {
            PaginatedList(
                items = viewModel.displayItems,
                onLoadMore = { viewModel.loadMore(context) },
                topContent = {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                },
                footer = ProgressIndicatorFooter,
            ) { item ->
                FeedCard(item) {
                    if (navDestination != null) {
                        onNavigate(navDestination)
                    } else {
                        Toast.makeText(context, "暂不支持打开该内容", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            DraggableRefreshButton(
                onClick = {
                    viewModel.refresh(context)
                },
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        }

        // Block user confirm dialog
        BlockUserConfirmDialog(
            showDialog = showBlockUserDialog,
            userToBlock = userToBlock,
            displayItems = viewModel.displayItems,
            context = context,
            onDismiss = {
                showBlockUserDialog = false
                userToBlock = null
            },
            onConfirm = {
                viewModel.refresh(context)
                showBlockUserDialog = false
                userToBlock = null
            },
        )
    }
}
