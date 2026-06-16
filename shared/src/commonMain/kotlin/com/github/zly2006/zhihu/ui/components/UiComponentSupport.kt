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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.nlp.KeywordWithWeight
import com.github.zly2006.zhihu.shared.platform.rememberImageSaver
import com.github.zly2006.zhihu.shared.platform.rememberImageSharer
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.filter.BlockedQuestionAuthor
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import kotlinx.coroutines.launch

data class FeedBlockActions(
    val handleBlockUser: (
        viewModel: BaseFeedViewModel,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<String, String>) -> Unit,
    ) -> Unit,
    val handleBlockQuestionAuthor: (
        viewModel: BaseFeedViewModel,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<String, String>) -> Unit,
    ) -> Unit,
    val handleBlockTopic: (
        viewModel: BaseFeedViewModel,
        topicId: String,
        topicName: String,
    ) -> Unit,
    val handleBlockByKeywords: (
        viewModel: BaseFeedViewModel,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<FeedDisplayItem, Triple<String, String, String?>>) -> Unit,
    ) -> Unit,
)

@Composable
expect fun rememberFeedBlockActions(): FeedBlockActions

data class BlockByKeywordsRuntime(
    val extractKeywords: suspend (
        title: String,
        excerpt: String?,
    ) -> List<KeywordWithWeight>,
    val addNlpPhrase: suspend (String) -> Unit,
)

@Composable
expect fun rememberBlockByKeywordsRuntime(): BlockByKeywordsRuntime

@Composable
fun BlockQuestionAuthorConfirmDialog(
    showDialog: Boolean,
    userToBlock: Pair<String, String>?,
    displayItems: List<FeedDisplayItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val database = remember { getContentFilterDatabase() }
    BlockQuestionAuthorConfirmDialogContent(
        showDialog = showDialog,
        userToBlock = userToBlock,
        displayItems = displayItems,
        onDismiss = onDismiss,
        onConfirmBlock = { author ->
            coroutineScope.launch {
                try {
                    database.blockedQuestionAuthorDao().insertUser(
                        BlockedQuestionAuthor(
                            userId = author.id,
                            userName = author.name,
                            urlToken = author.urlToken,
                            avatarUrl = author.avatarUrl,
                        ),
                    )
                    onConfirm()
                    userMessages.showShortMessage("已屏蔽提问者：${author.name}")
                } catch (e: Exception) {
                    Log.e("FeedBlockActions", "Failed to block question author", e)
                    userMessages.showShortMessage("屏蔽提问者失败: ${e.message}")
                }
            }
        },
    )
}

@Composable
fun BlockUserConfirmDialog(
    showDialog: Boolean,
    userToBlock: Pair<String, String>?,
    displayItems: List<FeedDisplayItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val database = remember { getContentFilterDatabase() }
    BlockUserConfirmDialogContent(
        showDialog = showDialog,
        userToBlock = userToBlock,
        displayItems = displayItems,
        onDismiss = onDismiss,
        onConfirmBlock = { author ->
            coroutineScope.launch {
                try {
                    database.blockedUserDao().insertUser(
                        BlockedUser(
                            userId = author.id,
                            userName = author.name,
                            urlToken = author.urlToken,
                            avatarUrl = author.avatarUrl,
                        ),
                    )
                    onConfirm()
                    userMessages.showShortMessage("已屏蔽用户：${author.name}")
                } catch (e: Exception) {
                    Log.e("FeedBlockActions", "Failed to block user", e)
                    userMessages.showShortMessage("屏蔽用户失败: ${e.message}")
                }
            }
        },
    )
}

@Composable
fun BlockByKeywordsDialog(
    showDialog: Boolean,
    feedTitle: String,
    feedExcerpt: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val runtime = rememberBlockByKeywordsRuntime()

    var extractedKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var keywordInfoList by remember { mutableStateOf<List<KeywordWithWeight>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isAdding by remember { mutableStateOf(false) }

    LaunchedEffect(showDialog, feedTitle, feedExcerpt) {
        if (showDialog) {
            isLoading = true
            try {
                val keywordsWithWeight = runtime.extractKeywords(feedTitle, feedExcerpt)
                keywordInfoList = keywordsWithWeight
                extractedKeywords = keywordsWithWeight.take(8).map { it.keyword }
            } catch (e: Exception) {
                Log.e("FeedBlockActions", "Failed to extract block keywords", e)
                userMessages.showShortMessage("提取关键词失败: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    BlockByKeywordsDialogContent(
        showDialog = showDialog,
        feedTitle = feedTitle,
        feedExcerpt = feedExcerpt,
        extractedKeywords = extractedKeywords,
        keywordInfoList = keywordInfoList,
        isLoading = isLoading,
        isAdding = isAdding,
        onDismiss = onDismiss,
        onConfirmPhrase = { phrase ->
            isAdding = true
            coroutineScope.launch {
                try {
                    runtime.addNlpPhrase(phrase)
                    userMessages.showShortMessage("已添加NLP屏蔽短语: $phrase")
                    onConfirm()
                } catch (e: Exception) {
                    Log.e("FeedBlockActions", "Failed to add NLP block phrase", e)
                    userMessages.showShortMessage("添加失败: ${e.message}")
                } finally {
                    isAdding = false
                }
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OpenImagePreviewContent(
    urls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onOpenInBrowser: (String) -> Unit,
    imageContent: @Composable (
        url: String,
        onClick: () -> Unit,
        onLongClick: (Offset) -> Unit,
        onPageSwipeEnabledChange: (Boolean) -> Unit,
    ) -> Unit,
) {
    val onSaveImage = rememberImageSaver()
    val onShareImage = rememberImageSharer()
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val imageUrls = remember(urls) {
        urls
            .filter { it.isNotBlank() && !it.startsWith("data") }
            .distinct()
            .ifEmpty { listOf("") }
    }
    val initialPage = initialIndex.coerceIn(0, imageUrls.lastIndex)
    val pagerState = rememberPagerState(initialPage = initialPage) { imageUrls.size }
    val pageSwipeEnabled = remember(imageUrls) {
        mutableStateMapOf<String, Boolean>().apply {
            imageUrls.forEach { put(it, true) }
        }
    }
    val currentPageSwipeEnabled = pageSwipeEnabled[imageUrls[pagerState.currentPage]] ?: true
    val verticalSwipeModifier = if (imageUrls.size > 1 && currentPageSwipeEnabled) {
        Modifier.pointerInput(imageUrls.size) {
            val threshold = 96f
            var totalDrag = 0f
            detectVerticalDragGestures(
                onVerticalDrag = { _, dragAmount ->
                    totalDrag += dragAmount
                },
                onDragEnd = {
                    val targetPage = when {
                        totalDrag < -threshold -> pagerState.currentPage + 1
                        totalDrag > threshold -> pagerState.currentPage - 1
                        else -> null
                    }?.coerceIn(0, imageUrls.lastIndex)

                    if (targetPage != null && targetPage != pagerState.currentPage) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                    totalDrag = 0f
                },
                onDragCancel = {
                    totalDrag = 0f
                },
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // 禁用图片查看器自带的震动反馈，保持长按菜单手感稳定。
        CompositionLocalProvider(LocalHapticFeedback provides NoopHapticFeedback) {
            HorizontalPager(
                state = pagerState,
                key = { page -> "$page:${imageUrls[page]}" },
                userScrollEnabled = imageUrls.size > 1 && currentPageSwipeEnabled,
                modifier = Modifier
                    .fillMaxSize()
                    .then(verticalSwipeModifier),
            ) { page ->
                imageContent(
                    imageUrls[page],
                    onDismiss,
                    { offset ->
                        menuOffset = offset
                        showMenu = true
                    },
                    { enabled ->
                        pageSwipeEnabled[imageUrls[page]] = enabled
                    },
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = with(density) {
                DpOffset(
                    menuOffset.x.toDp(),
                    menuOffset.y.toDp(),
                )
            },
        ) {
            DropdownMenuItem(
                text = { Text("保存图片") },
                onClick = {
                    showMenu = false
                    onSaveImage(imageUrls[pagerState.currentPage])
                },
            )
            DropdownMenuItem(
                text = { Text("分享图片") },
                onClick = {
                    showMenu = false
                    onShareImage(imageUrls[pagerState.currentPage])
                },
            )
            DropdownMenuItem(
                text = { Text("在浏览器中打开") },
                onClick = {
                    showMenu = false
                    onOpenInBrowser(imageUrls[pagerState.currentPage])
                },
            )
        }
    }
}

private object NoopHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        // 无操作。
    }
}
