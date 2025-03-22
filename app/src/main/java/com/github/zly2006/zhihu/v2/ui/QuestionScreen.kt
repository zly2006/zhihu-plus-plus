@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.v2.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.v2.ui.components.FeedCard
import com.github.zly2006.zhihu.v2.ui.components.PaginatedList
import com.github.zly2006.zhihu.v2.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.v2.viewmodel.QuestionFeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QuestionScreen(
    question: Question,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: QuestionFeedViewModel = viewModel {
        QuestionFeedViewModel(question.questionId)
    }
    var questionContent by remember { mutableStateOf("") }
    val httpClient = remember { AccountData.httpClient(context) }

    // 加载问题详情和答案
    LaunchedEffect(question.questionId) {
        withContext(Dispatchers.IO) {
            val questionData = DataHolder.getQuestion(context, httpClient, question.questionId)?.value
            if (questionData != null) {
                questionContent = questionData.detail
                viewModel.refresh(context)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row {
                Text(
                    text = question.title,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) { innerPadding ->
        PaginatedList(
            items = viewModel.displayItems,
            onLoadMore = { viewModel.loadMore(context) },
            modifier = Modifier.padding(innerPadding),
            footer = ProgressIndicatorFooter
        ) { item ->
            FeedCard(item) { feed ->
                feed?.let {
                    DataHolder.putFeed(it)
                    onNavigate(
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
    }
}
