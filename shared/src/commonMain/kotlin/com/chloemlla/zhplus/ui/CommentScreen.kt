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

package com.chloemlla.zhplus.ui

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
import com.chloemlla.zhplus.navigation.Article
import com.chloemlla.zhplus.navigation.CommentHolder
import com.chloemlla.zhplus.navigation.LocalNavigator
import com.chloemlla.zhplus.navigation.NavDestination
import com.chloemlla.zhplus.navigation.Person
import com.chloemlla.zhplus.navigation.Pin
import com.chloemlla.zhplus.navigation.Question
import com.chloemlla.zhplus.navigation.SegmentCommentHolder
import com.chloemlla.zhplus.navigation.resolveContent
import com.chloemlla.zhplus.shared.platform.PlatformBackHandler
import com.chloemlla.zhplus.shared.platform.rememberExternalUrlOpener
import com.chloemlla.zhplus.shared.platform.rememberImagePreviewOpener
import com.chloemlla.zhplus.shared.platform.rememberImageSaver
import com.chloemlla.zhplus.shared.platform.rememberImageSharer
import com.chloemlla.zhplus.shared.platform.rememberSettingsStore
import com.chloemlla.zhplus.shared.util.twoDigitString
import com.chloemlla.zhplus.shared.viewmodel.CommentItem
import com.chloemlla.zhplus.ui.components.replaceSelection
import com.chloemlla.zhplus.ui.subscreens.PREF_FONT_SIZE
import com.chloemlla.zhplus.ui.subscreens.PREF_LINE_HEIGHT
import com.chloemlla.zhplus.viewmodel.comment.BaseCommentViewModel
import com.chloemlla.zhplus.viewmodel.comment.ChildCommentViewModel
import com.chloemlla.zhplus.viewmodel.comment.CommentSortOrder
import com.chloemlla.zhplus.viewmodel.comment.RootCommentViewModel
import com.chloemlla.zhplus.viewmodel.rememberPaginationEnvironment
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
const val COMMENT_EMOJI_BUTTON_TAG = "comment_emoji_button"
const val COMMENT_EMOJI_PICKER_TAG = "comment_emoji_picker"
const val COMMENT_EMOJI_ITEM_TAG_PREFIX = "comment_emoji_item_"
const val COMMENT_SEND_BUTTON_TAG = "comment_send_button"
const val COMMENT_SORT_SCORE_TAG = "comment_sort_score"
const val COMMENT_SORT_TIME_TAG = "comment_sort_time"
const val COMMENT_IMAGE_MENU_OPEN_TAG = "comment_image_menu_open"
const val COMMENT_IMAGE_MENU_BROWSER_TAG = "comment_image_menu_browser"
const val COMMENT_IMAGE_MENU_SAVE_TAG = "comment_image_menu_save"
const val COMMENT_IMAGE_MENU_SHARE_TAG = "comment_image_menu_share"
const val COMMENT_DELETE_DIALOG_TAG = "comment_delete_dialog"
const val COMMENT_DELETE_CONFIRM_TAG = "comment_delete_confirm"
const val COMMENT_DELETE_CANCEL_TAG = "comment_delete_cancel"

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
    val commentEmojis: List<CommentEmoji>? = null,
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
    var showEmojiPicker by remember { mutableStateOf(false) }
    var commentPendingDeletion by remember { mutableStateOf<CommentModel?>(null) }
    var isDeletingComment by remember { mutableStateOf(false) }
    var deleteCommentError by remember { mutableStateOf<String?>(null) }
    val viewModelKey = commentViewModelKey(resolvedContent)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val commentInputFocusRequester = remember { FocusRequester() }
    var commen…9174 tokens truncated…                   modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .testTag(COMMENT_EMOJI_PICKER_TAG),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "暂无可用表情",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 48.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .testTag(COMMENT_EMOJI_PICKER_TAG),
                                    contentPadding = PaddingValues(8.dp),
                                ) {
                                    items(
                                        items = commentEmojis,
                                        key = CommentEmoji::placeholder,
                                    ) { emoji ->
                                        IconButton(
                                            onClick = {
                                                val updatedValue = commentFieldValue.replaceSelection(
                                                    insert = emoji.placeholder,
                                                    cursorOffsetInInsert = emoji.placeholder.length,
                                                )
                                                commentFieldValue = updatedValue
                                                onCommentInputChange(updatedValue.text)
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag(COMMENT_EMOJI_ITEM_TAG_PREFIX + emoji.placeholder),
                                        ) {
                                            Text(
                                                text = remember(emoji) {
                                                    buildAnnotatedString {
                                                        appendInlineContent(emoji.inlineKey, emoji.placeholder)
                                                    }
                                                },
                                                inlineContent = emojiInlineContent,
                                                fontSize = 28.sp,
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
    onDelete: (() -> Unit)? = null,
) {
    val navigator = LocalNavigator.current
    val commentData = comment.item
    var showMoreMenu by remember(commentData.id) { mutableStateOf(false) }

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

            if (onDelete != null) {
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("comment_more_button_${commentData.id}"),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多操作",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                    ) {
                        DropdownMenuItem(
                            modifier = Modifier.testTag("comment_delete_menu_item_${commentData.id}"),
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onDelete()
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

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
