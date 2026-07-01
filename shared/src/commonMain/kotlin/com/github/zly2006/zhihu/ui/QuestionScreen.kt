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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.github.zly2006.zhihu.ui.subscreens.PREF_FONT_SIZE
import com.github.zly2006.zhihu.ui.subscreens.PREF_LINE_HEIGHT
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
    val include = "read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics"
    val jsonObject = environment.fetchJson("https://www.zhihu.com/api/v4/questions/${question.questionId}", include)
        ?: return null
    val questionData = decodeQuestionContentDetail(jsonObject)
    environment.postHistoryDestination(Question(question.questionId, questionData.title))
    environment.recordContentOpenEvent(destination = question, questionId = question.questionId)
    return questionData
}

/**
 * 问题详情页。
 *
 * 顶部展示问题标题、描述、关注状态和统计信息，主体是该问题下回答的信息流列表。页面会记录内容打开来源和历史记录，
 * 并复用文章/回答卡片、评论底部表单和分享入口；正文描述同样受 WebView/Markdown 渲染设置影响。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionScreen(
    question: Question,
) {
    val settings = rememberSettingsStore()
    val shareRuntime = rememberShareDialogRuntime()
    val openZhihuWebUrl = rememberZhihuWebUrlOpener()
    val navigator = LocalNavigator.current
    val viewModel: QuestionFeedViewModel = viewModel(key = "question_${question.questionId}") {
        QuestionFeedViewModel(question.questionId)
    }
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = false)
    val answerSwitchState = paginationEnvironment.articleAnswerSwitchState()
    var questionContent by remember(question.questionId) {
        mutableStateOf("")
    }
    var answerCount by remember(question.questionId) {
        mutableIntStateOf(0)
    }
    var visitCount by remember(question.questionId) {
        mutableIntStateOf(0)
    }
    var commentCount by remember(question.questionId) {
        mutableIntStateOf(0)
    }
    var followerCount by remember(question.questionId) {
        mutableIntStateOf(0)
    }
    var title by remember(question.questionId, question.title) { mutableStateOf(question.title) }
    var showComments by rememberSaveable(question.questionId) { mutableStateOf(false) }
    var isFollowing by remember(question.questionId) {
        mutableStateOf(false)
    }
    var showShareDialog by remember { mutableStateOf(false) }
    val userMessages = rememberUserMessageSink()
    var isQuestionDetailExpanded by rememberSaveable(question.questionId) {
        mutableStateOf(true)
    }
    val questionContentPreview = remember(questionContent) { Ksoup.parse(questionContent).text().trim() }
    val shareText = getShareText(question, title)

    // 加载问题详情和答案
    LaunchedEffect(question.questionId, viewModel) {
        if (viewModel.displayItems.isEmpty()) {
            launch {
                viewModel.refresh(paginationEnvironment)
            }
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

    FeedPullToRefresh(viewModel, padding = PaddingValues(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
            topBar = {
                SelectionContainer(
                    modifier = Modifier.questionSelectionWorkaround(),
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
                onLoadMore = { viewModel.loadMore(paginationEnvironment) },
                isEnd = { viewModel.isEnd },
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
                                            QuestionDetailContent(
                                                questionId = question.questionId,
                                                html = questionContent,
                                            )
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = !isQuestionDetailExpanded && questionContentPreview.isNotEmpty(),
                                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                                    ) {
                                        val fontSizePercent = remember {
                                            settings.getInt(PREF_FONT_SIZE, 100)
                                        }
                                        val lineHeightPercent = remember {
                                            settings.getInt(PREF_LINE_HEIGHT, 160)
                                        }
                                        Text(
                                            text = questionContentPreview,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                                .testTag(QUESTION_DETAIL_PREVIEW_TAG),
                                            fontSize = 16.sp * fontSizePercent / 100,
                                            lineHeight = 16.sp * fontSizePercent / 100 * lineHeightPercent / 100,
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
                                        viewModel.refresh(paginationEnvironment)
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
                                        viewModel.refresh(paginationEnvironment)
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
                                        viewModel.followQuestion(
                                            paginationEnvironment,
                                            nextFollowing,
                                        )
                                        isFollowing = nextFollowing
                                        followerCount += if (isFollowing) 1 else -1
                                        userMessages.showShortMessage(if (isFollowing) "已关注问题" else "已取消关注问题")
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
                                    try {
                                        openZhihuWebUrl("https://www.zhihu.com/question/${question.questionId}/log")
                                    } catch (e: Exception) {
                                        userMessages.showShortMessage("打开日志失败: ${e.message}")
                                    }
                                },
                                modifier = Modifier.testTag(QUESTION_VIEW_LOG_BUTTON_TAG),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text("日志")
                            }

                            Button(
                                onClick = {
                                    if (shareText != null) {
                                        handleShareAction(question, settings, shareRuntime) {
                                            showShareDialog = true
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
                            }

                            Button(
                                onClick = {
                                    navigator.onNavigate(
                                        WriteAnswer(
                                            questionId = question.questionId,
                                            questionTitle = title,
                                            questionDetail = questionContent,
                                        ),
                                    )
                                },
                                modifier = Modifier.testTag(QUESTION_WRITE_ANSWER_BUTTON_TAG),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "写回答")
                                Spacer(Modifier.width(8.dp))
                                Text("写回答")
                            }
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
                    modifier = Modifier.testTag("question_feed_item_${item.stableKey}"),
                ) {
                    val dest = navDestination
                    answerSwitchState?.pendingNavigator = viewModel.createAnswerNavigatorFor(item, paginationEnvironment)
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
