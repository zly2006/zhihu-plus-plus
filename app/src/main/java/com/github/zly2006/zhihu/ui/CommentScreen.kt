@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.ui

import android.content.Context.MODE_PRIVATE
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.theme.Typography
import com.github.zly2006.zhihu.ui.components.OpenImageDislog
import com.github.zly2006.zhihu.util.createEmojiInlineContent
import com.github.zly2006.zhihu.util.dfsSimple
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.viewmodel.CommentItem
import com.github.zly2006.zhihu.viewmodel.comment.BaseCommentViewModel
import com.github.zly2006.zhihu.viewmodel.comment.ChildCommentViewModel
import com.github.zly2006.zhihu.viewmodel.comment.RootCommentViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

typealias CommentModel = CommentItem

private val HMS = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
private val MDHMS = SimpleDateFormat("MM-dd HH:mm:ss", Locale.ENGLISH)
val YMDHMS = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

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
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
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
                    .fillMaxSize()
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
    httpClient: HttpClient,
    modifier: Modifier = Modifier,
    contentDescription: String = "图片",
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier
                .combinedClickable(
                    onClick = { OpenImageDislog(context, httpClient, imageUrl).show() },
                    onLongClick = { showContextMenu = true },
                ),
        )

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("查看图片") },
                onClick = {
                    OpenImageDislog(context, httpClient, imageUrl).show()
                    showContextMenu = false
                },
            )
            DropdownMenuItem(
                text = { Text("在浏览器中打开") },
                onClick = {
                    luoTianYiUrlLauncher(context, imageUrl.toUri())
                    showContextMenu = false
                },
            )
            DropdownMenuItem(
                text = { Text("保存图片") },
                onClick = {
                    coroutineScope.launch {
                        saveImageToGallery(context, httpClient, imageUrl)
                    }
                    showContextMenu = false
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    httpClient: HttpClient,
    content: () -> NavDestination,
    activeCommentItem: CommentModel? = null,
    topPadding: Dp = 100.dp,
    onChildCommentClick: (CommentModel) -> Unit,
) {
    val context = LocalContext.current
    var commentInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var replyToComment by remember { mutableStateOf<CommentModel?>(null) }
    val preferences = remember {
        context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE)
    }
    val useWebview = remember { preferences.getBoolean("commentsUseWebview1", false) }

    // 根据内容类型选择合适的ViewModel
    val viewModel: BaseCommentViewModel = when (val content = content()) {
        is CommentHolder -> remember {
            // 子评论不进行状态保存
            ChildCommentViewModel(content)
        }

        else -> viewModel {
            RootCommentViewModel(content)
        }
    }
    val rootContent = when (val content = content()) {
        is CommentHolder -> content.article
        else -> content
    }

    val listState = rememberLazyListState()

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
            viewModel.loadMore(context)
        }
    }

    // 初始加载评论
    LaunchedEffect(content) {
        if (viewModel.article != content()) {
            error("Internal Error: Detected content mismatch")
        }
        if (viewModel.errorMessage == null) {
            viewModel.loadMore(context)
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
            httpClient = httpClient,
            context = context,
            replyToCommentId = replyToComment?.item?.id,
        ) {
            commentInput = ""
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
        modifier = Modifier
            .imePadding()
            .fillMaxSize(),
    ) {
        // 评论内容区域
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .fillMaxHeight()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CommentTopText(content())
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
                            // Note: see LazyColumn below for activeCommentItem != null
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
                                        httpClient = httpClient,
                                        isLiked = isLiked,
                                        likeCount = likeCount,
                                        isLikeLoading = isLikeLoading,
                                        toggleLike = {
                                            viewModel.toggleLikeComment(
                                                httpClient = httpClient,
                                                commentData = commentItem.item,
                                                context = context,
                                            ) {
                                                val newLikeState = !isLiked
                                                isLiked = newLikeState
                                                likeCount += if (newLikeState) 1 else -1
                                                commentItem.item.liked = newLikeState
                                                commentItem.item.likeCount = likeCount
                                            }
                                        },
                                        onChildCommentClick = onChildCommentClick,
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
                                                        httpClient = httpClient,
                                                        isLiked = liked,
                                                        likeCount = likeCount,
                                                        toggleLike = {
                                                            viewModel.toggleLikeComment(
                                                                commentData = childCommentItem.item,
                                                                httpClient = httpClient,
                                                                context = context,
                                                            ) {
                                                                val newLikeState = !liked
                                                                liked = newLikeState
                                                                likeCount += if (newLikeState) 1 else -1
                                                                childCommentItem.item.liked = newLikeState
                                                                childCommentItem.item.likeCount = likeCount
                                                            }
                                                        },
                                                        onChildCommentClick = onChildCommentClick,
                                                    )
                                                }
                                            }
                                            Button(
                                                onClick = { onChildCommentClick(commentItem) },
                                                modifier = Modifier
                                                    .height(28.dp),
                                                shape = RoundedCornerShape(50),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                                ),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Outlined.Comment,
                                                    contentDescription = "查看子评论",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.surfaceTint,
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
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
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
                                                        // Note: activeCommentItem != null, so this is a child comment view
                                                        "暂无回复",
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                items(
                                    items = viewModel.allData,
                                    key = { it.id },
                                ) { dto ->
                                    val commentItem = viewModel.createCommentItem(dto, article = rootContent)
                                    SwipeToReplyContainer(onReply = {
                                        if (activeCommentItem == null) {
                                            if (commentItem.clickTarget != null) {
                                                onChildCommentClick(commentItem)
                                            }
                                        } else {
                                            // Set reply target when swiping to reply
                                            replyToComment = commentItem
                                        }
                                    }) {
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
                                        ) { comment ->
                                            if (comment.clickTarget != null) {
                                                onChildCommentClick(comment)
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
                ) {
                    Column {
                        // Reply indicator bar
                        AnimatedVisibility(
                            visible = replyToComment != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth(),
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
                                        modifier = Modifier.size(24.dp),
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
                                onValueChange = { commentInput = it },
                                modifier = Modifier.weight(1f),
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
                                modifier = Modifier.size(24.dp),
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

@Composable
@Preview(showBackground = true)
fun CommentTopText(content: NavDestination? = null) {
    Text(
        if (content is CommentHolder) {
            "回复"
        } else {
            "评论"
        },
        style = Typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp),
        textAlign = TextAlign.Center,
        fontSize = 18.sp,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentItem(
    comment: CommentModel,
    httpClient: HttpClient,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
    likeCount: Int = 0,
    isLikeLoading: Boolean = false,
    toggleLike: () -> Unit = {},
    onChildCommentClick: (CommentModel) -> Unit,
) {
    val onNavigate = LocalNavigator.current
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
                        modifier = Modifier.clickable {
                            onNavigate(
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

                    if (commentData.replyToAuthor != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "回复",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = commentData.replyToAuthor.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable {
                                onNavigate(
                                    Person(
                                        id = commentData.replyToAuthor.id,
                                        name = commentData.replyToAuthor.name,
                                        urlToken = commentData.replyToAuthor.urlToken,
                                    ),
                                )
                            },
                        )
                    }
                }

                val document = Jsoup.parse(commentData.content)
                val commentImg =
                    document.selectFirst("a.comment_img")?.attr("href")
                        ?: document.selectFirst("a.comment_gif")?.attr("href")
                        ?: document.selectFirst("a.comment_sticker")?.attr("href")
                val context = LocalContext.current

                // 收集所有使用的emoji
                val emojisUsed = remember { mutableSetOf<String>() }
                val string = remember(commentData.content) {
                    emojisUsed.clear()
                    buildAnnotatedString {
                        val stripped = document.body().clone()
                        stripped.select("a.comment_img").forEach { it.remove() }
                        stripped.select("a.comment_gif").forEach { it.remove() }
                        stripped.select("a.comment_sticker").forEach { it.remove() }
                        dfsSimple(stripped, onNavigate, context, emojisUsed)
                    }
                }

                // 创建inlineContent映射
                val inlineContent = remember(emojisUsed.size) {
                    createEmojiInlineContent(emojisUsed)
                }

                Column {
                    SelectionContainer(
                        modifier = Modifier.fuckHonorService(),
                    ) {
                        Text(
                            text = string,
                            inlineContent = inlineContent,
                        )
                    }
                    if (commentImg != null) {
                        ClickableImageWithMenu(
                            imageUrl = commentImg,
                            httpClient = httpClient,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .sizeIn(maxHeight = 100.dp, maxWidth = 240.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentDescription = "评论图片",
                        )
                    }
                }
            }
        }

        // 底部信息栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 时间
            val formattedTime = remember(commentData.createdTime) {
                val time = commentData.createdTime * 1000
                val now = System.currentTimeMillis()
                val dateTime = Date(time)
                val nowDate = Date(now)

                when {
                    isSameDay(dateTime, nowDate) -> HMS.format(time)
                    isSameYear(dateTime, nowDate) -> MDHMS.format(time)
                    else -> YMDHMS.format(time)
                }
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
            if (comment.clickTarget != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onChildCommentClick(comment) },
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
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            // 点赞
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = !isLikeLoading) { toggleLike() },
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

private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isSameYear(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, heightDp = 100)
@Composable
@Suppress("SpellCheckingInspection")
private fun CommentItemPreview() {
    val comment = CommentModel(
        item = DataHolder.Comment(
            id = "123",
            content = "<p>这是一条评论<br/>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum eleifend nisl vitae est tincidunt, non rhoncus magna cursus. Donec non elit non urna dignissim dapibus. Curabitur tempus magna quis dui pellentesque, in venenatis leo mollis. Duis ornare turpis in fermentum mollis. In at fringilla odio. Morbi elementum cursus purus, ut mollis libero facilisis ac. Sed eu mattis ante, ac aliquet purus. Quisque non eros ut ligula tincidunt elementum in ac sem. Praesent diam metus, bibendum vitae mollis ut, vehicula eget ante. Quisque efficitur, odio at ornare commodo, nibh dui eleifend enim, eget consequat quam tortor sit amet arcu. Aliquam mollis auctor ligula, placerat sodales leo malesuada eu. Donec porta nisl at congue laoreet. Duis vel tellus tincidunt, malesuada urna in, maximus nisl. Maecenas rhoncus augue eros, non aliquet eros eleifend ut. Mauris dignissim quis nisi id suscipit. In imperdiet, odio id ornare pretium, eros ipsum faucibus felis, at accumsan mi ex vitae mi.</p>",
            createdTime = System.currentTimeMillis() / 1000,
            author = DataHolder.Comment.Author(
                name = "作者",
                avatarUrl = "https://i1.hdslb.com/bfs/face/b93b6ff0c1d434ae8026a4bedc82d0d883b5da95.jpg",
                isOrg = false,
                type = "people",
                url = "",
                urlToken = "",
                id = "",
                headline = "个人介绍",
                avatarUrlTemplate = "",
                isAdvertiser = false,
                gender = 0,
                userType = "",
            ),
            likeCount = 10,
            childCommentCount = 5,
            type = "",
            url = "",
            resourceType = "",
            collapsed = false,
            top = false,
            isDelete = false,
            reviewing = false,
            isAuthor = false,
            canCollapse = false,
            childComments = emptyList(),
        ),
        clickTarget = null,
    )
    CommentItem(
        comment,
        httpClient = HttpClient(),
        onChildCommentClick = { },
    )
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

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun CommentAuthorTagPreview() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "作者名",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.clickable { },
        )

        Spacer(modifier = Modifier.width(4.dp))
        AuthorTag("作者")

        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "回复",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "zly2006",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.clickable { },
        )
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, heightDp = 100)
@Composable
@Suppress("SpellCheckingInspection")
private fun NestedCommentPreview() {
    val comment = CommentModel(
        item = DataHolder.Comment(
            id = "123",
            content = "<p>这是一条评论<br/>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum eleifend nisl vitae est tincidunt, non rhoncus magna cursus. Donec non elit non urna dignissim dapibus. Curabitur tempus magna quis dui pellentesque, in venenatis leo mollis. Duis ornare turpis in fermentum mollis. In at fringilla odio. Morbi elementum cursus purus, ut mollis libero facilisis ac. Sed eu mattis ante, ac aliquet purus. Quisque non eros ut ligula tincidunt elementum in ac sem. Praesent diam metus, bibendum vitae mollis ut, vehicula eget ante. Quisque efficitur, odio at ornare commodo, nibh dui eleifend enim, eget consequat quam tortor sit amet arcu. Aliquam mollis auctor ligula, placerat sodales leo malesuada eu. Donec porta nisl at congue laoreet. Duis vel tellus tincidunt, malesuada urna in, maximus nisl. Maecenas rhoncus augue eros, non aliquet eros eleifend ut. Mauris dignissim quis nisi id suscipit. In imperdiet, odio id ornare pretium, eros ipsum faucibus felis, at accumsan mi ex vitae mi.</p>",
            createdTime = System.currentTimeMillis() / 1000,
            author = DataHolder.Comment.Author(
                name = "作者",
                avatarUrl = "https://i1.hdslb.com/bfs/face/b93b6ff0c1d434ae8026a4bedc82d0d883b5da95.jpg",
                isOrg = false,
                type = "people",
                url = "",
                urlToken = "",
                id = "",
                headline = "个人介绍",
                avatarUrlTemplate = "",
                isAdvertiser = false,
                gender = 0,
                userType = "",
            ),
            likeCount = 10,
            childCommentCount = 5,
            type = "",
            url = "",
            resourceType = "",
            collapsed = false,
            top = false,
            isDelete = false,
            reviewing = false,
            isAuthor = false,
            canCollapse = false,
            childComments = listOf(
                DataHolder.Comment(
                    id = "千早爱音",
                    content = "<p>我喜欢你</p>",
                    createdTime = System.currentTimeMillis() / 1000,
                    author = DataHolder.Comment.Author(
                        name = "长期素食",
                        avatarUrl = "",
                        isOrg = false,
                        type = "people",
                        url = "",
                        urlToken = "",
                        id = "",
                        headline = "个人介绍",
                        avatarUrlTemplate = "",
                        isAdvertiser = false,
                        gender = 0,
                        userType = "people",
                    ),
                    type = "",
                    isDelete = false,
                    url = "",
                    resourceType = "",
                    collapsed = false,
                    reviewing = false,
                ),
            ),
        ),
        clickTarget = null,
    )
    CommentItem(
        comment,
        httpClient = HttpClient(),
        onChildCommentClick = { },
    )
}
