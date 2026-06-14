/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.navigation.CommentHolder
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.shared.viewmodel.CommentItem
import com.github.zly2006.zhihu.ui.AuthorTag
import com.github.zly2006.zhihu.ui.COMMENT_CANCEL_REPLY_TAG
import com.github.zly2006.zhihu.ui.COMMENT_INPUT_TAG
import com.github.zly2006.zhihu.ui.COMMENT_REPLY_BANNER_TAG
import com.github.zly2006.zhihu.ui.COMMENT_SCREEN_LIST_TAG
import com.github.zly2006.zhihu.ui.COMMENT_SEND_BUTTON_TAG
import com.github.zly2006.zhihu.ui.COMMENT_SORT_SCORE_TAG
import com.github.zly2006.zhihu.ui.COMMENT_SORT_TIME_TAG
import com.github.zly2006.zhihu.ui.ClickableImageWithMenu
import com.github.zly2006.zhihu.ui.SwipeToReplyContainer
import com.github.zly2006.zhihu.ui.commentAuthorTag
import com.github.zly2006.zhihu.ui.commentChildButtonTag
import com.github.zly2006.zhihu.ui.commentImageTag
import com.github.zly2006.zhihu.ui.commentLikeButtonTag
import com.github.zly2006.zhihu.ui.commentLikeCountTag
import com.github.zly2006.zhihu.ui.commentReplyButtonTag
import com.github.zly2006.zhihu.ui.commentReplyCountTag
import com.github.zly2006.zhihu.ui.commentReplyToAuthorTag
import com.github.zly2006.zhihu.ui.commentRowTag
import com.github.zly2006.zhihu.ui.commentSelectionWorkaround
import com.github.zly2006.zhihu.ui.commentViewModelKey
import com.github.zly2006.zhihu.ui.dfsSimple
import com.github.zly2006.zhihu.ui.formatCommentTime
import com.github.zly2006.zhihu.ui.rememberCommentEmojiInlineContent
import com.github.zly2006.zhihu.ui.rememberCommentScreenRuntime
import com.github.zly2006.zhihu.viewmodel.comment.BaseCommentViewModel
import com.github.zly2006.zhihu.viewmodel.comment.ChildCommentViewModel
import com.github.zly2006.zhihu.viewmodel.comment.CommentSortOrder
import com.github.zly2006.zhihu.viewmodel.comment.RootCommentViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 评论列表的 miuix 版本。
 *
 * 复用 [CommentItem] / 评论 ViewModel / 富文本解析（[dfsSimple]）/ 时间格式化（[formatCommentTime]）/
 * 左右滑动手势（[SwipeToReplyContainer]）/ 图片长按菜单（[ClickableImageWithMenu]）等主题无关逻辑，
 * 仅把行/排序/输入栏的视觉层换成 miuix 组件。容器（圆角/拖动条/标题/背景）由外层 [MiuixCommentSheet] 提供，
 * 因此本组件自身不再绘制 Surface 外壳。
 */
@Composable
fun MiuixCommentScreen(
    content: () -> NavDestination,
    activeCommentItem: CommentItem? = null,
    onChildCommentClick: (CommentItem) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val runtime = rememberCommentScreenRuntime()
    var commentInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var replyToComment by remember { mutableStateOf<CommentItem?>(null) }
    val resolvedContent = content()
    val viewModelKey = commentViewModelKey(resolvedContent)

    val viewModel: BaseCommentViewModel = when (resolvedContent) {
        is CommentHolder -> remember(viewModelKey) { ChildCommentViewModel(resolvedContent) }
        else -> viewModel(key = viewModelKey) { RootCommentViewModel(resolvedContent) }
    }
    val rootContent = when (resolvedContent) {
        is CommentHolder -> resolvedContent.article
        else -> resolvedContent
    }

    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= layoutInfo.totalItemsCount - 3 && !viewModel.isLoading && !viewModel.isEnd
        }
    }
    LaunchedEffect(loadMore.value) {
        if (loadMore.value && viewModel.errorMessage == null) {
            viewModel.loadMore(environment)
        }
    }
    LaunchedEffect(resolvedContent) {
        if (viewModel.article != resolvedContent) error("Internal Error: Detected content mismatch")
        if (viewModel.errorMessage == null) viewModel.loadMore(environment)
    }
    val scope = rememberCoroutineScope()

    fun submitComment() {
        if (commentInput.isBlank() || isSending) return
        isSending = true
        viewModel.submitComment(
            content = resolvedContent,
            commentText = commentInput,
            environment = environment,
            replyToCommentId = replyToComment?.item?.id,
        ) {
            commentInput = ""
            replyToComment = null
            isSending = false
            scope.launch { listState.animateScrollToItem(0, 0) }
        }
    }

    // 让弹层内容撑到接近全屏高度（WindowBottomSheet 按内容尺寸定高）。
    val windowInfo = LocalWindowInfo.current
    val sheetHeight = with(LocalDensity.current) { (windowInfo.containerSize.height * 0.82f).toDp() }

    @Composable
    fun CommentBlock(commentItem: CommentItem, modifier: Modifier = Modifier) {
        var isLiked by remember { mutableStateOf(commentItem.item.liked) }
        var likeCount by remember { mutableIntStateOf(commentItem.item.likeCount) }

        Column(modifier = modifier) {
            MiuixCommentRow(
                comment = commentItem,
                runtime = runtime,
                isLiked = isLiked,
                likeCount = likeCount,
                toggleLike = {
                    viewModel.toggleLikeComment(commentItem.item, environment) {
                        isLiked = !isLiked
                        likeCount += if (isLiked) 1 else -1
                        commentItem.item.liked = isLiked
                        commentItem.item.likeCount = likeCount
                    }
                },
                onChildCommentClick = onChildCommentClick,
            )

            if (activeCommentItem == null && commentItem.item.childCommentCount > 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 40.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    commentItem.item.childComments.forEach { childComment ->
                        var liked by remember { mutableStateOf(childComment.liked) }
                        var childLikeCount by remember { mutableIntStateOf(childComment.likeCount) }
                        val childCommentItem = CommentItem(item = childComment, clickTarget = null)
                        MiuixCommentRow(
                            comment = childCommentItem,
                            runtime = runtime,
                            modifier = Modifier.testTag(commentRowTag(childComment.id)),
                            isLiked = liked,
                            likeCount = childLikeCount,
                            toggleLike = {
                                viewModel.toggleLikeComment(childCommentItem.item, environment) {
                                    liked = !liked
                                    childLikeCount += if (liked) 1 else -1
                                    childCommentItem.item.liked = liked
                                    childCommentItem.item.likeCount = childLikeCount
                                }
                            },
                            onChildCommentClick = onChildCommentClick,
                        )
                    }
                    Text(
                        text = "查看 ${commentItem.item.childCommentCount} 条子评论",
                        color = MiuixTheme.colorScheme.primary,
                        modifier = Modifier
                            .testTag(commentChildButtonTag(commentItem.item.id))
                            .clip(RoundedCornerShape(50))
                            .clickable { onChildCommentClick(commentItem) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().height(sheetHeight).imePadding(),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when {
                viewModel.isLoading && viewModel.allData.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

                viewModel.errorMessage != null && viewModel.allData.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(viewModel.errorMessage!!, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }

                activeCommentItem == null && viewModel.allData.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无评论", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag(COMMENT_SCREEN_LIST_TAG),
                    contentPadding = PaddingValues(bottom = 16.dp, start = 12.dp, end = 12.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (activeCommentItem != null) {
                        item(key = "active_${activeCommentItem.item.id}") {
                            Column {
                                CommentBlock(activeCommentItem)
                                HorizontalDivider(Modifier.padding(top = 12.dp))
                                if (viewModel.allData.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                        Text("暂无回复", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                    }
                                }
                            }
                        }
                    } else {
                        item("sorting") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                            ) {
                                SortChip("最热", viewModel.sortOrder == CommentSortOrder.SCORE, COMMENT_SORT_SCORE_TAG) {
                                    viewModel.changeSortOrder(CommentSortOrder.SCORE, environment)
                                }
                                Spacer(Modifier.width(16.dp))
                                SortChip("最新", viewModel.sortOrder == CommentSortOrder.TIME, COMMENT_SORT_TIME_TAG) {
                                    viewModel.changeSortOrder(CommentSortOrder.TIME, environment)
                                }
                            }
                        }
                    }

                    items(items = viewModel.allData, key = { it.id }) { dto ->
                        val commentItem = viewModel.createCommentItem(dto, article = rootContent)
                        SwipeToReplyContainer(
                            modifier = Modifier.testTag(commentRowTag(dto.id)),
                            onReply = {
                                if (activeCommentItem == null) {
                                    commentItem.clickTarget?.let { onChildCommentClick(commentItem) }
                                } else {
                                    replyToComment = commentItem
                                }
                            },
                        ) {
                            CommentBlock(commentItem)
                        }
                    }

                    if (viewModel.isLoading && viewModel.allData.isNotEmpty()) {
                        item(key = "loading_indicator") {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }

        // 回复目标提示栏 + 输入栏
        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = replyToComment != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(COMMENT_REPLY_BANNER_TAG)
                        .background(MiuixTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MiuixTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "回复 ${replyToComment?.item?.author?.name ?: ""}",
                        color = MiuixTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { replyToComment = null },
                        modifier = Modifier.size(28.dp).testTag(COMMENT_CANCEL_REPLY_TAG),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "取消回复",
                            modifier = Modifier.size(16.dp),
                            tint = MiuixTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    modifier = Modifier.weight(1f).heightIn(max = 140.dp).testTag(COMMENT_INPUT_TAG),
                    label = if (replyToComment != null) "回复 ${replyToComment?.item?.author?.name}..." else "写下你的评论...",
                    useLabelAsPlaceholder = true,
                    maxLines = 5,
                )
                IconButton(
                    onClick = { submitComment() },
                    enabled = !isSending && commentInput.isNotBlank(),
                    modifier = Modifier.size(40.dp).testTag(COMMENT_SEND_BUTTON_TAG),
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Icon(
                            Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "发送评论",
                            tint = if (commentInput.isNotBlank()) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(text: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .testTag(tag)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun MiuixCommentRow(
    comment: CommentItem,
    runtime: com.github.zly2006.zhihu.ui.CommentScreenRuntime,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
    likeCount: Int = 0,
    toggleLike: () -> Unit = {},
    onChildCommentClick: (CommentItem) -> Unit,
) {
    val navigator = LocalNavigator.current
    val commentData = comment.item
    val secondary = MiuixTheme.colorScheme.onSurfaceVariantSummary

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = commentData.author.avatarUrl,
                contentDescription = "头像",
                modifier = Modifier.size(36.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = commentData.author.name,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .testTag(commentAuthorTag(commentData.id))
                            .clickable {
                                navigator.onNavigate(
                                    Person(commentData.author.id, commentData.author.name, commentData.author.urlToken),
                                )
                            },
                    )
                    comment.item.authorTag.firstOrNull()?.get("text")?.jsonPrimitive?.contentOrNull?.let {
                        Spacer(Modifier.width(4.dp))
                        AuthorTag(it)
                    }
                    commentData.replyToAuthor?.let { replyToAuthor ->
                        Spacer(Modifier.width(4.dp))
                        Text("回复", color = secondary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = replyToAuthor.name,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .testTag(commentReplyToAuthorTag(commentData.id))
                                .clickable {
                                    navigator.onNavigate(Person(replyToAuthor.id, replyToAuthor.name, replyToAuthor.urlToken))
                                },
                        )
                    }
                }

                val document = remember(commentData.content) { Ksoup.parseBodyFragment(commentData.content) }
                val commentImg = document.selectFirst("a.comment_img")?.attr("href")
                    ?: document.selectFirst("a.comment_gif")?.attr("href")
                    ?: document.selectFirst("a.comment_sticker")?.attr("href")
                val emojisUsed = remember { mutableSetOf<String>() }
                val openExternalUrl = rememberExternalUrlOpener()
                val string = remember(commentData.content) {
                    emojisUsed.clear()
                    buildAnnotatedString {
                        val stripped = document.body().clone()
                        stripped.select("a.comment_img").forEach { it.remove() }
                        stripped.select("a.comment_gif").forEach { it.remove() }
                        stripped.select("a.comment_sticker").forEach { it.remove() }
                        dfsSimple(stripped, navigator.onNavigate, openExternalUrl, emojisUsed)
                    }
                }
                val inlineContent = rememberCommentEmojiInlineContent(emojisUsed)

                SelectionContainer(modifier = Modifier.commentSelectionWorkaround()) {
                    Text(text = string, color = MiuixTheme.colorScheme.onSurface, inlineContent = inlineContent)
                }
                if (commentImg != null) {
                    ClickableImageWithMenu(
                        imageUrl = commentImg,
                        runtime = runtime,
                        modifier = Modifier
                            .testTag(commentImageTag(commentData.id))
                            .padding(top = 8.dp)
                            .sizeIn(maxHeight = 100.dp, maxWidth = 240.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentDescription = "评论图片",
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(start = 44.dp, top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = remember(commentData.createdTime) { formatCommentTime(commentData.createdTime) }, color = secondary)

            comment.item.commentTag.firstOrNull { it.type == "ip_info" }?.text?.let {
                Spacer(Modifier.width(8.dp))
                Text(text = it, color = secondary)
            }

            Spacer(Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag(commentReplyButtonTag(commentData.id)).clickable { onChildCommentClick(comment) },
            ) {
                Icon(Icons.AutoMirrored.Outlined.Comment, "回复", modifier = Modifier.size(16.dp), tint = secondary)
                if (comment.item.childCommentCount > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = comment.item.childCommentCount.toString(),
                        color = secondary,
                        modifier = Modifier.testTag(commentReplyCountTag(commentData.id)),
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag(commentLikeButtonTag(commentData.id)).clickable { toggleLike() },
            ) {
                Icon(
                    if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    "点赞",
                    modifier = Modifier.size(16.dp),
                    tint = if (isLiked) MiuixTheme.colorScheme.primary else secondary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = likeCount.toString(),
                    color = if (isLiked) MiuixTheme.colorScheme.primary else secondary,
                    modifier = Modifier.testTag(commentLikeCountTag(commentData.id)),
                )
            }
        }
    }
}
