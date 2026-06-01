/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.WebviewActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.ARTICLE_USE_WEBVIEW_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.QUESTION_COMMENTS_BUTTON_TAG
import com.github.zly2006.zhihu.ui.QUESTION_DETAIL_TOGGLE_TAG
import com.github.zly2006.zhihu.ui.QUESTION_FOLLOW_BUTTON_TAG
import com.github.zly2006.zhihu.ui.QUESTION_SCREEN_LIST_TAG
import com.github.zly2006.zhihu.ui.QUESTION_SHARE_BUTTON_TAG
import com.github.zly2006.zhihu.ui.QUESTION_SORT_DEFAULT_TAG
import com.github.zly2006.zhihu.ui.QUESTION_SORT_UPDATED_TAG
import com.github.zly2006.zhihu.ui.QUESTION_STATS_TAG
import com.github.zly2006.zhihu.ui.QUESTION_VIEW_LOG_BUTTON_TAG
import com.github.zly2006.zhihu.ui.QuestionScreenTestOverrides
import com.github.zly2006.zhihu.ui.QuestionScreenUiState
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.ui.components.ShareDialog
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.getShareText
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.viewmodel.feed.QuestionFeedViewModel
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun MiuixQuestionScreen(
    question: Question,
    testOverrides: QuestionScreenTestOverrides? = null,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    val viewModel: QuestionFeedViewModel = testOverrides?.viewModel ?: viewModel(key = "question_${question.questionId}") {
        QuestionFeedViewModel(question.questionId)
    }
    val initialUiState = testOverrides?.initialUiState ?: QuestionScreenUiState(title = question.title)
    val onRefreshAnswers = testOverrides?.onRefreshAnswers ?: { viewModel.refresh(context) }
    val onLoadMore = testOverrides?.onLoadMore ?: { viewModel.loadMore(context) }
    val isEnd = testOverrides?.let { { it.isEnd } } ?: { viewModel.isEnd }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var questionContent by remember(question.questionId) { mutableStateOf(initialUiState.questionContent) }
    var answerCount by remember(question.questionId) { mutableIntStateOf(initialUiState.answerCount) }
    var visitCount by remember(question.questionId) { mutableIntStateOf(initialUiState.visitCount) }
    var commentCount by remember(question.questionId) { mutableIntStateOf(initialUiState.commentCount) }
    var followerCount by remember(question.questionId) { mutableIntStateOf(initialUiState.followerCount) }
    var title by remember(question.questionId) { mutableStateOf(initialUiState.title.ifEmpty { question.title }) }
    var isFollowing by remember(question.questionId) { mutableStateOf(initialUiState.isFollowing) }
    var showComments by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var isQuestionDetailExpanded by rememberSaveable(question.questionId) { mutableStateOf(initialUiState.isQuestionDetailExpanded) }
    val questionContentPreview = remember(questionContent) { Jsoup.parse(questionContent).text().trim() }
    val shareText = getShareText(question, title)

    val blurEnabled = remember { mutableStateOf(preferences.getBoolean("blurEnabled", true)) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled.value)
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(question.questionId, testOverrides) {
        if (testOverrides != null) return@LaunchedEffect
        launch { AccountData.addReadHistory(context, question.questionId.toString(), "question") }
        val activity = context as? MainActivity
        withContext(Dispatchers.IO) {
            try {
                if (viewModel.displayItems.isEmpty()) launch { viewModel.refresh(context) }
                val questionData = DataHolder.getContentDetail(context, question)
                if (questionData != null) {
                    questionContent = questionData.detail
                    title = questionData.title
                    answerCount = questionData.answerCount
                    visitCount = questionData.visitCount
                    commentCount = questionData.commentCount
                    followerCount = questionData.followerCount
                    isFollowing = questionData.relationship.isFollowing
                    activity?.postHistory(Question(question.questionId, title))
                    ContentOpenEventSupport.recordOpenEvent(
                        context = context,
                        destination = question,
                        questionId = question.questionId,
                        openFrom = activity?.consumePendingContentOpenFrom(question) ?: ContentOpenFrom.UNKNOWN,
                    )
                } else {
                    context.mainExecutor.execute { Toast.makeText(context, "获取问题详情失败", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                context.mainExecutor.execute { Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = title,
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            PullToRefresh(
                isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
                onRefresh = { scope.launch { viewModel.pullToRefresh(context) } },
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
                refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
            ) {
                PaginatedList(
                    items = viewModel.displayItems,
                    listState = listState,
                    modifier = Modifier.fillMaxSize()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .testTag(QUESTION_SCREEN_LIST_TAG),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding(),
                    ),
                    onLoadMore = onLoadMore,
                    isEnd = isEnd,
                    key = { it.stableKey },
                    footer = ProgressIndicatorFooter,
                    topContent = {
                        // 问题详情（可展开/收起）；标题由 TopAppBar 大字标题展示，不在此重复
                        item(1) {
                            if (questionContent.isNotEmpty()) {
                                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("问题详情", style = MiuixTheme.textStyles.title3)
                                            TextButton(
                                                text = if (isQuestionDetailExpanded) "收起详情" else "展开详情",
                                                onClick = { isQuestionDetailExpanded = !isQuestionDetailExpanded },
                                                modifier = Modifier.testTag(QUESTION_DETAIL_TOGGLE_TAG),
                                            )
                                        }
                                        AnimatedVisibility(
                                            visible = isQuestionDetailExpanded,
                                            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                                        ) {
                                            Column {
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
                                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                                style = MiuixTheme.textStyles.body2,
                                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // 排序 / 关注 / 操作 / 统计
                        item(2) {
                            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SortButton("默认", viewModel.sortOrder == "default", QUESTION_SORT_DEFAULT_TAG) {
                                                viewModel.updateSortOrder("default"); onRefreshAnswers()
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            SortButton("最新", viewModel.sortOrder == "updated", QUESTION_SORT_UPDATED_TAG) {
                                                viewModel.updateSortOrder("updated"); onRefreshAnswers()
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val next = !isFollowing
                                                    testOverrides?.onFollowQuestion?.invoke(next)
                                                        ?: viewModel.followQuestion(context, question.questionId, next)
                                                    isFollowing = next
                                                    followerCount += if (next) 1 else -1
                                                    if (testOverrides == null) {
                                                        Toast.makeText(context, if (next) "已关注问题" else "已取消关注问题", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag(QUESTION_FOLLOW_BUTTON_TAG),
                                            colors = ButtonDefaults.buttonColorsPrimary(),
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    if (isFollowing) Icons.Filled.Check else Icons.Filled.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.width(18.dp),
                                                    tint = MiuixTheme.colorScheme.onPrimary,
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(if (isFollowing) "已关注" else "关注问题", color = MiuixTheme.colorScheme.onPrimary)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            text = "查看日志",
                                            modifier = Modifier.weight(1f).testTag(QUESTION_VIEW_LOG_BUTTON_TAG),
                                            onClick = {
                                                testOverrides?.onOpenLog?.invoke() ?: try {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW).apply {
                                                            data = "https://www.zhihu.com/question/${question.questionId}/log".toUri()
                                                            setClass(context, WebviewActivity::class.java)
                                                        },
                                                    )
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "打开日志失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                        )
                                        TextButton(
                                            text = "分享",
                                            modifier = Modifier.weight(1f).testTag(QUESTION_SHARE_BUTTON_TAG),
                                            onClick = {
                                                if (shareText != null) {
                                                    if (testOverrides != null) {
                                                        testOverrides.onShareAction?.invoke(); showShareDialog = true
                                                    } else {
                                                        handleShareAction(context, question) { showShareDialog = true }
                                                    }
                                                }
                                            },
                                        )
                                        TextButton(
                                            text = "评论 $commentCount",
                                            modifier = Modifier.weight(1f).testTag(QUESTION_COMMENTS_BUTTON_TAG),
                                            onClick = { showComments = true },
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "$answerCount 个回答 · $visitCount 次浏览 · $commentCount 条评论 · $followerCount 人关注",
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                        modifier = Modifier.testTag(QUESTION_STATS_TAG),
                                    )
                                }
                            }
                        }
                    },
                ) { item ->
                    // horizontalPadding=12 与问题详情卡片（horizontal=12）对齐；vertical 走外层 modifier
                    MiuixFeedCard(item = item, modifier = Modifier.padding(vertical = 4.dp), horizontalPadding = 12.dp)
                }
            }
        }
    }

    testOverrides?.commentSheetContent?.let { content ->
        if (showComments) content { showComments = false }
    } ?: CommentScreenComponent(
        showComments = showComments,
        onDismiss = { showComments = false },
        content = question,
    )

    if (shareText != null) {
        testOverrides?.shareDialogContent?.let { content ->
            if (showShareDialog) content { showShareDialog = false }
        } ?: ShareDialog(
            content = question,
            shareText = shareText,
            showDialog = showShareDialog,
            onDismissRequest = { showShareDialog = false },
            context = context,
        )
    }
}

/** 排序按钮：选中态用实心 Button（primary），未选中用 TextButton */
@Composable
private fun SortButton(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.testTag(tag), colors = ButtonDefaults.buttonColorsPrimary()) {
            Text(label, color = MiuixTheme.colorScheme.onPrimary)
        }
    } else {
        TextButton(text = label, onClick = onClick, modifier = Modifier.testTag(tag))
    }
}
