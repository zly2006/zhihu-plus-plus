package com.github.zly2006.zhihu.v2.ui

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.ui.home.setupUpWebview
import com.github.zly2006.zhihu.v2.viewmodel.FeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

@Composable
fun QuestionScreen(
    question: Question,
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: FeedViewModel = viewModel()
    val listState = rememberLazyListState()
    var questionContent by remember { mutableStateOf("") }
    val httpClient = remember { AccountData.httpClient(context) }

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

    // 加载问题详情和答案
    LaunchedEffect(question.questionId) {
        withContext(Dispatchers.IO) {
            val questionData = DataHolder.getQuestion(context, httpClient, question.questionId)?.value
            if (questionData != null) {
                questionContent = questionData.detail
                viewModel.refreshQuestion(context, question.questionId)
            }
        }
    }

    // 加载更多答案
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore(context)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row {
                Text(
                    text = question.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            // 问题详情
            if (questionContent.isNotEmpty()) {
                item {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setupUpWebview(this, ctx)
                                loadDataWithBaseURL(
                                    "https://www.zhihu.com/question/${question.questionId}",
                                    """
                                    <head>
                                        <link rel="stylesheet" href="//zhihu-plus.internal/assets/stylesheet.css">
                                        <viewport content="width=device-width, initial-scale=1.0">
                                    </head>
                                    """.trimIndent() + Jsoup.parse(questionContent).toString(),
                                    "text/html",
                                    "utf-8",
                                    null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 答案列表
            items(viewModel.displayItems.filter { it.displayMode == FeedViewModel.DisplayMode.QUESTION }) { item ->
                FeedCard(item) { feed ->
                    feed?.let {
                        DataHolder.putFeed(it)
                        navController.navigate(
                            Article(
                                item.title,
                                "answer",
                                (it.target as Feed.AnswerTarget).id,
                                it.target.author.name,
                                it.target.author.headline,
                                it.target.author.avatar_url,
                                null
                            )
                        )
                    }
                }
            }

            // 加载更多指示器
            if (viewModel.displayItems.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
