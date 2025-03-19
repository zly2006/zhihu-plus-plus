package com.github.zly2006.zhihu.v2.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.v2.viewmodel.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
) {
    val viewModel: FeedViewModel = viewModel()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    
    // 检查是否需要加载更多内容
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
    
    // 初始加载
    LaunchedEffect(Unit) {
        val data = AccountData.getData(context)
        if (!data.login) {
            val myIntent = Intent(context, LoginActivity::class.java)
            context.startActivity(myIntent)
        } else if (viewModel.displayItems.isEmpty()) {
            // 只在第一次加载时刷新，这样可以避免在返回时刷新
            viewModel.refresh(context)
        }
    }
    
    // 加载更多
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && viewModel.displayItems.isNotEmpty()) {
            viewModel.loadMore(context)
        }
    }
    
    // 显示错误信息
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(state = listState) {
            items(viewModel.displayItems) { item ->
                FeedCard(item) { feed ->
                    feed?.let {
                        DataHolder.putFeed(it)
                        when (val target = it.target) {
                            is Feed.AnswerTarget -> {
                                navController.navigate(
                                    Article(
                                        target.question.title,
                                        "answer",
                                        target.id,
                                        target.author.name,
                                        target.author.headline,
                                        target.author.avatar_url,
                                        target.excerpt
                                    )
                                )
                            }
                            is Feed.ArticleTarget -> {
                                navController.navigate(
                                    Article(
                                        target.title,
                                        "article",
                                        target.id,
                                        target.author.name,
                                        target.author.headline,
                                        target.author.avatar_url,
                                        target.excerpt
                                    )
                                )
                            }

                            is Feed.AdvertTarget -> {}
                            is Feed.PinTarget -> {}
                            is Feed.VideoTarget -> {}
                        }
                    }
                }
            }
            
            item {
                if (viewModel.displayItems.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun FeedCard(
    item: FeedViewModel.FeedDisplayItem,
    onClick: (Feed?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onClick(item.feed) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (!item.isFiltered) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Text(
                text = item.summary,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp)
            )
            
            Text(
                text = item.details,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}
