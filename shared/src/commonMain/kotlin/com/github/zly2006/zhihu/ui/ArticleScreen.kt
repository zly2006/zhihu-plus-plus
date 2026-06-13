/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.FilterCenterFocus
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.shared.data.ZhihuPaging
import com.github.zly2006.zhihu.shared.platform.PlatformBackHandler
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.shared.util.formatCompactCount
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.ui.components.AnswerHorizontalOverscroll
import com.github.zly2006.zhihu.ui.components.AnswerVerticalOverscroll
import com.github.zly2006.zhihu.ui.components.AuthorBadge
import com.github.zly2006.zhihu.ui.components.CollectionDialogComponent
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.ExportDialogComponent
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.github.zly2006.zhihu.ui.components.VerticalReadingProgressBar
import com.github.zly2006.zhihu.ui.components.VotersSheet
import com.github.zly2006.zhihu.ui.components.ZhihuTwoRowsTopAppBar
import com.github.zly2006.zhihu.ui.components.rememberPreferCollapsedExitUntilCollapsedScrollBehavior
import com.github.zly2006.zhihu.util.smoothGradient
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.formatArticleDateTime
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import com.materialkolor.ktx.harmonize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import zhihu.shared.generated.resources.Res
import zhihu.shared.generated.resources.ic_vote_down_24dp
import zhihu.shared.generated.resources.ic_vote_up_24dp
import kotlin.math.abs
import kotlin.math.max

private const val SCROLL_THRESHOLD = 10 // 滑动阈值，单位为dp
private val ScrollThresholdDp = SCROLL_THRESHOLD.dp

/**
 * 修复 noscript 标签中的图片加载问题。
 * 提取为独立函数，确保主 WebView 和预览 WebView 使用相同的文档处理。
 */
internal fun prepareContentDocument(
    content: String,
    onImageLoadFailure: () -> Unit = {},
): String =
    Ksoup
        .parse(content)
        .apply {
            select("noscript").forEach { noscript ->
                (noscript.nextSibling() as? Element)?.let { actualImg ->
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
                                onImageLoadFailure()
                            }
                        }
                    }
                    noscript.after(node)
                }
            }
        }.body()
        .html()

@Composable
private fun rememberBottomBarAvoidingBringIntoViewSpec(
    obscuredBottomPx: Float,
): BringIntoViewSpec {
    val density = LocalDensity.current
    return remember(obscuredBottomPx) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float,
            ): Float {
                val effectiveContainerSize = (containerSize - obscuredBottomPx).coerceAtLeast(0f)
                val effectiveContainerTop = density.run { 110.dp.toPx() }
                val trailingEdge = offset + size
                return when {
                    offset >= effectiveContainerTop && trailingEdge <= effectiveContainerSize -> 0f
                    offset < effectiveContainerTop && trailingEdge > effectiveContainerSize -> 0f
                    abs(offset) < abs(trailingEdge + effectiveContainerTop - effectiveContainerSize) -> offset - effectiveContainerTop
                    else -> trailingEdge + effectiveContainerTop - effectiveContainerSize
                }
            }
        }
    }
}

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
private fun ArticleSummarySheet(
    showDialog: Boolean,
    summaryText: String,
    loading: Boolean,
    errorMessage: String?,
    onDismissRequest: () -> Unit,
    onRetryRequest: () -> Unit,
) {
    if (!showDialog) return
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    MyModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(scrollState),
        ) {
            Text("总结本文", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (loading && summaryText.isBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("正在生成总结...")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (summaryText.isNotBlank()) {
                    SelectionContainer {
                        Text(summaryText)
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    if (summaryText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("关闭")
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (!loading) {
                    TextButton(onClick = onRetryRequest) {
                        Text("重新总结")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleActionsMenu(
    article: Article,
    viewModel: ArticleViewModel,
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
    onSummaryRequest: () -> Unit,
    onExportRequest: () -> Unit,
    onSetImmersiveDoubleTap: () -> Unit = {},
) {
    val articleActionsRuntime = rememberArticleActionsRuntime()
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
        val ttsState = articleActionsRuntime.ttsState
        MenuActionButton(
            icon = {
                when (ttsState) {
                    TtsState.Initializing, TtsState.Uninitialized -> CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
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
                if (ttsState.isSpeaking) {
                    articleActionsRuntime.toggleSpeech(viewModel.title, viewModel.content)
                } else if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                    // 使用协程在后台处理文本提取，避免UI阻塞
                    viewModel.viewModelScope.launch {
                        try {
                            // 在IO线程中处理文本提取
                            withContext(Dispatchers.Default) {
                                val textToRead = articleSpeechText(viewModel.title, viewModel.content)

                                // 回到主线程执行TTS
                                withContext(Dispatchers.Main) {
                                    if (textToRead.isNotBlank()) {
                                        articleActionsRuntime.toggleSpeech(viewModel.title, viewModel.content)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Unit
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
                articleActionsRuntime.shareRuntime.share(
                    article,
                    articleActionText(article, viewModel.questionId, viewModel.title, viewModel.authorName),
                )
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        MenuActionButton(
            icon = Icons.AutoMirrored.Filled.Comment,
            text = "总结本文",
            onClick = {
                onDismissRequest()
                onSummaryRequest()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 复制链接按钮
        MenuActionButton(
            icon = Icons.Filled.ContentCopy,
            text = "复制链接",
            onClick = {
                onDismissRequest()
                articleActionsRuntime.shareRuntime.copyLink(
                    article,
                    articleActionText(article, viewModel.questionId, viewModel.title, viewModel.authorName),
                )
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 开关沉浸式
        MenuActionButton(
            icon = Icons.Filled.FilterCenterFocus,
            text = "进入沉浸式",
            onClick = {
                onDismissRequest()
                onSetImmersiveDoubleTap()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 导出按钮
        MenuActionButton(
            icon = Icons.Filled.GetApp,
            text = "导出文章 (Markdown、图片、HTML、PDF)",
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
                    articleActionsRuntime.openArticleInBrowser(article)
                    onDismissRequest()
                }
            },
        )

        // 底部安全区域
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showMenu) {
        com.github.zly2006.zhihu.ui.components.MyModalBottomSheet(onDismissRequest) {
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
 * 文章/回答详情页。
 *
 * 页面负责加载知乎回答或专栏文章，展示标题、作者、正文、附件视频、评论入口、分享/复制/朗读/浏览器打开等底部操作，
 * 并根据阅读设置切换 Compose Markdown 或 WebView 渲染。回答页还承载同题回答切换手势和对应转场状态，因此改动时要同时关注
 * `answerSwitchMode`、`buttonSkipAnswer`、`autoHideArticleBottomBar`、`titleAutoHide`、`answerDoubleTapAction` 和
 * `ARTICLE_USE_WEBVIEW_PREFERENCE_KEY`。
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun ArticleScreen(
    article: Article,
    viewModel: ArticleViewModel,
) {
    val navigator = LocalNavigator.current
    val articleScreenRuntime = rememberArticleScreenRuntime()
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val articleHost = articleScreenRuntime.articleHost
    val previewPreloader = articleScreenRuntime.previewPreloader
    val backStackEntry by articleHost?.articleNavController?.currentBackStackEntryAsState()
        ?: remember { mutableStateOf(null) }

    val scrollState = rememberScrollState()
    val articleSettings = rememberArticleScreenSettingsState()

    var isTitleAutoHide by remember { mutableStateOf(articleSettings.isTitleAutoHide) }
    var autoHideArticleBottomBar by remember {
        mutableStateOf(articleSettings.autoHideArticleBottomBar)
    }
    var answerSwitchMode by remember {
        mutableStateOf(articleSettings.answerSwitchMode)
    }
    var pinAnswerDate by remember { mutableStateOf(articleSettings.pinAnswerDate) }
    val readHistoryRecorder = rememberArticleReadHistoryRecorder()
    val userMessages = rememberUserMessageSink()

    var previousScrollValue by remember { mutableIntStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(false) }
    var navigatingToNextAnswer by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val scrollDeltaThreshold = with(density) { ScrollThresholdDp.toPx() }
    var topBarHeight by remember { mutableIntStateOf(0) }
    var showComments by rememberSaveable(article.type, article.id) { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDoubleTapActionDialog by remember { mutableStateOf(false) }
    var showVoters by rememberSaveable(article.type, article.id) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    val useDuo3ArticleActions = remember { articleSettings.useDuo3ArticleActions }
    var buttonSkipAnswer by remember { mutableStateOf(articleSettings.buttonSkipAnswer) }
    var autoHideSkipAnswerButton by remember { mutableStateOf(articleSettings.autoHideSkipAnswerButton) }
    var answerDoubleTapAction by remember {
        mutableStateOf(
            articleSettings.answerDoubleTapAction,
        )
    }
    var useWebView by remember { mutableStateOf(articleSettings.useWebView) }

    // 跟手隐藏标题栏和底栏：用滚动增量直接驱动像素偏移。
    val topBarOffset = remember { Animatable(0f) }
    val bottomBarOffset = remember { Animatable(0f) }
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    var bottomBarHeightPx by remember { mutableFloatStateOf(0f) }
    var previousScrollForBarOffset by remember { mutableIntStateOf(0) }
    var isBarSnapping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        readHistoryRecorder.addReadHistory(article)
    }

    fun upVoteFromDoubleTap() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
        if (viewModel.voteUpState != VoteUpState.Up) {
            viewModel.toggleVoteUp(environment, VoteUpState.Up)
        }
    }
    // 回答切换手势系统
    val sharedData = if (article.type == ArticleType.Answer) {
        environment.articleAnswerSwitchState()
    } else {
        null
    }

    // 沉浸式阅读模式
    var isImmersiveMode by remember(sharedData) {
        mutableStateOf(sharedData?.isImmersiveMode ?: false)
    }

    val toggleImmersive: () -> Unit = { isImmersiveMode = !isImmersiveMode }

    fun performAnswerDoubleTapAction(action: AnswerDoubleTapAction) {
        when (action) {
            AnswerDoubleTapAction.None -> Unit
            AnswerDoubleTapAction.Ask -> showDoubleTapActionDialog = true
            AnswerDoubleTapAction.VoteUp -> upVoteFromDoubleTap()
            AnswerDoubleTapAction.OpenComments -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                showComments = true
            }
            AnswerDoubleTapAction.ToggleImmersive -> {
                toggleImmersive()
            }
        }
    }

    fun saveAnswerDoubleTapAction(action: AnswerDoubleTapAction) {
        answerDoubleTapAction = action
        articleSettings.saveAnswerDoubleTapAction(action)
    }

    fun handleAnswerDoubleTap() {
        if (article.type != ArticleType.Answer) return
        performAnswerDoubleTapAction(answerDoubleTapAction)
    }

    val answerDoubleTapModifier = if (
        article.type == ArticleType.Answer &&
        answerDoubleTapAction != AnswerDoubleTapAction.None
    ) {
        Modifier.pointerInput(answerDoubleTapAction) {
            detectTapGestures(
                onDoubleTap = { handleAnswerDoubleTap() },
            )
        }
    } else {
        Modifier
    }

    LaunchedEffect(articleSettings.isTitleAutoHide) {
        isTitleAutoHide = articleSettings.isTitleAutoHide
    }
    LaunchedEffect(articleSettings.autoHideArticleBottomBar) {
        autoHideArticleBottomBar = articleSettings.autoHideArticleBottomBar
    }
    LaunchedEffect(articleSettings.buttonSkipAnswer) {
        buttonSkipAnswer = articleSettings.buttonSkipAnswer
    }
    LaunchedEffect(articleSettings.autoHideSkipAnswerButton) {
        autoHideSkipAnswerButton = articleSettings.autoHideSkipAnswerButton
    }
    LaunchedEffect(articleSettings.answerSwitchMode) {
        answerSwitchMode = articleSettings.answerSwitchMode
    }
    LaunchedEffect(articleSettings.pinAnswerDate) {
        pinAnswerDate = articleSettings.pinAnswerDate
    }
    LaunchedEffect(articleSettings.useWebView) {
        useWebView = articleSettings.useWebView
    }
    LaunchedEffect(articleSettings.answerDoubleTapAction) {
        answerDoubleTapAction = articleSettings.answerDoubleTapAction
    }

    // 自动隐藏关闭时重置栏位偏移。
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

        // 吸附动画期间滚动由程序驱动，跳过栏位偏移跟踪。
        if (!isBarSnapping) {
            val delta = currentScroll - previousScrollForBarOffset
            val atTop = currentScroll == 0
            val atBottom = currentScroll >= scrollState.maxValue

            // 顶栏：顶部强制显示，接近底部时按内容距离逐步露出。
            if (atTop) {
                topBarOffset.snapTo(0f)
            } else if (isTitleAutoHide && topBarHeightPx > 0f) {
                val deltaBasedOffset = (topBarOffset.value - delta).coerceIn(-topBarHeightPx, 0f)
                val distanceFromBottom = (scrollState.maxValue - currentScroll).coerceAtLeast(0)
                if (distanceFromBottom < topBarHeightPx.toInt()) {
                    // 底部区域取露出更多栏位的偏移，也就是更接近 0 的值。
                    val distanceBasedOffset = (-distanceFromBottom.toFloat()).coerceIn(-topBarHeightPx, 0f)
                    topBarOffset.snapTo(maxOf(distanceBasedOffset, deltaBasedOffset))
                } else {
                    topBarOffset.snapTo(deltaBasedOffset)
                }
            }

            // 底栏：顶部强制显示，接近底部时按内容距离逐步露出。
            if (atTop) {
                bottomBarOffset.snapTo(0f)
            } else if (autoHideArticleBottomBar && bottomBarHeightPx > 0f) {
                val deltaBasedOffset = (bottomBarOffset.value + delta).coerceIn(0f, bottomBarHeightPx)
                val distanceFromBottom = (scrollState.maxValue - currentScroll).coerceAtLeast(0)
                if (distanceFromBottom < bottomBarHeightPx.toInt()) {
                    // 底部区域取露出更多栏位的偏移。
                    val distanceBasedOffset = distanceFromBottom.toFloat().coerceIn(0f, bottomBarHeightPx)
                    bottomBarOffset.snapTo(minOf(distanceBasedOffset, deltaBasedOffset))
                } else {
                    bottomBarOffset.snapTo(deltaBasedOffset)
                }
            }
        }
        previousScrollForBarOffset = currentScroll

        if (viewModel.rememberedScrollYSync) {
            viewModel.rememberedScrollY = currentScroll
        }
        if (currentScroll == viewModel.rememberedScrollY && scrollState.maxValue != Int.MAX_VALUE) {
            viewModel.rememberedScrollYSync = true
        }
    }

    // 滚动停止时把栏位吸附到完全显示或完全隐藏，并让内容滚动跟随顶栏吸附。
    LaunchedEffect(scrollState.isScrollInProgress) {
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

            // 仅在靠近顶部时补偿顶栏吸附导致的内容位移；底栏交给距离触发的露出逻辑处理。
            val topInNaturalArea = scrollState.value <= topBarHeightPx
            val topDelta = if (topInNaturalArea) topBarOffset.value - topTarget else 0f

            if (topTarget != topBarOffset.value || bottomTarget != bottomBarOffset.value) {
                try {
                    isBarSnapping = true
                    kotlinx.coroutines.coroutineScope {
                        launch { topBarOffset.animateTo(topTarget, tween(150)) }
                        launch { bottomBarOffset.animateTo(bottomTarget, tween(150)) }
                        if (topDelta != 0f) {
                            launch { scrollState.animateScrollBy(topDelta, tween(150)) }
                        }
                    }
                } finally {
                    isBarSnapping = false
                }
            }
        }
    }

    // 主视觉风格的栏位显隐：按滚动方向控制，用于非跟手偏移路径。
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
    val showBottomBarSlot = backStackEntry?.hasRoute(Article::class) == true || articleHost == null
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    val bottomBarObscuredHeightPx by remember(
        showBottomBarSlot,
        true,
        showBottomBar,
        bottomBarHeightPx,
        bottomBarOffset.value,
    ) {
        derivedStateOf {
            val navBar = density.run {
                navigationBarsPadding.calculateBottomPadding().toPx().coerceAtLeast(0f)
            }
            val bottonBar = if (!showBottomBarSlot) {
                0f
            } else if (true) {
                (bottomBarHeightPx - bottomBarOffset.value).coerceIn(0f, bottomBarHeightPx)
            } else if (showBottomBar) {
                bottomBarHeightPx
            } else {
                0f
            }
            navBar + bottonBar
        }
    }
    val articleBringIntoViewSpec = rememberBottomBarAvoidingBringIntoViewSpec(bottomBarObscuredHeightPx)
    LaunchedEffect(sharedData, isImmersiveMode) {
        if (sharedData != null) sharedData.isImmersiveMode = isImmersiveMode
    }
    ArticleImmersiveModeEffect(isImmersiveMode)

    LaunchedEffect(article.id) {
        // Bug 2: 在主线程检查标志并重置（避免跨线程可见性问题）
        if (sharedData != null) {
            if (!sharedData.navigatingFromAnswerSwitch) {
                sharedData.reset()
            }
            sharedData.navigatingFromAnswerSwitch = false
            sharedData.answerTransitionDirection = ArticleAnswerTransitionDirection.DEFAULT

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
        viewModel.loadArticle(environment)
        viewModel.loadCollections(environment)
    }

    val navigateToPrevious: () -> Unit = {
        sharedData?.answerTransitionDirection = if (answerSwitchMode == "horizontal") {
            ArticleAnswerTransitionDirection.HORIZONTAL_PREVIOUS
        } else {
            ArticleAnswerTransitionDirection.VERTICAL_PREVIOUS
        }
        sharedData?.navigatingFromAnswerSwitch = true
        // 更新当前回答内容到历史
        sharedData?.navigator?.pushAnswer(viewModel.toCachedContent(sourceLabel = sharedData.navigator?.sourceName ?: "此问题"))
        val prev = sharedData?.navigator?.goToPrevious()
        if (prev != null) {
            sharedData.pendingInitialContent = prev
            sharedData.promoteForNavigation(sharedData.answerTransitionDirection)
            val navController = articleHost?.articleNavController
            if (navController != null) {
                if (navController.currentBackStackEntry?.hasRoute(Article::class) == true &&
                    navController.currentBackStackEntry
                        ?.toRoute<Article>()
                        ?.type == ArticleType.Answer
                ) {
                    navController.popBackStack()
                }
            }
            navigator.onNavigate(prev.article)
        } else {
            // 无历史时尝试从来源（如收藏夹）向前加载
            sharedData?.pendingInitialContent = sharedData.navigator?.previousAnswerPreview
            sharedData?.promoteForNavigation(sharedData.answerTransitionDirection)
            coroutineScope.launch {
                val prevCached = sharedData?.navigator?.loadPrevious()
                if (prevCached != null) {
                    sharedData.pendingInitialContent = prevCached
                    val navController = articleHost?.articleNavController
                    if (navController != null) {
                        if (navController.currentBackStackEntry?.hasRoute(Article::class) == true &&
                            navController.currentBackStackEntry
                                ?.toRoute<Article>()
                                ?.type == ArticleType.Answer
                        ) {
                            navController.popBackStack()
                        }
                    }
                    navigator.onNavigate(prevCached.article)
                }
            }
        }
    }

    val navigateToNext: () -> Unit = {
        sharedData?.answerTransitionDirection = if (answerSwitchMode == "horizontal") {
            ArticleAnswerTransitionDirection.HORIZONTAL_NEXT
        } else {
            ArticleAnswerTransitionDirection.VERTICAL_NEXT
        }
        sharedData?.navigatingFromAnswerSwitch = true
        // 更新当前回答内容到历史
        sharedData?.navigator?.pushAnswer(viewModel.toCachedContent(sourceLabel = sharedData.navigator?.sourceName ?: "此问题"))
        // 优先使用前向历史
        val historyNext = sharedData?.navigator?.goToNext()
        if (historyNext != null) {
            sharedData.pendingInitialContent = historyNext
            sharedData.promoteForNavigation(sharedData.answerTransitionDirection)
            val navController = articleHost?.articleNavController
            if (navController != null) {
                if (navController.currentBackStackEntry?.hasRoute(Article::class) == true &&
                    navController.currentBackStackEntry
                        ?.toRoute<Article>()
                        ?.type == ArticleType.Answer
                ) {
                    navController.popBackStack()
                }
            }
            navigator.onNavigate(historyNext.article)
        } else {
            // 没有前向历史，从导航器加载
            sharedData?.pendingInitialContent = sharedData.navigator?.nextAnswer
            sharedData?.promoteForNavigation(sharedData.answerTransitionDirection)
            coroutineScope.launch {
                val nextArticle = sharedData?.navigator?.loadNext()
                if (nextArticle != null) {
                    val navController = articleHost?.articleNavController
                    if (navController != null) {
                        if (navController.currentBackStackEntry?.hasRoute(Article::class) == true &&
                            navController.currentBackStackEntry
                                ?.toRoute<Article>()
                                ?.type == ArticleType.Answer
                        ) {
                            navController.popBackStack()
                        }
                    }
                    navigator.onNavigate(nextArticle)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    val answerSwitchContent: @Composable () -> Unit = {
        val scrollBehavior = rememberPreferCollapsedExitUntilCollapsedScrollBehavior()
        // 记录历史最大滚动范围，避免顶栏展开/收起时 maxValue 短暂变化导致 scrollBehavior 抖动。
        var scrollStateMaxValue by remember { mutableIntStateOf(0) }
        LaunchedEffect(scrollState.maxValue) {
            if (scrollState.maxValue != Int.MAX_VALUE) {
                scrollStateMaxValue = max(scrollState.maxValue, scrollStateMaxValue)
            }
        }
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isImmersiveMode) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier),
            topBar = if (isImmersiveMode) {
                {}
            } else {
                @Composable {
                    Box(
                        modifier = Modifier
                            .onSizeChanged {
                                topBarHeightPx = it.height.toFloat()
                                if (it.height >= 10) topBarHeight = it.height
                            }.graphicsLayer {
                                translationY = topBarOffset.value
                                alpha = if (topBarHeightPx > 0f) 1f + (topBarOffset.value / topBarHeightPx) else 1f
                            },
                    ) {
                        ZhihuTwoRowsTopAppBar(
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        articleHost?.articleNavController?.popBackStack()
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            },
                            actions = {
                                if (useDuo3ArticleActions) {
                                    IconButton(
                                        onClick = { showActionsMenu = true },
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
                                        .padding(end = 16.dp)
                                        .fillMaxWidth()
                                        .clickable {
                                            navigator.onNavigate(
                                                com.github.zly2006.zhihu.navigation.Person(
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

                                    Column(
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = viewModel.authorName,
                                                style = if (expanded) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                                                color = if (expanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            if (viewModel.authorBadge != null) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                AuthorBadge(
                                                    badge = viewModel.authorBadge,
                                                    compact = !expanded,
                                                )
                                            }
                                        }
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
                            colors = TopAppBarDefaults.topAppBarColors().copy(
                                scrolledContainerColor = if (MaterialTheme.colorScheme.surfaceContainer != MaterialTheme.colorScheme.background) {
                                    MaterialTheme.colorScheme.surfaceContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            ),
                        )
                    }
                }
            },
            bottomBar = if (isImmersiveMode) {
                {}
            } else {
                @Composable {
                    // 防止在导航动画和预测性返回手势过程中，底部操作栏闪烁。
                    val showBottomBarCondition = backStackEntry?.hasRoute(Article::class) == true || articleHost == null

                    // 操作栏内容的共享组合，按 useDuo3ArticleActions 切换两套视觉。
                    @Composable
                    fun ActionBarContent() {
                        if (!useDuo3ArticleActions) {
                            // ── 主视觉：按钮式投票与操作区 ────────────────────────
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
                                                onClick = { viewModel.toggleVoteUp(environment, VoteUpState.Up) },
                                                colors = voteUpNeutralButtonColors(),
                                                shape = RectangleShape,
                                                contentPadding = PaddingValues(horizontal = 0.dp),
                                            ) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(painterResource(Res.drawable.ic_vote_up_24dp), "赞同")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = viewModel.voteUpCount.toString())
                                            }
                                            Button(
                                                onClick = { viewModel.toggleVoteUp(environment, VoteUpState.Down) },
                                                colors = voteUpNeutralButtonColors(),
                                                shape = RectangleShape,
                                                modifier = Modifier.height(ButtonDefaults.MinHeight).width(ButtonDefaults.MinHeight),
                                                contentPadding = PaddingValues(horizontal = 0.dp),
                                            ) {
                                                Icon(painterResource(Res.drawable.ic_vote_down_24dp), "反对")
                                            }
                                        }

                                        VoteUpState.Up -> {
                                            Button(
                                                onClick = { viewModel.toggleVoteUp(environment, VoteUpState.Neutral) },
                                                colors = voteUpActiveButtonColors(),
                                                shape = RectangleShape,
                                                contentPadding = PaddingValues(horizontal = 0.dp),
                                            ) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(painterResource(Res.drawable.ic_vote_up_24dp), "赞同")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = viewModel.voteUpCount.toString())
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                        }

                                        VoteUpState.Down -> {
                                            Button(
                                                onClick = { viewModel.toggleVoteUp(environment, VoteUpState.Neutral) },
                                                colors = voteUpActiveButtonColors(),
                                                shape = RectangleShape,
                                                modifier = Modifier.height(ButtonDefaults.MinHeight),
                                                contentPadding = PaddingValues(horizontal = 0.dp),
                                            ) {
                                                Icon(painterResource(Res.drawable.ic_vote_down_24dp), "反对")
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
                                    val ttsState = articleHost?.articleTtsState
                                    if (ttsState?.isSpeaking == true) {
                                        IconButton(
                                            onClick = {
                                                articleHost.stopArticleSpeaking()
                                                userMessages.showMessage("已停止朗读")
                                            },
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "停止朗读")
                                        }
                                    }
                                    Button(
                                        onClick = { showComments = true },
                                        contentPadding = PaddingValues(start = 8.dp, end = 12.dp),
                                        colors = voteUpNeutralButtonColors(),
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
                            // ── duo3：药丸式动画投票与操作区 ────────────────────
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
                                                        environment,
                                                        if (viewModel.voteUpState == VoteUpState.Up) VoteUpState.Neutral else VoteUpState.Up,
                                                    )
                                                }.padding(6.dp, 8.dp, 12.dp, 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_vote_up_24dp),
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
                                                        environment,
                                                        if (viewModel.voteUpState == VoteUpState.Down) VoteUpState.Neutral else VoteUpState.Down,
                                                    )
                                                }.padding(6.dp, 8.dp, 8.dp, 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            AnimatedVisibility(visible = viewModel.voteUpState != VoteUpState.Down) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                            }
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_vote_down_24dp),
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

                                    val ttsState = articleHost?.articleTtsState
                                    AnimatedVisibility(visible = ttsState?.isSpeaking == true) {
                                        IconButton(
                                            onClick = {
                                                articleHost?.stopArticleSpeaking()
                                                userMessages.showMessage("已停止朗读")
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
                }
            },
        ) { innerPadding ->
            CompositionLocalProvider(LocalBringIntoViewSpec provides articleBringIntoViewSpec) {
                Box {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .verticalScroll(scrollState)
                            .padding(innerPadding)
                            .padding(top = 8.dp),
                    ) {
                        @Suppress("UnusedReceiverParameter") // 确保竖式布局
                        @Composable
                        fun ColumnScope.DateTexts() {
                            Text(
                                "发布于 " + formatArticleDateTime(viewModel.createdAt),
                                color = Color.Gray,
                                fontSize = 11.sp,
                            )
                            if (viewModel.createdAt != viewModel.updatedAt) {
                                Text(
                                    "编辑于 " + formatArticleDateTime(viewModel.updatedAt),
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                )
                            }
                        }

                        @Composable
                        fun ColumnScope.AnswerVotersSocialCredit() {
                            if (article.type != ArticleType.Answer || viewModel.votersTotal <= 0) return
                            val text = viewModel.votersSocialText.ifBlank {
                                "${formatCompactCount(viewModel.votersTotal)} 人赞同了该回答"
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        showVoters = true
                                        if (viewModel.voters.isEmpty()) {
                                            viewModel.loadMoreVoters(environment, reset = true)
                                        }
                                    },
                            )
                        }

                        @Composable
                        fun ColumnScope.AnswerLeadingMeta() {
                            val hasPinnedDate = pinAnswerDate
                            val hasSocialCredit = article.type == ArticleType.Answer && viewModel.votersTotal > 0
                            if (!hasPinnedDate && !hasSocialCredit) return
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                if (hasPinnedDate) {
                                    DateTexts()
                                }
                                AnswerVotersSocialCredit()
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (viewModel.content.isNotEmpty() || viewModel.attachment != null) {
                            if (useWebView) {
                                AnswerLeadingMeta()
                                ArticleWebViewContent(
                                    article = article,
                                    html = viewModel.content,
                                    title = viewModel.title,
                                    scrollState = scrollState,
                                    rememberedScrollY = viewModel.rememberedScrollY,
                                    rememberedScrollYSync = viewModel.rememberedScrollYSync,
                                    onRememberedScrollYSyncChange = { viewModel.rememberedScrollYSync = it },
                                    onImageLoadFailed = { userMessages.showMessage("图片加载失败，请向开发者反馈") },
                                    onDoubleTap = ::handleAnswerDoubleTap,
                                )
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
                            } else {
                                AnswerLeadingMeta()
                                RenderMarkdown(
                                    html = viewModel.content,
                                    modifier = Modifier.articleMarkdownSelectionWorkaround(),
                                    selectable = true,
                                    enableScroll = false,
                                    header = {},
                                    footer = {
                                        ArticleVideoAttachmentContent(viewModel.attachment)
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
                                    },
                                )
                            }
                        }
                    }
                    // 状态栏渐变遮罩，仅 duo3 路径需要；主视觉路径不绘制。
                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(statusBarHeight + 16.dp)
                            .background(
                                Brush.verticalGradient(smoothGradient(surfaceColor, 0.8f)),
                            ),
                    ) {}
                }
            }
        }
    } // answerSwitchContent 结束。

    val nav = sharedData?.navigator
    if (article.type == ArticleType.Answer && answerSwitchMode == "horizontal") {
        // 预加载预览内容，确保滑动前相邻回答已经准备好。
        LaunchedEffect(nav?.nextAnswer) {
            val cached = nav?.nextAnswer ?: return@LaunchedEffect
            previewPreloader.preloadPreview(cached, isNext = true, viewModel.title) {
                userMessages.showMessage("图片加载失败，请向开发者反馈")
            }
        }
        LaunchedEffect(nav?.previousAnswer) {
            val cached = nav?.previousAnswer ?: return@LaunchedEffect
            previewPreloader.preloadPreview(cached, isNext = false, viewModel.title) {
                userMessages.showMessage("图片加载失败，请向开发者反馈")
            }
        }
    }
    val progressBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
    val progressBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 96.dp

    Box(
        modifier = Modifier.fillMaxSize().then(answerDoubleTapModifier),
    ) {
        // 根据模式渲染
        if (article.type == ArticleType.Answer && answerSwitchMode == "vertical") {
            AnswerVerticalOverscroll(
                previousAnswer = nav?.previousAnswer,
                nextAnswer = nav?.nextAnswer,
                onNavigatePrevious = navigateToPrevious,
                onNavigateNext = navigateToNext,
                isAtTop = { scrollState.value == 0 },
                isAtBottom = { scrollState.value >= scrollState.maxValue },
                scrollState = scrollState,
            ) {
                answerSwitchContent()
            }
        } else if (article.type == ArticleType.Answer && answerSwitchMode == "horizontal") {
            AnswerHorizontalOverscroll(
                canGoPrevious = nav?.previousAnswer != null,
                canGoNext = nav?.nextAnswer != null,
                onNavigatePrevious = navigateToPrevious,
                onNavigateNext = navigateToNext,
                previousContent = nav?.previousAnswer?.let { cached ->
                    { CachedAnswerPreview(cached) }
                },
                nextContent = nav?.nextAnswer?.let { cached ->
                    { CachedAnswerPreview(cached) }
                },
            ) {
                answerSwitchContent()
            }
        } else {
            answerSwitchContent()
        }

        VerticalReadingProgressBar(
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    top = progressBarTopPadding,
                    bottom = progressBarBottomPadding,
                    end = 2.dp,
                ).then(if (isImmersiveMode) Modifier.graphicsLayer { alpha = 0f } else Modifier),
        )

        // 跳转按钮需要压在问题区和回答区之上。
        if (article.type == ArticleType.Answer && buttonSkipAnswer && !isImmersiveMode) {
            val showSkipButton = !autoHideSkipAnswerButton || isScrollingUp || scrollState.value == 0
            val skipButtonAlpha by animateFloatAsState(
                targetValue = if (showSkipButton) 1f else 0f,
                animationSpec = tween(200),
                label = "skipButtonAlpha",
            )
            var fabClickCount by remember { mutableIntStateOf(0) }
            LaunchedEffect(fabClickCount) {
                if (fabClickCount > 0) {
                    delay(350)
                    if (fabClickCount >= 2) {
                        toggleImmersive()
                    } else {
                        if (showSkipButton) {
                            navigatingToNextAnswer = true
                            navigateToNext()
                            navigatingToNextAnswer = false
                        }
                    }
                    fabClickCount = 0
                }
            }
            DraggableRefreshButton(
                modifier = Modifier.graphicsLayer { alpha = skipButtonAlpha },
                onClick = { fabClickCount++ },
                preferenceName = "buttonSkipAnswer",
            ) {
                if (navigatingToNextAnswer) {
                    CircularProgressIndicator(modifier = Modifier.size(30.dp))
                } else {
                    Icon(Icons.Filled.SkipNext, contentDescription = "下一个回答")
                }
            }
        }
    }

    // 全屏菜单
    ArticleActionsMenu(
        article = article,
        viewModel = viewModel,
        showMenu = showActionsMenu,
        onDismissRequest = { showActionsMenu = false },
        onSummaryRequest = {
            showSummaryDialog = true
            viewModel.requestAiSummary(environment)
        },
        onExportRequest = { showExportDialog = true },
        onSetImmersiveDoubleTap = {
            showActionsMenu = false
            // 沉浸式模式下，按返回键优先退出沉浸式，不会直接退出回答
            toggleImmersive()
            userMessages.showMessage("已进入沉浸式，按返回键即可退出")
        },
    )

    ArticleSummarySheet(
        showDialog = showSummaryDialog,
        summaryText = viewModel.aiSummaryText,
        loading = viewModel.aiSummaryLoading,
        errorMessage = viewModel.aiSummaryError,
        onDismissRequest = {
            showSummaryDialog = false
            viewModel.cancelAiSummary()
        },
        onRetryRequest = {
            viewModel.requestAiSummary(environment)
        },
    )

    // 沉浸式模式下，返回键优先退出沉浸式
    PlatformBackHandler(enabled = isImmersiveMode) {
        toggleImmersive()
    }

    PlatformBackHandler(showActionsMenu) {
        showActionsMenu = false
    }

    // 使用新的收藏夹对话框组件
    CollectionDialogComponent(
        showDialog = showCollectionDialog,
        onDismiss = { showCollectionDialog = false },
        collections = viewModel.collections,
        onLoadCollections = { viewModel.loadCollections(environment) },
        onToggleFavorite = { collection ->
            viewModel.toggleFavorite(collection.id, collection.isFavorited, environment)
        },
        onCreateCollection = { title, description, isPublic ->
            viewModel.createNewCollection(environment, title, description, isPublic)
        },
    )

    CommentScreenComponent(
        showComments = showComments,
        onDismiss = { showComments = false },
        content = article,
    )
    VotersSheet(
        show = showVoters,
        title = "${formatCompactCount(viewModel.votersTotal)} 人赞同了该回答",
        voters = viewModel.voters,
        isLoading = viewModel.votersLoading,
        errorMessage = viewModel.votersError,
        canLoadMore = viewModel.votersNextUrl != null,
        onDismissRequest = { showVoters = false },
        onLoadMore = { viewModel.loadMoreVoters(environment) },
        onRetry = { viewModel.loadMoreVoters(environment, reset = viewModel.voters.isEmpty()) },
        onNavigate = { person ->
            showVoters = false
            navigator.onNavigate(person)
        },
    )
    if (showDoubleTapActionDialog) {
        MyModalBottomSheet(
            onDismissRequest = { showDoubleTapActionDialog = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "设置双击回答动作",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "选择以后双击回答时默认执行的动作。选择后会立即保存到设置，你也可以稍后在设置中修改。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        showDoubleTapActionDialog = false
                        saveAnswerDoubleTapAction(AnswerDoubleTapAction.None)
                        userMessages.showMessage("已将双击回答动作设为：${AnswerDoubleTapAction.None.label}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("设为无操作")
                }
                Button(
                    onClick = {
                        showDoubleTapActionDialog = false
                        saveAnswerDoubleTapAction(AnswerDoubleTapAction.VoteUp)
                        upVoteFromDoubleTap()
                        userMessages.showMessage("已将双击回答动作设为：${AnswerDoubleTapAction.VoteUp.label}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("设为点赞")
                }
                Button(
                    onClick = {
                        showDoubleTapActionDialog = false
                        saveAnswerDoubleTapAction(AnswerDoubleTapAction.OpenComments)
                        showComments = true
                        userMessages.showMessage("已将双击回答动作设为：${AnswerDoubleTapAction.OpenComments.label}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("设为打开评论区")
                }
                Button(
                    onClick = {
                        showDoubleTapActionDialog = false
                        saveAnswerDoubleTapAction(AnswerDoubleTapAction.ToggleImmersive)
                        toggleImmersive()
                        userMessages.showMessage("已将双击回答动作设为：${AnswerDoubleTapAction.ToggleImmersive.label}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("设为开关沉浸式")
                }
            }
        }
    }
    // 导出对话框
    ExportDialogComponent(
        showDialog = showExportDialog,
        onDismiss = { showExportDialog = false },
        onExportHtml = { includeAppAttribution, onComplete ->
            viewModel.exportToHtml(environment, includeAppAttribution, onComplete)
        },
        onExportImage = { includeAppAttribution, onComplete ->
            viewModel.exportToImage(environment, includeAppAttribution, onComplete)
        },
        onExportMarkdown = {
            viewModel.exportToClipboard(environment)
        },
        onExportImageWithComments = { commentCount, includeAppAttribution, onComplete ->
            viewModel.exportToImageWithComments(environment, commentCount, includeAppAttribution, onComplete)
        },
    )
}

/**
 * 渲染缓存的回答完整内容，用于水平滑动预览。
 *
 * 内容来自 [CachedAnswerContent]，包含标题、作者信息、投票/评论计数和 HTML 正文。正文使用 Compose Markdown，
 * 因此这里是轻量预览，不持有 WebView 或答案切换共享状态。
 */
@Composable
private fun CachedAnswerPreview(
    cached: CachedAnswerContent,
) {
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
                            Icon(painterResource(Res.drawable.ic_vote_up_24dp), "赞同")
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cached.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (cached.authorBadge != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            AuthorBadge(
                                badge = cached.authorBadge,
                            )
                        }
                    }
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
                Spacer(Modifier.height(10.dp))
                RenderMarkdown(
                    html = cached.content,
                    modifier = Modifier,
                    selectable = true,
                    enableScroll = false,
                    header = {},
                    footer = {},
                )
            }
            Spacer(modifier = Modifier.height((16 + 36).dp))
        }
    }
}

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
    val paging: ZhihuPaging,
)
