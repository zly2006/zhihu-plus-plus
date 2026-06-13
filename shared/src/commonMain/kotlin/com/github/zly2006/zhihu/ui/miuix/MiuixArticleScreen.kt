/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.platform.PlatformBackHandler
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.ArticleAnswerTransitionDirection
import com.github.zly2006.zhihu.ui.ArticleImmersiveModeEffect
import com.github.zly2006.zhihu.ui.ArticleVideoAttachmentContent
import com.github.zly2006.zhihu.ui.LocalArticleAnswerSwitcher
import com.github.zly2006.zhihu.ui.TtsState
import com.github.zly2006.zhihu.ui.VoteUpState
import com.github.zly2006.zhihu.ui.components.AnswerHorizontalOverscroll
import com.github.zly2006.zhihu.ui.components.AnswerVerticalOverscroll
import com.github.zly2006.zhihu.ui.components.AuthorBadge
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.ExportDialogComponent
import com.github.zly2006.zhihu.ui.components.VerticalReadingProgressBar
import com.github.zly2006.zhihu.ui.components.ZhihuTwoRowsTopAppBar
import com.github.zly2006.zhihu.ui.components.rememberPreferCollapsedExitUntilCollapsedScrollBehavior
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.rememberArticleActionsRuntime
import com.github.zly2006.zhihu.ui.rememberArticleScreenRuntime
import com.github.zly2006.zhihu.ui.rememberArticleScreenSettingsState
import com.github.zly2006.zhihu.ui.voteUpNeutralContent
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.formatArticleDateTime
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import kotlin.math.abs
import androidx.compose.material3.Text as M3Text

/**
 * 回答/文章页的 miuix 版本。
 *
 * 已迁移：加载、顶栏（标题/返回/分享/更多）、作者卡（含徽章）、正文（RenderMarkdown）、底部操作栏
 * （赞同/反对/收藏/朗读停止/评论）、收藏夹选择、评论（复用 M3 CommentScreenComponent）、
 * 标题/底栏自动隐藏、答案切换手势（横/竖）、跳转下一答按钮、双击回答动作、置顶日期、阅读进度条、
 * 更多操作菜单（朗读/AI 总结/复制链接/导出/电脑打开）、AI 总结弹层、导出对话框。
 *
 * 按项目约定，miuix 正文只走 Compose（RenderMarkdown），不接 WebView。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiuixArticleScreen(
    article: Article,
    viewModel: ArticleViewModel,
) {
    val navigator = LocalNavigator.current
    // 回答切换在单个导航 entry 内进行（见 ArticleAnswerSlot）；无 slot 时回退到 push 导航。
    val answerSwitch = LocalArticleAnswerSwitcher.current
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val articleActions = rememberArticleActionsRuntime()
    val settings = rememberSettingsStore()
    val blurEnabled = settings.getBoolean("blurEnabled", true)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = rememberPreferCollapsedExitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val articleScreenRuntime = rememberArticleScreenRuntime()
    val articleHost = articleScreenRuntime.articleHost
    val articleSettings = rememberArticleScreenSettingsState()
    val answerSwitchMode = articleSettings.answerSwitchMode
    val sharedData = if (article.type == ArticleType.Answer) environment.articleAnswerSwitchState() else null
    var isImmersiveMode by remember(sharedData) {
        mutableStateOf(sharedData?.isImmersiveMode ?: false)
    }
    val toggleImmersive: () -> Unit = { isImmersiveMode = !isImmersiveMode }
    val userMessages = rememberUserMessageSink()
    val haptic = LocalHapticFeedback.current
    var showComments by remember { mutableStateOf(false) }
    val showCollections = remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    var showCreateCollection by remember { mutableStateOf(false) }
    var newCollectionTitle by remember { mutableStateOf("") }
    var newCollectionPublic by remember { mutableStateOf(false) }
    var showSummarySheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDoubleTapActionDialog by remember { mutableStateOf(false) }
    var navigatingToNextAnswer by remember { mutableStateOf(false) }

    // 双击回答动作（对齐 M3）：点赞/打开评论/询问并记住。
    fun upVoteFromDoubleTap() {
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        if (viewModel.voteUpState != VoteUpState.Up) viewModel.toggleVoteUp(environment, VoteUpState.Up)
    }

    fun handleAnswerDoubleTap() {
        if (article.type != ArticleType.Answer) return
        when (articleSettings.answerDoubleTapAction) {
            AnswerDoubleTapAction.None -> Unit
            AnswerDoubleTapAction.Ask -> showDoubleTapActionDialog = true
            AnswerDoubleTapAction.VoteUp -> upVoteFromDoubleTap()
            AnswerDoubleTapAction.OpenComments -> {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                showComments = true
            }
            AnswerDoubleTapAction.ToggleImmersive -> {
                toggleImmersive()
            }
        }
    }
    val answerDoubleTapModifier = if (
        article.type == ArticleType.Answer &&
        articleSettings.answerDoubleTapAction != AnswerDoubleTapAction.None
    ) {
        Modifier.pointerInput(articleSettings.answerDoubleTapAction) {
            detectTapGestures(onDoubleTap = { handleAnswerDoubleTap() })
        }
    } else {
        Modifier
    }

    // 标题/底栏自动隐藏：方向驱动的显隐（对齐 M3 的 master-style showTopBar/showBottomBar）。
    // articleSettings.* 是 mutableStateOf 且监听 key 变化，直接在 derivedStateOf 内读取保持响应式。
    var previousScrollValue by remember { mutableIntStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(false) }
    LaunchedEffect(scrollState.value) {
        val cur = scrollState.value
        if (abs(cur - previousScrollValue) > 12) {
            isScrollingUp = cur < previousScrollValue
            previousScrollValue = cur
        }
    }
    val showTopBar by remember {
        derivedStateOf {
            when {
                !articleSettings.isTitleAutoHide -> true
                scrollState.maxValue <= 0 -> true
                isScrollingUp -> true
                scrollState.value < 100 -> true
                else -> false
            }
        }
    }
    val showBottomBar by remember {
        derivedStateOf {
            when {
                !articleSettings.autoHideArticleBottomBar -> true
                scrollState.maxValue <= 0 -> true
                isScrollingUp -> true
                scrollState.value == 0 -> true
                else -> false
            }
        }
    }
    LaunchedEffect(sharedData, isImmersiveMode) {
        if (sharedData != null) sharedData.isImmersiveMode = isImmersiveMode
    }
    ArticleImmersiveModeEffect(isImmersiveMode)

    LaunchedEffect(article.id) {
        // 答案切换时用 pendingInitialContent 预填充，消除空白帧（逻辑同 M3 ArticleScreen）。
        if (sharedData != null) {
            if (!sharedData.navigatingFromAnswerSwitch) sharedData.reset()
            sharedData.navigatingFromAnswerSwitch = false
            sharedData.answerTransitionDirection = ArticleAnswerTransitionDirection.DEFAULT
            sharedData.pendingInitialContent?.let { pending ->
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

    val nav = sharedData?.navigator
    val navigateToPrevious: () -> Unit = {
        sharedData?.answerTransitionDirection = if (answerSwitchMode == "horizontal") {
            ArticleAnswerTransitionDirection.HORIZONTAL_PREVIOUS
        } else {
            ArticleAnswerTransitionDirection.VERTICAL_PREVIOUS
        }
        sharedData?.navigatingFromAnswerSwitch = true
        sharedData?.navigator?.pushAnswer(viewModel.toCachedContent(sourceLabel = sharedData.navigator?.sourceName ?: "此问题"))
        val prev = sharedData?.navigator?.goToPrevious()
        if (prev != null) {
            sharedData.pendingInitialContent = prev
            sharedData.promoteForNavigation(sharedData.answerTransitionDirection)
            answerSwitch?.invoke(prev.article, sharedData.answerTransitionDirection)
                ?: navigator.onNavigate(prev.article)
        } else {
            sharedData?.pendingInitialContent = sharedData.navigator?.previousAnswerPreview
            sharedData?.promoteForNavigation(sharedData.answerTransitionDirection)
            coroutineScope.launch {
                val prevCached = sharedData?.navigator?.loadPrevious()
                if (prevCached != null) {
                    sharedData.pendingInitialContent = prevCached
                    answerSwitch?.invoke(prevCached.article, sharedData.answerTransitionDirection)
                        ?: navigator.onNavigate(prevCached.article)
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
        sharedData?.navigator?.pushAnswer(viewModel.toCachedContent(sourceLabel = sharedData.navigator?.sourceName ?: "此问题"))
        val historyNext = sharedData?.navigator?.goToNext()
        if (historyNext != null) {
            sharedData.pendingInitialContent = historyNext
            sharedData.promoteForNavigation(sharedData.answerTransitionDirection)
            answerSwitch?.invoke(historyNext.article, sharedData.answerTransitionDirection)
                ?: navigator.onNavigate(historyNext.article)
        } else {
            sharedData?.pendingInitialContent = sharedData.navigator?.nextAnswer
            sharedData?.promoteForNavigation(sharedData.answerTransitionDirection)
            coroutineScope.launch {
                val nextArticle = sharedData?.navigator?.loadNext()
                if (nextArticle != null) {
                    answerSwitch?.invoke(nextArticle, sharedData.answerTransitionDirection)
                        ?: navigator.onNavigate(nextArticle)
                }
            }
        }
    }

    val articleScaffold: @Composable () -> Unit = {
        Scaffold(
            // 顶栏折叠（两行→一行）由 scrollBehavior 驱动，必须把其 nestedScrollConnection 挂到滚动祖先上，
            // 否则内容滚动喂不到 heightOffset，两行大标题永不折叠。
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isImmersiveMode) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier),
            topBar = {
                AnimatedVisibility(
                    visible = !isImmersiveMode && showTopBar,
                    enter = slideInVertically(tween(200)) { -it },
                    exit = slideOutVertically(tween(200)) { -it },
                ) {
                    ZhihuTwoRowsTopAppBar(
                        // 顶栏对齐 M3：展开 headlineMedium / 折叠 titleLarge；容器透明以透出 miuix 模糊背景
                        modifier = Modifier.installerMiuixBlurEffect(backdrop),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            titleContentColor = MiuixTheme.colorScheme.onBackground,
                            navigationIconContentColor = MiuixTheme.colorScheme.onBackground,
                            actionIconContentColor = MiuixTheme.colorScheme.onBackground,
                        ),
                        scrollBehavior = scrollBehavior,
                        navigationIcon = {
                            IconButton(onClick = navigator.onNavigateBack) {
                                Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                articleActions.shareArticle(article, viewModel.questionId, viewModel.title, viewModel.authorName)
                            }) {
                                Icon(Icons.Default.Share, "分享", tint = MiuixTheme.colorScheme.onBackground)
                            }
                        },
                        title = { expanded ->
                            // 回答页标题可点击跳到问题页（对齐 M3）
                            M3Text(
                                text = viewModel.title.ifEmpty {
                                    if (article.type == ArticleType.Answer) "回答" else "文章"
                                },
                                modifier = if (article.type == ArticleType.Answer) {
                                    Modifier.clickable {
                                        navigator.onNavigate(Question(viewModel.questionId, viewModel.title))
                                    }
                                } else {
                                    Modifier
                                },
                                maxLines = if (expanded) Int.MAX_VALUE else 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = !isImmersiveMode && showBottomBar,
                    enter = slideInVertically(tween(200)) { it },
                    exit = slideOutVertically(tween(200)) { it },
                ) {
                    Row(
                        // 去掉底栏整体背景，只保留各按钮自身的药丸背景。
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 底栏所有操作按钮统一高度，避免评论/赞同与图标按钮高度不一致。
                        val barH = 40.dp
                        // 赞同/反对药丸（对齐 M3：可分别切换，激活态蓝底白字）。
                        val up = viewModel.voteUpState == VoteUpState.Up
                        val down = viewModel.voteUpState == VoteUpState.Down
                        Row(
                            modifier = Modifier.height(barH).clip(RoundedCornerShape(50)).background(MiuixTheme.colorScheme.surfaceContainerHigh),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(if (up) voteUpNeutralContent() else Color.Transparent)
                                    .clickable { viewModel.toggleVoteUp(environment, if (up) VoteUpState.Neutral else VoteUpState.Up) }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.ThumbUp, "赞同", modifier = Modifier.size(18.dp), tint = if (up) Color.White else MiuixTheme.colorScheme.onSurface)
                                Spacer(Modifier.width(4.dp))
                                Text(viewModel.voteUpCount.toString(), color = if (up) Color.White else MiuixTheme.colorScheme.onSurface, fontSize = 13.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(if (down) voteUpNeutralContent() else Color.Transparent)
                                    .clickable { viewModel.toggleVoteUp(environment, if (down) VoteUpState.Neutral else VoteUpState.Down) }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.ThumbDown, "反对", modifier = Modifier.size(18.dp), tint = if (down) Color.White else MiuixTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // 收藏
                        Box(
                            modifier = Modifier
                                .size(barH)
                                .clip(RoundedCornerShape(50))
                                .background(if (viewModel.isFavorited) voteUpNeutralContent() else MiuixTheme.colorScheme.surfaceContainerHigh)
                                .clickable { showCollections.value = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (viewModel.isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                "收藏",
                                modifier = Modifier.size(20.dp),
                                tint = if (viewModel.isFavorited) Color.White else MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        // 朗读中显示停止按钮（对齐 M3）
                        AnimatedVisibility(visible = articleActions.ttsState.isSpeaking) {
                            Box(
                                modifier = Modifier
                                    .size(barH)
                                    .clip(RoundedCornerShape(50))
                                    .background(voteUpNeutralContent())
                                    .clickable {
                                        articleActions.toggleSpeech(viewModel.title, viewModel.content)
                                        userMessages.showMessage("已停止朗读")
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeOff, "停止朗读", modifier = Modifier.size(20.dp), tint = Color.White)
                            }
                        }
                        // 评论
                        Row(
                            modifier = Modifier
                                .height(barH)
                                .clip(RoundedCornerShape(50))
                                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                .clickable { showComments = true }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Comment, "评论", modifier = Modifier.size(18.dp), tint = MiuixTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(4.dp))
                            Text(viewModel.commentCount.toString(), color = MiuixTheme.colorScheme.onSurface, fontSize = 13.sp)
                        }
                        // 更多操作（朗读/总结/复制/导出/电脑打开）
                        Box(
                            modifier = Modifier
                                .size(barH)
                                .clip(RoundedCornerShape(50))
                                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                .clickable { showActionsMenu = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.MoreVert, "更多操作", modifier = Modifier.size(20.dp), tint = MiuixTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(innerPadding)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    // 作者卡（正文之前）
                    if (viewModel.authorName.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navigator.onNavigate(
                                            Person(id = viewModel.authorId, urlToken = viewModel.authorUrlToken, name = viewModel.authorName),
                                        )
                                    }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (viewModel.authorAvatarSrc.isNotEmpty()) {
                                    AsyncImage(viewModel.authorAvatarSrc, "头像", modifier = Modifier.size(44.dp).clip(CircleShape))
                                    Spacer(Modifier.width(12.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            viewModel.authorName,
                                            style = MiuixTheme.textStyles.title4,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        if (viewModel.authorBadge != null) {
                                            Spacer(Modifier.width(4.dp))
                                            AuthorBadge(badge = viewModel.authorBadge)
                                        }
                                    }
                                    if (viewModel.authorBio.isNotEmpty()) {
                                        Text(
                                            viewModel.authorBio,
                                            style = MiuixTheme.textStyles.footnote1,
                                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // 正文内容卡
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 置顶日期：置顶模式下日期放在内容顶部
                            if (articleSettings.pinAnswerDate && viewModel.createdAt > 0) {
                                Text(
                                    "发布于 " + formatArticleDateTime(viewModel.createdAt),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                )
                                if (viewModel.createdAt != viewModel.updatedAt) {
                                    Text(
                                        "编辑于 " + formatArticleDateTime(viewModel.updatedAt),
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            if (viewModel.content.isEmpty()) {
                                Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                Box(modifier = answerDoubleTapModifier) {
                                    RenderMarkdown(html = viewModel.content, selectable = true, enableScroll = false)
                                }
                            }
                            // 视频附件（type=video）入口，对齐 M3
                            ArticleVideoAttachmentContent(viewModel.attachment)
                            // 非置顶日期 + IP属地 放在内容底部
                            if (!articleSettings.pinAnswerDate && viewModel.createdAt > 0) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "发布于 " + formatArticleDateTime(viewModel.createdAt),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                )
                                if (viewModel.createdAt != viewModel.updatedAt) {
                                    Text(
                                        "编辑于 " + formatArticleDateTime(viewModel.updatedAt),
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    )
                                }
                            }
                            if (viewModel.ipInfo != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "IP属地：${viewModel.ipInfo}",
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                // 右侧阅读进度条（对齐 M3）。
                VerticalReadingProgressBar(
                    scrollState = scrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp)
                        .then(if (isImmersiveMode) Modifier.graphicsLayer { alpha = 0f } else Modifier),
                )
            }
        }
    }

    // 回答页滑动切换上/下一答（逻辑同 M3）。overscroll 包裹整个 Scaffold，预览覆盖全屏不被底栏遮挡。
    // 取色由顶层 ZhihuMiuixTheme 提供的 MaterialTheme（miuix 派生）兜底，预览组件直接用 miuix 配色。
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            article.type == ArticleType.Answer && answerSwitchMode == "vertical" ->
                AnswerVerticalOverscroll(
                    previousAnswer = nav?.previousAnswer,
                    nextAnswer = nav?.nextAnswer,
                    onNavigatePrevious = navigateToPrevious,
                    onNavigateNext = navigateToNext,
                    isAtTop = { scrollState.value == 0 },
                    isAtBottom = { scrollState.value >= scrollState.maxValue },
                    scrollState = scrollState,
                    previewCard = { authorName, excerpt, avatarUrl, label, icon, isTriggered, progress, reverseLayout, modifier ->
                        MiuixAnswerPreviewCard(authorName, excerpt, avatarUrl, label, icon, isTriggered, progress, reverseLayout, modifier)
                    },
                ) { articleScaffold() }

            article.type == ArticleType.Answer && answerSwitchMode == "horizontal" ->
                AnswerHorizontalOverscroll(
                    canGoPrevious = nav?.previousAnswer != null,
                    canGoNext = nav?.nextAnswer != null,
                    onNavigatePrevious = navigateToPrevious,
                    onNavigateNext = navigateToNext,
                    previousContent = nav?.previousAnswer?.let { cached -> { MiuixCachedAnswerPreview(cached) } },
                    nextContent = nav?.nextAnswer?.let { cached -> { MiuixCachedAnswerPreview(cached) } },
                ) { articleScaffold() }

            else -> articleScaffold()
        }

        // 跳到下一个回答（对齐 M3）：仅回答页 + buttonSkipAnswer 开启时显示，可自动隐藏。
        if (article.type == ArticleType.Answer && articleSettings.buttonSkipAnswer && !isImmersiveMode) {
            val showSkip = !articleSettings.autoHideSkipAnswerButton || isScrollingUp || scrollState.value == 0
            val skipAlpha by animateFloatAsState(if (showSkip) 1f else 0f, tween(200), label = "skipAlpha")
            var fabClickCount by remember { mutableIntStateOf(0) }
            LaunchedEffect(fabClickCount) {
                if (fabClickCount > 0) {
                    delay(350)
                    if (fabClickCount >= 2) {
                        toggleImmersive()
                    } else if (showSkip) {
                        navigatingToNextAnswer = true
                        navigateToNext()
                        navigatingToNextAnswer = false
                    }
                    fabClickCount = 0
                }
            }
            DraggableRefreshButton(
                modifier = Modifier.graphicsLayer { alpha = skipAlpha },
                onClick = { fabClickCount++ },
                preferenceName = "buttonSkipAnswer",
            ) {
                if (navigatingToNextAnswer) {
                    CircularProgressIndicator(modifier = Modifier.size(30.dp))
                } else {
                    Icon(Icons.Default.SkipNext, "下一个回答", tint = MiuixTheme.colorScheme.onSurface)
                }
            }
        }
    }

    // 评论区暂未 miuix 化，复用 M3 CommentScreenComponent（与想法/问题页一致）。
    CommentScreenComponent(showComments = showComments, onDismiss = { showComments = false }, content = article)

    // 收藏夹选择（始终留在树里，show 用 MutableState）。
    WindowBottomSheet(
        show = showCollections.value,
        onDismissRequest = { showCollections.value = false },
        title = "收藏到收藏夹",
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            // 新建收藏夹（对齐 M3 的 onCreateCollection）。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreateCollection = !showCreateCollection }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Add, "新建", tint = MiuixTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("新建收藏夹", color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            }
            AnimatedVisibility(visible = showCreateCollection) {
                Column {
                    TextField(
                        newCollectionTitle,
                        { newCollectionTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "收藏夹名称",
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("公开", color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Switch(checked = newCollectionPublic, onCheckedChange = { newCollectionPublic = it })
                    }
                    TextButton(
                        text = "创建",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val title = newCollectionTitle.trim()
                            if (title.isNotEmpty()) {
                                viewModel.createNewCollection(environment, title, "", newCollectionPublic)
                                newCollectionTitle = ""
                                newCollectionPublic = false
                                showCreateCollection = false
                            }
                        },
                    )
                }
            }

            if (viewModel.collections.isEmpty()) {
                Text(
                    "暂无收藏夹",
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                viewModel.collections.forEach { collection ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleFavorite(collection.id, collection.isFavorited, environment) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (collection.isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            null,
                            tint = MiuixTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(collection.title, color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // 更多操作菜单（对齐 M3 ArticleActionsMenu）：朗读 / 总结 / 复制链接 / 导出 / 电脑打开。
    WindowBottomSheet(
        show = showActionsMenu,
        onDismissRequest = { showActionsMenu = false },
        title = "更多操作",
    ) {
        val speaking = articleActions.ttsState.isSpeaking
        val ttsEnabled = articleActions.ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            MiuixActionMenuRow(
                if (speaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                if (speaking) "停止朗读" else "开始朗读",
                enabled = ttsEnabled,
            ) {
                showActionsMenu = false
                articleActions.toggleSpeech(viewModel.title, viewModel.content)
            }
            MiuixActionMenuRow(Icons.Default.Summarize, "总结本文") {
                showActionsMenu = false
                showSummarySheet = true
                viewModel.requestAiSummary(environment)
            }
            MiuixActionMenuRow(Icons.Default.ContentCopy, "复制链接") {
                showActionsMenu = false
                articleActions.copyArticleLink(article, viewModel.questionId, viewModel.title, viewModel.authorName)
            }
            MiuixActionMenuRow(Icons.Default.GetApp, "导出文章 (Markdown、图片、HTML、PDF)") {
                showActionsMenu = false
                showExportDialog = true
            }
            MiuixActionMenuRow(Icons.Default.DesktopWindows, "在电脑中打开") {
                showActionsMenu = false
                articleActions.openArticleInBrowser(article)
            }
            MiuixActionMenuRow(Icons.Default.FilterCenterFocus, "沉浸式阅读") {
                showActionsMenu = false
                toggleImmersive()
                userMessages.showMessage("已进入沉浸式，按返回键即可退出")
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // AI 总结弹层（对齐 M3 ArticleSummarySheet）。
    WindowBottomSheet(
        show = showSummarySheet,
        onDismissRequest = {
            showSummarySheet = false
            viewModel.cancelAiSummary()
        },
        title = "总结本文",
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            if (viewModel.aiSummaryLoading && viewModel.aiSummaryText.isBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text("正在生成总结...", color = MiuixTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(12.dp))
            }
            if (viewModel.aiSummaryText.isNotBlank()) {
                Text(viewModel.aiSummaryText, color = MiuixTheme.colorScheme.onSurface)
            }
            viewModel.aiSummaryError?.takeIf { it.isNotBlank() }?.let {
                if (viewModel.aiSummaryText.isNotBlank()) Spacer(Modifier.height(12.dp))
                Text(it, color = MiuixTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!viewModel.aiSummaryLoading) {
                    TextButton(text = "重新总结", onClick = { viewModel.requestAiSummary(environment) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // 双击回答动作配置（对齐 M3）。
    WindowBottomSheet(
        show = showDoubleTapActionDialog,
        onDismissRequest = { showDoubleTapActionDialog = false },
        title = "设置双击回答动作",
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "选择以后双击回答时默认执行的动作，选择后会立即保存到设置。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
            )
            val applyDoubleTap: (AnswerDoubleTapAction) -> Unit = { action ->
                showDoubleTapActionDialog = false
                articleSettings.saveAnswerDoubleTapAction(action)
                userMessages.showMessage("已将双击回答动作设为：${action.label}")
            }
            TextButton(text = "无操作", modifier = Modifier.fillMaxWidth(), onClick = { applyDoubleTap(AnswerDoubleTapAction.None) })
            TextButton(text = "点赞", modifier = Modifier.fillMaxWidth(), onClick = {
                applyDoubleTap(AnswerDoubleTapAction.VoteUp)
                upVoteFromDoubleTap()
            })
            TextButton(text = "打开评论区", modifier = Modifier.fillMaxWidth(), onClick = {
                applyDoubleTap(AnswerDoubleTapAction.OpenComments)
                showComments = true
            })
            TextButton(text = "开关沉浸式", modifier = Modifier.fillMaxWidth(), onClick = {
                applyDoubleTap(AnswerDoubleTapAction.ToggleImmersive)
                toggleImmersive()
            })
            Spacer(Modifier.height(8.dp))
        }
    }

    // 导出对话框（复用 M3 共享组件）。
    ExportDialogComponent(
        showDialog = showExportDialog,
        onDismiss = { showExportDialog = false },
        onExportHtml = { includeAppAttribution, onComplete -> viewModel.exportToHtml(environment, includeAppAttribution, onComplete) },
        onExportImage = { includeAppAttribution, onComplete -> viewModel.exportToImage(environment, includeAppAttribution, onComplete) },
        onExportMarkdown = { viewModel.exportToClipboard(environment) },
        onExportImageWithComments = { commentCount, includeAppAttribution, onComplete ->
            viewModel.exportToImageWithComments(environment, commentCount, includeAppAttribution, onComplete)
        },
    )

    PlatformBackHandler(enabled = isImmersiveMode) { toggleImmersive() }
    PlatformBackHandler(showActionsMenu) { showActionsMenu = false }
}

/** 更多操作菜单的单行（图标 + 文字，整行可点）。 */
@Composable
private fun MiuixActionMenuRow(
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (enabled) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, color = tint, modifier = Modifier.weight(1f))
    }
}

/** 横向滑动切换时的全屏预览（miuix 样式），渲染缓存的上/下一答内容。 */
@Composable
private fun MiuixCachedAnswerPreview(cached: CachedAnswerContent) {
    // 拖拽预览要与正文页“同形”：复刻 articleScaffold 的顶栏 + 作者卡 + 正文卡 + 底部操作栏（均不可交互、不带模糊）。
    val barH = 40.dp
    Scaffold(
        // 不指定 containerColor，与正文页 articleScaffold 的 Scaffold 一致（默认 MaterialTheme.colorScheme.background，
        // miuix 暗色下即黑色背景）；卡片仍由 miuix Card 用原 surface 色，整页与真页同色。
        topBar = {
            // 与正文页 ZhihuTwoRowsTopAppBar 的“展开态”一致：上行图标 + 下方完整大标题（headlineMedium，不折叠）。
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {}) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Share, "分享", tint = MiuixTheme.colorScheme.onBackground)
                    }
                }
                M3Text(
                    text = cached.title.ifEmpty { "回答" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .height(barH)
                        .clip(RoundedCornerShape(50))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.ThumbUp, "赞同", modifier = Modifier.size(18.dp), tint = MiuixTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(4.dp))
                    Text(cached.voteUpCount.toString(), color = MiuixTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .height(barH)
                        .clip(RoundedCornerShape(50))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Comment, "评论", modifier = Modifier.size(18.dp), tint = MiuixTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(4.dp))
                    Text(cached.commentCount.toString(), color = MiuixTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (cached.authorName.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (cached.authorAvatarUrl.isNotEmpty()) {
                            AsyncImage(cached.authorAvatarUrl, "头像", modifier = Modifier.size(44.dp).clip(CircleShape))
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    cached.authorName,
                                    style = MiuixTheme.textStyles.title4,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (cached.authorBadge != null) {
                                    Spacer(Modifier.width(4.dp))
                                    AuthorBadge(badge = cached.authorBadge)
                                }
                            }
                            if (cached.authorBio.isNotEmpty()) {
                                Text(
                                    cached.authorBio,
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (cached.content.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        RenderMarkdown(html = cached.content, selectable = false, enableScroll = false)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** miuix 样式的上/下一答预览卡片，替代 M3 版 AnswerPreviewCard（避免 M3 圆角/密度/取色在 miuix 下违和）。 */
@Composable
private fun MiuixAnswerPreviewCard(
    authorName: String,
    excerpt: String,
    avatarUrl: String,
    label: String,
    icon: ImageVector,
    isTriggered: Boolean,
    progress: Float,
    reverseLayout: Boolean,
    modifier: Modifier,
) {
    val accent = if (isTriggered) voteUpNeutralContent() else MiuixTheme.colorScheme.onSurfaceSecondary
    val bgColor = if (isTriggered) voteUpNeutralContent().copy(alpha = 0.16f) else MiuixTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .alpha(progress.coerceIn(0f, 1f))
            .padding(12.dp),
    ) {
        val labelRow: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, label, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isTriggered) "松手切换" else label,
                    style = MiuixTheme.textStyles.footnote1,
                    color = accent,
                    fontWeight = if (isTriggered) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
        val authorRow: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (avatarUrl.isNotEmpty()) {
                    AsyncImage(avatarUrl, "头像", modifier = Modifier.size(28.dp).clip(CircleShape))
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    if (authorName.isNotEmpty()) {
                        Text(authorName, style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.onSurface)
                    }
                    if (excerpt.isNotEmpty()) {
                        Text(
                            excerpt,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Column {
            if (reverseLayout) {
                authorRow()
                Spacer(Modifier.height(6.dp))
                labelRow()
            } else {
                labelRow()
                Spacer(Modifier.height(6.dp))
                authorRow()
            }
        }
    }
}
