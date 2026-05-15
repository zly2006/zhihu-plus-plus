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

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.WebviewActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.ui.components.ShareDialog
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.getShareText
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.viewmodel.feed.QuestionFeedViewModel
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/**
 * Instrumented tests inject fixed state and side-effect callbacks here so QuestionScreen can be
 * exercised offline without triggering real detail fetches, follow requests, or comment loading.
 */
data class QuestionScreenTestOverrides(
    val viewModel: QuestionFeedViewModel,
    val initialUiState: QuestionScreenUiState,
    val isEnd: Boolean = true,
    val onRefreshAnswers: (() -> Unit)? = null,
    val onLoadMore: (() -> Unit)? = null,
    val onFollowQuestion: ((Boolean) -> Unit)? = null,
    val onOpenLog: (() -> Unit)? = null,
    val onShareAction: (() -> Unit)? = null,
    val commentSheetContent: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,
    val shareDialogContent: (@Composable (onDismissRequest: () -> Unit) -> Unit)? = null,
)

data class QuestionScreenUiState(
    val questionContent: String = "",
    val answerCount: Int = 0,
    val visitCount: Int = 0,
    val commentCount: Int = 0,
    val followerCount: Int = 0,
    val title: String = "",
    val isFollowing: Boolean = false,
    val isQuestionDetailExpanded: Boolean = true,
)

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
const val QUESTION_COMMENTS_BUTTON_TAG = "question_comments_button"
const val QUESTION_STATS_TAG = "question_stats"

fun questionFeedItemTag(stableKey: String) = "question_feed_item_$stableKey"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionScreen(
    question: Question,
    testOverrides: QuestionScreenTestOverrides? = null,
) {
    val context = LocalContext.current
    val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    val viewModel: QuestionFeedViewModel = testOverrides?.viewModel ?: viewModel(key = "question_${question.questionId}") {
        QuestionFeedViewModel(question.questionId)
    }
    val initialUiState = testOverrides?.initialUiState ?: QuestionScreenUiState(title = question.title)
    val initialTitle = initialUiState.title.ifEmpty { question.title }
    val onRefreshAnswers = testOverrides?.onRefreshAnswers ?: { viewModel.refresh(context) }
    val onLoadMore = testOverrides?.onLoadMore ?: { viewModel.loadMore(context) }
    val isEnd = testOverrides?.let { { it.isEnd } } ?: { viewModel.isEnd }
    var questionContent by remember(question.questionId, initialUiState.questionContent) {
        mutableStateOf(initialUiState.questionContent)
    }
    var answerCount by remember(question.questionId, initialUiState.answerCount) {
        mutableIntStateOf(initialUiState.answerCount)
    }
    var visitCount by remember(question.questionId, initialUiState.visitCount) {
        mutableIntStateOf(initialUiState.visitCount)
    }
    var commentCount by remember(question.questionId, initialUiState.commentCount) {
        mutableIntStateOf(initialUiState.commentCount)
    }
    var followerCount by remember(question.questionId, initialUiState.followerCount) {
        mutableIntStateOf(initialUiState.followerCount)
    }
    var title by remember(question.questionId, initialTitle) { mutableStateOf(initialTitle) }
    var showComments by remember { mutableStateOf(false) }
    var isFollowing by remember(question.questionId, initialUiState.isFollowing) {
        mutableStateOf(initialUiState.isFollowing)
    }
    var showShareDialog by remember { mutableStateOf(false) }
    var isQuestionDetailExpanded by rememberSaveable(question.questionId, initialUiState.isQuestionDetailExpanded) {
        mutableStateOf(initialUiState.isQuestionDetailExpanded)
    }
    val questionContentPreview = remember(questionContent) { Jsoup.parse(questionContent).text().trim() }
    val shareText = getShareText(question, title)

    // 加载问题详情和答案
    LaunchedEffect(question.questionId, testOverrides) {
        if (testOverrides != null) {
            return@LaunchedEffect
        }
        launch {
            AccountData.addReadHistory(
                context,
                question.questionId.toString(),
                "question",
            )
        }
        val activity = context as? MainActivity
        withContext(Dispatchers.IO) {
            try {
                if (viewModel.displayItems.isEmpty()) {
                    launch {
                        viewModel.refresh(context)
                    }
                }
                val questionData = DataHolder.getContentDetail(context, question)
                if (questionData != null) {
                    questionContent = questionData.detail
                    title = questionData.title
                    answerCount = questionData.answerCount
                    visitCount = questionData.visitCount
                    commentCount = questionData.commentCount
                    followerCount = questionData.followerCount
                    isFollowing = questionData.relationship.isFollowing
                    activity?.postHistory(
                        Question(
                            question.questionId,
                            title,
                        ),
                    )
                    ContentOpenEventSupport.recordOpenEvent(
                        context = context,
                        destination = question,
                        questionId = question.questionId,
                        openFrom = activity?.consumePendingContentOpenFrom(question) ?: ContentOpenFrom.UNKNOWN,
                    )
                } else {
                    context.mainExecutor.execute {
                        Toast.makeText(context, "获取问题详情失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                context.mainExecutor.execute {
                    Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    FeedPullToRefresh(viewModel, padding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
            topBar = {
                SelectionContainer(
                    modifier = Modifier.fuckHonorService(),
                ) {
                    Row {
                        Text(
                            text = title,
                            fontSize = 24.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(16.dp)
                                .testTag(QUESTION_TITLE_TAG),
                        )
                    }
                }
            },
        ) { innerPadding ->
            PaginatedList(
                items = viewModel.displayItems,
                onLoadMore = onLoadMore,
                isEnd = isEnd,
                key = { it.stableKey },
                modifier = Modifier
                    .padding(innerPadding)
                    .testTag(QUESTION_SCREEN_LIST_TAG),
                footer = ProgressIndicatorFooter,
                topContent = {
                    item(1) {
                        Box(
                            Modifier.padding(horizontal = 16.dp),
                        ) {
                            if (questionContent.isNotEmpty()) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "问题详情",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        TextButton(
                                            onClick = { isQuestionDetailExpanded = !isQuestionDetailExpanded },
                                            modifier = Modifier.testTag(QUESTION_DETAIL_TOGGLE_TAG),
                                        ) {
                                            Icon(
                                                imageVector = if (isQuestionDetailExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = null,
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (isQuestionDetailExpanded) "收起详情" else "展开详情")
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = isQuestionDetailExpanded,
                                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                                    ) {
                                        Column(
                                            modifier = Modifier.testTag(QUESTION_DETAIL_CONTENT_TAG),
                                        ) {
                                            Spacer(Modifier.height(10.dp))
                                            if (preferences.getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false)) {
                                                WebviewComp {
                                                    it.loadZhihu(
                                                        "https://www.zhihu.com/question/${question.questionId}",
                                                        Jsoup.parse(questionContent),
                                                    )
                                                }
                                            } else {
                                                RenderMarkdown(
                                                    html = questionContent,
                                                    modifier = Modifier.fuckHonorService(),
                                                    selectable = true,
                                                    enableScroll = false,
                                                )
                                            }
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = !isQuestionDetailExpanded && questionContentPreview.isNotEmpty(),
                                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                                    ) {
                                        Text(
                                            text = questionContentPreview,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                                .testTag(QUESTION_DETAIL_PREVIEW_TAG),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(2) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            itemVerticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("排序：", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.updateSortOrder("default")
                                        onRefreshAnswers()
                                    },
                                    modifier = Modifier
                                        .testTag(QUESTION_SORT_DEFAULT_TAG)
                                        .semantics { selected = viewModel.sortOrder == "default" },
                                    colors = if (viewModel.sortOrder == "default") {
                                        ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    } else {
                                        ButtonDefaults.filledTonalButtonColors()
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Text("默认")
                                }
                                Spacer(Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.updateSortOrder("updated")
                                        onRefreshAnswers()
                                    },
                                    modifier = Modifier
                                        .testTag(QUESTION_SORT_UPDATED_TAG)
                                        .semantics { selected = viewModel.sortOrder == "updated" },
                                    colors = if (viewModel.sortOrder == "updated") {
                                        ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    } else {
                                        ButtonDefaults.filledTonalButtonColors()
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Text("最新")
                                }
                            }

                            val scope = rememberCoroutineScope()
                            Button(
                                onClick = {
                                    scope.launch {
                                        val nextFollowing = !isFollowing
                                        testOverrides?.onFollowQuestion?.invoke(nextFollowing) ?: viewModel.followQuestion(
                                            context,
                                            question.questionId,
                                            nextFollowing,
                                        )
                                        isFollowing = nextFollowing
                                        followerCount += if (isFollowing) 1 else -1
                                        if (testOverrides == null) {
                                            Toast
                                                .makeText(
                                                    context,
                                                    if (isFollowing) "已关注问题" else "已取消关注问题",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .testTag(QUESTION_FOLLOW_BUTTON_TAG)
                                    .semantics { selected = isFollowing },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                colors = if (isFollowing) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                },
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = if (isFollowing) "取消关注" else "关注问题")
                                Spacer(Modifier.width(8.dp))
                                Text(if (isFollowing) "已关注" else "关注问题")
                            }
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            itemVerticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Button(
                                onClick = {
                                    testOverrides?.onOpenLog?.invoke() ?: run {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = "https://www.zhihu.com/question/${question.questionId}/log".toUri()
                                                setClass(context, WebviewActivity::class.java)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "打开日志失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.testTag(QUESTION_VIEW_LOG_BUTTON_TAG),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text("查看日志")
                            }
                            Spacer(Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (shareText != null) {
                                        if (testOverrides != null) {
                                            testOverrides.onShareAction?.invoke()
                                            showShareDialog = true
                                        } else {
                                            handleShareAction(context, question) {
                                                showShareDialog = true
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.testTag(QUESTION_SHARE_BUTTON_TAG),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ),
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "分享")
                                Spacer(Modifier.width(8.dp))
                                Text("分享")
                            }

                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { showComments = true },
                                modifier = Modifier.testTag(QUESTION_COMMENTS_BUTTON_TAG),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "$commentCount")
                            }
                        }
                        Text(
                            "$answerCount 个回答  $visitCount 次浏览  $commentCount 条评论  $followerCount 人关注",
                            modifier = Modifier
                                .padding(16.dp)
                                .testTag(QUESTION_STATS_TAG),
                        )
                    }
                },
            ) { item ->
                FeedCard(
                    item = item,
                    modifier = Modifier.testTag(questionFeedItemTag(item.stableKey)),
                )
            }
        }
    }
    testOverrides?.commentSheetContent?.let { content ->
        if (showComments) {
            content { showComments = false }
        }
    } ?: CommentScreenComponent(
        showComments = showComments,
        onDismiss = { showComments = false },
        content = question,
    )

    // 分享对话框
    if (shareText != null) {
        testOverrides?.shareDialogContent?.let { content ->
            if (showShareDialog) {
                content { showShareDialog = false }
            }
        } ?: ShareDialog(
            content = question,
            shareText = shareText,
            showDialog = showShareDialog,
            onDismissRequest = { showShareDialog = false },
            context = context,
        )
    }
}

@Composable
@Preview(showBackground = true)
fun QuestionScreenPreview() {
    val question = Question(123456789, "这是一个问题的标题")
    QuestionScreen(
        question = question,
    )
}
