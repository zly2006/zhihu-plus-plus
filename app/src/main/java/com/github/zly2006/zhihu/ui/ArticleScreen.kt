@file:Suppress("FunctionName", "PropertyName")

package com.github.zly2006.zhihu.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.*
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.loadZhihu
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel.Paging
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
    val is_auto_send_moments: Boolean,
)

@Serializable
data class Collection(
    val id: String,
    val is_favorited: Boolean = false,
    val type: String = "collection",
    val title: String = "",
    val is_public: Boolean = false,
    val url: String = "",
    val description: String = "",
    val follower_count: Int = 0,
    val answer_count: Int = 0,
    val item_count: Int = 0,
    val like_count: Int = 0,
    val view_count: Int = 0,
    val comment_count: Int = 0,
    val is_following: Boolean = false,
    val is_liking: Boolean = false,
    val created_time: Long = 0L,
    val updated_time: Long = 0L,
    val creator: Person? = null,
    val is_default: Boolean = false,
)

@Serializable
data class CollectionResponse(
    val data: List<Collection>,
    val paging: Paging,
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
    val context = LocalContext.current
    val viewModel: ArticleViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        ArticleViewModel(article, (context as? MainActivity)?.httpClient)
    }
    val backStackEntry by (context as? MainActivity)?.navController?.currentBackStackEntryAsState()
        ?: remember { mutableStateOf(null) }

    val scrollState = rememberScrollState()
    val preferences = LocalContext.current.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    val isTitleAutoHide by remember { mutableStateOf(preferences.getBoolean("titleAutoHide", false)) }
    var previousScrollValue by remember { mutableStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val scrollDeltaThreshold = with(density) { ScrollThresholdDp.toPx() }
    var topBarHeight by remember { mutableStateOf(0) }
    var showComments by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.value) {
        val currentScroll = scrollState.value
        val scrollDelta = abs(currentScroll - previousScrollValue)
        if (scrollDelta > scrollDeltaThreshold) {
            isScrollingUp = currentScroll < previousScrollValue
            previousScrollValue = currentScroll
        }
    }

    val showTopBar by remember {
        derivedStateOf {
            val canScroll = scrollState.maxValue > topBarHeight
            val isNearTop = scrollState.value < topBarHeight
            when {
                !isTitleAutoHide -> true
                !canScroll -> true
                isScrollingUp -> true
                isNearTop -> true
                else -> false
            }
        }
    }

    LaunchedEffect(article.id) {
        viewModel.loadArticle(context)
        viewModel.loadCollections()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
                top = 0.dp,
            ),
        topBar = {
            Box(
                modifier = Modifier
                    .wrapContentHeight(unbounded = true)
                    .onGloballyPositioned { coordinates ->
                        if (coordinates.size.height >= topBarHeight)
                            topBarHeight = coordinates.size.height
                    }
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RectangleShape
                    )
            ) {
                AnimatedVisibility(
                    visible = showTopBar,
                    enter = fadeIn() + expandVertically(
                        expandFrom = Alignment.Top,
                        initialHeight = { 0 }
                    ) + slideInVertically { it / 2 },
                    exit = fadeOut() + shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        targetHeight = { 0 }
                    ) + slideOutVertically { it / 2 },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = viewModel.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clickable { onNavigate(Question(viewModel.questionId, viewModel.title)) }
                    )
                }
            }
        },
        bottomBar = {
            if (backStackEntry?.hasRoute(Article::class) == true) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { viewModel.toggleVoteUp(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                if (viewModel.voteUpState == VoteUpState.Up) Color(0xFF0D47A1)
                                else Color(0xFF40B6F6),
                            contentColor =
                                if (viewModel.voteUpState == VoteUpState.Up) Color.White
                                else Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(
                            if (viewModel.voteUpState == VoteUpState.Up) Icons.Filled.ThumbUp
                            else Icons.Outlined.ThumbUp,
                            contentDescription = "赞同"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (viewModel.voteUpState == VoteUpState.Up) "已赞 ${viewModel.voteUpCount}" else "赞同 ${viewModel.voteUpCount}")
                    }

                    IconButton(
                        onClick = { showCollectionDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (viewModel.isFavorited) Color(0xFFF57C00) else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (viewModel.isFavorited) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            if (viewModel.isFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
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
                        Text(text = "${viewModel.commentCount}")
                    }

                    Button(
                        onClick = {
                            val clipboard = (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                            val text = when (article.type) {
                                "answer" -> {
                                    "https://www.zhihu.com/question/${viewModel.questionId}/answer/${article.id}\n【${viewModel.title} - ${viewModel.authorName} 的回答】"
                                }

                                "article" -> {
                                    "https://zhuanlan.zhihu.com/p/${article.id}\n【${viewModel.title} - ${viewModel.authorName} 的文章】"
                                }

                                else -> {
                                    "暂不支持的链接类型"
                                }
                            }
                            (context as? MainActivity)?.sharedData?.clipboardDestination = article
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Link", text))
                            Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
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
            modifier = Modifier.padding(
                start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                bottom = innerPadding.calculateBottomPadding(),
            ).verticalScroll(scrollState),
        ) {
            Spacer(
                modifier = Modifier.height(
                    height = LocalDensity.current.run {
                        topBarHeight.toDp()
                    }
                )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.authorAvatarSrc.isNotEmpty()) {
                    AsyncImage(
                        model = viewModel.authorAvatarSrc,
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
                        text = viewModel.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = viewModel.authorBio,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            if (viewModel.content.isNotEmpty()) {
                WebviewComp {
                    it.settings.javaScriptEnabled = true
                    it.loadZhihu(
                        "https://www.zhihu.com/${article.type}/${article.id}",
                        Jsoup.parse(viewModel.content).apply {
                            select("noscript").forEach { noscript ->
                                if (noscript.childrenSize() > 0) {
                                    val node = noscript.child(0)
                                    if (node.tagName() == "img") {
                                        if (node.attr("class").contains("content_image")) {
                                            node.attr("src", node.attr("data-thumbnail"))
                                        }
                                    }
                                    noscript.after(node)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showCollectionDialog
    ) {
        AlertDialog(
            onDismissRequest = { showCollectionDialog = false },
            title = { Text("选择收藏夹") },
            text = {
                Column {
                    viewModel.collections.forEach { collection ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleFavorite(collection.id, collection.is_favorited, context)
                                    viewModel.loadCollections()
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = collection.title,
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp
                            )
                            if (collection.is_favorited) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = "已收藏",
                                    tint = Color(0xFFF57C00)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCollectionDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    viewModel.httpClient?.let {
        CommentScreenComponent(
            showComments = showComments,
            onDismiss = { showComments = false },
            httpClient = it,
            content = article
        )
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
