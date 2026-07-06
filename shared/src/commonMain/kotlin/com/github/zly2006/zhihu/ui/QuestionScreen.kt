/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.data.decodeQuestionContentDetail
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberZhihuWebUrlOpener
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.ui.components.ShareDialog
import com.github.zly2006.zhihu.ui.components.getShareText
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.viewmodel.ContentLoadEnvironment
import com.github.zly2006.zhihu.viewmodel.addReadHistory
import com.github.zly2006.zhihu.viewmodel.feed.QuestionFeedViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

const val QUESTION_SCREEN_LIST_TAG = "question_screen_list"
const val QUESTION_TITLE_TAG = "question_title"
const val QUESTION_DETAIL_TOGGLE_TAG = "question_detail_toggle"
const val QUESTION_DETAIL_CONTENT_TAG = "question_detail_content"
const val QUESTION_DETAIL_PREVIEW_TAG = "question_detail_preview"
const val QUESTION_SORT_DEFAULT_TAG = "question_sort_default"
const val QUESTION_SORT_UPDATED_TAG = "question_sort_updated"
const val QUESTION_FOLLOW_BUTTON_TAG = "question_follow_button"
const val QUESTION_VIEW_LOG_BUTTON_TAG = "question_view_log_button"
const val QUESTION_SHARE_BUTTON_TAG = "question_share_button"
const val QUESTION_WRITE_ANSWER_BUTTON_TAG = "question_write_answer_button"
const val QUESTION_COMMENTS_BUTTON_TAG = "question_comments_button"
private const val QUESTION_DETAIL_COLLAPSE_THRESHOLD = 100
private val QUESTION_DETAIL_COLLAPSED_MAX_HEIGHT: Dp = 180.dp
private val QUESTION_DETAIL_MASK_HEIGHT: Dp = 88.dp
private val QUESTION_DETAIL_TOGGLE_ZONE_HEIGHT: Dp = 56.dp
const val QUESTION_STATS_TAG = "question_stats"

private suspend fun loadQuestion(
    environment: ContentLoadEnvironment,
    question: Question,
): DataHolder.Question? {
    environment.addReadHistory(question.questionId.toString(), "question")
    val include =
        "read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics"
    val jsonObject =
        environment.fetchJson("https://www.zhihu.com/api/v4/questions/${question.questionId}", include)
            ?: return null
    val questionData = decodeQuestionContentDetail(jsonObject)
    environment.postHistoryDestination(Question(question.questionId, questionData.title))
    environment.recordContentOpenEvent(destination = question, questionId = question.questionId)
    return questionData
}

/**
 * 问题详情页。
 *
 * 顶部展示问题标题、描述、关注状态和统计信息，主体是该问题下回答的信息流列表。页面会记录内容打开来源和历史记录， 并复用文章/回答卡片、评论底部表单和分享入口；正文描述同样受
 * WebView/Markdown 渲染设置影响。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionScreen(question: Question) {
    val settings = rememberSettingsStore()
    val shareRuntime = rememberShareDialogRuntime()
    val openZhihuWebUrl = rememberZhihuWebUrlOpener()
    val navigator = LocalNavigator.current
    val viewModel: QuestionFeedViewModel =
        viewModel(key = "question_${question.questionId}") {
            QuestionFeedViewModel(question.questionId)
        }
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = false)
    val answerSwitchState = paginationEnvironment.articleAnswerSwitchState()
    val listState = rememberLazyListState()
    var questionContent by remember(question.questionId) { mutableStateOf("") }
    var answerCount by remember(question.questionId) { mutableIntStateOf(0) }
    var visitCount by remember(question.questionId) { mutableIntStateOf(0) }
    var commentCount by remember(question.questionId) { mutableIntStateOf(0) }
    var followerCount by remember(question.questionId) { mutableIntStateOf(0) }
    var title by remember(question.questionId, question.title) { mutableStateOf(question.title) }
    var showComments by rememberSaveable(question.questionId) { mutableStateOf(false) }
    var isFollowing by remember(question.questionId) { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    val userMessages = rememberUserMessageSink()
    var isQuestionDetailExpanded by rememberSaveable(question.questionId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val questionContentPlainText =
        remember(questionContent) { Ksoup.parse(questionContent).text().trim() }
    val shareText = getShareText(question, title)
    val topBarTitleThresholdPx = with(LocalDensity.current) { 160.dp.roundToPx() }
    val showTopBarTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset >= topBarTitleThresholdPx
        }
    }

    // 加载问题详情和答案
    LaunchedEffect(question.questionId, viewModel) {
        if (viewModel.displayItems.isEmpty()) {
            launch { viewModel.refresh(paginationEnvironment) }
        }
        try {
            val questionData = loadQuestion(paginationEnvironment, question)
            if (questionData != null) {
                questionContent = questionData.detail
                title = questionData.title
                answerCount = questionData.answerCount
                visitCount = questionData.visitCount
                commentCount = questionData.commentCount
                followerCount = questionData.followerCount
                isFollowing = questionData.relationship.isFollowing
            } else {
                userMessages.showShortMessage("获取问题详情失败")
            }
        } catch (e: Exception) {
            userMessages.showShortMessage("加载失败: ${e.message}")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            QuestionTopBar(
                title = title,
                showTitle = showTopBarTitle,
                onNavigateBack = navigator.onNavigateBack,
                onOpenLog = {
                    try {
                        openZhihuWebUrl("https://www.zhihu.com/question/${question.questionId}/log")
                    } catch (e: Exception) {
                        userMessages.showShortMessage("打开日志失败: ${e.message}")
                    }
                },
                onShare = {
                    if (shareText != null) {
                        handleShareAction(question, settings, shareRuntime) { showShareDialog = true }
                    }
                },
                canShare = shareText != null,
            )
        },
    ) { innerPadding ->
        FeedPullToRefresh(
            viewModel,
            padding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            PaginatedList(
                items = viewModel.displayItems,
                onLoadMore = { viewModel.loadMore(paginationEnvironment) },
                isEnd = { viewModel.isEnd },
                key = { it.stableKey },
                listState = listState,
                modifier = Modifier.testTag(QUESTION_SCREEN_LIST_TAG),
                contentPadding =
                    PaddingValues(
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = innerPadding.calculateBottomPadding(),
                    ),
                footer = ProgressIndicatorFooter,
                topContent = {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            QuestionHeaderSection(
                                title = title,
                                answerCount = answerCount,
                                visitCount = visitCount,
                                commentCount = commentCount,
                                followerCount = followerCount,
                                onShowComments = { showComments = true },
                            )
                            if (questionContent.isNotEmpty()) {
                                QuestionAnimatedBodyHeader(
                                    questionId = question.questionId,
                                    questionContent = questionContent,
                                    questionContentPlainText = questionContentPlainText,
                                    isExpanded = isQuestionDetailExpanded,
                                    onToggleExpanded = { isQuestionDetailExpanded = !isQuestionDetailExpanded },
                                    isFollowing = isFollowing,
                                    onFollowClick = {
                                        scope.launch {
                                            val nextFollowing = !isFollowing
                                            viewModel.followQuestion(paginationEnvironment, nextFollowing)
                                            isFollowing = nextFollowing
                                            followerCount = (followerCount + if (isFollowing) 1 else -1).coerceAtLeast(0)
                                            userMessages.showShortMessage(if (isFollowing) "已关注问题" else "已取消关注问题")
                                        }
                                    },
                                    onWriteAnswerClick = {
                                        navigator.onNavigate(
                                            WriteAnswer(
                                                questionId = question.questionId,
                                                questionTitle = title,
                                                questionDetail = questionContent,
                                            ),
                                        )
                                    },
                                    currentSort = viewModel.sortOrder,
                                    onSortChange = { sortOrder ->
                                        viewModel.updateSortOrder(sortOrder)
                                        viewModel.refresh(paginationEnvironment)
                                    },
                                )
                            }
                        }
                    }
                },
            ) { item ->
                FeedCard(item = item, modifier = Modifier.testTag("question_feed_item_${item.stableKey}")) {
                    val dest = navDestination
                    answerSwitchState?.pendingNavigator =
                        viewModel.createAnswerNavigatorFor(item, paginationEnvironment)
                    dest?.let { navigator.onNavigate(it) }
                }
            }
        }
    }
    CommentScreenComponent(
        showComments = showComments,
        onDismiss = { showComments = false },
        content = question,
    )

    // 分享对话框
    if (shareText != null) {
        ShareDialog(
            content = question,
            shareText = shareText,
            showDialog = showShareDialog,
            onDismissRequest = { showShareDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionTopBar(
    title: String,
    showTitle: Boolean,
    onNavigateBack: () -> Unit,
    onOpenLog: () -> Unit,
    onShare: () -> Unit,
    canShare: Boolean,
) {
    TopAppBar(
        title = {
            AnimatedContent(
                targetState = showTitle,
                transitionSpec = {
                    (fadeIn() + slideInVertically(initialOffsetY = { it / 2 })) togetherWith
                        (fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }))
                },
                label = "question_top_bar_title",
            ) { shouldShowTitle ->
                Text(
                    text = if (shouldShowTitle) title else "问题",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            IconButton(onClick = onOpenLog, modifier = Modifier.testTag(QUESTION_VIEW_LOG_BUTTON_TAG)) {
                Icon(Icons.Filled.History, contentDescription = "日志")
            }
            IconButton(
                onClick = onShare,
                enabled = canShare,
                modifier = Modifier.testTag(QUESTION_SHARE_BUTTON_TAG),
            ) {
                Icon(Icons.Filled.Share, contentDescription = "分享")
            }
        },
    )
}

@Composable
private fun QuestionHeaderSection(
    title: String,
    answerCount: Int,
    visitCount: Int,
    commentCount: Int,
    followerCount: Int,
    onShowComments: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectionContainer(modifier = Modifier.questionSelectionWorkaround()) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(QUESTION_TITLE_TAG),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowRow(
                modifier = Modifier.weight(1f).testTag(QUESTION_STATS_TAG),
                horizontalArrangement = Arrangement.spacedBy(16.dp), // 水平间距
                verticalArrangement = Arrangement.spacedBy(8.dp), // 垂直间距
            ) {
                StatItem(icon = Icons.Outlined.QuestionAnswer, text = "$answerCount 回答")
                StatItem(icon = Icons.Outlined.Visibility, text = "$visitCount 浏览")
                StatItem(icon = Icons.Outlined.ChatBubbleOutline, text = "$commentCount 评论")
                StatItem(icon = Icons.Outlined.FavoriteBorder, text = "$followerCount 关注")
            }
            OutlinedButton(
                onClick = onShowComments,
                modifier = Modifier.testTag(QUESTION_COMMENTS_BUTTON_TAG),
            ) {
                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                Spacer(Modifier.width(8.dp))
                Text("$commentCount")
            }
        }
    }
}

@Composable
private fun QuestionAnimatedBodyHeader(
    questionId: Long,
    questionContent: String,
    questionContentPlainText: String,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onWriteAnswerClick: () -> Unit,
    currentSort: String,
    onSortChange: (String) -> Unit,
) {
    val allowDetailCollapse =
        questionContentPlainText.length >= QUESTION_DETAIL_COLLAPSE_THRESHOLD || questionContent.contains("<img")
    val density = LocalDensity.current
    val sectionSpacingPx = with(density) { 16.dp.roundToPx() }
    val collapsedViewportHeightPx = with(density) { QUESTION_DETAIL_COLLAPSED_MAX_HEIGHT.roundToPx() }
    var fullViewportHeightPx by remember(questionId) { mutableIntStateOf(0) }
    var animationInitialized by remember(questionId) { mutableStateOf(false) }
    val animatedViewportHeightPx = remember(questionId) { Animatable(collapsedViewportHeightPx.toFloat()) }
    val animationSpec = remember { tween<Float>(durationMillis = 420, easing = FastOutSlowInEasing) }

    LaunchedEffect(questionId) {
        animationInitialized = false
        animatedViewportHeightPx.snapTo(collapsedViewportHeightPx.toFloat())
    }
    LaunchedEffect(isExpanded, fullViewportHeightPx, collapsedViewportHeightPx) {
        if (!allowDetailCollapse || fullViewportHeightPx <= 0) return@LaunchedEffect
        val collapsedTarget = collapsedViewportHeightPx.coerceAtMost(fullViewportHeightPx).toFloat()
        val targetHeight = if (isExpanded) fullViewportHeightPx.toFloat() else collapsedTarget
        if (!animationInitialized) {
            animationInitialized = true
            animatedViewportHeightPx.snapTo(targetHeight)
            return@LaunchedEffect
        }
        if ((animatedViewportHeightPx.value - targetHeight).let { if (it < 0f) -it else it } < 0.5f) {
            return@LaunchedEffect
        }
        animatedViewportHeightPx.animateTo(targetValue = targetHeight, animationSpec = animationSpec)
    }

    val collapsedTargetPx = collapsedViewportHeightPx.coerceAtMost(fullViewportHeightPx).toFloat()
    val expandedRangePx = (fullViewportHeightPx.toFloat() - collapsedTargetPx).coerceAtLeast(1f)
    val expandProgress =
        if (!allowDetailCollapse || fullViewportHeightPx <= 0) {
            1f
        } else {
            ((animatedViewportHeightPx.value - collapsedTargetPx) / expandedRangePx).coerceIn(0f, 1f)
        }
    val viewportHeightPx =
        if (allowDetailCollapse) {
            animatedViewportHeightPx.value.roundToInt()
        } else {
            fullViewportHeightPx
        }

    SubcomposeLayout(modifier = Modifier.fillMaxWidth()) { constraints ->
        val looseConstraints =
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxHeight = Constraints.Infinity,
            )
        val detailPlaceable =
            subcompose("detail") {
                if (allowDetailCollapse) {
                    QuestionDetailAnimatedViewport(
                        questionId = questionId,
                        questionContent = questionContent,
                        viewportHeightPx = viewportHeightPx,
                        isExpanded = isExpanded,
                        overlayAlpha = 1f - expandProgress,
                        onToggleExpanded = onToggleExpanded,
                        onMeasuredFullHeight = { fullViewportHeightPx = it },
                    )
                } else {
                    QuestionDetailStaticContent(
                        questionId = questionId,
                        questionContent = questionContent,
                        onMeasuredHeight = { fullViewportHeightPx = it },
                    )
                }
            }.single().measure(looseConstraints)
        val controlsPlaceable =
            subcompose("controls") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    QuestionPrimaryActions(
                        isFollowing = isFollowing,
                        onFollowClick = onFollowClick,
                        onWriteAnswerClick = onWriteAnswerClick,
                    )
                    QuestionSortBar(
                        currentSort = currentSort,
                        onSortChange = onSortChange,
                    )
                }
            }.single().measure(looseConstraints)
        val totalHeight = detailPlaceable.height + sectionSpacingPx + controlsPlaceable.height
        layout(width = constraints.maxWidth, height = totalHeight) {
            detailPlaceable.place(0, 0)
            controlsPlaceable.place(0, detailPlaceable.height + sectionSpacingPx)
        }
    }
}

@Composable
private fun QuestionDetailStaticContent(
    questionId: Long,
    questionContent: String,
    onMeasuredHeight: (Int) -> Unit,
) {
    SubcomposeLayout(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(QUESTION_DETAIL_CONTENT_TAG),
    ) { constraints ->
        val placeable =
            subcompose("static_detail") {
                QuestionDetailContent(questionId = questionId, html = questionContent)
            }.single().measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxHeight = Constraints.Infinity,
                ),
            )
        if (placeable.height > 0) {
            onMeasuredHeight(placeable.height)
        }
        layout(width = constraints.maxWidth, height = placeable.height) {
            placeable.place(0, 0)
        }
    }
}

@Composable
private fun QuestionDetailAnimatedViewport(
    questionId: Long,
    questionContent: String,
    viewportHeightPx: Int,
    isExpanded: Boolean,
    overlayAlpha: Float,
    onToggleExpanded: () -> Unit,
    onMeasuredFullHeight: (Int) -> Unit,
) {
    val maskHeightPx = with(LocalDensity.current) { QUESTION_DETAIL_MASK_HEIGHT.roundToPx() }
    val buttonZoneHeightPx = with(LocalDensity.current) { QUESTION_DETAIL_TOGGLE_ZONE_HEIGHT.roundToPx() }
    SubcomposeLayout(
        modifier =
            Modifier
                .fillMaxWidth()
                .clipToBounds()
                .testTag(if (isExpanded) QUESTION_DETAIL_CONTENT_TAG else QUESTION_DETAIL_PREVIEW_TAG),
    ) { constraints ->
        val looseConstraints =
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxHeight = Constraints.Infinity,
            )
        val contentPlaceable =
            subcompose("content") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = QUESTION_DETAIL_TOGGLE_ZONE_HEIGHT),
                ) {
                    QuestionDetailContent(questionId = questionId, html = questionContent)
                }
            }.single().measure(looseConstraints)
        if (contentPlaceable.height > 0) {
            onMeasuredFullHeight(contentPlaceable.height)
        }
        val layoutHeight = viewportHeightPx.coerceIn(0, contentPlaceable.height.coerceAtLeast(0))
        val buttonPlaceable =
            subcompose("button") {
                QuestionDetailToggleButton(
                    isExpanded = isExpanded,
                    onClick = onToggleExpanded,
                )
            }.single().measure(looseConstraints)
        val overlayPlaceable =
            subcompose("overlay") {
                QuestionDetailOverlayMask(
                    alpha = overlayAlpha,
                    modifier = Modifier.fillMaxWidth(),
                )
            }.single().measure(
                Constraints(
                    minWidth = constraints.maxWidth,
                    maxWidth = constraints.maxWidth,
                    minHeight = 0,
                    maxHeight = layoutHeight.coerceAtLeast(0),
                ),
            )
        layout(width = constraints.maxWidth, height = layoutHeight) {
            contentPlaceable.place(0, 0)
            if (overlayAlpha > 0f) {
                overlayPlaceable.place(0, (layoutHeight - minOf(maskHeightPx, overlayPlaceable.height)).coerceAtLeast(0))
            }
            val buttonY = (layoutHeight - buttonZoneHeightPx + (buttonZoneHeightPx - buttonPlaceable.height) / 2).coerceAtLeast(0)
            val buttonX = (constraints.maxWidth - buttonPlaceable.width).coerceAtLeast(0)
            buttonPlaceable.place(buttonX, buttonY)
        }
    }
}

@Composable
private fun QuestionDetailOverlayMask(alpha: Float, modifier: Modifier = Modifier) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Box(
        modifier =
            modifier
                .height(QUESTION_DETAIL_MASK_HEIGHT)
                .graphicsLayer { this.alpha = alpha }
                .blur(12.dp)
                .background(
                    brush =
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                surfaceColor.copy(alpha = 0.7f),
                                surfaceColor,
                            ),
                        ),
                ),
    )
}

@Composable
private fun QuestionDetailToggleButton(
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier =
            Modifier
                .offset(y = 4.dp)
                .padding(end = 4.dp, bottom = 0.dp)
                .testTag(QUESTION_DETAIL_TOGGLE_TAG),
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
        )
        Spacer(Modifier.width(4.dp))
        Text(if (isExpanded) "收起详情" else "展开详情")
    }
}

@Composable
private fun QuestionPrimaryActions(
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onWriteAnswerClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onWriteAnswerClick,
            modifier = Modifier.weight(1f).testTag(QUESTION_WRITE_ANSWER_BUTTON_TAG),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
        ) {
            Icon(Icons.Filled.Edit, contentDescription = "写回答")
            Spacer(Modifier.width(8.dp))
            Text("写回答")
        }
        FilledTonalButton(
            onClick = onFollowClick,
            modifier =
                Modifier.weight(1f).testTag(QUESTION_FOLLOW_BUTTON_TAG).semantics {
                    selected = isFollowing
                },
            colors =
                if (isFollowing) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                },
        ) {
            Icon(
                imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = if (isFollowing) "取消关注" else "关注问题",
            )
            Spacer(Modifier.width(8.dp))
            Text(if (isFollowing) "已关注" else "关注问题")
        }
    }
}

@Composable
private fun QuestionSortBar(currentSort: String, onSortChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "回答排序",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        FilterChip(
            selected = currentSort == "default",
            onClick = { onSortChange("default") },
            modifier =
                Modifier.testTag(QUESTION_SORT_DEFAULT_TAG).semantics {
                    selected = currentSort == "default"
                },
            label = { Text("默认") },
        )
        FilterChip(
            selected = currentSort == "updated",
            onClick = { onSortChange("updated") },
            modifier =
                Modifier.testTag(QUESTION_SORT_UPDATED_TAG).semantics {
                    selected = currentSort == "updated"
                },
            label = { Text("最新") },
        )
    }
}

@Composable
private fun StatItem(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null, // 装饰性图标不需要无障碍描述
            modifier = Modifier.size(16.dp), // 图标稍微小一点，匹配辅助文字
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp)) // 图标和文字的间距
        Text(
            text = text,
            // 辅助信息通常使用更小一号的字重，比如 bodySmall 或 labelMedium
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
