@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.v2.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.*
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.v2.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.v2.ui.components.WebviewComp
import com.github.zly2006.zhihu.v2.ui.components.loadZhihu
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import kotlin.math.abs

private const val SCROLL_THRESHOLD = 10 // 滑动阈值，单位为dp
private val ScrollThresholdDp = SCROLL_THRESHOLD.dp

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

@Serializable
data class CollectionItem(
    val id: String,
    val is_favorited: Boolean,
    val title: String = ""
)

@Serializable
data class CollectionResponse(
    val data: List<CollectionItem>
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
    // 获取自动隐藏配置
    val preferences = LocalContext.current.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE)
    val isTitleAutoHide by remember { mutableStateOf(preferences.getBoolean("titleAutoHide", false)) }
    // 判断滚动方向
    var previousScrollValue by remember { mutableStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val scrollDeltaThreshold = with(density) { ScrollThresholdDp.toPx() }
    var topBarHeight by remember { mutableStateOf(0) }

    LaunchedEffect(scrollState.value) {
        val currentScroll = scrollState.value
        val scrollDelta = abs(currentScroll - previousScrollValue)
        // 仅当滚动量超过阈值时更新状态
        if (scrollDelta > scrollDeltaThreshold) {
            isScrollingUp = currentScroll < previousScrollValue
            previousScrollValue = currentScroll
        }
    }

    val showTopBar by remember {
        derivedStateOf {
            val canScroll = scrollState.maxValue > topBarHeight
            val remainingScrollSpace = scrollState.maxValue - scrollState.value
            val isNearBottom = remainingScrollSpace < scrollDeltaThreshold
            val isNearTop = scrollState.value < scrollDeltaThreshold
            when {
                !isTitleAutoHide -> true       // 强制显示模式
                !canScroll -> true             // 内容不足时强制显示
                isNearBottom -> false          // 底部区域强制隐藏
                isScrollingUp -> true          // 向上滚动时显示
                isNearTop -> true              // 顶部区域强制显示
                else -> false                  // 向下滚动且不在顶部时隐藏
            }
        }
    }
//    var topBarHeight by remember { mutableStateOf(0) }
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
    var isFavorited by remember { mutableStateOf(false) }
    var favoriteCollectionId by remember { mutableStateOf("") }

    // todo: 谁有空再支持一下多收藏夹选择
    fun toggleFavorite() {
        coroutineScope.launch {
            try {
                val contentType = if (article.type == "answer") "answer" else "article"

                val collectionsUrl = "https://www.zhihu.com/api/v4/collections/contents/$contentType/${article.id}"
                val collectionsResponse = httpClient.get(collectionsUrl)

                if (collectionsResponse.status.isSuccess()) {
                    val collections = collectionsResponse.body<CollectionResponse>()
                    val defaultCollection = collections.data.firstOrNull()

                    if (defaultCollection != null) {
                        val collectionId = defaultCollection.id
                        val isCurrentlyFavorited = defaultCollection.is_favorited
                        favoriteCollectionId = collectionId

                        val action = if (isCurrentlyFavorited) "remove" else "add"
                        val url = "https://api.zhihu.com/collections/contents/$contentType/${article.id}"
                        val body = "${action}_collections=$collectionId"

                        val response = httpClient.put(url) {
                            contentType(ContentType.Application.FormUrlEncoded)
                            setBody(body)
                        }

                        if (response.status.isSuccess()) {
                            isFavorited = !isCurrentlyFavorited
                            val message = if (isFavorited) "收藏成功" else "取消收藏成功"
                            context.mainExecutor.execute {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            context.mainExecutor.execute {
                                Toast.makeText(context, "收藏操作失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ArticleScreen", "Favorite toggle failed", e)
                context.mainExecutor.execute {
                    Toast.makeText(context, "收藏操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

                val contentType = if (article.type == "answer") "answer" else "article"
                try {
                    val collectionsUrl = "https://www.zhihu.com/api/v4/collections/contents/$contentType/${article.id}"
                    val collectionsResponse = httpClient.get(collectionsUrl) {
                        signFetchRequest(context)
                    }

                    if (collectionsResponse.status.isSuccess()) {
                        val collections = collectionsResponse.body<CollectionResponse>()
                        val defaultCollection = collections.data.firstOrNull {
                            it.is_favorited
                        }

                        if (defaultCollection != null) {
                            isFavorited = defaultCollection.is_favorited
                            favoriteCollectionId = defaultCollection.id
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ArticleScreen", "Failed to get favorite status", e)
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
            Box(
                modifier = Modifier
                    .wrapContentHeight(unbounded = true)
                    .onGloballyPositioned { coordinates ->
                        topBarHeight = coordinates.size.height
                    }
            ) {
                AnimatedVisibility(
                    visible = showTopBar,
                    enter = fadeIn() + expandVertically(
                        expandFrom = Alignment.Top,
                        initialHeight = { 0 }  // 更自然的展开动画
                    ) + slideInVertically { it / 2 },  // 添加滑动效果
                    exit = fadeOut() + shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        targetHeight = { 0 }
                    ) + slideOutVertically { it / 2 },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clickable { onNavigate(Question(questionId, title)) }
                    )
                }
            }
        },
        bottomBar = {
            if (backStackEntry.hasRoute(Article::class)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            val newState = if (voteUpState == VoteUpState.Up) VoteUpState.Neutral else VoteUpState.Up

                            coroutineScope.launch {
                                try {
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

                    IconButton(
                        onClick = { toggleFavorite() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isFavorited) Color(0xFFF57C00) else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (isFavorited) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            if (isFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, 
                            contentDescription = "收藏"
                        )
                    }

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

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = if (article.type == "answer") {
                                "https://www.zhihu.com/question/${questionId}/answer/${article.id}\n【$title - $authorName 的回答】"
                            } else if (article.type == "article") {
                                "https://zhuanlan.zhihu.com/p/${article.id}\n【$title - $authorName 的文章】"
                            } else {
                                "暂不支持的链接类型"
                            }
                            clipboard.setPrimaryClip(ClipData.newPlainText("Link", text))
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
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

            if (content.isNotEmpty()) {
                WebviewComp(httpClient) {
                    it.settings.javaScriptEnabled = true
                    it.loadZhihu(
                        "https://www.zhihu.com/${article.type}/${article.id}",
                        Jsoup.parse(content).apply {
                            select("noscript").forEach {
                                if (it.childrenSize() > 0) {
                                    // 把 img 标签移出 noscript
                                    it.after(it.child(0))
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    CommentScreenComponent(
        showComments = showComments,
        onDismiss = { showComments = false },
        httpClient = httpClient,
        content = article
    )
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
