@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.ui

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.WebviewActivity
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.viewmodel.feed.QuestionFeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

@OptIn(ExperimentalLayoutApi::class)
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
    var answerCount by remember { mutableIntStateOf(0) }
    var visitCount by remember { mutableIntStateOf(0) }
    var commentCount by remember { mutableIntStateOf(0) }
    var followerCount by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf(question.title) }
    var showComments by remember { mutableStateOf(false) }

    // 加载问题详情和答案
    LaunchedEffect(question.questionId) {
        context as MainActivity
        withContext(Dispatchers.IO) {
            try {
                if (viewModel.displayItems.isEmpty()) {
                    launch {
                        viewModel.refresh(context)
                    }
                }
                val questionData = DataHolder.getContentDetail(context, question)
                if (questionData != null) {
                    questionContent = questionData.detail
                    title = questionData.title
                    answerCount = questionData.answerCount
                    visitCount = questionData.visitCount
                    commentCount = questionData.commentCount
                    followerCount = questionData.followerCount
                    context.postHistory(
                        Question(
                            question.questionId,
                            title,
                        ),
                    )
                } else {
                    context.mainExecutor.execute {
                        Toast.makeText(context, "获取问题详情失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                context.mainExecutor.execute {
                    Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SelectionContainer(
                modifier = Modifier.fuckHonorService(),
            ) {
                Row {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        },
    ) { innerPadding ->
        FeedPullToRefresh(viewModel) {
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
                            WebviewComp {
                                it.loadZhihu(
                                    "https://www.zhihu.com/question/${question.questionId}",
                                    Jsoup.parse(questionContent),
                                )
                            }
                        }
                    }
                    item(2) {
                        val handle = LocalPinnableContainer.current?.pin()
                        // 排序选项
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("排序：", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    viewModel.updateSortOrder("default")
                                    viewModel.refresh(context)
                                },
                                colors = if (viewModel.sortOrder == "default") {
                                    ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    ButtonDefaults.filledTonalButtonColors()
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text("默认")
                            }
                            Spacer(Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    viewModel.updateSortOrder("updated")
                                    viewModel.refresh(context)
                                },
                                colors = if (viewModel.sortOrder == "updated") {
                                    ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    ButtonDefaults.filledTonalButtonColors()
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text("最新")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = "https://www.zhihu.com/question/${question.questionId}/log".toUri()
                                            setClass(context, WebviewActivity::class.java)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "打开日志失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text("查看日志")
                            }
                            Spacer(Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val clip = ClipData.newPlainText(
                                        "Link",
                                        "https://www.zhihu.com/question/${question.questionId}" +
                                            "\n【${question.title}】",
                                    )
                                    context.clipboardManager.setPrimaryClip(clip)
                                    (context as? MainActivity)?.sharedData?.clipboardDestination = question
                                    Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ),
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "复制链接")
                                Spacer(Modifier.width(8.dp))
                                Text("复制链接")
                            }

                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { showComments = true },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "$commentCount")
                            }
                        }
                        Text(
                            "$answerCount 个回答  $visitCount 次浏览  $commentCount 条评论  $followerCount 人关注",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                },
            ) { item ->
                FeedCard(item) {
//                    feed?.let { DataHolder.putFeed(it) }
                    navDestination?.let { onNavigate(it) }
                }
            }
        }
    }
    if (context is MainActivity) {
        CommentScreenComponent(
            showComments = showComments,
            onDismiss = { showComments = false },
            httpClient = context.httpClient,
            onNavigate = onNavigate,
            content = question,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun QuestionScreenPreview() {
    val question = Question(123456789, "这是一个问题的标题")
    QuestionScreen(
        question = question,
        onNavigate = { },
    )
}
