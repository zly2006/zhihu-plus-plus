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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.markdown.RenderVideoBox
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.R
import com.github.zly2006.zhihu.shared.article.CachedAnswerContent
import com.github.zly2006.zhihu.shared.article.VoteUpState
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.components.CollectionDialogComponent
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.ExportDialogComponent
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.util.smoothGradient
import com.github.zly2006.zhihu.viewmodel.AndroidArticleViewModelRuntime
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleActionsMenu(
    article: Article,
    viewModel: ArticleViewModel,
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
    onSummaryRequest: () -> Unit,
    onExportRequest: () -> Unit,
) {
    val articleActionsRuntime = rememberArticleActionsRuntime()

    ArticleActionsMenuSheet(
        showMenu = showMenu,
        ttsState = articleActionsRuntime.ttsState,
        onDismissRequest = onDismissRequest,
        onToggleSpeech = {
            onDismissRequest()
            articleActionsRuntime.toggleSpeech(viewModel.title, viewModel.content)
        },
        onShareRequest = {
            onDismissRequest()
            articleActionsRuntime.shareArticle(article, viewModel.questionId, viewModel.title, viewModel.authorName)
        },
        onSummaryRequest = {
            onDismissRequest()
            onSummaryRequest()
        },
        onCopyLinkRequest = {
            onDismissRequest()
            articleActionsRuntime.copyArticleLink(article, viewModel.questionId, viewModel.title, viewModel.authorName)
        },
        onExportRequest = {
            onDismissRequest()
            onExportRequest()
        },
        onOpenInBrowserRequest = {
            onDismissRequest()
            articleActionsRuntime.openArticleInBrowser(article)
        },
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun ArticleScreen(
    article: Article,
    viewModel: ArticleViewModel,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val articleRuntime = remember(context) { AndroidArticleViewModelRuntime(context) }
    val articleHost = context.articleHost()
    val backStackEntry by articleHost?.articleNavController?.currentBackStackEntryAsState()
        ?: remember { mutableStateOf(null) }

    val scrollState = rememberScrollState()
    val articleSettings = rememberArticleScreenSettingsState()
    val readHistoryRecorder = rememberArticleReadHistoryRecorder()
    val userMessages = rememberUserMessageSink()

    var previousScrollValue by remember { mutableIntStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val scrollDeltaThreshold = with(density) { ScrollThresholdDp.toPx() }
    var topBarHeight by remember { mutableIntStateOf(0) }
    var showComments by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDoubleTapActionDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Follow-the-finger bar hide: pixel-based offsets driven by scroll delta
    val topBarOffset = remember { Animatable(0f) }
    val bottomBarOffset = remember { Animatable(0f) }
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    var bottomBarHeightPx by remember { mutableFloatStateOf(0f) }
    var previousScrollForBarOffset by remember { mutableIntStateOf(0) }
    var isBarSnapping by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        readHistoryRecorder.addReadHistory(article)
    }

    fun upVoteFromDoubleTap() {
        if (viewModel.voteUpState != VoteUpState.Up) {
            viewModel.toggleVoteUp(articleRuntime, VoteUpState.Up)
        }
    }

    fun performAnswerDoubleTapAction(action: AnswerDoubleTapAction) {
        when (action) {
            AnswerDoubleTapAction.None -> Unit
            AnswerDoubleTapAction.Ask -> showDoubleTapActionDialog = true
            AnswerDoubleTapAction.VoteUp -> upVoteFromDoubleTap()
            AnswerDoubleTapAction.OpenComments -> showComments = true
        }
    }

    fun saveAnswerDoubleTapAction(action: AnswerDoubleTapAction) {
        articleSettings.saveAnswerDoubleTapAction(action)
    }

    fun handleAnswerDoubleTap() {
        if (article.type != ArticleType.Answer) return
        performAnswerDoubleTapAction(articleSettings.answerDoubleTapAction)
    }

    val answerDoubleTapModifier = if (
        article.type == ArticleType.Answer &&
        articleSettings.answerDoubleTapAction != AnswerDoubleTapAction.None
    ) {
        Modifier.pointerInput(articleSettings.answerDoubleTapAction) {
            detectTapGestures(
                onDoubleTap = { handleAnswerDoubleTap() },
            )
        }
    } else {
        Modifier
    }

    // Reset bar offsets when auto-hide preferences are turned off
    LaunchedEffect(articleSettings.isTitleAutoHide) {
        if (!articleSettings.isTitleAutoHide) topBarOffset.snapTo(0f)
    }
    LaunchedEffect(articleSettings.autoHideArticleBottomBar) {
        if (!articleSettings.autoHideArticleBottomBar) bottomBarOffset.snapTo(0f)
    }

    LaunchedEffect(scrollState.value) {
        val currentScroll = scrollState.value
        val scrollDeltaAbs = abs(currentScroll - previousScrollValue)
        if (scrollDeltaAbs > scrollDeltaThreshold) {
            isScrollingUp = currentScroll < previousScrollValue
            previousScrollValue = currentScroll
        }

        // Skip bar offset tracking during snap animation (scroll is driven programmatically)
        if (!isBarSnapping) {
            val delta = currentScroll - previousScrollForBarOffset
            val atTop = currentScroll == 0
            val atBottom = currentScroll >= scrollState.maxValue

            // Top bar: force show at top; content-like reveal at bottom
            if (atTop) {
                topBarOffset.snapTo(0f)
            } else if (articleSettings.isTitleAutoHide && topBarHeightPx > 0f) {
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
            } else if (articleSettings.autoHideArticleBottomBar && bottomBarHeightPx > 0f) {
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
            viewModel.rememberedScrollY = currentScroll
        }
        if (currentScroll == viewModel.rememberedScrollY && scrollState.maxValue != Int.MAX_VALUE) {
            viewModel.rememberedScrollYSync = true
        }
    }

    // Snap bars to fully visible or fully hidden when scrolling stops,
    // and animate content scroll to follow the snap
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {
            val topTarget = if (articleSettings.isTitleAutoHide && topBarHeightPx > 0f) {
                if (abs(topBarOffset.value) > topBarHeightPx / 2) -topBarHeightPx else 0f
            } else {
                topBarOffset.value
            }

            val bottomTarget = if (articleSettings.autoHideArticleBottomBar && bottomBarHeightPx > 0f) {
                if (bottomBarOffset.value > bottomBarHeightPx / 2) bottomBarHeightPx else 0f
            } else {
                bottomBarOffset.value
            }

            // Only compensate scroll for the top bar near the top
            // Bottom bar: no scroll compensation (distance-based reveal handles it)
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

    LaunchedEffect(article.id) {
        viewModel.loadArticle(articleRuntime)
        viewModel.loadCollections(articleRuntime)
    }

    // Master-style bar visibility (direction-based, used when true is false)
    val showTopBar by remember {
        derivedStateOf {
            val canScroll = scrollState.maxValue > topBarHeight
            val isNearTop = scrollState.value < topBarHeight
            when {
                !articleSettings.isTitleAutoHide -> true
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
                !articleSettings.autoHideArticleBottomBar -> true
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

    // 回答切换手势系统
    val sharedData = if (articleHost != null && article.type == ArticleType.Answer) {
        articleHost.articleAnswerSwitchState
    } else {
        null
    }
    val previewWebViewStore = sharedData as? ArticlePreviewWebViewStore

    fun popCurrentAnswerRouteIfNeeded() {
        val navController = articleHost?.articleNavController ?: return
        if (navController.currentBackStackEntry?.hasRoute(Article::class) == true &&
            navController.currentBackStackEntry
                ?.toRoute<Article>()
                ?.type == ArticleType.Answer
        ) {
            navController.popBackStack()
        }
    }

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
        viewModel.loadArticle(articleRuntime)
        viewModel.loadCollections(articleRuntime)
    }

    val navigateToPrevious: () -> Unit = {
        sharedData?.answerTransitionDirection = if (articleSettings.answerSwitchMode == "horizontal") {
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
            if (articleHost != null) {
                popCurrentAnswerRouteIfNeeded()
                navigator.onNavigate(prev.article)
            }
        } else {
            // 无历史时尝试从来源（如收藏夹）向前加载
            sharedData?.pendingInitialContent = sharedData.navigator?.previousAnswerPreview
            sharedData?.promoteForNavigation(sharedData.answerTransitionDirection)
            coroutineScope.launch {
                val prevCached = sharedData?.navigator?.loadPrevious()
                if (prevCached != null) {
                    sharedData.pendingInitialContent = prevCached
                    if (articleHost != null) {
                        popCurrentAnswerRouteIfNeeded()
                        navigator.onNavigate(prevCached.article)
                    }
                }
            }
        }
    }

    val navigateToNext: () -> Unit = {
        sharedData?.answerTransitionDirection = if (articleSettings.answerSwitchMode == "horizontal") {
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
            if (articleHost != null) {
                popCurrentAnswerRouteIfNeeded()
                navigator.onNavigate(historyNext.article)
            }
        } else {
            // 没有前向历史，从导航器加载
            sharedData?.pendingInitialContent = sharedData.navigator?.nextAnswer
            sharedData?.promoteForNavigation(sharedData.answerTransitionDirection)
            coroutineScope.launch {
                val nextArticle = sharedData?.navigator?.loadNext()
                if (nextArticle != null) {
                    if (articleHost != null) {
                        popCurrentAnswerRouteIfNeeded()
                        navigator.onNavigate(nextArticle)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    val answerSwitchContent: @Composable () -> Unit = {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        // 不受到是否收起影响，在topbar最大时是否可以滚动？
        var scrollStateMaxValue by remember { mutableIntStateOf(0) }
        LaunchedEffect(scrollState.maxValue) {
            if (scrollState.maxValue != Int.MAX_VALUE) {
                scrollStateMaxValue = max(scrollState.maxValue, scrollStateMaxValue)
            }
        }
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
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
                    ArticleTopAppBar(
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
                            if (articleSettings.useDuo3ArticleActions) {
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
                            ArticleAuthorRow(
                                expanded = expanded,
                                authorId = viewModel.authorId,
                                authorUrlToken = viewModel.authorUrlToken,
                                authorName = viewModel.authorName,
                                authorAvatarSrc = viewModel.authorAvatarSrc,
                                authorBio = viewModel.authorBio,
                                authorBadge = viewModel.authorBadge,
                            )
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
            },
            bottomBar = {
                // 防止在导航动画和预测性返回手势的过程中，bottom bar闪烁
                val showBottomBarCondition = backStackEntry?.hasRoute(Article::class) == true || articleHost == null

                // Shared composable for the action bar content (gated by useDuo3ArticleActions)
                @Composable
                fun ActionBarContent() {
                    ArticleActionBarContent(
                        useDuo3ArticleActions = articleSettings.useDuo3ArticleActions,
                        voteUpState = viewModel.voteUpState,
                        voteUpCount = viewModel.voteUpCount,
                        isFavorited = viewModel.isFavorited,
                        commentCount = viewModel.commentCount,
                        ttsState = articleHost?.articleTtsState,
                        voteUpIcon = { contentDescription, tint, modifier ->
                            Icon(
                                painter = painterResource(R.drawable.ic_vote_up_24dp),
                                contentDescription = contentDescription,
                                tint = tint ?: LocalContentColor.current,
                                modifier = modifier,
                            )
                        },
                        voteDownIcon = { contentDescription, tint, modifier ->
                            Icon(
                                painter = painterResource(R.drawable.ic_vote_down_24dp),
                                contentDescription = contentDescription,
                                tint = tint ?: LocalContentColor.current,
                                modifier = modifier,
                            )
                        },
                        onVoteUpStateChange = { viewModel.toggleVoteUp(articleRuntime, it) },
                        onCollectionRequest = { showCollectionDialog = true },
                        onStopSpeakingRequest = {
                            articleHost?.stopArticleSpeaking()
                            userMessages.showMessage("已停止朗读")
                        },
                        onCommentsRequest = { showComments = true },
                        onActionsMenuRequest = { showActionsMenu = true },
                    )
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
            CompositionLocalProvider(LocalBringIntoViewSpec provides articleBringIntoViewSpec) {
                Box {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .verticalScroll(scrollState)
                            .padding(innerPadding)
                            .padding(top = 8.dp),
                    ) {
                        ArticleContentArea(
                            hasContent = viewModel.content.isNotEmpty() || viewModel.attachment != null,
                            useWebView = articleSettings.useWebView,
                            pinAnswerDate = articleSettings.pinAnswerDate,
                            ipInfo = viewModel.ipInfo,
                            dateTexts = { DateTexts() },
                            webViewContent = {
                                WebviewComp(
                                    onDoubleTap = ::handleAnswerDoubleTap,
                                    scrollState = scrollState,
//                            existingWebView = sharedData?.getOrCreateMainWebView(context),
                                ) {
                                    it.isVerticalScrollBarEnabled = false
                                    it.setupUpWebviewClient {
                                        if (!viewModel.rememberedScrollYSync) {
                                            coroutineScope.launch {
                                                val rememberedY = viewModel.rememberedScrollY
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
                                        prepareContentDocument(viewModel.content) {
                                            userMessages.showMessage("图片加载失败，请向开发者反馈")
                                        },
                                        viewModel.title,
                                    )
                                }
                            },
                            markdownContent = { header, footer ->
                                RenderMarkdown(
                                    html = viewModel.content,
                                    modifier = answerDoubleTapModifier.fuckHonorService(),
                                    selectable = true,
                                    enableScroll = false,
                                    header = header,
                                    footer = footer,
                                )
                            },
                            footerMediaContent = {
                                if (viewModel.attachment
                                        ?.jsonObject
                                        ?.get("type")
                                        ?.jsonPrimitive
                                        ?.content == "video"
                                ) {
                                    val videoId = viewModel.attachment!!
                                        .jsonObject["attachmentId"]
                                        ?.jsonPrimitive
                                        ?.content
                                        ?.toLongOrNull()
                                    if (videoId != null) {
                                        val thumbnail = viewModel.attachment!!
                                            .jsonObject["video"]!!
                                            .jsonObject["videoInfo"]!!
                                            .jsonObject["thumbnail"]!!
                                            .jsonPrimitive.content
                                        RenderVideoBox(
                                            videoId = videoId,
                                            thumbnailUrl = thumbnail,
                                        )
                                    }
                                }
                            },
                        )
                    }
                    // Skip answer button
                    ArticleSkipAnswerButton(
                        visible = article.type == ArticleType.Answer && articleSettings.buttonSkipAnswer,
                        autoHideSkipAnswerButton = articleSettings.autoHideSkipAnswerButton,
                        isScrollingUp = isScrollingUp,
                        scrollValue = scrollState.value,
                        onNavigateNext = { navigateToNext() },
                    )
                    // Status bar gradient overlay (duo3 only — not needed in master path)
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
    } // end answerSwitchContent

    val nav = sharedData?.navigator
    if (article.type == ArticleType.Answer && articleSettings.answerSwitchMode == "horizontal") {
        // 预加载预览 WebView 内容，确保滑动前 WebView 已渲染完成
        LaunchedEffect(nav?.nextAnswer) {
            val cached = nav?.nextAnswer ?: return@LaunchedEffect
            val wv = previewWebViewStore?.getOrCreatePreviewWebView(context, isNext = true, cached.article.id)
                ?: return@LaunchedEffect
            val articleId = cached.article.id.toString()
            if (wv.contentId != articleId) {
                wv.contentId = articleId
                wv.loadZhihu(
                    "https://www.zhihu.com/answer/${cached.article.id}",
                    prepareContentDocument(cached.content) {
                        userMessages.showMessage("图片加载失败，请向开发者反馈")
                    },
                    viewModel.title,
                )
            }
        }
        LaunchedEffect(nav?.previousAnswer) {
            val cached = nav?.previousAnswer ?: return@LaunchedEffect
            val wv = previewWebViewStore?.getOrCreatePreviewWebView(context, isNext = false, cached.article.id)
                ?: return@LaunchedEffect
            val articleId = cached.article.id.toString()
            if (wv.contentId != articleId) {
                wv.contentId = articleId
                wv.loadZhihu(
                    "https://www.zhihu.com/answer/${cached.article.id}",
                    prepareContentDocument(cached.content) {
                        userMessages.showMessage("图片加载失败，请向开发者反馈")
                    },
                    viewModel.title,
                )
            }
        }
    }
    ArticleAnswerSwitchContainer(
        article = article,
        answerSwitchMode = articleSettings.answerSwitchMode,
        navigator = nav,
        scrollState = scrollState,
        onNavigatePrevious = { navigateToPrevious() },
        onNavigateNext = { navigateToNext() },
        previousContent = nav?.previousAnswer?.let { cached ->
            { CachedAnswerPreview(cached) }
        },
        nextContent = nav?.nextAnswer?.let { cached ->
            { CachedAnswerPreview(cached) }
        },
    ) {
        answerSwitchContent()
    }

    // 全屏菜单
    ArticleActionsMenu(
        article = article,
        viewModel = viewModel,
        showMenu = showActionsMenu,
        onDismissRequest = { showActionsMenu = false },
        onSummaryRequest = {
            showSummaryDialog = true
            viewModel.requestAiSummary(articleRuntime)
        },
        onExportRequest = { showExportDialog = true },
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
            viewModel.requestAiSummary(articleRuntime)
        },
    )

    BackHandler(showActionsMenu) {
        showActionsMenu = false
    }

    // 使用新的收藏夹对话框组件
    CollectionDialogComponent(
        showDialog = showCollectionDialog,
        onDismiss = { showCollectionDialog = false },
        collections = viewModel.collections,
        onLoadCollections = { viewModel.loadCollections(articleRuntime) },
        onToggleFavorite = { collection ->
            viewModel.toggleFavorite(collection.id, collection.isFavorited, articleRuntime)
        },
        onCreateCollection = { title, description, isPublic ->
            viewModel.createNewCollection(articleRuntime, title, description, isPublic)
        },
    )

    CommentScreenComponent(
        showComments = showComments,
        onDismiss = { showComments = false },
        content = article,
    )
    AnswerDoubleTapActionDialog(
        showDialog = showDoubleTapActionDialog,
        onDismissRequest = { showDoubleTapActionDialog = false },
        onActionSelected = { action ->
            showDoubleTapActionDialog = false
            saveAnswerDoubleTapAction(action)
            when (action) {
                AnswerDoubleTapAction.VoteUp -> upVoteFromDoubleTap()
                AnswerDoubleTapAction.OpenComments -> showComments = true
                else -> Unit
            }
            userMessages.showMessage("已将双击回答动作设为：${action.label}")
        },
    )
    // 导出对话框
    ExportDialogComponent(
        showDialog = showExportDialog,
        onDismiss = { showExportDialog = false },
        onExportHtml = { includeAppAttribution, onComplete ->
            viewModel.exportToHtml(context, articleRuntime, includeAppAttribution, onComplete)
        },
        onExportImage = { includeAppAttribution, onComplete ->
            viewModel.exportToImage(context, articleRuntime, includeAppAttribution, onComplete)
        },
        onExportMarkdown = {
            viewModel.exportToClipboard(articleRuntime)
        },
        onExportImageWithComments = { commentCount, includeAppAttribution, onComplete ->
            viewModel.exportToImageWithComments(context, articleRuntime, commentCount, includeAppAttribution, onComplete)
        },
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
    cached: CachedAnswerContent,
) {
    CachedAnswerPreviewContent(
        cached = cached,
        voteUpIcon = {
            Icon(painterResource(R.drawable.ic_vote_up_24dp), "赞同")
        },
        content = { RenderMarkdown(html = it) },
    )
}
