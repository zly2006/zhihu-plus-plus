package com.github.zly2006.zhihu.v2.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.ui.home.Reaction
import com.github.zly2006.zhihu.ui.home.ReadArticleFragment.ReadArticleViewModel.VoteUpState
import com.github.zly2006.zhihu.ui.home.setupUpWebview
import com.github.zly2006.zhihu.v2.MainActivity
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

@Composable
fun ArticleScreen(
    article: Article,
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val httpClient = remember { AccountData.httpClient(context) }

    val scrollState = rememberScrollState()
    var content by remember { mutableStateOf("") }
    var voteUpCount by remember { mutableStateOf(0) }
    var commentCount by remember { mutableStateOf(0) }
    var voteUpState by remember { mutableStateOf(VoteUpState.Neutral) }
    var questionId by remember { mutableStateOf(0L) }

    LaunchedEffect(article.id) {
        withContext(Dispatchers.IO) {
            if (article.type == "answer") {
                DataHolder.getAnswerCallback(context, httpClient, article.id) { answer ->
                    if (answer != null) {
                        content = answer.content
                        voteUpCount = answer.voteupCount
                        commentCount = answer.commentCount
                        questionId = answer.question.id

                        // 更新文章信息并记录历史
                        val updatedArticle = Article(
                            answer.question.title,
                            "answer",
                            article.id,
                            answer.author.name,
                            answer.author.headline,
                            answer.author.avatarUrl,
                            answer.excerpt
                        )
                        (context as? MainActivity)?.postHistory(updatedArticle)
                    } else {
                        content = "<h1>回答不存在</h1>"
                        Log.e("ArticleScreen", "Answer not found")
                    }
                }
            } else if (article.type == "article") {
                DataHolder.getArticleCallback(context, httpClient, article.id) { articleData ->
                    if (articleData != null) {
                        content = articleData.content
                        voteUpCount = articleData.voteupCount
                        commentCount = articleData.commentCount

                        // 更新文章信息并记录历史
                        val updatedArticle = Article(
                            articleData.title,
                            "article",
                            article.id,
                            articleData.author.name,
                            articleData.author.headline,
                            articleData.author.avatarUrl,
                            articleData.excerpt
                        )
                        (context as? MainActivity)?.postHistory(updatedArticle)
                    } else {
                        content = "<h1>文章不存在</h1>"
                        Log.e("ArticleScreen", "Article not found")
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        topBar = {
            // 标题
            Text(
                text = article.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        bottomBar = {
            // 底部操作栏
            if (backStackEntry.hasRoute(Article::class)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 点赞按钮
                    Button(
                        onClick = {
                            val newState = if (voteUpState == VoteUpState.Up) VoteUpState.Neutral else VoteUpState.Up

                            coroutineScope.launch {
                                try {
                                    // 统一处理答案和文章的点赞逻辑
                                    val endpoint = when (article.type) {
                                        "answer" -> "https://www.zhihu.com/api/v4/answers/${article.id}/voters"
                                        "article" -> "https://www.zhihu.com/api/v4/articles/${article.id}/vote"
                                        else -> return@launch
                                    }

                                    val response = httpClient.post(endpoint) {
                                        when (article.type) {
                                            "answer" -> setBody(mapOf("type" to newState.key))
                                            "article" -> setBody(mapOf("voting" to if (newState == VoteUpState.Up) 1 else 0))
                                            else -> return@launch
                                        }
                                        setBody(body)
                                        contentType(ContentType.Application.Json)
                                    }.body<Reaction>()

                                    voteUpState = newState
                                    voteUpCount = response.voteup_count
                                } catch (e: Exception) {
                                    Log.e("ArticleScreen", "Vote up failed", e)
                                    context.mainExecutor.execute {
                                        Toast.makeText(context, "点赞失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (voteUpState == VoteUpState.Up) Color(0xFF0D47A1) else Color(0xFF29B6F6)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(Icons.Filled.ThumbUp, contentDescription = "赞同")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (voteUpState == VoteUpState.Up) "已赞 $voteUpCount" else "赞同 $voteUpCount")
                    }

                    // 评论按钮
                    Button(
                        onClick = {
                            // todo: 跳转到评论dialog
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "$commentCount")
                    }

                    // 复制链接按钮
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val linkType =
                                if (article.type == "answer") "question/${questionId}/answer" else article.type
                            val clip = ClipData.newPlainText(
                                "Link",
                                "https://www.zhihu.com/$linkType/${article.id}"
                                        + "\n【${article.title} - ${article.authorName}的回答】"
                            )
                            clipboard.setPrimaryClip(clip)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "复制链接")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "复制链接")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).verticalScroll(scrollState),
        ) {
            // 作者信息
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 作者头像
                if (article.avatarSrc != null) {
                    AsyncImage(
                        model = article.avatarSrc,
                        contentDescription = "作者头像",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 作者名称和简介
                Column {
                    Text(
                        text = article.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = article.authorBio,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // 文章内容 WebView
            if (content.isNotEmpty()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setupUpWebview(this, ctx)
                            loadDataWithBaseURL(
                                "https://www.zhihu.com/${article.type}/${article.id}",
                                """
                            <head>
                            <link rel="stylesheet" href="//zhihu-plus.internal/assets/stylesheet.css">
                            <viewport content="width=device-width, initial-scale=1.0">
                            </head>
                            """.trimIndent() + Jsoup.parse(content).toString(),
                                "text/html",
                                "utf-8",
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview
@Composable
fun ArticleScreenPreview() {
    val navController = rememberNavController()
    ArticleScreen(
        Article(
            "如何看待《狂暴之翼》中的人物设定？",
            "answer",
            123456789,
            "知乎用户",
            "知乎用户",
            "",
        ),
        navController
    )
}
