package com.github.zly2006.zhihu.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.markdown.MarkdownRenderContext
import com.github.zly2006.zhihu.markdown.Render
import com.github.zly2006.zhihu.markdown.htmlToMdAst
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.ui.components.AnswerHorizontalOverscroll
import com.github.zly2006.zhihu.ui.components.AnswerVerticalOverscroll
import com.github.zly2006.zhihu.ui.components.CollectionDialogComponent
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.CustomWebView
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.ExportDialogComponent
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.OpenInBrowser
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel.Paging
import com.materialkolor.ktx.harmonize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.math.abs
import kotlin.math.max

private const val SCROLL_THRESHOLD = 10 // 滑动阈值，单位为dp
private val ScrollThresholdDp = SCROLL_THRESHOLD.dp

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

private val VoteUpNeutralContent = Color(0xFF3671EE)
private val VoteUpNeutralContentDark = Color(0xFF628DF7)

@Composable
fun voteUpNeutralContent() = if (ThemeManager.isDarkTheme()) VoteUpNeutralContentDark else VoteUpNeutralContent

@Composable
fun voteUpNeutralContentDuo3() = if (ThemeManager.isDarkTheme()) {
    VoteUpNeutralContentDark.harmonize(MaterialTheme.colorScheme.primary)
} else {
    VoteUpNeutralContent.harmonize(MaterialTheme.colorScheme.primary)
}

@Composable
fun voteUpActiveButtonColors() = ButtonDefaults.buttonColors(
    containerColor = voteUpNeutralContent(),
    contentColor = Color.White,
)

@Composable
fun voteUpNeutralButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
)

@OptIn(ExperimentalMaterial3Api::class)
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

    @Composable
    fun Content() {
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
            text = "在电脑中打开（我计划使用浏览器插件实现，还在写，点击后请手动前往收藏夹打开）",
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

    if (showMenu) {
        MyModalBottomSheet(onDismissRequest) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Content()
            }
        }
    }
}

/**
 * 修复 noscript 标签中的图片加载问题。
 * 提取为独立函数，确保主 WebView 和预览 WebView 使用相同的文档处理。
 */
private fun prepareContentDocument(content: String, context: Context): Document =
    Jsoup.parse(content).apply {
        select("noscript").forEach { noscript ->
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
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArticleScreen(
    article: Article,
    viewModel: ArticleViewModel,
    // 仅用于master 分支
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val backStackEntry by (context as? MainActivity)?.navController?.currentBackStackEntryAsState()
        ?: remember { mutableStateOf(null) }

    val scrollState = rememberScrollState()
    val preferences = LocalContext.current.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    var isTitleAutoHide by remember { mutableStateOf(preferences.getBoolean("titleAutoHide", false)) }
    var autoHideArticleBottomBar by remember {
        mutableStateOf(preferences.getBoolean("autoHideArticleBottomBar", false))
    }
    var answerSwitchMode by remember {
        mutableStateOf(preferences.getString("answerSwitchMode", "vertical") ?: "vertical")
    }
    var pinAnswerDate by remember { mutableStateOf(preferences.getBoolean("pinAnswerDate", false)) }
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

    val useDuo3ArticleBar = remember { preferences.getBoolean("duo3_article_bar", false) }
    val useDuo3ArticleActions = remember { preferences.getBoolean("duo3_article_actions", false) }
    var buttonSkipAnswer by remember { mutableStateOf(preferences.getBoolean("buttonSkipAnswer", true)) }
    var autoHideSkipAnswerButton by remember { mutableStateOf(preferences.getBoolean("autoHideSkipAnswerButton", true)) }

    // Follow-the-finger bar hide: pixel-based offsets driven by scroll delta
    val topBarOffset = remember { Animatable(0f) }
    val bottomBarOffset = remember { Animatable(0f) }
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    var bottomBarHeightPx by remember { mutableFloatStateOf(0f) }
    var previousScrollForBarOffset by remember { mutableIntStateOf(0) }
    var isBarSnapping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AccountData.addReadHistory(
            context,
            article.id.toString(),
            article.type.name.lowercase(),
        )
    }

    val preferenceListener = remember(preferences) {
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "titleAutoHide" -> isTitleAutoHide = preferences.getBoolean(key, false)
                "autoHideArticleBottomBar" -> {
                    autoHideArticleBottomBar = preferences.getBoolean(key, false)
                }

                "buttonSkipAnswer" -> buttonSkipAnswer = preferences.getBoolean(key, true)
                "autoHideSkipAnswerButton" -> autoHideSkipAnswerButton = preferences.getBoolean(key, true)

                "answerSwitchMode" -> {
                    answerSwitchMode = preferences.getString(key, "vertical") ?: "vertical"
                }

                "pinAnswerDate" -> pinAnswerDate = preferences.getBoolean(key, false)
            }
        }
    }

    DisposableEffect(preferences, preferenceListener) {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        }
    }

    // Reset bar offsets when auto-hide preferences are turned off
    LaunchedEffect(isTitleAutoHide) {
        if (!isTitleAutoHide) topBarOffset.snapTo(0f)
    }
    LaunchedEffect(autoHideArticleBottomBar) {
        if (!autoHideArticleBottomBar) bottomBarOffset.snapTo(0f)
    }

    LaunchedEffect(scrollState.value) {
        val currentScroll = scrollState.value
        val scrollDeltaAbs = abs(currentScroll - previousScrollValue)
        if (scrollDeltaAbs > scrollDeltaThreshold) {
            isScrollingUp = currentScroll < previousScrollValue
            previousScrollValue = currentScroll
        }

        // Skip bar offset tracking during snap animation (scroll is driven programmatically)
        if (useDuo3ArticleBar && !isBarSnapping) {
            val delta = currentScroll - previousScrollForBarOffset
            val atTop = currentScroll == 0
            val atBottom = currentScroll >= scrollState.maxValue

            // Top bar: force show at top; content-like reveal at bottom
            if (atTop) {
                topBarOffset.snapTo(0f)
            } else if (isTitleAutoHide && topBarHeightPx > 0f) {
                val deltaBasedOffset = (topBarOffset.value - delta).coerceIn(-topBarHeightPx, 0f)
                val distanceFromBottom = (scrollState.maxValue - currentScroll).coerceAtLeast(0)
                if (distanceFromBottom < topBarHeightPx.toInt()) {
                    // Bottom region: use whichever shows MORE of the bar (closer to 0)
                    val distanceBasedOffset = (-distanceFromBottom.toFloat()).coerceIn(-topBarHeightPx, 0f)
                    topBarOffset.snapTo(maxOf(distanceBasedOffset, deltaBasedOffset))
                } else {
                    topBarOffset.snapTo(deltaBasedOffset)
                }
            }

            // Bottom bar: force show at top; content-like reveal at bottom
            if (atTop) {
                bottomBarOffset.snapTo(0f)
            } else if (autoHideArticleBottomBar && bottomBarHeightPx > 0f) {
                val deltaBasedOffset = (bottomBarOffset.value + delta).coerceIn(0f, bottomBarHeightPx)
                val distanceFromBottom = (scrollState.maxValue - currentScroll).coerceAtLeast(0)
                if (distanceFromBottom < bottomBarHeightPx.toInt()) {
                    // Bottom region: use whichever shows MORE of the bar
                    val distanceBasedOffset = distanceFromBottom.toFloat().coerceIn(0f, bottomBarHeightPx)
                    bottomBarOffset.snapTo(minOf(distanceBasedOffset, deltaBasedOffset))
                } else {
                    bottomBarOffset.snapTo(deltaBasedOffset)
                }
            }
        }
        previousScrollForBarOffset = currentScroll

        if (viewModel.rememberedScrollYSync) {
            viewModel.rememberedScrollY.value = currentScroll
        }
        if (currentScroll == viewModel.rememberedScrollY.value && scrollState.maxValue != Int.MAX_VALUE) {
            viewModel.rememberedScrollYSync = true
        }
    }

    // Snap bars to fully visible or fully hidden when scrolling stops,
    // and animate content scroll to follow the snap
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!useDuo3ArticleBar) return@LaunchedEffect
        if (!scrollState.isScrollInProgress) {
            val topTarget = if (isTitleAutoHide && topBarHeightPx > 0f) {
                if (abs(topBarOffset.value) > topBarHeightPx / 2) -topBarHeightPx else 0f
            } else {
                topBarOffset.value
            }

            val bottomTarget = if (autoHideArticleBottomBar && bottomBarHeightPx > 0f) {
                if (bottomBarOffset.value > bottomBarHeightPx / 2) bottomBarHeightPx else 0f
            } else {
                bottomBarOffset.value
            }

            // Only compensate scroll for the top bar near the top
            // Bottom bar: no scroll compensation (distance-based reveal handles it)
            val topInNaturalArea = scrollState.value <= topBarHeightPx
            val topDelta = if (topInNaturalArea) topBarOffset.value - topTarget else 0f
            val scrollCompensation = topDelta

            if (topTarget != topBarOffset.value || bottomTarget != bottomBarOffset.value) {
                try {
                    isBarSnapping = true
                    kotlinx.coroutines.coroutineScope {
                        launch { topBarOffset.animateTo(topTarget, tween(150)) }
                        launch { bottomBarOffset.animateTo(bottomTarget, tween(150)) }
                        if (scrollCompensation != 0f) {
                            launch { scrollState.animateScrollBy(scrollCompensation, tween(150)) }
                        }
                    }
                } finally {
                    isBarSnapping = false
                }
            }
        }
    }

    LaunchedEffect(article.id) {
        viewModel.loadArticle(context)
        viewModel.loadCollections(context)
    }

    // Master-style bar visibility (direction-based, used when useDuo3ArticleBar is false)
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
    val showBottomBar by remember {
        derivedStateOf {
            val canScroll = scrollState.maxValue > 0
            val isNearTop = scrollState.value == 0
            when {
                !autoHideArticleBottomBar -> true
                !canScroll -> true
                isScrollingUp -> true
                isNearTop -> true
                else -> false
            }
        }
    }

    // 回答切换手势系统
    val sharedData = if (context is MainActivity && article.type == ArticleType.Answer) {
        val sd by context.viewModels<ArticleViewModel.ArticlesSharedData>()
        sd
    } else {
        null
    }

    LaunchedEffect(article.id) {
        // Bug 2: 在主线程检查标志并重置（避免跨线程可见性问题）
        if (sharedData != null) {
            if (!sharedData.navigatingFromAnswerSwitch) {
                sharedData.reset()
            }
            sharedData.navigatingFromAnswerSwitch = false
            sharedData.answerTransitionDirection = ArticleViewModel.AnswerTransitionDirection.DEFAULT

            // 从 pendingInitialContent 预填充 viewModel，消除空白帧
            val pending = sharedData.pendingInitialContent
            if (pending != null) {
                viewModel.title = pending.title
                viewModel.authorName = pending.authorName
                viewModel.authorBio = pending.authorBio
                viewModel.authorAvatarSrc = pending.authorAvatarUrl
                viewModel.content = pending.content
                viewModel.voteUpCount = pending.voteUpCount
                viewModel.commentCount = pending.commentCount
                sharedData.pendingInitialContent = null
            }
        }
        viewModel.loadArticle(context)
        viewModel.loadCollections(context)
    }

    val navigateToPrevious: () -> Unit = {
        sharedData?.answerTransitionDirection = if (answerSwitchMode == "horizontal") {
            ArticleViewModel.AnswerTransitionDirection.HORIZONTAL_PREVIOUS
        } else {
            ArticleViewModel.AnswerTransitionDirection.VERTICAL_PREVIOUS
        }
        sharedData?.navigatingFromAnswerSwitch = true
        // 更新当前回答内容到历史
        sharedData?.navigator?.pushAnswer(viewModel.toCachedContent(sourceLabel = sharedData?.navigator?.sourceName ?: "此问题"))
        val prev = sharedData?.navigator?.goToPrevious()
        if (prev != null) {
            sharedData?.pendingInitialContent = prev
            sharedData?.promoteForNavigation(sharedData?.answerTransitionDirection ?: ArticleViewModel.AnswerTransitionDirection.DEFAULT)
            val activity = context as? MainActivity
            if (activity != null) {
                if (activity.navController.currentBackStackEntry.hasRoute(Article::class) &&
                    activity.navController.currentBackStackEntry
                        ?.toRoute<Article>()
                        ?.type == ArticleType.Answer
                ) {
                    activity.navController.popBackStack()
                }
                navigator.onNavigate(prev.article)
            }
        } else {
            // 无历史时尝试从来源（如收藏夹）向前加载
            sharedData?.pendingInitialContent = sharedData?.navigator?.previousAnswerPreview
            sharedData?.promoteForNavigation(sharedData?.answerTransitionDirection ?: ArticleViewModel.AnswerTransitionDirection.DEFAULT)
            coroutineScope.launch {
                val prevCached = sharedData?.navigator?.loadPrevious(context)
                if (prevCached != null) {
                    sharedData?.pendingInitialContent = prevCached
                    val activity = context as? MainActivity
                    if (activity != null) {
                        if (activity.navController.currentBackStackEntry.hasRoute(Article::class) &&
                            activity.navController.currentBackStackEntry
                                ?.toRoute<Article>()
                                ?.type == ArticleType.Answer
                        ) {
                            activity.navController.popBackStack()
                        }
                        navigator.onNavigate(prevCached.article)
                    }
                }
            }
        }
    }

    val navigateToNext: () -> Unit = {
        sharedData?.answerTransitionDirection = if (answerSwitchMode == "horizontal") {
            ArticleViewModel.AnswerTransitionDirection.HORIZONTAL_NEXT
        } else {
            ArticleViewModel.AnswerTransitionDirection.VERTICAL_NEXT
        }
        sharedData?.navigatingFromAnswerSwitch = true
        // 更新当前回答内容到历史
        sharedData?.navigator?.pushAnswer(viewModel.toCachedContent(sourceLabel = sharedData?.navigator?.sourceName ?: "此问题"))
        // 优先使用前向历史
        val historyNext = sharedData?.navigator?.goToNext()
        if (historyNext != null) {
            sharedData?.pendingInitialContent = historyNext
            sharedData?.promoteForNavigation(sharedData?.answerTransitionDirection ?: ArticleViewModel.AnswerTransitionDirection.DEFAULT)
            val activity = context as? MainActivity
            if (activity != null) {
                if (activity.navController.currentBackStackEntry.hasRoute(Article::class) &&
                    activity.navController.currentBackStackEntry
                        ?.toRoute<Article>()
                        ?.type == ArticleType.Answer
                ) {
                    activity.navController.popBackStack()
                }
                navigator.onNavigate(historyNext.article)
            }
        } else {
            // 没有前向历史，从导航器加载
            sharedData?.pendingInitialContent = sharedData?.navigator?.nextAnswer
            sharedData?.promoteForNavigation(sharedData?.answerTransitionDirection ?: ArticleViewModel.AnswerTransitionDirection.DEFAULT)
            coroutineScope.launch {
                val nextArticle = sharedData?.navigator?.loadNext(context)
                if (nextArticle != null) {
                    val activity = context as? MainActivity
                    if (activity != null) {
                        if (activity.navController.currentBackStackEntry.hasRoute(Article::class) &&
                            activity.navController.currentBackStackEntry
                                ?.toRoute<Article>()
                                ?.type == ArticleType.Answer
                        ) {
                            activity.navController.popBackStack()
                        }
                        navigator.onNavigate(nextArticle)
                    }
                }
            }
        }
    }

    val answerSwitchContentOld: @Composable () -> Unit = {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 16.dp,
                ).background(
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
                        }.background(
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
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = fadeIn() + expandVertically(
                                expandFrom = Alignment.Bottom,
                                initialHeight = { 0 },
                            ) + slideInVertically { it / 2 },
                            exit = fadeOut() + shrinkVertically(
                                shrinkTowards = Alignment.Bottom,
                                targetHeight = { 0 },
                            ) + slideOutVertically { it / 2 },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
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
                                            color = if (viewModel.voteUpState == VoteUpState.Neutral) {
                                                voteUpNeutralContent().copy(alpha = 0.1f)
                                            } else {
                                                voteUpNeutralContent()
                                            },
                                        ),
                                    horizontalArrangement = Arrangement.Start,
                                ) {
                                    when (viewModel.voteUpState) {
                                        VoteUpState.Neutral -> {
                                            Button(
                                                onClick = { viewModel.toggleVoteUp(context, VoteUpState.Up) },
                                                colors = voteUpNeutralButtonColors(),
                                                shape = RectangleShape,
                                                contentPadding = PaddingValues(horizontal = 0.dp),
                                            ) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(painterResource(R.drawable.ic_vote_up_24dp), "赞同")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = viewModel.voteUpCount.toString())
                                            }
                                            Button(
                                                onClick = { viewModel.toggleVoteUp(context, VoteUpState.Down) },
                                                colors = voteUpNeutralButtonColors(),
                                                shape = RectangleShape,
                                                modifier = Modifier
                                                    .height(ButtonDefaults.MinHeight)
                                                    .width(ButtonDefaults.MinHeight),
                                                contentPadding = PaddingValues(horizontal = 0.dp),
                                            ) {
                                                Icon(painterResource(R.drawable.ic_vote_down_24dp), "反对")
                                            }
                                        }

                                        VoteUpState.Up -> {
                                            Button(
                                                onClick = { viewModel.toggleVoteUp(context, VoteUpState.Neutral) },
                                                colors = voteUpActiveButtonColors(),
                                                shape = RectangleShape,
                                                contentPadding = PaddingValues(horizontal = 0.dp),
                                            ) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(painterResource(R.drawable.ic_vote_up_24dp), "赞同")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = viewModel.voteUpCount.toString())
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                        }

                                        VoteUpState.Down -> {
                                            Button(
                                                onClick = { viewModel.toggleVoteUp(context, VoteUpState.Neutral) },
                                                colors = voteUpActiveButtonColors(),
                                                shape = RectangleShape,
                                                modifier = Modifier.height(ButtonDefaults.MinHeight),
                                                contentPadding = PaddingValues(horizontal = 0.dp),
                                            ) {
                                                Icon(painterResource(R.drawable.ic_vote_down_24dp), "反对")
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
                    ).verticalScroll(scrollState),
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

                @Suppress("UnusedReceiverParameter") // 确保竖式布局
                @Composable
                fun ColumnScope.DateTexts() {
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
                if (pinAnswerDate) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        DateTexts()
                    }
                }

                if (viewModel.content.isNotEmpty()) {
                    if (preferences.getBoolean("articleUseWebview", true)) {
                        WebviewComp(
                            scrollState = scrollState,
//                            existingWebView = sharedData?.getOrCreateMainWebView(context),
                        ) {
                            it.isVerticalScrollBarEnabled = false
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
                                prepareContentDocument(viewModel.content, context).apply {
                                    title(viewModel.title)
                                },
                            )
                        }
                    } else {
                        val astNode = remember(viewModel.content) {
                            htmlToMdAst(viewModel.content)
                        }
                        val context = MarkdownRenderContext()
                        Spacer(Modifier.height(10.dp))
                        SelectionContainer(Modifier.fuckHonorService()) {
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
                    if (!pinAnswerDate) {
                        DateTexts()
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
    } // end answerSwitchContentOld

    @OptIn(ExperimentalMaterial3Api::class)
    val answerSwitchContent: @Composable () -> Unit = {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        // 不受到是否收起影响，在topbar最大时是否可以滚动？
        var scrollStateMaxValue by remember { mutableStateOf(0) }
        LaunchedEffect(scrollState.maxValue) {
            if (scrollState.maxValue != Int.MAX_VALUE) {
                scrollStateMaxValue = max(scrollState.maxValue, scrollStateMaxValue)
            }
        }
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                Box(
                    modifier = Modifier
                        .onSizeChanged {
                            topBarHeightPx = it.height.toFloat()
                            if (it.height >= 10) topBarHeight = it.height
                        }.let {
                            it.graphicsLayer {
                                translationY = topBarOffset.value
                                alpha = if (topBarHeightPx > 0f) 1f + (topBarOffset.value / topBarHeightPx) else 1f
                            }
                        },
                ) {
                    TwoRowsTopAppBar(
                        navigationIcon = {
                            IconButton(onClick = {
                                val activity = context as? MainActivity
                                activity?.navController?.popBackStack()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        },
                        actions = {
                            if (useDuo3ArticleActions) {
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
                        },
                        title = { expanded ->
                            Text(
                                text = viewModel.title,
                                modifier = Modifier
                                    .padding(if (expanded) PaddingValues(end = 16.dp) else PaddingValues())
                                    .let {
                                        if (article.type == ArticleType.Answer) {
                                            it.clickable {
                                                navigator.onNavigate(Question(viewModel.questionId, viewModel.title))
                                            }
                                        } else {
                                            it
                                        }
                                    },
                                maxLines = if (expanded) Int.MAX_VALUE else 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        subtitle = { expanded ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(if (expanded) PaddingValues(vertical = 16.dp) else PaddingValues(top = 2.dp, bottom = 8.dp))
                                    .clickable {
                                        navigator.onNavigate(
                                            com.github.zly2006.zhihu.Person(
                                                id = viewModel.authorId,
                                                urlToken = viewModel.authorUrlToken,
                                                name = viewModel.authorName,
                                            ),
                                        )
                                    }.padding(end = 16.dp),
                            ) {
                                if (viewModel.authorAvatarSrc.isNotEmpty()) {
                                    AsyncImage(
                                        model = viewModel.authorAvatarSrc,
                                        contentDescription = "作者头像",
                                        modifier = Modifier
                                            .size(if (expanded) 40.dp else 20.dp)
                                            .clip(CircleShape),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(if (expanded) 40.dp else 20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                    )
                                }

                                Spacer(modifier = Modifier.width(if (expanded) 8.dp else 4.dp))

                                Column {
                                    Text(
                                        text = viewModel.authorName,
                                        style = if (expanded) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                                        color = if (expanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (viewModel.authorBio.isNotEmpty() && expanded) {
                                        Text(
                                            text = viewModel.authorBio,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        },
                        scrollBehavior = if (scrollStateMaxValue > 0) scrollBehavior else null,
                    )
                }
            },
            bottomBar = {
                // 防止在导航动画和预测性返回手势的过程中，bottom bar闪烁
                val showBottomBarCondition = backStackEntry?.hasRoute(Article::class) == true || context !is MainActivity

                // Shared composable for the action bar content (gated by useDuo3ArticleActions)
                @Composable
                fun ActionBarContent() {
                    if (!useDuo3ArticleActions) {
                        // ── master: Button-based vote + actions ────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 8.dp)
                                .height(36.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        color = if (viewModel.voteUpState == VoteUpState.Neutral) {
                                            voteUpNeutralContent().copy(alpha = 0.1f)
                                        } else {
                                            voteUpNeutralContent()
                                        },
                                    ),
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                when (viewModel.voteUpState) {
                                    VoteUpState.Neutral -> {
                                        Button(
                                            onClick = { viewModel.toggleVoteUp(context, VoteUpState.Up) },
                                            colors = voteUpNeutralButtonColors(),
                                            shape = RectangleShape,
                                            contentPadding = PaddingValues(horizontal = 0.dp),
                                        ) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(painterResource(R.drawable.ic_vote_up_24dp), "赞同")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = viewModel.voteUpCount.toString())
                                        }
                                        Button(
                                            onClick = { viewModel.toggleVoteUp(context, VoteUpState.Down) },
                                            colors = voteUpNeutralButtonColors(),
                                            shape = RectangleShape,
                                            modifier = Modifier.height(ButtonDefaults.MinHeight).width(ButtonDefaults.MinHeight),
                                            contentPadding = PaddingValues(horizontal = 0.dp),
                                        ) {
                                            Icon(painterResource(R.drawable.ic_vote_down_24dp), "反对")
                                        }
                                    }

                                    VoteUpState.Up -> {
                                        Button(
                                            onClick = { viewModel.toggleVoteUp(context, VoteUpState.Neutral) },
                                            colors = voteUpActiveButtonColors(),
                                            shape = RectangleShape,
                                            contentPadding = PaddingValues(horizontal = 0.dp),
                                        ) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(painterResource(R.drawable.ic_vote_up_24dp), "赞同")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = viewModel.voteUpCount.toString())
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                    }

                                    VoteUpState.Down -> {
                                        Button(
                                            onClick = { viewModel.toggleVoteUp(context, VoteUpState.Neutral) },
                                            colors = voteUpActiveButtonColors(),
                                            shape = RectangleShape,
                                            modifier = Modifier.height(ButtonDefaults.MinHeight),
                                            contentPadding = PaddingValues(horizontal = 0.dp),
                                        ) {
                                            Icon(painterResource(R.drawable.ic_vote_down_24dp), "反对")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("反对")
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.End) {
                                IconButton(
                                    onClick = { showCollectionDialog = true },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (viewModel.isFavorited) Color(0xFFF57C00) else MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = if (viewModel.isFavorited) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                                ) {
                                    Icon(if (viewModel.isFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = "收藏")
                                }
                                val mainActivity = context as? MainActivity
                                val ttsState = mainActivity?.ttsState
                                if (ttsState?.isSpeaking == true) {
                                    IconButton(
                                        onClick = {
                                            mainActivity.stopSpeaking()
                                            Toast.makeText(context, "已停止朗读", Toast.LENGTH_SHORT).show()
                                        },
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "停止朗读")
                                    }
                                }
                                Button(
                                    onClick = { showComments = true },
                                    contentPadding = PaddingValues(start = 8.dp, end = 12.dp),
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
                    } else {
                        // ── duo3: pill-shaped animated vote + actions ────────────────────
                        Row(
                            modifier = Modifier
                                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AnimatedVisibility(
                                    visible = viewModel.voteUpState == VoteUpState.Neutral || viewModel.voteUpState == VoteUpState.Up,
                                ) {
                                    val upBgColor by animateColorAsState(
                                        targetValue = if (viewModel.voteUpState == VoteUpState.Up) voteUpNeutralContentDuo3() else MaterialTheme.colorScheme.surfaceContainer,
                                    )
                                    val upContentColor by animateColorAsState(
                                        targetValue = if (viewModel.voteUpState == VoteUpState.Up) Color.White else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(upBgColor)
                                            .clickable {
                                                viewModel.toggleVoteUp(
                                                    context,
                                                    if (viewModel.voteUpState == VoteUpState.Up) VoteUpState.Neutral else VoteUpState.Up,
                                                )
                                            }.padding(6.dp, 8.dp, 12.dp, 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_vote_up_24dp),
                                            contentDescription = "赞同",
                                            tint = upContentColor,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = viewModel.voteUpCount.toString(),
                                            color = upContentColor,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = viewModel.voteUpState == VoteUpState.Neutral) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                AnimatedVisibility(
                                    visible = viewModel.voteUpState == VoteUpState.Neutral || viewModel.voteUpState == VoteUpState.Down,
                                ) {
                                    val downBgColor by animateColorAsState(
                                        targetValue = if (viewModel.voteUpState == VoteUpState.Down) voteUpNeutralContentDuo3() else MaterialTheme.colorScheme.surfaceContainer,
                                    )
                                    val downContentColor by animateColorAsState(
                                        targetValue = if (viewModel.voteUpState == VoteUpState.Down) Color.White else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(downBgColor)
                                            .clickable {
                                                viewModel.toggleVoteUp(
                                                    context,
                                                    if (viewModel.voteUpState == VoteUpState.Down) VoteUpState.Neutral else VoteUpState.Down,
                                                )
                                            }.padding(6.dp, 8.dp, 8.dp, 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AnimatedVisibility(visible = viewModel.voteUpState != VoteUpState.Down) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                        }
                                        Icon(
                                            painter = painterResource(R.drawable.ic_vote_down_24dp),
                                            contentDescription = "反对",
                                            tint = downContentColor,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        AnimatedVisibility(visible = viewModel.voteUpState == VoteUpState.Down) {
                                            Row {
                                                Text(
                                                    text = "反对",
                                                    color = downContentColor,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    modifier = Modifier.padding(horizontal = 4.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(end = 4.dp),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                IconButton(
                                    onClick = { showCollectionDialog = true },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (viewModel.isFavorited) {
                                            Color(0xFFF57C00).harmonize(MaterialTheme.colorScheme.primary)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainer
                                        },
                                        contentColor = if (viewModel.isFavorited) {
                                            Color.White.copy(alpha = 0.87f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    ),
                                ) {
                                    Icon(
                                        if (viewModel.isFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = "收藏",
                                    )
                                }

                                val mainActivity = context as? MainActivity
                                val ttsState = mainActivity?.ttsState
                                AnimatedVisibility(visible = ttsState?.isSpeaking == true) {
                                    IconButton(
                                        onClick = {
                                            mainActivity?.stopSpeaking()
                                            Toast.makeText(context, "已停止朗读", Toast.LENGTH_SHORT).show()
                                        },
                                        enabled = ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing, null),
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = Color(0xFF4CAF50).harmonize(MaterialTheme.colorScheme.primary),
                                            contentColor = Color.White,
                                        ),
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "停止朗读")
                                    }
                                }

                                Button(
                                    onClick = { showComments = true },
                                    contentPadding = PaddingValues(start = 8.dp, end = 12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "${viewModel.commentCount}", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }

                if (showBottomBarCondition) {
                    Box(
                        modifier = Modifier
                            .onSizeChanged { bottomBarHeightPx = it.height.toFloat() }
                            .graphicsLayer {
                                translationY = bottomBarOffset.value
                                alpha = if (bottomBarHeightPx > 0f) 1f - (bottomBarOffset.value / bottomBarHeightPx) else 1f
                            },
                    ) {
                        ActionBarContent()
                    }
                }
            },
        ) { innerPadding ->
            Box {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState)
                        .padding(innerPadding)
                        .padding(top = 32.dp),
                ) {
                    @Suppress("UnusedReceiverParameter") // 确保竖式布局
                    @Composable
                    fun ColumnScope.DateTexts() {
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
                    if (pinAnswerDate) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            DateTexts()
                        }
                    }

                    if (viewModel.content.isNotEmpty()) {
                        if (preferences.getBoolean("articleUseWebview", true)) {
                            WebviewComp(
                                scrollState = scrollState,
//                            existingWebView = sharedData?.getOrCreateMainWebView(context),
                            ) {
                                it.isVerticalScrollBarEnabled = false
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
                                    prepareContentDocument(viewModel.content, context).apply {
                                        title(viewModel.title)
                                    },
                                )
                            }
                        } else {
                            val astNode = remember(viewModel.content) {
                                htmlToMdAst(viewModel.content)
                            }
                            val context = MarkdownRenderContext()
                            Spacer(Modifier.height(10.dp))
                            SelectionContainer(Modifier.fuckHonorService()) {
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
                        if (!pinAnswerDate) {
                            DateTexts()
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
                // Skip answer button
                if (article.type == ArticleType.Answer && buttonSkipAnswer) {
                    var navigatingToNextAnswer by remember { mutableStateOf(false) }
                    val showSkipButton = !autoHideSkipAnswerButton || isScrollingUp || scrollState.value == 0
                    val skipButtonAlpha by animateFloatAsState(
                        targetValue = if (showSkipButton) 1f else 0f,
                        animationSpec = tween(200),
                        label = "skipButtonAlpha",
                    )
                    DraggableRefreshButton(
                        modifier = Modifier.graphicsLayer { alpha = skipButtonAlpha },
                        onClick = {
                            if (showSkipButton) {
                                navigatingToNextAnswer = true
                                navigateToNext()
                                navigatingToNextAnswer = false
                            }
                        },
                        preferenceName = "buttonSkipAnswer",
                    ) {
                        if (navigatingToNextAnswer) {
                            CircularProgressIndicator(modifier = Modifier.size(30.dp))
                        } else {
                            Icon(Icons.Filled.SkipNext, contentDescription = "下一个回答")
                        }
                    }
                }
                // Status bar gradient overlay (duo3 only — not needed in master path)
                val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val surfaceColor = MaterialTheme.colorScheme.surface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarHeight + 8.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(surfaceColor, surfaceColor.copy(alpha = 0f)),
                            ),
                        ),
                ) {}
            }
        }
    } // end answerSwitchContent

    Box(
        modifier = if (useDuo3ArticleBar) Modifier else Modifier.padding(innerPadding),
    ) {
        // 根据模式渲染
        if (article.type == ArticleType.Answer && answerSwitchMode == "vertical") {
            val nav = sharedData?.navigator
            AnswerVerticalOverscroll(
                previousAnswer = nav?.previousAnswer,
                nextAnswer = nav?.nextAnswer,
                onNavigatePrevious = navigateToPrevious,
                onNavigateNext = navigateToNext,
                isAtTop = { scrollState.value == 0 },
                isAtBottom = { scrollState.value >= scrollState.maxValue },
                scrollState = scrollState,
            ) {
                (if (useDuo3ArticleBar) answerSwitchContent else answerSwitchContentOld)()
            }
        } else if (article.type == ArticleType.Answer && answerSwitchMode == "horizontal") {
            val nav = sharedData?.navigator
            // 预加载预览 WebView 内容，确保滑动前 WebView 已渲染完成
            LaunchedEffect(nav?.nextAnswer) {
                val cached = nav?.nextAnswer ?: return@LaunchedEffect
                val wv = sharedData?.getOrCreatePreviewWebView(context, isNext = true, cached.article.id) ?: return@LaunchedEffect
                val articleId = cached.article.id.toString()
                if (wv.contentId != articleId) {
                    wv.contentId = articleId
                    wv.loadZhihu(
                        "https://www.zhihu.com/answer/${cached.article.id}",
                        prepareContentDocument(cached.content, context).apply {
                            title(viewModel.title)
                        },
                    )
                }
            }
            LaunchedEffect(nav?.previousAnswer) {
                val cached = nav?.previousAnswer ?: return@LaunchedEffect
                val wv = sharedData.getOrCreatePreviewWebView(context, isNext = false, cached.article.id)
                val articleId = cached.article.id.toString()
                if (wv.contentId != articleId) {
                    wv.contentId = articleId
                    wv.loadZhihu(
                        "https://www.zhihu.com/answer/${cached.article.id}",
                        prepareContentDocument(cached.content, context).apply {
                            title(viewModel.title)
                        },
                    )
                }
            }
            AnswerHorizontalOverscroll(
                canGoPrevious = nav?.previousAnswer != null,
                canGoNext = true,
                onNavigatePrevious = navigateToPrevious,
                onNavigateNext = navigateToNext,
                previousContent = nav?.previousAnswer?.let { cached ->
                    { CachedAnswerPreview(cached, sharedData, isNext = false) }
                },
                nextContent = nav?.nextAnswer?.let { cached ->
                    { CachedAnswerPreview(cached, sharedData, isNext = true) }
                },
            ) {
                (if (useDuo3ArticleBar) answerSwitchContent else answerSwitchContentOld)()
            }
        } else {
            (if (useDuo3ArticleBar) answerSwitchContent else answerSwitchContentOld)()
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

/**
 * 渲染缓存的回答完整内容，用于水平滑动预览。
 * 显示标题、作者信息、HTML 内容（WebView）。
 * sharedData: ViewModel 中的共享数据，提供缓存 WebView 实例。
 * isNext: 标识是下一个还是上一个回答的预览。
 */
@Composable
private fun CachedAnswerPreview(
    cached: ArticleViewModel.CachedAnswerContent,
    sharedData: ArticleViewModel.ArticlesSharedData? = null,
    isNext: Boolean = false,
) {
    val context = LocalContext.current
    val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RectangleShape,
            ),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                Text(
                    text = cached.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        },
        bottomBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(color = Color(0xFF40B6F6)),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF40B6F6),
                                contentColor = Color.Black,
                            ),
                            shape = RectangleShape,
                            contentPadding = PaddingValues(horizontal = 0.dp),
                        ) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(painterResource(R.drawable.ic_vote_up_24dp), "赞同")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = cached.voteUpCount.toString())
                        }
                    }
                    Button(
                        onClick = {},
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${cached.commentCount}")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
        ) {
            Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (cached.authorAvatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = cached.authorAvatarUrl,
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
                Column {
                    Text(
                        text = cached.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    if (cached.authorBio.isNotEmpty()) {
                        Text(
                            text = cached.authorBio,
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                    }
                }
            }
            if (cached.content.isNotEmpty()) {
                if (preferences.getBoolean("articleUseWebview", true)) {
                    if (sharedData != null) {
                        AndroidView(
                            factory = { ctx ->
                                val wv = sharedData.getOrCreatePreviewWebView(ctx, isNext, cached.article.id)
                                (wv.parent as? ViewGroup)?.removeView(wv)
                                FrameLayout(ctx).apply { addView(wv) }
                            },
                            update = { frameLayout ->
                                val tag = if (isNext) sharedData.nextTag else sharedData.prevTag
                                val wv = frameLayout.findViewWithTag<CustomWebView>(tag)
                                    ?: return@AndroidView
                                val articleId = cached.article.id.toString()
                                if (wv.contentId != articleId) {
                                    wv.contentId = articleId
                                    wv.loadZhihu(
                                        "https://www.zhihu.com/answer/${cached.article.id}",
                                        prepareContentDocument(cached.content, context).apply {
                                            title(cached.title)
                                        },
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            onRelease = { frameLayout ->
                                val wv = frameLayout.getChildAt(0) as? CustomWebView ?: return@AndroidView
                                // Only detach if still the shared preview WebView (not claimed as main)
                                val isStillPreview = if (isNext) {
                                    sharedData.nextPreviewWebView === wv
                                } else {
                                    sharedData.previousPreviewWebView === wv
                                }
                                if (isStillPreview) {
                                    (wv.parent as? ViewGroup)?.removeView(wv)
                                }
                            },
                        )
                    } else {
                        // no-op
                    }
                } else {
                    val astNode = remember(cached.content) {
                        htmlToMdAst(cached.content)
                    }
                    val mdContext = MarkdownRenderContext()
                    Spacer(Modifier.height(10.dp))
                    Column {
                        for (ast in astNode) {
                            ast.Render(mdContext)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height((16 + 36).dp))
        }
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
