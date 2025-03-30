@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.v2.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.v2.ui.components.WebviewComp
import com.github.zly2006.zhihu.v2.ui.components.loadZhihu
import com.github.zly2006.zhihu.v2.viewmodel.CommentItem
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

@Serializable
data class Reaction(
    val reaction_count: Int,
    val reaction_state: Boolean,
    val reaction_value: String,
    val success: Boolean,
    val is_thanked: Boolean,
    val thanks_count: Int,
    val red_heart_count: Int,
    val red_heart_has_set: Boolean,
    val is_liked: Boolean,
    val liked_count: Int,
    val is_up: Boolean,
    val voteup_count: Int,
    val is_upped: Boolean,
    val up_count: Int,
    val is_down: Boolean,
    val voting: Int,
    val heavy_up_result: String,
    val is_auto_send_moments: Boolean
)

enum class VoteUpState(val key: String) {
    Up("up"),
    Down("down"),
    Neutral("neutral"),
}

@Composable
fun ArticleScreen(
    article: Article,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current as MainActivity
    val coroutineScope = rememberCoroutineScope()
    val backStackEntry by context.navController.currentBackStackEntryAsState()
    val httpClient = context.httpClient

    val scrollState = rememberScrollState()
    var title by remember { mutableStateOf(article.title) }
    var authorName by remember { mutableStateOf(article.authorName) }
    var authorBio by remember { mutableStateOf(article.authorBio) }
    var authorAvatarSrc by remember { mutableStateOf(article.avatarSrc) }
    var content by remember { mutableStateOf("") }
    var voteUpCount by remember { mutableStateOf(0) }
    var commentCount by remember { mutableStateOf(0) }
    var voteUpState by remember { mutableStateOf(VoteUpState.Neutral) }
    var questionId by remember { mutableStateOf(0L) }
    var showComments by remember { mutableStateOf(false) }

    LaunchedEffect(article.id) {
        withContext(Dispatchers.IO) {
            try {
                if (article.type == "answer") {
                    DataHolder.getAnswerCallback(context, httpClient, article.id) { answer ->
                        if (answer != null) {
                            title = answer.question.title
                            authorName = answer.author.name
                            content = answer.content
                            authorBio = answer.author.headline
                            authorAvatarSrc = answer.author.avatarUrl
                            voteUpCount = answer.voteupCount
                            commentCount = answer.commentCount
                            questionId = answer.question.id

                            // 更新文章信息并记录历史
                            val updatedArticle = Article(
                                title,
                                "answer",
                                article.id,
                                authorName,
                                authorBio,
                                authorAvatarSrc,
                                answer.excerpt
                            )
                            context.postHistory(updatedArticle)
                        } else {
                            content = "<h1>回答不存在</h1>"
                            Log.e("ArticleScreen", "Answer not found")
                            context.mainExecutor.execute {
                                Toast.makeText(context, "回答不存在或已被删除", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else if (article.type == "article") {
                    DataHolder.getArticleCallback(context, httpClient, article.id) { articleData ->
                        if (articleData != null) {
                            title = articleData.title
                            content = articleData.content
                            voteUpCount = articleData.voteupCount
                            commentCount = articleData.commentCount
                            authorName = articleData.author.name
                            authorBio = articleData.author.headline
                            authorAvatarSrc = articleData.author.avatarUrl

                            // 更新文章信息并记录历史
                            val updatedArticle = Article(
                                title,
                                "article",
                                article.id,
                                authorName,
                                authorBio,
                                authorAvatarSrc,
                                articleData.excerpt
                            )
                            context.postHistory(updatedArticle)
                        } else {
                            content = "<h1>文章不存在</h1>"
                            Log.e("ArticleScreen", "Article not found")
                            context.mainExecutor.execute {
                                Toast.makeText(context, "文章不存在或已被删除", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ArticleScreen", "Failed to load content", e)
                context.mainExecutor.execute {
                    Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                modifier = Modifier.padding(bottom = 8.dp)
                    .clickable {
                        onNavigate(Question(questionId, title))
                    }
            )
        },
        bottomBar = {
            // 底部操作栏
            if (backStackEntry.hasRoute(Article::class)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 6.dp),
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
                        onClick = { showComments = true },
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
                                        + "\n【$title - $authorName 的回答】"
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
                if (authorAvatarSrc != null) {
                    AsyncImage(
                        model = authorAvatarSrc,
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
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = authorBio,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // 文章内容 WebView
            if (content.isNotEmpty()) {
                WebviewComp(httpClient) {
                    it.loadZhihu(
                        "https://www.zhihu.com/${article.type}/${article.id}",
                        Jsoup.parse(content)
                    )
                }
            }
        }
    }

    // 0 - 不显示半透明背景 1 - 显示在两个评论最下方 2 - 显示在两个评论中间
    var backFilterState by remember { mutableStateOf(0) }

    // 评论弹窗
    var activeChildComment by remember { mutableStateOf<CommentItem?>(null) }
    AnimatedVisibility(
        visible = showComments && activeChildComment == null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                .clickable { showComments = false }
        )
    }
    CommentScreen(
        visible = showComments,
        httpClient = httpClient,
        content = article,
        onChildCommentClick = {
            activeChildComment = it
        }
    )
    AnimatedVisibility(
        visible = showComments && activeChildComment != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        // 半透明背景,点击上方区域关闭
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                .clickable { activeChildComment = null }
        )
    }
    CommentScreen(
        visible = activeChildComment != null,
        httpClient = httpClient,
        content = activeChildComment?.clickTarget,
        activeCommentItem = activeChildComment,
        onChildCommentClick = { }
    )

    BackHandler(
        enabled = activeChildComment != null,
    ) {
        activeChildComment = null
    }

    BackHandler(
        enabled = showComments && activeChildComment == null,
    ) {
        showComments = false
    }
}

@Preview
@Composable
fun ArticleScreenPreview() {
    ArticleScreen(
        Article(
            "如何看待《狂暴之翼》中的人物设定？",
            "answer",
            123456789,
            "知乎用户",
            "知乎用户",
            "",
        )
    ) {}
}
