@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.v2.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.*
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.v2.ui.components.*
import com.github.zly2006.zhihu.v2.viewmodel.QuestionFeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

@Composable
fun QuestionScreen(
    question: Question,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current as MainActivity
    val viewModel: QuestionFeedViewModel = viewModel {
        QuestionFeedViewModel(question.questionId)
    }
    var questionContent by remember { mutableStateOf("") }
    var title by remember { mutableStateOf(question.title) }
    val httpClient = remember { AccountData.httpClient(context) }

    // 加载问题详情和答案
    LaunchedEffect(question.questionId) {
        withContext(Dispatchers.IO) {
            val questionData = DataHolder.getQuestion(context, httpClient, question.questionId)?.value
            if (questionData != null) {
                questionContent = questionData.detail
                title = questionData.title
                viewModel.refresh(context)
                context.postHistory(
                    Question(
                        question.questionId,
                        title
                    )
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row {
                Text(
                    text = title,
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
            isEnd = { viewModel.isEnd },
            modifier = Modifier.padding(innerPadding),
            footer = ProgressIndicatorFooter,
            topContent = {
                item(1) {
                    val handle = LocalPinnableContainer.current?.pin()
                    if (questionContent.isNotEmpty()) {
                        WebviewComp(httpClient) {
                            it.loadUrl(
                                "https://www.zhihu.com/question/${question.questionId}",
                                Jsoup.parse(questionContent)
                            )
                        }
                    }
                }
                item(2) {
                    val handle = LocalPinnableContainer.current?.pin()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://www.zhihu.com/question/${question.questionId}/log")
                                    setClass(context, WebviewActivity::class.java)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.padding(16.dp).weight(1f)
                        ) {
                            Text("查看日志")
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(
                                    "Link",
                                    "https://www.zhihu.com/question/${question.questionId}" +
                                            "\n【${question.title}】"
                                )
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.padding(end = 16.dp).weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "复制链接")
                            Spacer(Modifier.width(8.dp))
                            Text("复制链接")
                        }
                    }
                }
            }
        ) { item ->
            FeedCard(item) { feed ->
                feed?.let {
                    DataHolder.putFeed(it)
                    val target = it.target as Feed.AnswerTarget
                    onNavigate(
                        Article(
                            item.title,
                            "answer",
                            target.id,
                            target.author.name,
                            target.author.headline,
                            target.author.avatar_url,
                            null
                        )
                    )
                }
            }
        }
    }
}
