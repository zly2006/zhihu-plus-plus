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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.FollowRecommendViewModel
import com.github.zly2006.zhihu.viewmodel.feed.FollowViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FollowScreenData : ViewModel() {
    var selectedTabIndex by mutableIntStateOf(0)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FollowScreen(
    onNavigate: (NavDestination) -> Unit,
) {
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
                0 -> FollowDynamicScreen(onNavigate)
                1 -> FollowRecommendScreen(onNavigate)
            }
        }
    }
}

@Composable
fun FollowDynamicScreen(
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalActivity.current as MainActivity
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
                    feedItem.feed?.target?.author?.let { author ->
                        userToBlock = Pair(author.id, author.name)
                        showBlockUserDialog = true
                    } ?: run {
                        Toast.makeText(context, "无法获取用户信息", Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
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

        // 屏蔽用户确认对话框
        if (showBlockUserDialog && userToBlock != null) {
            AlertDialog(
                onDismissRequest = {
                    showBlockUserDialog = false
                    userToBlock = null
                },
                title = { Text("屏蔽用户") },
                text = {
                    Column {
                        Text("确定要屏蔽用户 \"${userToBlock?.second}\" 吗？")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "屏蔽后，该用户的内容将不会在推荐流中显示。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            userToBlock?.let { (userId, userName) ->
                                GlobalScope.launch {
                                    try {
                                        val blocklistManager = com.github.zly2006.zhihu.viewmodel.filter.BlocklistManager.getInstance(context)
                                        viewModel.displayItems.find { item ->
                                            item.feed?.target?.author?.id == userId
                                        }?.feed?.target?.author?.let { author ->
                                            blocklistManager.addBlockedUser(
                                                userId = author.id,
                                                userName = author.name,
                                                urlToken = author.urlToken,
                                                avatarUrl = author.avatarUrl,
                                            )
                                            viewModel.refresh(context)
                                            Toast.makeText(context, "已屏蔽用户：${author.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "屏蔽用户失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            showBlockUserDialog = false
                            userToBlock = null
                        },
                    ) {
                        Text("确定屏蔽")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showBlockUserDialog = false
                            userToBlock = null
                        },
                    ) {
                        Text("取消")
                    }
                },
            )
        }
    }
}

@Composable
fun FollowRecommendScreen(
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalActivity.current as MainActivity
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
                    feedItem.feed?.target?.author?.let { author ->
                        userToBlock = Pair(author.id, author.name)
                        showBlockUserDialog = true
                    } ?: run {
                        Toast.makeText(context, "无法获取用户信息", Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
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

        // 屏蔽用户确认对话框
        if (showBlockUserDialog && userToBlock != null) {
            AlertDialog(
                onDismissRequest = {
                    showBlockUserDialog = false
                    userToBlock = null
                },
                title = { Text("屏蔽用户") },
                text = {
                    Column {
                        Text("确定要屏蔽用户 \"${userToBlock?.second}\" 吗？")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "屏蔽后，该用户的内容将不会在推荐流中显示。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            userToBlock?.let { (userId, userName) ->
                                GlobalScope.launch {
                                    try {
                                        val blocklistManager = com.github.zly2006.zhihu.viewmodel.filter.BlocklistManager.getInstance(context)
                                        viewModel.displayItems.find { item ->
                                            item.feed?.target?.author?.id == userId
                                        }?.feed?.target?.author?.let { author ->
                                            blocklistManager.addBlockedUser(
                                                userId = author.id,
                                                userName = author.name,
                                                urlToken = author.urlToken,
                                                avatarUrl = author.avatarUrl,
                                            )
                                            viewModel.refresh(context)
                                            Toast.makeText(context, "已屏蔽用户：${author.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "屏蔽用户失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            showBlockUserDialog = false
                            userToBlock = null
                        },
                    ) {
                        Text("确定屏蔽")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showBlockUserDialog = false
                            userToBlock = null
                        },
                    ) {
                        Text("取消")
                    }
                },
            )
        }
    }
}
