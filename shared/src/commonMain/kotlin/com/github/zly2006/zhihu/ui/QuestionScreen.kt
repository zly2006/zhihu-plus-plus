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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    var isQuestionDetailExpanded by rememberSaveable(question.questionId) { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val questionContentPreview =
        remember(questionContent) { Ksoup.parse(questionContent).text().trim() }
    val shareText = getShareText(question, title)

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
                                QuestionDetailSection(
                                    questionId = question.questionId,
                                    questionContent = questionContent,
                                    questionContentPreview = questionContentPreview,
                                    isExpanded = isQuestionDetailExpanded,
                                    onToggleExpanded = { isQuestionDetailExpanded = !isQuestionDetailExpanded },
                                )
                            }
                            QuestionPrimaryActions(
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
                            )
                            QuestionSortBar(
                                currentSort = viewModel.sortOrder,
                                onSortChange = { sortOrder ->
                                    viewModel.updateSortOrder(sortOrder)
                                    viewModel.refresh(paginationEnvironment)
                                },
                            )
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
    onNavigateBack: () -> Unit,
    onOpenLog: () -> Unit,
    onShare: () -> Unit,
    canShare: Boolean,
) {
    TopAppBar(
        title = { Text("问题") },
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
                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "查看评论")
                Spacer(Modifier.width(8.dp))
                Text("查看评论")
            }
        }
    }
}

@Composable
private fun QuestionDetailSection(
    questionId: Long,
    questionContent: String,
    questionContentPreview: String,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                onClick = onToggleExpanded,
                modifier = Modifier.testTag(QUESTION_DETAIL_TOGGLE_TAG),
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
                Spacer(Modifier.width(4.dp))
                Text(if (isExpanded) "收起详情" else "展开详情")
            }
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column(modifier = Modifier.testTag(QUESTION_DETAIL_CONTENT_TAG)) {
                QuestionDetailContent(questionId = questionId, html = questionContent)
            }
        }
        AnimatedVisibility(
            visible = !isExpanded && questionContentPreview.isNotEmpty(),
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Text(
                text = questionContentPreview,
                modifier = Modifier.fillMaxWidth().testTag(QUESTION_DETAIL_PREVIEW_TAG),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
