@file:Suppress("FunctionName", "PropertyName", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.github.zly2006.zhihu.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.internal.toClipEntry
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalSelectionRegistrar
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionContainer
import com.github.zly2006.zhihu.ui.hack.SelectionContainer as MySelectionContainer
import androidx.compose.foundation.text.selection.SelectionManager
import androidx.compose.foundation.text.selection.SelectionRegistrarImpl
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.MainActivity.TtsState
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.markdown.MarkdownRenderContext
import com.github.zly2006.zhihu.markdown.Render
import com.github.zly2006.zhihu.markdown.htmlToMdAst
import com.github.zly2006.zhihu.ui.components.CollectionDialogComponent
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.ExportDialogComponent
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.OpenInBrowser
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel.Paging
import kotlinx.coroutines.delay
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
    val isFavorited: Boolean = false,
    val type: String = "collection",
    val title: String = "",
    val isPublic: Boolean = false,
    val url: String = "",
    val description: String = "",
    val followerCount: Int = 0,
    val answerCount: Int = 0,
    val itemCount: Int = 0,
    val likeCount: Int = 0,
    val viewCount: Int = 0,
    val commentCount: Int = 0,
    val isFollowing: Boolean = false,
    val isLiking: Boolean = false,
    val createdTime: Long = 0L,
    val updatedTime: Long = 0L,
    val creator: Person? = null,
    val isDefault: Boolean = false,
)

@Serializable
data class CollectionResponse(
    val data: List<Collection>,
    val paging: Paging,
)

enum class VoteUpState(
    val key: String,
) {
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
    onExportRequest: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    @Composable
    fun MenuActionButton(
        icon: @Composable () -> Unit,
        text: String,
        enabled: Boolean = true,
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick: () -> Unit,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onClick() },
            shape = RoundedCornerShape(12.dp),
            color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(24.dp)) {
                    icon()
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                )
            }
        }
    }

    @Composable
    fun MenuActionButton(
        icon: ImageVector,
        text: String,
        enabled: Boolean = true,
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick: () -> Unit,
    ) {
        MenuActionButton(
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                )
            },
            text = text,
            enabled = enabled,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            onClick = onClick,
        )
    }

    AnimatedVisibility(
        visible = showMenu,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        // 背景遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismissRequest() },
        ) {
            // 菜单内容
            AnimatedVisibility(
                visible = showMenu,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300, easing = EaseOutCubic),
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300, easing = EaseInCubic),
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
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
                            .padding(16.dp),
                    ) {
                        // 顶部拖拽指示器
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        RoundedCornerShape(2.dp),
                                    ),
                            )
                        }

                        val ttsState = (context as? MainActivity)?.ttsState ?: TtsState.Uninitialized
                        MenuActionButton(
                            icon = {
                                when (ttsState) {
                                    TtsState.Initializing, TtsState.Uninitialized -> CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )

                                    else -> Icon(
                                        if (ttsState.isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            text = if (ttsState.isSpeaking) "停止朗读" else "开始朗读",
                            enabled = ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing),
                            onClick = {
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
                                                Toast
                                                    .makeText(context, "朗读失败：${e.message}", Toast.LENGTH_SHORT)
                                                    .show()
                                            }
                                        }
                                    }
                                }
                            },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 分享按钮
                        MenuActionButton(
                            icon = Icons.Filled.Share,
                            text = "分享",
                            onClick = {
                                onDismissRequest()
                                val text = when (article.type) {
                                    ArticleType.Answer -> {
                                        "https://www.zhihu.com/question/${viewModel.questionId}/answer/${article.id}\n【${viewModel.title} - ${viewModel.authorName} 的回答】"
                                    }
                                    ArticleType.Article -> {
                                        "https://zhuanlan.zhihu.com/p/${article.id}\n【${viewModel.title} - ${viewModel.authorName} 的文章】"
                                    }
                                }
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                val chooserIntent = Intent.createChooser(shareIntent, "分享到")
                                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooserIntent)
                            },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 复制链接按钮
                        MenuActionButton(
                            icon = Icons.Filled.ContentCopy,
                            text = "复制链接",
                            onClick = {
                                onDismissRequest()
                                val text = when (article.type) {
                                    ArticleType.Answer -> {
                                        "https://www.zhihu.com/question/${viewModel.questionId}/answer/${article.id}\n【${viewModel.title} - ${viewModel.authorName} 的回答】"
                                    }
                                    ArticleType.Article -> {
                                        "https://zhuanlan.zhihu.com/p/${article.id}\n【${viewModel.title} - ${viewModel.authorName} 的文章】"
                                    }
                                }
                                (context as? MainActivity)?.sharedData?.clipboardDestination = article
                                context.clipboardManager.setPrimaryClip(ClipData.newPlainText("Link", text))
                                Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                            },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 复制 Markdown 按钮
                        MenuActionButton(
                            icon = Icons.Filled.ContentCopy,
                            text = "复制 Markdown",
                            onClick = {
                                onDismissRequest()
                                viewModel.exportToClipboard(context)
                                Toast.makeText(context, "已复制 Markdown", Toast.LENGTH_SHORT).show()
                            },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 导出按钮
                        MenuActionButton(
                            icon = Icons.Filled.GetApp,
                            text = "导出文章 (此功能目前由 AI 实现, bug 极多)",
                            onClick = {
                                onDismissRequest()
                                onExportRequest()
                            },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MenuActionButton(
                            icon = Icons.Outlined.DesktopWindows,
                            text = "在浏览器中打开",
                            onClick = {
                                coroutineScope.launch {
                                    OpenInBrowser.openUrlInBrowser(context, article)
                                    onDismissRequest()
                                    Toast.makeText(context, "已发送到浏览器", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )

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
    viewModel: ArticleViewModel,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
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
    var showActionsMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AccountData.addReadHistory(
            context,
            article.id.toString(),
            article.type.name.lowercase(),
        )
    }

    LaunchedEffect(scrollState.value) {
        val currentScroll = scrollState.value
        val scrollDelta = abs(currentScroll - previousScrollValue)
        if (scrollDelta > scrollDeltaThreshold) {
            isScrollingUp = currentScroll < previousScrollValue
            previousScrollValue = currentScroll
        }

        if (viewModel.rememberedScrollYSync) {
            viewModel.rememberedScrollY.value = currentScroll
        }
        if (currentScroll == viewModel.rememberedScrollY.value && scrollState.maxValue != Int.MAX_VALUE) {
            viewModel.rememberedScrollYSync = true
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
        viewModel.loadCollections(context)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
            )
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RectangleShape,
            ),
        topBar = {
            Box(
                modifier = Modifier
                    .wrapContentHeight(unbounded = true)
                    .onGloballyPositioned { coordinates ->
                        if (coordinates.size.height >= topBarHeight) {
                            topBarHeight = coordinates.size.height
                        }
                    }
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RectangleShape,
                    ),
            ) {
                AnimatedVisibility(
                    visible = showTopBar,
                    enter = fadeIn() + expandVertically(
                        expandFrom = Alignment.Top,
                        initialHeight = { 0 },
                    ) + slideInVertically { it / 2 },
                    exit = fadeOut() + shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        targetHeight = { 0 },
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
                            .let {
                                if (article.type == ArticleType.Answer) {
                                    it.clickable {
                                        navigator.onNavigate(Question(viewModel.questionId, viewModel.title))
                                    }
                                } else {
                                    it
                                }
                            },
                    )
                }
            }
        },
        bottomBar = {
            Column {
                if (backStackEntry?.hasRoute(Article::class) == true || context !is MainActivity) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .padding(horizontal = 0.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    color = Color(0xFF40B6F6),
                                ),
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            when (viewModel.voteUpState) {
                                VoteUpState.Neutral -> {
                                    Button(
                                        onClick = { viewModel.toggleVoteUp(context, VoteUpState.Up) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF40B6F6),
                                            contentColor = Color.Black,
                                        ),
                                        shape = RectangleShape,
                                        contentPadding = PaddingValues(horizontal = 0.dp),
                                    ) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Filled.ArrowUpward, "赞同")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = viewModel.voteUpCount.toString())
                                    }
                                    Button(
                                        onClick = { viewModel.toggleVoteUp(context, VoteUpState.Down) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF40B6F6),
                                            contentColor = Color.Black,
                                        ),
                                        shape = RectangleShape,
                                        modifier = Modifier
                                            .height(ButtonDefaults.MinHeight)
                                            .width(ButtonDefaults.MinHeight),
                                        contentPadding = PaddingValues(horizontal = 0.dp),
                                    ) {
                                        Icon(Icons.Filled.ArrowDownward, "反对")
                                    }
                                }

                                VoteUpState.Up -> {
                                    Button(
                                        onClick = { viewModel.toggleVoteUp(context, VoteUpState.Neutral) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0D47A1),
                                            contentColor = Color.White,
                                        ),
                                        shape = RectangleShape,
                                        contentPadding = PaddingValues(horizontal = 0.dp),
                                    ) {
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
                                    ) {
                                        Icon(Icons.Filled.ArrowDownward, "反对")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("反对")
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(
                                onClick = { showCollectionDialog = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (viewModel.isFavorited) Color(0xFFF57C00) else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (viewModel.isFavorited) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            ) {
                                Icon(
                                    if (viewModel.isFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = "收藏",
                                )
                            }

                            if ((context as? MainActivity)?.ttsState?.isSpeaking == true) {
                                IconButton(
                                    onClick = {
                                        context.stopSpeaking()
                                        Toast
                                            .makeText(
                                                context,
                                                "已停止朗读",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    },
                                    enabled = (
                                        context.ttsState !in listOf(
                                            TtsState.Error,
                                            TtsState.Uninitialized,
                                            TtsState.Initializing,
                                        )
                                    ),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color(0xFF4CAF50),
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeOff,
                                        contentDescription = "停止朗读",
                                    )
                                }
                            }

                            Button(
                                onClick = { showComments = true },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "${viewModel.commentCount}")
                            }

                            IconButton(
                                onClick = { showActionsMenu = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            ) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = "更多选项",
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                )
                .verticalScroll(scrollState),
        ) {
            Spacer(
                modifier = Modifier.height(
                    height = LocalDensity.current.run {
                        topBarHeight.toDp()
                    },
                ),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navigator.onNavigate(
                            com.github.zly2006.zhihu.Person(
                                id = viewModel.authorId,
                                urlToken = viewModel.authorUrlToken,
                                name = viewModel.authorName,
                            ),
                        )
                    },
            ) {
                if (viewModel.authorAvatarSrc.isNotEmpty()) {
                    AsyncImage(
                        model = viewModel.authorAvatarSrc,
                        contentDescription = "作者头像",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = viewModel.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    if (viewModel.authorBio.isNotEmpty()) {
                        Text(
                            text = viewModel.authorBio,
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                    }
                }
            }

            if (viewModel.content.isNotEmpty()) {
                if (preferences.getBoolean("articleUseWebview", true)) {
                    WebviewComp {
                        it.setupUpWebviewClient {
                            if (!viewModel.rememberedScrollYSync && viewModel.rememberedScrollY.value != null) {
                                coroutineScope.launch {
                                    val rememberedY = viewModel.rememberedScrollY.value ?: 0
                                    while (scrollState.maxValue < rememberedY) {
                                        delay(100)
                                    }
                                    Log.i("zhihu-scroll", "scroll to $rememberedY, max= ${scrollState.maxValue}, sync on")
                                    scrollState.animateScrollTo(rememberedY)
                                    viewModel.rememberedScrollYSync = true
                                }
                            }
                        }
                        it.contentId = article.id.toString()
                        it.loadZhihu(
                            "https://www.zhihu.com/${article.type}/${article.id}",
                            Jsoup.parse(viewModel.content).apply {
                                select("noscript").forEach { noscript ->
                                    /*
                                     * 已修复的图片异常:
                                     * https://www.zhihu.com/question/263764510/answer/273310677
                                     * https://www.zhihu.com/question/21725193/answer/1931362214
                                     * https://www.zhihu.com/question/419720398/answer/3155540572
                                     */
                                    noscript.nextSibling()?.let { actualImg ->
                                        if (actualImg.nodeName() == "img") {
                                            if (actualImg.attr("data-actualsrc").isNotEmpty()) {
                                                actualImg.attr("src", actualImg.attr("data-actualsrc"))
                                                actualImg.attr("class", actualImg.attr("class").replace("lazy", ""))
                                                noscript.remove()
                                                return@forEach
                                            }
                                        }
                                    }

                                    if (noscript.childrenSize() > 0) {
                                        val node = noscript.child(0)
                                        if (node.tagName() == "img") {
                                            if (node.attr("class").contains("content_image")) {
                                                // GIF 优化
                                                node.attr("src", node.attr("data-thumbnail"))
                                            }
                                            if (node.attr("src").isEmpty()) {
                                                if (node.attr("data-default-watermark-src").isNotEmpty()) {
                                                    node.attr("src", node.attr("data-default-watermark-src"))
                                                } else {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                        context.mainExecutor.execute {
                                                            Toast.makeText(context, "图片加载失败，请向开发者反馈", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        noscript.after(node)
                                    }
                                }
                            },
                        )
                    }
                } else {
                    val astNode = remember(viewModel.content) {
                        htmlToMdAst(viewModel.content)
                    }
                    val context = MarkdownRenderContext()
                    Spacer(Modifier.height(10.dp))

                    val view = LocalView.current
                    SelectionContainer(
                        modifier = Modifier
                            .fuckHonorService()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        val clipData = ClipData.newPlainText(
                                            "Selected Text",
                                            "fuck u google",
                                        )
                                        view.startDragAndDrop(
                                            clipData,
                                            View.DragShadowBuilder(view),
                                            null,
                                            View.DRAG_FLAG_GLOBAL,
                                        )
                                    },
                                )
                            },
                    ) {
                        Column {
                            for (ast in astNode) {
                                ast.Render(context)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
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
                if (viewModel.ipInfo != null) {
                    Text(
                        "IP属地：${viewModel.ipInfo}",
                        color = Color.Gray,
                        fontSize = 11.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height((16 + 36).dp))
        }
    }

    if (article.type == ArticleType.Answer && buttonSkipAnswer) {
        var navigatingToNextAnswer by remember { mutableStateOf(false) }
        DraggableRefreshButton(
            onClick = {
                navigatingToNextAnswer = true
                coroutineScope.launch {
                    val dest = viewModel.nextAnswerFuture.await()
                    if (dest == null) {
                        Toast.makeText(context, "没有更多回答了", Toast.LENGTH_SHORT).show()
                        navigatingToNextAnswer = false
                        return@launch
                    }
                    val activity = context as? MainActivity ?: return@launch
                    val target = dest.target!!
                    if (target is Feed.AnswerTarget && target.question.id == viewModel.questionId) {
                        if (activity.navController.currentBackStackEntry.hasRoute(Article::class) &&
                            activity.navController.currentBackStackEntry
                                ?.toRoute<Article>()
                                ?.type == ArticleType.Answer
                        ) {
                            activity.navController.popBackStack()
                        }
                        navigator.onNavigate(target.navDestination)
                    }
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
        onDismissRequest = { showActionsMenu = false },
        onExportRequest = { showExportDialog = true },
    )

    BackHandler(showActionsMenu) {
        showActionsMenu = false
    }

    // 使用新的收藏夹对话框组件
    CollectionDialogComponent(
        showDialog = showCollectionDialog,
        onDismiss = { showCollectionDialog = false },
        viewModel = viewModel,
        context = context,
    )

    viewModel.httpClient?.let {
        CommentScreenComponent(
            showComments = showComments,
            onDismiss = { showComments = false },
            httpClient = it,
            content = article,
        )
    }

    // 导出对话框
    ExportDialogComponent(
        showDialog = showExportDialog,
        onDismiss = { showExportDialog = false },
        viewModel = viewModel,
    )
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
        ),
        viewModel = viewModel {
            ArticleViewModel(
                Article(
                    "如何看待《狂暴之翼》中的人物设定？",
                    ArticleType.Answer,
                    123456789,
                    "知乎用户",
                    "知乎用户",
                    "",
                ),
                null,
                null,
            )
        },
    )
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
                viewModel = viewModel {
                    ArticleViewModel(
                        Article(
                            "如何看待《狂暴之翼》中的人物设定？",
                            ArticleType.Answer,
                            123456789,
                            "知乎用户",
                            "知乎用户",
                            "",
                        ),
                        null,
                        null,
                    )
                },
                context = LocalContext.current,
                showMenu = true,
                onDismissRequest = {},
                onExportRequest = {},
            )
        }
    }
}
