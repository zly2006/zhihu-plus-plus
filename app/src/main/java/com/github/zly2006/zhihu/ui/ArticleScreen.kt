@file:Suppress("FunctionName", "PropertyName")

package com.github.zly2006.zhihu.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.*
import com.github.zly2006.zhihu.MainActivity.TtsState
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.loadZhihu
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel.Paging
import kotlinx.coroutines.launch
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
fun ArticleActionsMenu(
    article: Article,
    viewModel: ArticleViewModel,
    context: Context,
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
) {
    AnimatedVisibility(
        visible = showMenu,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        // 背景遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismissRequest() }
        ) {
            // 菜单内容
            AnimatedVisibility(
                visible = showMenu,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300, easing = EaseOutCubic)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300, easing = EaseInCubic)
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) { /* 阻止点击穿透 */ },
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // 顶部拖拽指示器
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }

                        val ttsState = (context as? MainActivity)?.ttsState ?: TtsState.Uninitialized
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)
                                ) {
                                    onDismissRequest()
                                    val mainActivity = context as? MainActivity
                                    if (ttsState.isSpeaking) {
                                        mainActivity?.stopSpeaking()
                                    } else if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                                        // 使用协程在后台处理文本提取，避免UI阻塞
                                        viewModel.viewModelScope.launch {
                                            try {
                                                // 在IO线程中处理文本提取
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    val textToRead = buildString {
                                                        append(viewModel.title)
                                                        append("。")
                                                        if (viewModel.content.isNotEmpty()) {
                                                            // 从HTML内容中提取纯文本，限制处理的内容长度
                                                            val contentToProcess =
                                                                if (viewModel.content.length > 50000) {
                                                                    viewModel.content.substring(0, 50000) + "..."
                                                                } else {
                                                                    viewModel.content
                                                                }
                                                            val plainText = Jsoup.parse(contentToProcess).text()
                                                            append(plainText)
                                                        }
                                                    }

                                                    // 回到主线程执行TTS
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        if (textToRead.isNotBlank()) {
                                                            mainActivity?.speakText(textToRead, viewModel.title)
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    Toast.makeText(context, "朗读失败：${e.message}", Toast.LENGTH_SHORT)
                                                        .show()
                                                }
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing))
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (ttsState) {
                                    TtsState.Initializing, TtsState.Uninitialized -> CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                    else -> Icon(
                                        if (ttsState.isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing))
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = if (ttsState.isSpeaking) "停止朗读" else "开始朗读",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing))
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 分享按钮
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismissRequest()
                                    val clipboard =
                                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                                    val text = when (article.type) {
                                        ArticleType.Answer -> "https://www.zhihu.com/question/${viewModel.questionId}/answer/${article.id}\n【${viewModel.title} - ${viewModel.authorName} 的回答】"
                                        ArticleType.Article -> "https://zhuanlan.zhihu.com/p/${article.id}\n【${viewModel.title} - ${viewModel.authorName} 的文章】"
                                    }
                                    (context as? MainActivity)?.sharedData?.clipboardDestination = article
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Link", text))
                                    Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "复制链接",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 底部安全区域
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
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
    val buttonSkipAnswer by remember { mutableStateOf(preferences.getBoolean("buttonSkipAnswer", true)) }
    var previousScrollValue by remember { mutableIntStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val scrollDeltaThreshold = with(density) { ScrollThresholdDp.toPx() }
    var topBarHeight by remember { mutableIntStateOf(0) }
    var showComments by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    // 下拉菜单按钮 - 包含朗读和分享功能
    var showActionsMenu by remember { mutableStateOf(false) }

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
            if (backStackEntry?.hasRoute(Article::class) == true || context !is MainActivity) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50)),
                        horizontalArrangement = Arrangement.Start
                    )
                    {
                        when (viewModel.voteUpState) {
                            VoteUpState.Neutral -> {
                                Button(
                                    onClick = { viewModel.toggleVoteUp(context, VoteUpState.Up) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF40B6F6),
                                        contentColor = Color.Black
                                    ),
                                    shape = RectangleShape,
                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                )
                                {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Filled.ArrowUpward, "赞同")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = viewModel.voteUpCount.toString())
                                }
                                Button(
                                    onClick = { viewModel.toggleVoteUp(context, VoteUpState.Down) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF40B6F6),
                                        contentColor = Color.Black
                                    ),
                                    shape = RectangleShape,
                                    modifier = Modifier.height(ButtonDefaults.MinHeight).width(ButtonDefaults.MinHeight),
                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                )
                                {
                                    Icon(Icons.Filled.ArrowDownward, "反对")
                                }
                            }
                            VoteUpState.Up -> {
                                Button(
                                    onClick = { viewModel.toggleVoteUp(context, VoteUpState.Neutral) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0D47A1),
                                        contentColor = Color.White
                                    ),
                                    shape = RectangleShape,
                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                )
                                {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Filled.ArrowUpward, "赞同")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = viewModel.voteUpCount.toString())
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                            VoteUpState.Down -> {
                                Button(
                                    onClick = { viewModel.toggleVoteUp(context, VoteUpState.Neutral) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0D47A1),
                                        contentColor = Color.White,
                                    ),
                                    shape = RectangleShape,
                                    modifier = Modifier.height(ButtonDefaults.MinHeight),
                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                )
                                {
                                    Icon(Icons.Filled.ArrowDownward, "反对")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("反对")
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.End
                    ) {
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

                        if ((context as? MainActivity)?.ttsState?.isSpeaking == true) {
                            IconButton(
                                onClick = {
                                    context.stopSpeaking()
                                    Toast.makeText(
                                        context,
                                        "已停止朗读",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                enabled = (context.ttsState !in listOf(
                                    TtsState.Error,
                                    TtsState.Uninitialized,
                                    TtsState.Initializing
                                )),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color(0xFF4CAF50),
                                    contentColor = Color.White,
                                )
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "停止朗读"
                                )
                            }
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

                        IconButton(
                            onClick = { showActionsMenu = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "更多选项"
                            )
                        }
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
                    .clickable {
                        onNavigate(
                            Person(
                                id = viewModel.authorId,
                                urlToken = viewModel.authorUrlToken,
                                name = viewModel.authorName
                            )
                        )
                    }
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
                    @SuppressLint("SetJavaScriptEnabled")
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    "发布于 " + YMDHMS.format(viewModel.createdAt * 1000),
                    color = Color.Gray,
                    fontSize = 11.sp,
                )
                if (viewModel.createdAt != viewModel.updatedAt) {
                    Text(
                        "编辑于 " + YMDHMS.format(viewModel.updatedAt * 1000),
                        color = Color.Gray,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }

    if (article.type == ArticleType.Answer && buttonSkipAnswer) {
        var navigatingToNextAnswer by remember { mutableStateOf(false) }
        DraggableRefreshButton(
            onClick = {
                navigatingToNextAnswer = true
                viewModel.viewModelScope.launch {
                    val dest = viewModel.nextAnswerFuture.await()
                    val activity = context as? MainActivity ?: return@launch
                    if (activity.navController.currentBackStackEntry.hasRoute(Article::class)) {
                        activity.navController.popBackStack()
                    }
                    onNavigate(dest.target!!.navDestination!!)
                }
            },
            preferenceName = "buttonSkipAnswer",
        ) {
            if (navigatingToNextAnswer) {
                CircularProgressIndicator(modifier = Modifier.size(30.dp))
            } else {
                Icon(Icons.Default.SkipNext, contentDescription = "下一个回答")
            }
        }
    }

    // 全屏菜单
    ArticleActionsMenu(
        article = article,
        viewModel = viewModel,
        context = context,
        showMenu = showActionsMenu,
        onDismissRequest = { showActionsMenu = false }
    )

    BackHandler(showActionsMenu) {
        showActionsMenu = false
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
            onNavigate = onNavigate,
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
            ArticleType.Answer,
            123456789,
            "知乎用户",
            "知乎用户",
            "",
        )
    ) {}
}

@Preview
@Composable
fun ArticleActionsMenuPreview() {
    MaterialTheme {
        Surface {
            ArticleActionsMenu(
                article = Article(
                    "如何看待《狂暴之翼》中的人物设定？",
                    ArticleType.Answer,
                    123456789,
                    "知乎用户",
                    "知乎用户",
                    "",
                ),
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                    ArticleViewModel(
                        Article(
                            "如何看待《狂暴之翼》中的人物设定？",
                            ArticleType.Answer,
                            123456789,
                            "知乎用户",
                            "知乎用户",
                            "",
                        ),
                        null
                    )
                },
                context = LocalContext.current,
                showMenu = true,
                onDismissRequest = {}
            )
        }
    }
}
