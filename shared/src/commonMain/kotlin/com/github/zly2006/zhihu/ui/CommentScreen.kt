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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.CommentHolder
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.SegmentCommentHolder
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.shared.platform.PlatformBackHandler
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberImagePreviewOpener
import com.github.zly2006.zhihu.shared.platform.rememberImageSaver
import com.github.zly2006.zhihu.shared.platform.rememberImageSharer
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.util.twoDigitString
import com.github.zly2006.zhihu.shared.viewmodel.CommentItem
import com.github.zly2006.zhihu.ui.subscreens.PREF_FONT_SIZE
import com.github.zly2006.zhihu.ui.subscreens.PREF_LINE_HEIGHT
import com.github.zly2006.zhihu.viewmodel.comment.BaseCommentViewModel
import com.github.zly2006.zhihu.viewmodel.comment.ChildCommentViewModel
import com.github.zly2006.zhihu.viewmodel.comment.CommentSortOrder
import com.github.zly2006.zhihu.viewmodel.comment.RootCommentViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant

typealias CommentModel = CommentItem

const val COMMENT_SCREEN_LIST_TAG = "comment_screen_list"
const val COMMENT_REPLY_BANNER_TAG = "comment_reply_banner"
const val COMMENT_CANCEL_REPLY_TAG = "comment_cancel_reply"
const val COMMENT_INPUT_TAG = "comment_input"
const val COMMENT_SEND_BUTTON_TAG = "comment_send_button"
const val COMMENT_SORT_SCORE_TAG = "comment_sort_score"
const val COMMENT_SORT_TIME_TAG = "comment_sort_time"
const val COMMENT_IMAGE_MENU_OPEN_TAG = "comment_image_menu_open"
const val COMMENT_IMAGE_MENU_BROWSER_TAG = "comment_image_menu_browser"
const val COMMENT_IMAGE_MENU_SAVE_TAG = "comment_image_menu_save"
const val COMMENT_IMAGE_MENU_SHARE_TAG = "comment_image_menu_share"

enum class CommentImageMenuAction {
    Open,
    OpenInBrowser,
    Save,
    Share,
}

data class CommentScreenTestOverrides(
    val viewModel: BaseCommentViewModel? = null,
    val onArchiveComment: ((CommentModel) -> Unit)? = null,
    val onImageMenuAction: ((CommentImageMenuAction, String) -> Unit)? = null,
)

@Composable
fun SwipeToReplyContainer(
    modifier: Modifier = Modifier,
    onArchive: (() -> Unit)? = null, // 向右滑触发，传 null 则禁向右滑
    onReply: (() -> Unit)? = null, // 向左滑触发，传 null 则禁向左滑
    archiveIcon: ImageVector = Icons.Default.Archive,
    replyIcon: ImageVector = Icons.AutoMirrored.Filled.Reply,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current

    // 触发阈值 (60dp)
    val triggerThreshold = with(density) { 60.dp.toPx() }
    // 最大滑动距离 (100dp)
    val maxDragDistance = with(density) { 100.dp.toPx() }

    var hasVibrated by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        // --- 背景层 (图标) ---
        // 只有在发生位移时才计算显示逻辑
        if (offsetX.value != 0f) {
            val isRightSwipe = offsetX.value > 0

            // 计算进度 (0.0 ~ 1.0)
            val progress = (abs(offsetX.value) / triggerThreshold).coerceIn(0f, 1f)

            val iconScale = 0.5f + (0.5f * progress)

            // 决定背景颜色和对齐方式
            val align = if (isRightSwipe) Alignment.CenterStart else Alignment.CenterEnd
            val icon = if (isRightSwipe) archiveIcon else replyIcon
            val iconTint = if (isRightSwipe) Color.Gray else MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = align,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        alpha = progress
                    },
                )
            }
        }

        // --- 前景层 (内容) ---
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val current = offsetX.value
                            // 加上阻尼系数 0.5
                            val target = current + delta * 0.5f

                            // --- 核心逻辑：判断是否允许滑动 ---
                            val isTryingToSwipeRight = target > 0
                            val isTryingToSwipeLeft = target < 0

                            val canSwipeRight = onArchive != null
                            val canSwipeLeft = onReply != null

                            // 如果试图向右滑但 onReply 为空 -> 强制为 0 (或保持在非正数)
                            // 如果试图向左滑但 onArchive 为空 -> 强制为 0 (或保持在非负数)
                            var newOffset = target

                            if (isTryingToSwipeRight && !canSwipeRight) {
                                newOffset = 0f
                            }
                            if (isTryingToSwipeLeft && !canSwipeLeft) {
                                newOffset = 0f
                            }

                            // 限制最大滑动距离 (正负方向)
                            if (newOffset > maxDragDistance) newOffset = maxDragDistance
                            if (newOffset < -maxDragDistance) newOffset = -maxDragDistance

                            // 应用位移
                            offsetX.snapTo(newOffset)

                            // --- 震动反馈逻辑 ---
                            // 绝对值超过阈值
                            if (abs(newOffset) >= triggerThreshold) {
                                if (!hasVibrated) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    hasVibrated = true
                                }
                            } else {
                                hasVibrated = false
                            }
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        val currentVal = offsetX.value

                        // 判断触发逻辑
                        if (currentVal >= triggerThreshold && onArchive != null) {
                            onArchive()
                        } else if (currentVal <= -triggerThreshold && onReply != null) {
                            onReply()
                        }

                        // 无论如何回弹归零
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            )
                            hasVibrated = false
                        }
                    },
                ),
        ) {
            content()
        }
    }
}

/**
 * 可点击的图片组件，支持点击查看和长按显示菜单
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClickableImageWithMenu(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "图片",
    onAction: ((CommentImageMenuAction, String) -> Unit)? = null,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val openImagePreview = rememberImagePreviewOpener()
    val openExternalUrl = rememberExternalUrlOpener()
    val saveImage = rememberImageSaver()
    val shareImage = rememberImageSharer()

    PlatformBackHandler(enabled = showContextMenu) {
        showContextMenu = false
    }

    fun handleAction(action: CommentImageMenuAction) {
        if (onAction != null) {
            onAction(action, imageUrl)
            return
        }
        when (action) {
            CommentImageMenuAction.Open -> openImagePreview(imageUrl)
            CommentImageMenuAction.OpenInBrowser -> openExternalUrl(imageUrl)
            CommentImageMenuAction.Save -> saveImage(imageUrl)
            CommentImageMenuAction.Share -> shareImage(imageUrl)
        }
    }

    Box(
        modifier = modifier.combinedClickable(
            onClick = { handleAction(CommentImageMenuAction.Open) },
            onLongClick = { showContextMenu = true },
        ),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
        )

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
        ) {
            DropdownMenuItem(
                modifier = Modifier.testTag(COMMENT_IMAGE_MENU_OPEN_TAG),
                text = { Text("查看图片") },
                onClick = {
                    handleAction(CommentImageMenuAction.Open)
                    showContextMenu = false
                },
            )
            DropdownMenuItem(
                modifier = Modifier.testTag(COMMENT_IMAGE_MENU_BROWSER_TAG),
                text = { Text("在浏览器中打开") },
                onClick = {
                    handleAction(CommentImageMenuAction.OpenInBrowser)
                    showContextMenu = false
                },
            )
            DropdownMenuItem(
                modifier = Modifier.testTag(COMMENT_IMAGE_MENU_SAVE_TAG),
                text = { Text("保存图片") },
                onClick = {
                    handleAction(CommentImageMenuAction.Save)
                    showContextMenu = false
                },
            )
            DropdownMenuItem(
                modifier = Modifier.testTag(COMMENT_IMAGE_MENU_SHARE_TAG),
                text = { Text("分享图片") },
                onClick = {
                    showContextMenu = false
                    handleAction(CommentImageMenuAction.Share)
                },
            )
        }
    }
}

/**
 * 评论列表与回复页面。
 *
 * 页面根据 [content] 指向的文章、问题、想法或片段选择对应评论 ViewModel，展示父评论和子评论层级，并提供发送回复、图片预览、
 * 长按图片菜单等交互。因为评论页经常以底部弹窗形式被其他页面嵌入，布局和状态不能假设自己拥有完整 NavHost。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    content: () -> NavDestination,
    activeCommentItem: CommentModel? = null,
    onChildCommentClick: (CommentModel) -> Unit,
    commentInput: String,
    onCommentInputChange: (String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    testOverrides: CommentScreenTestOverrides? = null,
) {
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = false)
    val resolvedContent = content()
    var isSending by remember { mutableStateOf(false) }
    var replyToComment by remember { mutableStateOf<CommentModel?>(null) }
    val viewModelKey = commentViewModelKey(resolvedContent)

    // 根据内容类型选择合适的ViewModel
    val viewModel: BaseCommentViewModel = testOverrides?.viewModel ?: when (resolvedContent) {
        is CommentHolder -> remember(viewModelKey) {
            // 子评论不进行状态保存
            ChildCommentViewModel(resolvedContent)
        }

        else -> viewModel(key = viewModelKey) {
            RootCommentViewModel(resolvedContent)
        }
    }
    val rootContent = when (resolvedContent) {
        is CommentHolder -> resolvedContent.article
        else -> resolvedContent
    }
    val commentBackgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
    val commentInputBarColor = MaterialTheme.colorScheme.surfaceContainer
    val actionChipColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val actionChipIconColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 监控滚动位置以实现加载更多
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisibleItemIndex >= totalItemsCount - 3 && !viewModel.isLoading && !viewModel.isEnd
        }
    }

    // 监控滚动加载更多
    LaunchedEffect(loadMore.value) {
        if (loadMore.value && viewModel.errorMessage == null) {
            viewModel.loadMore(paginationEnvironment)
        }
    }

    // 初始加载评论
    LaunchedEffect(resolvedContent) {
        if (viewModel.article != resolvedContent) {
            error("Internal Error: Detected content mismatch")
        }
        if (viewModel.errorMessage == null) {
            viewModel.loadMore(paginationEnvironment)
        }
    }
    val coroutineScope = rememberCoroutineScope()

    // 提交评论函数
    fun submitComment() {
        if (commentInput.isBlank() || isSending) return

        isSending = true
        viewModel.submitComment(
            content = content(),
            commentText = commentInput,
            environment = paginationEnvironment,
            replyToCommentId = replyToComment?.item?.id,
        ) {
            onCommentInputChange("")
            replyToComment = null
            isSending = false
            coroutineScope.launch {
                listState.animateScrollToItem(
                    0,
                    0,
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // 评论内容区域
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = commentBackgroundColor,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        viewModel.isLoading && viewModel.allData.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        viewModel.errorMessage != null && viewModel.allData.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(viewModel.errorMessage!!, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        activeCommentItem == null && viewModel.allData.isEmpty() -> {
                            // activeCommentItem != null 的空态在下面的 LazyColumn 中处理。
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("暂无评论")
                            }
                        }

                        else -> {
                            @Composable
                            fun Comment(
                                commentItem: CommentModel,
                                modifier: Modifier = Modifier,
                                onChildCommentClick: (CommentModel) -> Unit,
                            ) {
                                var isLiked by remember { mutableStateOf(commentItem.item.liked) }
                                var likeCount by remember { mutableIntStateOf(commentItem.item.likeCount) }
                                var isLikeLoading by remember { mutableStateOf(false) }

                                Column(modifier = modifier) {
                                    CommentItem(
                                        comment = commentItem,
                                        isLiked = isLiked,
                                        likeCount = likeCount,
                                        isLikeLoading = isLikeLoading,
                                        toggleLike = {
                                            viewModel.toggleLikeComment(
                                                commentData = commentItem.item,
                                                environment = paginationEnvironment,
                                            ) {
                                                val newLikeState = !isLiked
                                                isLiked = newLikeState
                                                likeCount += if (newLikeState) 1 else -1
                                                commentItem.item.liked = newLikeState
                                                commentItem.item.likeCount = likeCount
                                            }
                                        },
                                        onChildCommentClick = onChildCommentClick,
                                        onImageMenuAction = testOverrides?.onImageMenuAction,
                                    )

                                    // 在根评论区时 子评论
                                    if (activeCommentItem == null && commentItem.item.childCommentCount > 0) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 40.dp, top = 8.dp),
                                        ) {
                                            if (commentItem.item.childComments.isNotEmpty()) {
                                                commentItem.item.childComments.forEach { childComment ->
                                                    var liked by remember { mutableStateOf(childComment.liked) }
                                                    var likeCount by remember { mutableIntStateOf(childComment.likeCount) }
                                                    val childCommentItem = CommentModel(
                                                        item = childComment,
                                                        clickTarget = null, // 子评论不需要点击跳转
                                                    )
                                                    CommentItem(
                                                        comment = childCommentItem,
                                                        modifier = Modifier.testTag("comment_row_${childComment.id}"),
                                                        isLiked = liked,
                                                        likeCount = likeCount,
                                                        toggleLike = {
                                                            viewModel.toggleLikeComment(
                                                                commentData = childCommentItem.item,
                                                                environment = paginationEnvironment,
                                                            ) {
                                                                val newLikeState = !liked
                                                                liked = newLikeState
                                                                likeCount += if (newLikeState) 1 else -1
                                                                childCommentItem.item.liked = newLikeState
                                                                childCommentItem.item.likeCount = likeCount
                                                            }
                                                        },
                                                        onChildCommentClick = onChildCommentClick,
                                                        onImageMenuAction = testOverrides?.onImageMenuAction,
                                                    )
                                                }
                                            }
                                            Button(
                                                onClick = { onChildCommentClick(commentItem) },
                                                modifier = Modifier
                                                    .height(28.dp)
                                                    .testTag("comment_child_button_${commentItem.item.id}"),
                                                shape = RoundedCornerShape(50),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = actionChipColor,
                                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                                ),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Outlined.Comment,
                                                    contentDescription = "查看子评论",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = actionChipIconColor,
                                                )
                                                Text(
                                                    "查看 ${commentItem.item.childCommentCount} 条子评论",
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(vertical = 1.dp, horizontal = 4.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(COMMENT_SCREEN_LIST_TAG),
                                contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                if (activeCommentItem != null) {
                                    item(
                                        key = "active_${activeCommentItem.item.id}",
                                    ) {
                                        Column(
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                placementSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                ),
                                            ),
                                        ) {
                                            Comment(activeCommentItem) { }
                                            HorizontalDivider()
                                            if (viewModel.allData.isEmpty()) {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Text(
                                                        // activeCommentItem 非空时这里展示的是子评论回复列表。
                                                        "暂无回复",
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    item("sorting") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .layout { measurable, constraints ->
                                                    val contentHeight = 32.dp.roundToPx()
                                                    val placeable = measurable.measure(
                                                        constraints.copy(
                                                            minHeight = contentHeight,
                                                            maxHeight = contentHeight,
                                                        ),
                                                    )
                                                    layout(placeable.width, 24.dp.roundToPx()) {
                                                        placeable.placeRelative(0, 0)
                                                    }
                                                },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End,
                                        ) {
                                            SuggestionChip(
                                                modifier = Modifier.testTag(COMMENT_SORT_SCORE_TAG),
                                                label = {
                                                    Text(
                                                        "最热",
                                                        color = if (viewModel.sortOrder == CommentSortOrder.SCORE) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        fontWeight = if (viewModel.sortOrder == CommentSortOrder.SCORE) {
                                                            FontWeight.SemiBold
                                                        } else {
                                                            FontWeight.Normal
                                                        },
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.changeSortOrder(CommentSortOrder.SCORE, paginationEnvironment)
                                                },
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            SuggestionChip(
                                                modifier = Modifier.testTag(COMMENT_SORT_TIME_TAG),
                                                label = {
                                                    Text(
                                                        "最新",
                                                        color = if (viewModel.sortOrder == CommentSortOrder.TIME) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        fontWeight = if (viewModel.sortOrder == CommentSortOrder.TIME) {
                                                            FontWeight.SemiBold
                                                        } else {
                                                            FontWeight.Normal
                                                        },
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.changeSortOrder(CommentSortOrder.TIME, paginationEnvironment)
                                                },
                                            )
                                        }
                                    }
                                }

                                items(
                                    items = viewModel.allData,
                                    key = { it.id },
                                ) { dto ->
                                    val commentItem = viewModel.createCommentItem(dto, article = rootContent)
                                    SwipeToReplyContainer(
                                        modifier = Modifier.testTag("comment_row_${dto.id}"),
                                        onArchive = testOverrides?.onArchiveComment?.let { onArchive ->
                                            {
                                                onArchive(commentItem)
                                            }
                                        },
                                        onReply = {
                                            if (activeCommentItem == null) {
                                                if (commentItem.clickTarget != null) {
                                                    onChildCommentClick(commentItem)
                                                }
                                            } else {
                                                // 滑动回复时设置回复目标。
                                                replyToComment = commentItem
                                            }
                                        },
                                    ) {
                                        Comment(
                                            commentItem,
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                placementSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                ),
                                            ),
                                        ) { _ ->
                                            if (activeCommentItem == null) {
                                                if (commentItem.clickTarget != null) {
                                                    onChildCommentClick(commentItem)
                                                }
                                            } else {
                                                // 滑动回复时设置回复目标。
                                                replyToComment = commentItem
                                            }
                                        }
                                    }
                                }

                                if (viewModel.isLoading && viewModel.allData.isNotEmpty()) {
                                    item(key = "loading_indicator") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 评论输入框
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                    color = commentInputBarColor,
                ) {
                    Column {
                        // 回复目标提示栏。
                        AnimatedVisibility(
                            visible = replyToComment != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(COMMENT_REPLY_BANNER_TAG),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "回复 ${replyToComment?.item?.author?.name ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = { replyToComment = null },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .testTag(COMMENT_CANCEL_REPLY_TAG),
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "取消回复",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp, max = 140.dp)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicTextField(
                                value = commentInput,
                                onValueChange = onCommentInputChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag(COMMENT_INPUT_TAG),
                                decorationBox = { inner ->
                                    Box {
                                        if (commentInput.isEmpty()) {
                                            Text(
                                                if (replyToComment != null) {
                                                    "回复 ${replyToComment?.item?.author?.name}..."
                                                } else {
                                                    "写下你的评论..."
                                                },
                                                fontSize = 16.sp,
                                            )
                                        }
                                        inner()
                                    }
                                },
                                textStyle = TextStyle.Default.copy(
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                            )

                            IconButton(
                                onClick = { submitComment() },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag(COMMENT_SEND_BUTTON_TAG),
                                enabled = !isSending && commentInput.isNotBlank(),
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.Send,
                                        contentDescription = "发送评论",
                                        tint = if (commentInput.isNotBlank()) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun commentViewModelKey(content: NavDestination): String = when (content) {
    is Article -> "article:${content.type}:${content.id}"
    is Pin -> "pin:${content.id}"
    is Question -> "question:${content.questionId}"
    is SegmentCommentHolder -> "segment:${content.contentType}:${content.contentId}:${content.segmentId}"
    is CommentHolder -> "comment:${content.commentId}:${commentViewModelKey(content.article)}"
    else -> "comment:${content::class.qualifiedName}:${content.hashCode()}"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentItem(
    comment: CommentModel,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
    likeCount: Int = 0,
    isLikeLoading: Boolean = false,
    toggleLike: () -> Unit = {},
    onChildCommentClick: (CommentModel) -> Unit,
    onImageMenuAction: ((CommentImageMenuAction, String) -> Unit)? = null,
) {
    val navigator = LocalNavigator.current
    val commentData = comment.item

    Column(modifier = modifier.fillMaxWidth()) {
        // 作者信息
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 头像
            AsyncImage(
                model = commentData.author.avatarUrl,
                contentDescription = "头像",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.Top,
                modifier = Modifier,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 作者名
                    Text(
                        text = commentData.author.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .testTag("comment_author_${commentData.id}")
                            .clickable {
                                navigator.onNavigate(
                                    Person(
                                        id = commentData.author.id,
                                        name = commentData.author.name,
                                        urlToken = commentData.author.urlToken,
                                    ),
                                )
                            },
                    )

                    val authorTag = comment.item.authorTag
                        .firstOrNull()
                        ?.get("text")
                        ?.jsonPrimitive
                        ?.contentOrNull

                    if (authorTag != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        AuthorTag(authorTag)
                    }

                    val replyToAuthor = commentData.replyToAuthor
                    if (replyToAuthor != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "回复",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = replyToAuthor.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .testTag("comment_reply_to_author_${commentData.id}")
                                .clickable {
                                    navigator.onNavigate(
                                        Person(
                                            id = replyToAuthor.id,
                                            name = replyToAuthor.name,
                                            urlToken = replyToAuthor.urlToken,
                                        ),
                                    )
                                },
                        )
                    }
                }

                val document = Ksoup.parseBodyFragment(commentData.content)
                val commentImg =
                    document.selectFirst("a.comment_img")?.attr("href")
                        ?: document.selectFirst("a.comment_gif")?.attr("href")
                        ?: document.selectFirst("a.comment_sticker")?.attr("href")
                // 收集所有使用的emoji
                val emojisUsed = remember { mutableSetOf<String>() }
                val openExternalUrl = rememberExternalUrlOpener()
                val string = remember(commentData.content) {
                    emojisUsed.clear()
                    buildAnnotatedString {
                        val stripped = document.body().clone()
                        stripped.select("a.comment_img").forEach { it.remove() }
                        stripped.select("a.comment_gif").forEach { it.remove() }
                        stripped.select("a.comment_sticker").forEach { it.remove() }
                        dfsSimple(
                            node = stripped,
                            onNavigate = navigator.onNavigate,
                            openExternalUrl = openExternalUrl,
                            componentUsed = emojisUsed,
                        )
                    }
                }

                // 创建inlineContent映射
                val inlineContent = rememberCommentEmojiInlineContent(emojisUsed)

                Column {
                    val settings = rememberSettingsStore()
                    val fontSizePercent = remember { settings.getInt(PREF_FONT_SIZE, 100) }
                    val lineHeightPercent = remember { settings.getInt(PREF_LINE_HEIGHT, 160) }
                    SelectionContainer(
                        modifier = Modifier.commentSelectionWorkaround(),
                    ) {
                        Text(
                            text = string,
                            fontSize = 16.sp * fontSizePercent / 100,
                            lineHeight = 16.sp * fontSizePercent / 100 * lineHeightPercent / 100,
                            inlineContent = inlineContent,
                        )
                    }
                    if (commentImg != null) {
                        ClickableImageWithMenu(
                            imageUrl = commentImg,
                            modifier = Modifier
                                .testTag("comment_image_${commentData.id}")
                                .padding(top = 8.dp)
                                .sizeIn(maxHeight = 100.dp, maxWidth = 240.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentDescription = "评论图片",
                            onAction = onImageMenuAction,
                        )
                    }
                }
            }
        }

        // 底部信息栏
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 时间
            val formattedTime = remember(commentData.createdTime) {
                formatCommentTime(commentData.createdTime)
            }

            Text(
                text = formattedTime,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val ipInfo = comment.item.commentTag
                .firstOrNull {
                    it.type == "ip_info"
                }?.text
            if (ipInfo != null) {
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = ipInfo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 回复按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .testTag("comment_reply_button_${commentData.id}")
                    .clickable { onChildCommentClick(comment) },
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Column(
                    modifier = Modifier.height(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Comment,
                        contentDescription = "回复",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                if (comment.item.childCommentCount > 0) {
                    Text(
                        text = comment.item.childCommentCount.toString(),
                        modifier = Modifier.testTag("comment_reply_count_${commentData.id}"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 点赞
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .testTag("comment_like_button_${commentData.id}")
                    .clickable(enabled = !isLikeLoading) { toggleLike() },
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    if (isLiked) {
                        Icons.Filled.ThumbUp
                    } else {
                        Icons.Outlined.ThumbUp
                    },
                    contentDescription = "点赞",
                    modifier = Modifier.size(16.dp),
                    tint = if (isLiked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = likeCount.toString(),
                    modifier = Modifier.testTag("comment_like_count_${commentData.id}"),
                    fontSize = 12.sp,
                    color = if (isLiked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

private fun formatCommentTime(createdTimeSeconds: Long): String {
    val zone = TimeZone.currentSystemDefault()
    val dateTime = Instant.fromEpochSeconds(createdTimeSeconds).toLocalDateTime(zone)
    val now = Clock.System.now().toLocalDateTime(zone)
    return when {
        dateTime.date == now.date -> dateTime.formatHms()
        dateTime.year == now.year -> "${dateTime.month.number.twoDigitString()}-${dateTime.day.twoDigitString()} ${dateTime.formatHms()}"
        else -> "${dateTime.year}-${dateTime.month.number.twoDigitString()}-${dateTime.day.twoDigitString()} ${dateTime.formatHms()}"
    }
}

private fun LocalDateTime.formatHms(): String =
    "${hour.twoDigitString()}:${minute.twoDigitString()}:${second.twoDigitString()}"

private fun AnnotatedString.Builder.processTextWithEmoji(
    text: String,
    componentUsed: MutableSet<String>?,
) {
    var buffer = StringBuilder()
    var emojiBuffer = StringBuilder()
    var isEmoji = false

    for (ch in text) {
        if (ch == '[') {
            if (buffer.isNotEmpty()) {
                append(buffer.toString())
                buffer = StringBuilder()
            }
            isEmoji = true
            emojiBuffer.append(ch)
        } else if (ch == ']') {
            if (isEmoji) {
                emojiBuffer.append(ch)
                val placeholder = emojiBuffer.toString()
                val emojiKey = commentEmojiInlineKey(placeholder)
                if (emojiKey != null) {
                    appendInlineContent(emojiKey, placeholder)
                    componentUsed?.add(emojiKey)
                } else {
                    append(placeholder)
                }
                emojiBuffer = StringBuilder()
                isEmoji = false
            } else {
                buffer.append(ch)
            }
        } else {
            if (isEmoji) {
                emojiBuffer.append(ch)
            } else {
                buffer.append(ch)
            }
        }
    }

    if (buffer.isNotEmpty()) {
        append(buffer.toString())
    }
    if (isEmoji && emojiBuffer.isNotEmpty()) {
        append(emojiBuffer.toString())
    }
}

private fun AnnotatedString.Builder.dfsSimple(
    node: Node,
    onNavigate: (NavDestination) -> Unit,
    openExternalUrl: (String) -> Unit,
    componentUsed: MutableSet<String>? = null,
) {
    when (node) {
        is Element -> {
            when (node.tagName()) {
                "br" -> append("\n")
                "a" -> {
                    val href = node.attr("href")
                    val linkText = node.text()
                    if (linkText.isNotEmpty()) {
                        withLink(
                            LinkAnnotation.Clickable(
                                href,
                                TextLinkStyles(style = SpanStyle(color = Color(0xff66CCFF))),
                            ) {
                                resolveContent(href)?.let(onNavigate) ?: openExternalUrl(href)
                            },
                        ) {
                            append(linkText)
                        }
                    }
                }

                else -> node.childNodes().forEach {
                    dfsSimple(it, onNavigate, openExternalUrl, componentUsed)
                }
            }
        }

        is TextNode -> processTextWithEmoji(node.text(), componentUsed)
        else -> append(node.outerHtml())
    }
}

@Composable
fun AuthorTag(authorTag: String) {
    Box(
        modifier = Modifier
            .border(
                width = 0.5.dp,
                color = Color.Gray,
                shape = RoundedCornerShape(3.dp),
            ).padding(horizontal = 3.dp),
    ) {
        Text(
            text = authorTag,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
