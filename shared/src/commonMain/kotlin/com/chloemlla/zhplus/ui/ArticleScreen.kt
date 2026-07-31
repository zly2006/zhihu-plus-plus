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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.chloemlla.zhplus.markdown.RenderMarkdown
import com.chloemlla.zhplus.markdown.RenderVideoBox
import com.chloemlla.zhplus.navigation.Article
import com.chloemlla.zhplus.navigation.ArticleType
import com.chloemlla.zhplus.navigation.LocalNavigator
import com.chloemlla.zhplus.navigation.Question
import com.chloemlla.zhplus.shared.data.DataHolder
import com.chloemlla.zhplus.shared.data.Person
import com.chloemlla.zhplus.shared.data.ZhihuPaging
import com.chloemlla.zhplus.shared.platform.PlatformBackHandler
import com.chloemlla.zhplus.shared.platform.rememberUserMessageSink
import com.chloemlla.zhplus.shared.ui.AnswerDoubleTapAction
import com.chloemlla.zhplus.shared.util.formatCompactCount
import com.chloemlla.zhplus.theme.ThemeManager
import com.chloemlla.zhplus.ui.components.AnswerHorizontalOverscroll
import com.chloemlla.zhplus.ui.components.AnswerVerticalOverscroll
import com.chloemlla.zhplus.ui.components.AuthorBadge
import com.chloemlla.zhplus.ui.components.CollectionDialogComponent
import com.chloemlla.zhplus.ui.components.CommentScreenComponent
import com.chloemlla.zhplus.ui.components.DraggableRefreshButton
import com.chloemlla.zhplus.ui.components.ExportDialogComponent
import com.chloemlla.zhplus.ui.components.MyModalBottomSheet
import com.chloemlla.zhplus.ui.components.VerticalReadingProgressBar
import com.chloemlla.zhplus.ui.components.VotersSheet
import com.chloemlla.zhplus.ui.components.ZhihuTwoRowsTopAppBar
import com.chloemlla.zhplus.ui.components.rememberPreferCollapsedExitUntilCollapsedScrollBehavior
import com.chloemlla.zhplus.ui.components.rememberShareDialogRuntime
import com.chloemlla.zhplus.util.smoothGradient
import com.chloemlla.zhplus.viewmodel.ArticleViewModel
import com.chloemlla.zhplus.viewmodel.ArticleViewModel.CachedAnswerContent
import com.chloemlla.zhplus.viewmodel.addReadHistory
import com.chloemlla.zhplus.viewmodel.formatArticleDateTime
import com.chloemlla.zhplus.viewmodel.rememberPaginationEnvironment
import com.materialkolor.ktx.harmonize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.painterResource
import zhihu.shared.generated.resources.Res
import zhihu.shared.generated.resources.ic_vote_down_24dp
import zhihu.shared.generated.resources.ic_vote_up_24dp
import kotlin.math.abs
import kotlin.math.max

private const val SCROLL_THRESHOLD = 10 // 滑动阈值，单位为dp
private val ScrollThresholdDp = SCROLL_THRESHOLD.dp

/**
 * 修复 noscript 标签中的图片加载问题。
 * 提取为独立函数，确保主 WebView 和预览 WebView 使用相同的文档处理。
 */
internal fun prepareContentDocument(
    content: String,
    onImageLoadFailure: () -> Unit = {},
): String =
    Ksoup
        .parse(content)
        .apply {
            select("noscript").forEach { noscript ->
                (noscript.nextSibling() as? Element)?.let { actualImg ->
                    if (actualImg.nodeName() == "img") {
                        if (actualImg.attr("data-actualsrc").isNotEmpty()) {
                            actualImg.attr("src", actualImg.attr("data-actualsrc"))
                            actualImg.attr("class", actualImg.attr("class").replace("lazy", ""))
                            noscript.remove()
                            return@forEach
                        }
                    }
                }
                if (noscript.childrenSize() > 0) {
                    val node = noscript.child(0)
                    if (node.tagName() == "img") {
                        if (node.attr("class").contains("content_image")) {
                            node.attr("src", node.attr("data-thumbnail"))
                        }
                        if (node.attr("src").isEmpty()) {
                            if (node.attr("data-default-watermark-src").isNotEmpty()) {
                                node.attr("src", node.attr("data-default-watermark-src"))
                            } else {
                                onImageLoadFailure()
                            }
                        }
                    }
                    noscript.after(node)
                }
            }
        }.body()
        .html()

@Composable
private fun rememberBottomBarAvoidingBringIntoViewSpec(
    obscuredBottomPx: Float,
): BringIntoViewSpec {
    val density = LocalDensity.current
    return remember(obscuredBottomPx) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float,
            ): Float {
                val effectiveContainerSize = (containerSize - obscuredBottomPx).coerceAtLeast(0f)
                val effectiveContainerTop = density.run { 110.dp.toPx() }
                val trailingEdge = offset + size
                return when {
                    offset >= effectiveContainerTop && trailingEdge <= effectiveContainerSize -> 0f
                    offset < effectiveContainerTop && trailingEdge > effectiveContainerSize -> 0f
                    abs(offset) < abs(trailingEdge + effectiveContainerTop - effectiveContainerSize) -> offset - effectiveContainerTop
                    else -> trailingEdge + effectiveContainerTop - effectiveContainerSize
                }
            }
        }
    }
}

enum class VoteUpState(
    val key: String,
) {
    Up("up"),
    Down("down"),
    Neutral("neutral"),
}

private val VoteUpNeutralContent = Color(0xFF3671EE)
private val VoteUpNeutralContentDark = Color(0xFF628DF7)

@Composable
fun voteUpNeutralContent() = if (ThemeManager.isDarkTheme()) VoteUpNeutralContentDark else VoteUpNeutralContent

@Composable
fun voteUpNeutralContentDuo3() = if (ThemeManager.isDarkTheme()) {
    VoteUpNeutralContentDark.harmonize(MaterialTheme.colorScheme.primary)
} else {
    VoteUpNeutralContent.harmonize(MaterialTheme.colorScheme.primary)
}

@Composable
fun voteUpActiveButtonColors() = ButtonDefaults.buttonColors(
    containerColor = voteUpNeutralContent(),
    contentColor = Color.White,
)

@Composable
fun voteUpNeutralButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleSummarySheet(
    showDialog: Boolean,
    summaryText: String,
    loading: Boolean,
    errorMessage: String?,
    onDismissRequest: () -> Unit,
    onRetryRequest: () -> Unit,
) {
    if (!showDialog) return
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    MyModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(scrollState),
        ) {
            Text("总结本文", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (loading && summaryText.isBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("正在生成总结...")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (summaryText.isNotBlank()) {
                    SelectionContainer {
                        Text(summaryText)
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    if (summaryText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("关闭")
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (!loading) {
                    TextButton(onClick = onRetryRequest) {
                        Text("重新总结")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AigcFlagSheet(
    showDialog: Boolean,
    viewModel: ArticleViewModel,
    onDismissRequest: () -> Unit,
    onSubmitRequest: () -> Unit,
) {
    if (!showDialog) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    MyModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val canSubmitAigcFlag = viewModel.aigcVoteAvailable &&
                !viewModel.aigcVoteLoading &&
                !viewModel.aigcFlagged &&
                viewModel.aigcVoterName.isNotBlank() &&
                (
                    viewModel.aigcCreditBypassAvailable ||
                        (viewModel.aigcVoteCredit > 0 && viewModel.isAigcFlagEvidenceReady())
                )
            Text(
                text = "标记疑似 AIGC",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "每浏览 20 篇内容获得 1 点投票积分，最多保留 ${viewModel.aigcVoteCap} 点。标记会上传当前正文 HTML、编辑时间和投票人身份，服务端按内容版本统计。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (!viewModel.aigcVoteAvailable) {
                    "AIGC 标记未启用"
                } else if (viewModel.aigcVoterName.isBlank()) {
                    "未登录，无法记名投票"
                } else {
                    "投票人：${viewModel.aigcVoterName}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (viewModel.aigcCreditBypassAvailable) {
                    "积分 ${viewModel.aigcVoteCredit}/${viewModel.aigcVoteCap} · 当前账号可免积分标记"
                } else {
                    "积分 ${viewModel.aigcVoteCredit}/${viewModel.aigcVoteCap} · 进度 ${viewModel.aigcVoteProgress}/20"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (viewModel.aigcEffectiveFlagCount > 0) {
                    "已有 ${viewModel.aigcEffectiveFlagCount} 个有效标记"
                } else {
                    "当前还没有有效标记"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (viewModel.aigcNamedVoters.isNotEmpty()) {
                Text(
                    text = "记名投票：" + viewModel.aigcNamedVoters.joinToString("、") { voter ->
                        if (voter.creditBypassed) {
                            "${voter.voterName}（免积分）"
                        } else {
                            voter.voterName
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            viewModel.aigcVoteError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("关闭")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSubmitRequest,
                    enabled = canSubmitAigcFlag,
                ) {
                    Text(
                        when {
                            !viewModel.aigcVoteAvailable -> "未启用"
                            viewModel.aigcFlagged -> "已标记"
                            viewModel.aigcVoteLoading -> "提交中"
                            viewModel.aigcVoterName.isBlank() -> "需登录"
                            viewM…17249 tokens truncated…           "IP属地：${viewModel.ipInfo}",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp,
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height((16 + 36).dp))
                                    },
                                )
                            }
                        }
                    }
                    // 状态栏渐变遮罩，仅 duo3 路径需要；主视觉路径不绘制。
                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(statusBarHeight + 16.dp)
                            .background(
                                Brush.verticalGradient(smoothGradient(surfaceColor, 0.8f)),
                            ),
                    ) {}
                }
            }
        }
    } // answerSwitchContent 结束。

    val nav = sharedData?.navigator
    if (article.type == ArticleType.Answer && answerSwitchMode == "horizontal") {
        ArticlePreviewPreloadEffect(nav?.nextAnswer, isNext = true, viewModel.title) {
            userMessages.showMessage("图片加载失败，请向开发者反馈")
        }
        ArticlePreviewPreloadEffect(nav?.previousAnswer, isNext = false, viewModel.title) {
            userMessages.showMessage("图片加载失败，请向开发者反馈")
        }
    }
    val progressBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
    val progressBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 96.dp

    Box(
        modifier = Modifier.fillMaxSize().then(answerDoubleTapModifier),
    ) {
        // 根据模式渲染
        if (article.type == ArticleType.Answer && answerSwitchMode == "vertical") {
            AnswerVerticalOverscroll(
                previousAnswer = nav?.previousAnswer,
                nextAnswer = nav?.nextAnswer,
                onNavigatePrevious = navigateToPrevious,
                onNavigateNext = navigateToNext,
                isAtTop = { scrollState.value == 0 },
                isAtBottom = { scrollState.value >= scrollState.maxValue },
                scrollState = scrollState,
                answerSwitchSensitivity = answerSwitchSensitivity,
            ) {
                MainContent()
            }
        } else if (article.type == ArticleType.Answer && answerSwitchMode == "horizontal") {
            AnswerHorizontalOverscroll(
                canGoPrevious = nav?.previousAnswer != null,
                canGoNext = nav?.nextAnswer != null,
                onNavigatePrevious = navigateToPrevious,
                onNavigateNext = navigateToNext,
                previousContent = nav?.previousAnswer?.let { cached ->
                    { CachedAnswerPreview(cached) }
                },
                nextContent = nav?.nextAnswer?.let { cached ->
                    { CachedAnswerPreview(cached) }
                },
                answerSwitchSensitivity = answerSwitchSensitivity,
            ) {
                MainContent()
            }
        } else {
            MainContent()
        }

        VerticalReadingProgressBar(
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    top = progressBarTopPadding,
                    bottom = progressBarBottomPadding,
                    end = 2.dp,
                ).then(if (isImmersiveMode) Modifier.graphicsLayer { alpha = 0f } else Modifier),
        )

        // 跳转按钮需要压在问题区和回答区之上。
        if (article.type == ArticleType.Answer && buttonSkipAnswer && !isImmersiveMode) {
            val showSkipButton = !autoHideSkipAnswerButton || isScrollingUp || scrollState.value == 0
            val skipButtonAlpha by animateFloatAsState(
                targetValue = if (showSkipButton) 1f else 0f,
                animationSpec = tween(200),
                label = "skipButtonAlpha",
            )
            var fabClickCount by remember { mutableIntStateOf(0) }
            LaunchedEffect(fabClickCount) {
                if (fabClickCount > 0) {
                    delay(350)
                    if (fabClickCount >= 2) {
                        toggleImmersive()
                    } else {
                        if (showSkipButton) {
                            navigatingToNextAnswer = true
                            navigateToNext()
                            navigatingToNextAnswer = false
                        }
                    }
                    fabClickCount = 0
                }
            }
            DraggableRefreshButton(
                modifier = Modifier.graphicsLayer { alpha = skipButtonAlpha },
                onClick = { fabClickCount++ },
                preferenceName = "buttonSkipAnswer",
            ) {
                if (navigatingToNextAnswer) {
                    CircularProgressIndicator(modifier = Modifier.size(30.dp))
                } else {
                    Icon(Icons.Filled.SkipNext, contentDescription = "下一个回答")
                }
            }
        }
    }

    // 全屏菜单
    ArticleActionsMenu(
        article = article,
        viewModel = viewModel,
        showMenu = showActionsMenu,
        onDismissRequest = { showActionsMenu = false },
        onSummaryRequest = {
            showSummaryDialog = true
            viewModel.requestAiSummary(environment)
        },
        onAigcFlagRequest = {
            showAigcFlagSheet = true
            viewModel.loadAigcFlagStatus(environment)
        },
        onExportRequest = { showExportDialog = true },
        onSetImmersiveDoubleTap = {
            showActionsMenu = false
            // 沉浸式模式下，按返回键优先退出沉浸式，不会直接退出回答
            toggleImmersive()
            userMessages.showMessage("已进入沉浸式，按返回键即可退出")
        },
    )

    ArticleSummarySheet(
        showDialog = showSummaryDialog,
        summaryText = viewModel.aiSummaryText,
        loading = viewModel.aiSummaryLoading,
        errorMessage = viewModel.aiSummaryError,
        onDismissRequest = {
            showSummaryDialog = false
            viewModel.cancelAiSummary()
        },
        onRetryRequest = {
            viewModel.requestAiSummary(environment)
        },
    )

    // 沉浸式模式下，返回键优先退出沉浸式
    PlatformBackHandler(enabled = isImmersiveMode) {
        toggleImmersive()
    }

    PlatformBackHandler(showActionsMenu) {
        showActionsMenu = false
    }

    AigcFlagSheet(
        showDialog = showAigcFlagSheet,
        viewModel = viewModel,
        onDismissRequest = { showAigcFlagSheet = false },
        onSubmitRequest = { viewModel.submitAigcFlag(environment) },
    )

    // 使用新的收藏夹对话框组件
    CollectionDialogComponent(
        showDialog = showCollectionDialog,
        onDismiss = { showCollectionDialog = false },
        collections = viewModel.collections,
        onLoadCollections = { viewModel.loadCollections(environment) },
        onToggleFavorite = { collection ->
            viewModel.toggleFavorite(collection.id, collection.isFavorited, environment)
        },
        onCreateCollection = { title, description, isPublic ->
            viewModel.createNewCollection(environment, title, description, isPublic)
        },
    )

    CommentScreenComponent(
        showComments = showComments,
        onDismiss = { showComments = false },
        content = article,
        isZhPlusAuthorContent = article.type == ArticleType.Answer &&
            viewModel.authorId == DataHolder.ZH_PLUS_AUTHOR_USER_ID,
    )
    VotersSheet(
        show = showVoters,
        title = "${formatCompactCount(viewModel.votersTotal)} 人赞同了该回答",
        voters = viewModel.voters,
        isLoading = viewModel.votersLoading,
        errorMessage = viewModel.votersError,
        canLoadMore = viewModel.votersNextUrl != null,
        onDismissRequest = { showVoters = false },
        onLoadMore = { viewModel.loadMoreVoters(environment) },
        onRetry = { viewModel.loadMoreVoters(environment, reset = viewModel.voters.isEmpty()) },
        onNavigate = { person ->
            showVoters = false
            navigator.onNavigate(person)
        },
    )
    if (showDoubleTapActionDialog) {
        MyModalBottomSheet(
            onDismissRequest = { showDoubleTapActionDialog = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "设置双击回答动作",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "选择以后双击回答时默认执行的动作。选择后会立即保存到设置，你也可以稍后在设置中修改。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        showDoubleTapActionDialog = false
                        saveAnswerDoubleTapAction(AnswerDoubleTapAction.None)
                        userMessages.showMessage("已将双击回答动作设为：${AnswerDoubleTapAction.None.label}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("设为无操作")
                }
                Button(
                    onClick = {
                        showDoubleTapActionDialog = false
                        saveAnswerDoubleTapAction(AnswerDoubleTapAction.VoteUp)
                        upVoteFromDoubleTap()
                        userMessages.showMessage("已将双击回答动作设为：${AnswerDoubleTapAction.VoteUp.label}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("设为点赞")
                }
                Button(
                    onClick = {
                        showDoubleTapActionDialog = false
                        saveAnswerDoubleTapAction(AnswerDoubleTapAction.OpenComments)
                        showComments = true
                        userMessages.showMessage("已将双击回答动作设为：${AnswerDoubleTapAction.OpenComments.label}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("设为打开评论区")
                }
                Button(
                    onClick = {
                        showDoubleTapActionDialog = false
                        saveAnswerDoubleTapAction(AnswerDoubleTapAction.ToggleImmersive)
                        toggleImmersive()
                        userMessages.showMessage("已将双击回答动作设为：${AnswerDoubleTapAction.ToggleImmersive.label}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("设为开关沉浸式")
                }
            }
        }
    }
    // 导出对话框
    ExportDialogComponent(
        showDialog = showExportDialog,
        onDismiss = { showExportDialog = false },
        onExportHtml = { includeAppAttribution, onComplete ->
            viewModel.exportToHtml(environment, includeAppAttribution, onComplete)
        },
        onExportImage = { includeAppAttribution, onComplete ->
            viewModel.exportToImage(environment, includeAppAttribution, onComplete)
        },
        onExportMarkdown = {
            viewModel.exportToClipboard(environment)
        },
        onExportImageWithComments = { commentCount, includeAppAttribution, onComplete ->
            viewModel.exportToImageWithComments(environment, commentCount, includeAppAttribution, onComplete)
        },
    )
}

/**
 * 渲染缓存的回答完整内容，用于水平滑动预览。
 *
 * 内容来自 [CachedAnswerContent]，包含标题、作者信息、投票/评论计数和 HTML 正文。正文使用 Compose Markdown，
 * 因此这里是轻量预览，不持有 WebView 或答案切换共享状态。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CachedAnswerPreview(
    cached: CachedAnswerContent,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RectangleShape,
            ),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                Text(
                    text = cached.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        },
        bottomBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(color = Color(0xFF40B6F6)),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF40B6F6),
                                contentColor = Color.Black,
                            ),
                            shape = RectangleShape,
                            contentPadding = PaddingValues(horizontal = 0.dp),
                        ) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(painterResource(Res.drawable.ic_vote_up_24dp), "赞同")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = cached.voteUpCount.toString())
                        }
                    }
                    Button(
                        onClick = {},
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${cached.commentCount}")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
        ) {
            Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (cached.authorAvatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = cached.authorAvatarUrl,
                        contentDescription = "作者头像",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cached.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (cached.authorBadge != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            AuthorBadge(
                                badge = cached.authorBadge,
                            )
                        }
                    }
                    if (cached.authorBio.isNotEmpty()) {
                        Text(
                            text = cached.authorBio,
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                    }
                }
            }
            if (cached.endorsements.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cached.endorsements.forEach { endorsement ->
                        AnswerEndorsementChip(endorsement)
                    }
                }
            }
            if (cached.content.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                RenderMarkdown(
                    html = cached.content,
                    modifier = Modifier,
                    selectable = true,
                    enableScroll = false,
                    header = {},
                    footer = {},
                )
            }
            Spacer(modifier = Modifier.height((16 + 36).dp))
        }
    }
}

@Serializable
data class Collection(
    val id: String,
    val isFavorited: Boolean = false,
    val type: String = "collection",
    val title: String = "",
    val isPublic: Boolean = false,
    val url: String = "",
    val description: String = "",
    val followerCount: Int = 0,
    val answerCount: Int = 0,
    val itemCount: Int = 0,
    val likeCount: Int = 0,
    val viewCount: Int = 0,
    val commentCount: Int = 0,
    val isFollowing: Boolean = false,
    val isLiking: Boolean = false,
    val createdTime: Long = 0L,
    val updatedTime: Long = 0L,
    val creator: Person? = null,
    val isDefault: Boolean = false,
)

@Serializable
data class CollectionResponse(
    val data: List<Collection>,
    val paging: ZhihuPaging,
)
