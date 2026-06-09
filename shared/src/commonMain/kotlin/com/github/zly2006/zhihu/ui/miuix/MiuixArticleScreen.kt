/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.ArticleAnswerTransitionDirection
import com.github.zly2006.zhihu.ui.VoteUpState
import com.github.zly2006.zhihu.ui.components.AnswerHorizontalOverscroll
import com.github.zly2006.zhihu.ui.components.AnswerVerticalOverscroll
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.VerticalReadingProgressBar
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.rememberArticleActionsRuntime
import com.github.zly2006.zhihu.ui.rememberArticleScreenRuntime
import com.github.zly2006.zhihu.ui.rememberArticleScreenSettingsState
import com.github.zly2006.zhihu.ui.voteUpNeutralContent
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.formatArticleDateTime
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import kotlin.math.abs

/**
 * 回答/文章页的 miuix 版本（阶段一骨架）。
 *
 * 已迁移：加载、顶栏（标题/返回/分享）、作者卡、正文（RenderMarkdown）、底部操作栏
 * （赞同/评论/收藏/分享）、收藏夹选择 BottomSheet、评论（暂复用 M3 CommentScreenComponent）。
 *
 * 待后续阶段：标题自动隐藏、底栏自动隐藏、跳转下一答、双击手势、答案切换手势、置顶日期、
 * WebView 正文、AI 摘要、语音朗读等高级交互。
 */
@Composable
fun MiuixArticleScreen(
    article: Article,
    viewModel: ArticleViewModel,
) {
    val navigator = LocalNavigator.current
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val articleActions = rememberArticleActionsRuntime()
    val settings = rememberSettingsStore()
    val blurEnabled = settings.getBoolean("blurEnabled", true)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val articleScreenRuntime = rememberArticleScreenRuntime()
    val articleHost = articleScreenRuntime.articleHost
    val articleSettings = rememberArticleScreenSettingsState()
    val answerSwitchMode = articleSettings.answerSwitchMode
    val sharedData = if (article.type == ArticleType.Answer) environment.articleAnswerSwitchState() else null
    var showComments by remember { mutableStateOf(false) }
    val showCollections = remember { mutableStateOf(false) }

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
            popCurrentAnswer(articleHost?.articleNavController)
            navigator.onNavigate(prev.article)
        } else {
            sharedData?.pendingInitialContent = sharedData.navigator?.previousAnswerPreview
            sharedData?.promoteForNavigation(sharedData.answerTransitionDirection)
            coroutineScope.launch {
                val prevCached = sharedData?.navigator?.loadPrevious()
                if (prevCached != null) {
                    sharedData.pendingInitialContent = prevCached
                    popCurrentAnswer(articleHost?.articleNavController)
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
        sharedData?.navigator?.pushAnswer(viewModel.toCachedContent(sourceLabel = sharedData.navigator?.sourceName ?: "此问题"))
        val historyNext = sharedData?.navigator?.goToNext()
        if (historyNext != null) {
            sharedData.pendingInitialContent = historyNext
            sharedData.promoteForNavigation(sharedData.answerTransitionDirection)
            popCurrentAnswer(articleHost?.articleNavController)
            navigator.onNavigate(historyNext.article)
        } else {
            sharedData?.pendingInitialContent = sharedData.navigator?.nextAnswer
            sharedData?.promoteForNavigation(sharedData.answerTransitionDirection)
            coroutineScope.launch {
                val nextArticle = sharedData?.navigator?.loadNext()
                if (nextArticle != null) {
                    popCurrentAnswer(articleHost?.articleNavController)
                    navigator.onNavigate(nextArticle)
                }
            }
        }
    }

    val articleScaffold: @Composable () -> Unit = {
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = showTopBar,
                    enter = slideInVertically(tween(200)) { -it },
                    exit = slideOutVertically(tween(200)) { -it },
                ) {
                    TopAppBar(
                        modifier = Modifier.installerMiuixBlurEffect(backdrop),
                        color = backdrop.getMiuixAppBarColor(),
                        title = viewModel.title.ifEmpty {
                            if (article.type == ArticleType.Answer) "回答" else "文章"
                        },
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
                        scrollBehavior = scrollBehavior,
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(tween(200)) { it },
                    exit = slideOutVertically(tween(200)) { it },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 底栏背景模糊（与顶栏同一 backdrop）。
                            .installerMiuixBlurEffect(backdrop)
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 赞同/反对药丸（对齐 M3：可分别切换，激活态蓝底白字）。
                        val up = viewModel.voteUpState == VoteUpState.Up
                        val down = viewModel.voteUpState == VoteUpState.Down
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(MiuixTheme.colorScheme.surfaceContainerHigh),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (up) voteUpNeutralContent() else Color.Transparent)
                                    .clickable { viewModel.toggleVoteUp(environment, if (up) VoteUpState.Neutral else VoteUpState.Up) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.ThumbUp, "赞同", modifier = Modifier.size(18.dp), tint = if (up) Color.White else MiuixTheme.colorScheme.onSurface)
                                Spacer(Modifier.width(4.dp))
                                Text(viewModel.voteUpCount.toString(), color = if (up) Color.White else MiuixTheme.colorScheme.onSurface, fontSize = 13.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (down) voteUpNeutralContent() else Color.Transparent)
                                    .clickable { viewModel.toggleVoteUp(environment, if (down) VoteUpState.Neutral else VoteUpState.Down) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Icon(Icons.Default.ThumbDown, "反对", modifier = Modifier.size(18.dp), tint = if (down) Color.White else MiuixTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // 收藏
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (viewModel.isFavorited) voteUpNeutralContent() else MiuixTheme.colorScheme.surfaceContainerHigh)
                                .clickable { showCollections.value = true }
                                .padding(10.dp),
                        ) {
                            Icon(
                                if (viewModel.isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                "收藏",
                                modifier = Modifier.size(20.dp),
                                tint = if (viewModel.isFavorited) Color.White else MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        // 评论
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                .clickable { showComments = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Comment, "评论", modifier = Modifier.size(18.dp), tint = MiuixTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(4.dp))
                            Text(viewModel.commentCount.toString(), color = MiuixTheme.colorScheme.onSurface, fontSize = 13.sp)
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(innerPadding)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    // 正文内容卡（优先显示）
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
                                RenderMarkdown(html = viewModel.content, selectable = true, enableScroll = false)
                            }
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

                    // 作者卡（正文之后）
                    if (viewModel.authorName.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
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
                                    Text(viewModel.authorName, style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.Bold)
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
                    }
                    Spacer(Modifier.height(16.dp))
                }
                // 右侧阅读进度条（对齐 M3）。
                VerticalReadingProgressBar(
                    scrollState = scrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
                )
            }
        }
    }

    // 回答页滑动切换上/下一答（逻辑同 M3）。overscroll 包裹整个 Scaffold，预览覆盖全屏不被底栏遮挡。
    // 取色由顶层 ZhihuMiuixTheme 提供的 MaterialTheme（miuix 派生）兜底，预览组件直接用 miuix 配色。
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

    // 评论区暂未 miuix 化，复用 M3 CommentScreenComponent（与想法/问题页一致）。
    CommentScreenComponent(showComments = showComments, onDismiss = { showComments = false }, content = article)

    // 收藏夹选择（始终留在树里，show 用 MutableState）。
    WindowBottomSheet(
        show = showCollections.value,
        onDismissRequest = { showCollections.value = false },
        title = "收藏到收藏夹",
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
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
}

/** 横向滑动切换时的全屏预览（miuix 样式），渲染缓存的上/下一答内容。 */
@Composable
private fun MiuixCachedAnswerPreview(cached: CachedAnswerContent) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        if (cached.title.isNotEmpty()) {
            Text(cached.title, style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (cached.authorAvatarUrl.isNotEmpty()) {
                AsyncImage(cached.authorAvatarUrl, "头像", modifier = Modifier.size(40.dp).clip(CircleShape))
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    cached.authorName,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
        if (cached.content.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            RenderMarkdown(html = cached.content, selectable = false, enableScroll = false)
        }
    }
}

/** 切换答案前把当前答案路由出栈，避免回退时堆叠一长串答案（逻辑同 M3 ArticleScreen）。 */
private fun popCurrentAnswer(navController: NavHostController?) {
    if (navController == null) return
    val currentIsAnswer = runCatching {
        navController.currentBackStackEntry?.toRoute<Article>()?.type == ArticleType.Answer
    }.getOrDefault(false)
    if (currentIsAnswer) navController.popBackStack()
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
