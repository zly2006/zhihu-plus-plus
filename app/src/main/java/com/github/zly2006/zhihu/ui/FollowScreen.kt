package com.github.zly2006.zhihu.ui

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.ui.components.BlockUserConfirmDialog
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.FollowRecommendViewModel
import com.github.zly2006.zhihu.viewmodel.feed.FollowViewModel
import kotlinx.coroutines.launch

class FollowScreenData : ViewModel() {
    var selectedTabIndex by mutableIntStateOf(0)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FollowScreen() {
    val viewModel = viewModel<FollowScreenData>()
    val titles = listOf("动态", "推荐")
    val pagerState = rememberPagerState(pageCount = { titles.size })
    val coroutineScope = rememberCoroutineScope()

    // 同步PagerState和ViewModel的selectedTabIndex
    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectedTabIndex = pagerState.currentPage
    }

    LaunchedEffect(viewModel.selectedTabIndex) {
        if (pagerState.currentPage != viewModel.selectedTabIndex) {
            pagerState.animateScrollToPage(viewModel.selectedTabIndex)
        }
    }

    Column {
        PrimaryTabRow(selectedTabIndex = viewModel.selectedTabIndex) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = viewModel.selectedTabIndex == index,
                    onClick = {
                        viewModel.selectedTabIndex = index
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> FollowDynamicScreen()
                1 -> FollowRecommendScreen()
            }
        }
    }
}

@Composable
fun FollowDynamicScreen() {
    val navigator = LocalNavigator.current
    val context = LocalActivity.current as MainActivity
    val coroutineScope = rememberCoroutineScope()
    val viewModel: FollowViewModel by context.viewModels()

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

    // 屏蔽用户确认对话框
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
                FeedCard(
                    item,
                    onLike = {
                        Toast.makeText(context, "收到喜欢，功能正在优化", Toast.LENGTH_SHORT).show()
                    },
                    onDislike = {
                        Toast.makeText(context, "收到反馈，功能正在优化", Toast.LENGTH_SHORT).show()
                    },
                    onBlockUser = { feedItem ->
                        viewModel.handleBlockUser(context, feedItem) { authorInfo ->
                            userToBlock = authorInfo
                            showBlockUserDialog = true
                        }
                    },
                    onBlockTopic = { topicId, topicName ->
                        viewModel.handleBlockTopic(context, topicId, topicName)
                    },
                ) {
                    if (navDestination != null) {
                        navigator.onNavigate(navDestination)
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

        // 屏蔽用户确认对话框
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

@Composable
fun FollowRecommendScreen() {
    val navigator = LocalNavigator.current
    val context = LocalActivity.current as MainActivity
    val coroutineScope = rememberCoroutineScope()
    val viewModel: FollowRecommendViewModel by context.viewModels()

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

    // 屏蔽用户确认对话框
    var showBlockUserDialog by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column {
        FeedPullToRefresh(viewModel) {
            PaginatedList(
                items = viewModel.displayItems,
                topContent = {
                    item {
                        Text(
                            "提示：此 API 已不再在知乎官网使用，未来有可能被移除。",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                },
                onLoadMore = { viewModel.loadMore(context) },
                footer = ProgressIndicatorFooter,
            ) { item ->
                FeedCard(
                    item,
                    onBlockUser = { feedItem ->
                        viewModel.handleBlockUser(context, feedItem) { authorInfo ->
                            userToBlock = authorInfo
                            showBlockUserDialog = true
                        }
                    },
                    onBlockTopic = { topicId, topicName ->
                        viewModel.handleBlockTopic(context, topicId, topicName)
                    },
                ) {
                    if (navDestination != null) {
                        navigator.onNavigate(navDestination)
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

        // 屏蔽用户确认对话框
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
