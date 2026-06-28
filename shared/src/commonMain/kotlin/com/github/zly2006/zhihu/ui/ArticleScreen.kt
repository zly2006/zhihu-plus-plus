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
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.markdown.RenderVideoBox
import com.github.zly2006.zhihu.navigation.AnswerNavigator
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.SegmentCommentHolder
import com.github.zly2006.zhihu.navigation.newAnswerSessionId
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.shared.data.ZhihuPaging
import com.github.zly2006.zhihu.shared.platform.PlatformBackHandler
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.shared.util.formatCompactCount
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.ui.components.AnswerContentSkeleton
import com.github.zly2006.zhihu.ui.components.AuthorBadge
import com.github.zly2006.zhihu.ui.components.CollectionDialogComponent
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.ExportDialogComponent
import com.github.zly2006.zhihu.ui.components.LocalPageTurnChannel
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.github.zly2006.zhihu.ui.components.PageTurnGuideOverlay
import com.github.zly2006.zhihu.ui.components.VerticalReadingProgressBar
import com.github.zly2006.zhihu.ui.components.VotersSheet
import com.github.zly2006.zhihu.ui.components.ZhihuTwoRowsTopAppBar
import com.github.zly2006.zhihu.ui.components.pageTurnModalDepth
import com.github.zly2006.zhihu.ui.components.rememberPreferCollapsedExitUntilCollapsedScrollBehavior
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.ui.components.verticalPagerScrollGate
import com.github.zly2006.zhihu.ui.subscreens.DEFAULT_PAGE_TURN_PERCENT
import com.github.zly2006.zhihu.ui.subscreens.PREF_PAGE_TURN_PERCENT
import com.github.zly2006.zhihu.ui.subscreens.PREF_SHOW_PAGE_TURN_GUIDE
import com.github.zly2006.zhihu.util.smoothGradient
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.addReadHistory
import com.github.zly2006.zhihu.viewmodel.formatArticleDateTime
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
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
                            viewModel.aigcCreditBypassAvailable -> "免积分标记"
                            viewModel.aigcVoteCredit <= 0 -> "积分不足"
                            !viewModel.isAigcFlagEvidenceReady() -> "继续阅读"
                            else -> "消耗 1 点标记"
                        },
                    )
                }
            }
        }
    }
}

/**
 * 文章附件中的视频入口渲染。
 *
 * 只处理知乎接口里 `attachment.type=video` 的情况，将视频 ID 和缩略图交给统一的视频卡片。普通正文视频仍由 Markdown/WebView 路径处理。
 */
@Composable
fun ArticleVideoAttachmentContent(attachment: JsonElement?) {
    if (attachment
            ?.jsonObject
            ?.get("type")
            ?.jsonPrimitive
            ?.content == "video"
    ) {
        val videoId = attachment
            .jsonObject["attachmentId"]
            ?.jsonPrimitive
            ?.content
            ?.toLongOrNull()
        if (videoId != null) {
            val thumbnail = attachment
                .jsonObject["video"]!!
                .jsonObject["videoInfo"]!!
                .jsonObject["thumbnail"]!!
                .jsonPrimitive.content
            RenderVideoBox(
                videoId = videoId,
                thumbnailUrl = thumbnail,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleActionsMenu(
    article: Article,
    viewModel: ArticleViewModel,
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
    onSummaryRequest: () -> Unit,
    onAigcFlagRequest: () -> Unit,
    onExportRequest: () -> Unit,
    onSetImmersiveDoubleTap: () -> Unit = {},
) {
    val ttsState = rememberArticleTtsState()
    val toggleSpeech = rememberArticleSpeechToggler()
    val openArticleInBrowser = rememberArticleBrowserOpener()
    val shareRuntime = rememberShareDialogRuntime()
    val coroutineScope = rememberCoroutineScope()

    @Composable
    fun MenuActionButton(
        icon: @Composable () -> Unit,
        text: String,
        enabled: Boolean = true,
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick: () -> Unit,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onClick() },
            shape = RoundedCornerShape(12.dp),
            color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(24.dp)) {
                    icon()
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                )
            }
        }
    }

    @Composable
    fun MenuActionButton(
        icon: ImageVector,
        text: String,
        enabled: Boolean = true,
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick: () -> Unit,
    ) {
        MenuActionButton(
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                )
            },
            text = text,
            enabled = enabled,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            onClick = onClick,
        )
    }

    @Composable
    fun Content() {
        MenuActionButton(
            icon = {
                when (ttsState) {
                    TtsState.Initializing, TtsState.Uninitialized -> CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        strokeWidth = 2.dp,
                    )

                    else -> Icon(
                        if (ttsState.isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            text = if (ttsState.isSpeaking) "停止朗读" else "开始朗读",
            enabled = ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing),
            onClick = {
                onDismissRequest()
                if (ttsState.isSpeaking) {
                    toggleSpeech(viewModel.title, viewModel.content)
                } else if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                    // 使用协程在后台处理文本提取，避免UI阻塞
                    viewModel.viewModelScope.launch {
                        try {
                            // 在IO线程中处理文本提取
                            withContext(Dispatchers.Default) {
                                val textToRead = articleSpeechText(viewModel.title, viewModel.content)

                                // 回到主线程执行TTS
                                withContext(Dispatchers.Main) {
                                    if (textToRead.isNotBlank()) {
                                        toggleSpeech(viewModel.title, viewModel.content)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Unit
                            }
                        }
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 分享按钮
        MenuActionButton(
            icon = Icons.Filled.Share,
            text = "分享",
            onClick = {
                onDismissRequest()
                shareRuntime.share(
                    article,
                    articleActionText(article, viewModel.questionId, viewModel.title, viewModel.authorName),
                )
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        MenuActionButton(
            icon = Icons.AutoMirrored.Filled.Comment,
            text = "总结本文",
            onClick = {
                onDismissRequest()
                onSummaryRequest()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        MenuActionButton(
            icon = Icons.Filled.Flag,
            text = "标记疑似 AIGC",
            onClick = {
                onDismissRequest()
                onAigcFlagRequest()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 复制链接按钮
        MenuActionButton(
            icon = Icons.Filled.ContentCopy,
            text = "复制链接",
            onClick = {
                onDismissRequest()
                shareRuntime.copyLink(
                    article,
                    articleActionText(article, viewModel.questionId, viewModel.title, viewModel.authorName),
                )
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 开关沉浸式
        MenuActionButton(
            icon = Icons.Filled.FilterCenterFocus,
            text = "进入沉浸式",
            onClick = {
                onDismissRequest()
                onSetImmersiveDoubleTap()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 导出按钮
        MenuActionButton(
            icon = Icons.Filled.GetApp,
            text = "导出文章 (Markdown、图片、HTML、PDF)",
            onClick = {
                onDismissRequest()
                onExportRequest()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        MenuActionButton(
            icon = Icons.Outlined.DesktopWindows,
            text = "在电脑中打开（我计划使用浏览器插件实现，还在写，点击后请手动前往收藏夹打开）",
            onClick = {
                coroutineScope.launch {
                    openArticleInBrowser(article)
                    onDismissRequest()
                }
            },
        )

        // 底部安全区域
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showMenu) {
        MyModalBottomSheet(onDismissRequest) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Content()
            }
        }
    }
}

/**
 * 文章/回答详情页。
 *
 * 页面负责加载知乎回答或专栏文章，展示标题、作者、正文、附件视频、评论入口、分享/复制/朗读/浏览器打开等底部操作，
 * 正文主路径使用 Compose Markdown 渲染。回答页还承载同题回答切换手势和对应转场状态，因此改动时要同时关注
 * `answerSwitchMode`、`buttonSkipAnswer`、`autoHideArticleBottomBar`、`titleAutoHide`、`answerDoubleTapAction` 和
 * `ARTICLE_USE_WEBVIEW_PREFERENCE_KEY`。
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
fun ArticleScreen(
    article: Article,
    viewModel: ArticleViewModel,
    answerSwitchPagerEnabled: Boolean = true,
    pagerNavigateToPrevious: (() -> Unit)? = null,
    pagerNavigateToNext: (() -> Unit)? = null,
    commentsHostedByPager: Boolean = false,
    onRequestOpenComments: () -> Unit = {},
    isPagerPageActive: Boolean = true,
) {
    val navigator = LocalNavigator.current
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val articleHost = rememberArticleHost()
    val backStackEntry by articleHost?.articleNavController?.currentBackStackEntryAsState()
        ?: remember { mutableStateOf(null) }

    val scrollState = rememberScrollState()
    val articleSettings = rememberArticleScreenSettingsState()

    var isTitleAutoHide by remember { mutableStateOf(articleSettings.isTitleAutoHide) }
    var autoHideArticleBottomBar by remember {
        mutableStateOf(articleSettings.autoHideArticleBottomBar)
    }
    var answerSwitchMode by remember {
        mutableStateOf(articleSettings.answerSwitchMode)
    }
    val pageOwnsHistory = !commentsHostedByPager &&
        !(answerSwitchPagerEnabled && article.type == ArticleType.Answer && answerSwitchMode != "off")
    var answerSwitchSensitivity by remember {
        mutableFloatStateOf(articleSettings.answerSwitchSensitivity)
    }
    var pinAnswerDate by remember { mutableStateOf(articleSettings.pinAnswerDate) }
    val userMessages = rememberUserMessageSink()

    var previousScrollValue by remember { mutableIntStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(false) }
    var navigatingToNextAnswer by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val scrollDeltaThreshold = with(density) { ScrollThresholdDp.toPx() }
    var pageKeyViewportHeight by remember { mutableIntStateOf(0) }
    var lastPageTurnDirection by remember { mutableIntStateOf(0) }
    var isPageTurnScrolling by remember { mutableStateOf(false) }
    val pageKeySettings = rememberSettingsStore()
    val pageTurnPercent = remember { pageKeySettings.getInt(PREF_PAGE_TURN_PERCENT, DEFAULT_PAGE_TURN_PERCENT) }
    val showPageTurnGuide = remember { pageKeySettings.getBoolean(PREF_SHOW_PAGE_TURN_GUIDE, false) }

    if (showPageTurnGuide && lastPageTurnDirection != 0 && scrollState.isScrollInProgress && !isPageTurnScrolling) {
        lastPageTurnDirection = 0
    }
    var topBarHeight by remember { mutableIntStateOf(0) }
    var showComments by rememberSaveable(article.type, article.id) { mutableStateOf(false) }
    var segmentCommentTarget by remember { mutableStateOf<SegmentCommentHolder?>(null) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var showAigcFlagSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDoubleTapActionDialog by remember { mutableStateOf(false) }
    var showVoters by rememberSaveable(article.type, article.id) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(isPagerPageActive) {
        if (!isPagerPageActive) {
            if (!commentsHostedByPager) {
                showComments = false
            }
            showCollectionDialog = false
            showActionsMenu = false
            showSummaryDialog = false
            showAigcFlagSheet = false
            showExportDialog = false
            showDoubleTapActionDialog = false
            showVoters = false
            viewModel.cancelAiSummary()
        }
    }

    val useDuo3ArticleActions = remember { articleSettings.useDuo3ArticleActions }
    var buttonSkipAnswer by remember { mutableStateOf(articleSettings.buttonSkipAnswer) }
    var autoHideSkipAnswerButton by remember { mutableStateOf(articleSettings.autoHideSkipAnswerButton) }
    var answerDoubleTapAction by remember {
        mutableStateOf(
            articleSettings.answerDoubleTapAction,
        )
    }
    // 跟手隐藏标题栏和底栏：用滚动增量直接驱动像素偏移。
    val topBarOffset = remember { Animatable(0f) }
    val bottomBarOffset = remember { Animatable(0f) }
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    var bottomBarHeightPx by remember { mutableFloatStateOf(0f) }
    var previousScrollForBarOffset by remember { mutableIntStateOf(0) }
    var isBarSnapping by remember { mutableStateOf(false) }

    LaunchedEffect(article.type, article.id, isPagerPageActive, pageOwnsHistory) {
        if (isPagerPageActive && pageOwnsHistory) {
            environment.addReadHistory(
                contentToken = article.id.toString(),
                contentTypeName = article.type.name.lowercase(),
            )
        }
    }

    val pageTurnChannel = LocalPageTurnChannel.current
    LaunchedEffect(pageTurnChannel) {
        pageTurnChannel.collect { direction ->
            if (pageTurnModalDepth.intValue > 0) return@collect
            lastPageTurnDirection = direction.coerceIn(-1, 1)
            isPageTurnScrolling = true
            try {
                when (direction) {
                    Int.MAX_VALUE -> {
                        scrollState.scrollTo(scrollState.maxValue)
                        if (isTitleAutoHide && topBarHeightPx > 0f) topBarOffset.snapTo(-topBarHeightPx)
                    }
                    Int.MIN_VALUE -> {
                        scrollState.scrollTo(0)
                        topBarOffset.snapTo(0f)
                    }
                    else -> {
                        if (isTitleAutoHide && topBarHeightPx > 0f && direction > 0) {
                            topBarOffset.snapTo(-topBarHeightPx)
                        }
                        val visibleTopBar = (topBarHeightPx + topBarOffset.value).coerceAtLeast(0f)
                        val visibleBottomBar = (bottomBarHeightPx - bottomBarOffset.value).coerceAtLeast(0f)
                        val effectiveViewport = pageKeyViewportHeight - visibleTopBar.toInt() - visibleBottomBar.toInt()
                        if (effectiveViewport > 0) {
                            scrollState.scrollBy(effectiveViewport * pageTurnPercent / 100f * direction)
                        }
                        if (direction < 0 && scrollState.value == 0 && isTitleAutoHide) {
                            topBarOffset.snapTo(0f)
                        }
                    }
                }
            } finally {
                isPageTurnScrolling = false
            }
        }
    }

    fun upVoteFromDoubleTap() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
        if (viewModel.voteUpState != VoteUpState.Up) {
            viewModel.toggleVoteUp(environment, VoteUpState.Up)
        }
    }
    // 回答切换手势系统
    val sharedData = if (article.type == ArticleType.Answer) {
        environment.articleAnswerSwitchState()
    } else {
        null
    }
    val effectiveAnswerSessionId = if (article.type == ArticleType.Answer) {
        remember(article.type, article.id, article.answerSessionId) {
            article.answerSessionId ?: newAnswerSessionId(article.id)
        }
    } else {
        null
    }
    if (article.type == ArticleType.Answer) {
        viewModel.answerSessionId = effectiveAnswerSessionId
    }

    LaunchedEffect(article.id, isPagerPageActive, pageOwnsHistory, viewModel.answerOpenRecordRevision) {
        if (pageOwnsHistory) {
            viewModel.tryRecordAnswerOpenIfReady(
                environment = environment,
                isActive = isPagerPageActive,
            )
        }
    }

    fun currentAnswerNavigator(): AnswerNavigator? =
        if (effectiveAnswerSessionId != null) {
            sharedData?.sessionRegistry?.get(effectiveAnswerSessionId)
        } else {
            sharedData?.navigator
        }

    if (
        answerSwitchPagerEnabled &&
        article.type == ArticleType.Answer &&
        answerSwitchMode != "off"
    ) {
        val nav = effectiveAnswerSessionId?.let { sessionId ->
            sharedData?.sessionRegistry?.get(sessionId)
        }
        val sessionArticle = if (article.answerSessionId == effectiveAnswerSessionId) {
            article
        } else {
            article.copy(answerSessionId = effectiveAnswerSessionId)
        }
        if (nav == null || nav.session.orderedIds.isEmpty()) {
            ArticleScreen(
                article = sessionArticle,
                viewModel = viewModel,
                answerSwitchPagerEnabled = false,
                isPagerPageActive = true,
            )
        } else {
            val orientation = when (answerSwitchMode) {
                "horizontal" -> AnswerPagerOrientation.Horizontal
                else -> AnswerPagerOrientation.Vertical
            }
            AnswerSwitchPagerScreen(
                orientation = orientation,
                initialArticle = sessionArticle,
                initialViewModel = viewModel,
                navigator = nav,
                environment = environment,
                userMessages = userMessages,
                answerSwitchSensitivity = answerSwitchSensitivity,
            )
        }
        return
    }

    // 沉浸式阅读模式
    var isImmersiveMode by remember(sharedData) {
        mutableStateOf(sharedData?.isImmersiveMode ?: false)
    }

    val toggleImmersive: () -> Unit = { isImmersiveMode = !isImmersiveMode }

    fun openComments() {
        if (commentsHostedByPager) {
            onRequestOpenComments()
        } else {
            showComments = true
        }
    }

    fun performAnswerDoubleTapAction(action: AnswerDoubleTapAction) {
        when (action) {
            AnswerDoubleTapAction.None -> Unit
            AnswerDoubleTapAction.Ask -> showDoubleTapActionDialog = true
            AnswerDoubleTapAction.VoteUp -> upVoteFromDoubleTap()
            AnswerDoubleTapAction.OpenComments -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                openComments()
            }
            AnswerDoubleTapAction.ToggleImmersive -> {
                toggleImmersive()
            }
        }
    }

    fun saveAnswerDoubleTapAction(action: AnswerDoubleTapAction) {
        answerDoubleTapAction = action
        articleSettings.saveAnswerDoubleTapAction(action)
    }

    fun handleAnswerDoubleTap() {
        if (article.type != ArticleType.Answer) return
        performAnswerDoubleTapAction(answerDoubleTapAction)
    }

    val answerDoubleTapModifier = if (
        article.type == ArticleType.Answer &&
        answerDoubleTapAction != AnswerDoubleTapAction.None
    ) {
        Modifier.pointerInput(answerDoubleTapAction) {
            detectTapGestures(
                onDoubleTap = { handleAnswerDoubleTap() },
            )
        }
    } else {
        Modifier
    }

    LaunchedEffect(articleSettings.isTitleAutoHide) {
        isTitleAutoHide = articleSettings.isTitleAutoHide
    }
    LaunchedEffect(articleSettings.autoHideArticleBottomBar) {
        autoHideArticleBottomBar = articleSettings.autoHideArticleBottomBar
    }
    LaunchedEffect(articleSettings.buttonSkipAnswer) {
        buttonSkipAnswer = articleSettings.buttonSkipAnswer
    }
    LaunchedEffect(articleSettings.autoHideSkipAnswerButton) {
        autoHideSkipAnswerButton = articleSettings.autoHideSkipAnswerButton
    }
    LaunchedEffect(articleSettings.answerSwitchMode) {
        answerSwitchMode = articleSettings.answerSwitchMode
    }
    LaunchedEffect(articleSettings.answerSwitchSensitivity) {
        answerSwitchSensitivity = articleSettings.answerSwitchSensitivity
    }
    LaunchedEffect(articleSettings.pinAnswerDate) {
        pinAnswerDate = articleSettings.pinAnswerDate
    }
    LaunchedEffect(articleSettings.answerDoubleTapAction) {
        answerDoubleTapAction = articleSettings.answerDoubleTapAction
    }

    // 自动隐藏关闭时重置栏位偏移。
    LaunchedEffect(isTitleAutoHide) {
        if (!isTitleAutoHide) topBarOffset.snapTo(0f)
    }
    LaunchedEffect(autoHideArticleBottomBar) {
        if (!autoHideArticleBottomBar) bottomBarOffset.snapTo(0f)
    }

    LaunchedEffect(scrollState.value) {
        val currentScroll = scrollState.value
        val scrollDeltaAbs = abs(currentScroll - previousScrollValue)
        if (scrollDeltaAbs > scrollDeltaThreshold) {
            isScrollingUp = currentScroll < previousScrollValue
            previousScrollValue = currentScroll
        }

        // 吸附动画期间滚动由程序驱动，跳过栏位偏移跟踪。
        if (!isBarSnapping) {
            val delta = currentScroll - previousScrollForBarOffset
            val atTop = currentScroll == 0
            val atBottom = currentScroll >= scrollState.maxValue

            // 顶栏：顶部强制显示，接近底部时按内容距离逐步露出。
            if (atTop) {
                topBarOffset.snapTo(0f)
            } else if (isTitleAutoHide && topBarHeightPx > 0f) {
                val deltaBasedOffset = (topBarOffset.value - delta).coerceIn(-topBarHeightPx, 0f)
                val distanceFromBottom = (scrollState.maxValue - currentScroll).coerceAtLeast(0)
                if (distanceFromBottom < topBarHeightPx.toInt()) {
                    // 底部区域取露出更多栏位的偏移，也就是更接近 0 的值。
                    val distanceBasedOffset = (-distanceFromBottom.toFloat()).coerceIn(-topBarHeightPx, 0f)
                    topBarOffset.snapTo(maxOf(distanceBasedOffset, deltaBasedOffset))
                } else {
                    topBarOffset.snapTo(deltaBasedOffset)
                }
            }

            // 底栏：顶部强制显示，接近底部时按内容距离逐步露出。
            if (atTop) {
                bottomBarOffset.snapTo(0f)
            } else if (autoHideArticleBottomBar && bottomBarHeightPx > 0f) {
                val deltaBasedOffset = (bottomBarOffset.value + delta).coerceIn(0f, bottomBarHeightPx)
                val distanceFromBottom = (scrollState.maxValue - currentScroll).coerceAtLeast(0)
                if (distanceFromBottom < bottomBarHeightPx.toInt()) {
                    // 底部区域取露出更多栏位的偏移。
                    val distanceBasedOffset = distanceFromBottom.toFloat().coerceIn(0f, bottomBarHeightPx)
                    bottomBarOffset.snapTo(minOf(distanceBasedOffset, deltaBasedOffset))
                } else {
                    bottomBarOffset.snapTo(deltaBasedOffset)
                }
            }
        }
        previousScrollForBarOffset = currentScroll

        viewModel.updateAigcReadProgress(currentScroll, scrollState.maxValue)
        viewModel.syncAigcReadEventIfEligible(environment)

        if (viewModel.rememberedScrollYSync) {
            viewModel.rememberedScrollY = currentScroll
        }
        if (currentScroll == viewModel.rememberedScrollY && scrollState.maxValue != Int.MAX_VALUE) {
            viewModel.rememberedScrollYSync = true
        }
    }

    // 滚动停止时把栏位吸附到完全显示或完全隐藏，并让内容滚动跟随顶栏吸附。
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {
            val topTarget = if (isTitleAutoHide && topBarHeightPx > 0f) {
                if (abs(topBarOffset.value) > topBarHeightPx / 2) -topBarHeightPx else 0f
            } else {
                topBarOffset.value
            }

            val bottomTarget = if (autoHideArticleBottomBar && bottomBarHeightPx > 0f) {
                if (bottomBarOffset.value > bottomBarHeightPx / 2) bottomBarHeightPx else 0f
            } else {
                bottomBarOffset.value
            }

            // 仅在靠近顶部时补偿顶栏吸附导致的内容位移；底栏交给距离触发的露出逻辑处理。
            val topInNaturalArea = scrollState.value <= topBarHeightPx
            val topDelta = if (topInNaturalArea) topBarOffset.value - topTarget else 0f

            if (topTarget != topBarOffset.value || bottomTarget != bottomBarOffset.value) {
                try {
                    isBarSnapping = true
                    kotlinx.coroutines.coroutineScope {
                        launch { topBarOffset.animateTo(topTarget, tween(150)) }
                        launch { bottomBarOffset.animateTo(bottomTarget, tween(150)) }
                        if (topDelta != 0f) {
                            launch { scrollState.animateScrollBy(topDelta, tween(150)) }
                        }
                    }
                } finally {
                    isBarSnapping = false
                }
            }
        }
    }

    // 主视觉风格的栏位显隐：按滚动方向控制，用于非跟手偏移路径。
    val showTopBar by remember {
        derivedStateOf {
            val canScroll = scrollState.maxValue > topBarHeight
            val isNearTop = scrollState.value < topBarHeight
            when {
                !isTitleAutoHide -> true
                !canScroll -> true
                isScrollingUp -> true
                isNearTop -> true
                else -> false
            }
        }
    }
    val showBottomBar by remember {
        derivedStateOf {
            val canScroll = scrollState.maxValue > 0
            val isNearTop = scrollState.value == 0
            when {
                !autoHideArticleBottomBar -> true
                !canScroll -> true
                isScrollingUp -> true
                isNearTop -> true
                else -> false
            }
        }
    }
    val showBottomBarSlot = backStackEntry?.hasRoute(Article::class) == true || articleHost == null
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    val bottomBarObscuredHeightPx by remember(
        showBottomBarSlot,
        true,
        showBottomBar,
        bottomBarHeightPx,
        bottomBarOffset.value,
    ) {
        derivedStateOf {
            val navBar = density.run {
                navigationBarsPadding.calculateBottomPadding().toPx().coerceAtLeast(0f)
            }
            val bottonBar = if (!showBottomBarSlot) {
                0f
            } else if (true) {
                (bottomBarHeightPx - bottomBarOffset.value).coerceIn(0f, bottomBarHeightPx)
            } else if (showBottomBar) {
                bottomBarHeightPx
            } else {
                0f
            }
            navBar + bottonBar
        }
    }
    val articleBringIntoViewSpec = rememberBottomBarAvoidingBringIntoViewSpec(bottomBarObscuredHeightPx)
    LaunchedEffect(sharedData, isImmersiveMode) {
        if (sharedData != null) sharedData.isImmersiveMode = isImmersiveMode
    }
    ArticleImmersiveModeEffect(isImmersiveMode)

    DisposableEffect(backStackEntry?.id, sharedData) {
        onDispose {
            // Navigator 按本次回答详情实例 id 归属，不再按返回栈 entry 迁移或复用。
        }
    }

    LaunchedEffect(article.id) {
        if (sharedData != null) {
            if (answerSwitchPagerEnabled) {
                currentAnswerNavigator()?.let { routeNavigator ->
                    sharedData.navigator = routeNavigator
                }
                sharedData.clearSwitchUiState()
            }

            viewModel.onCurrentPageReady(
                environment = environment,
                warmStart = false,
                alignNavigatorOnReady = !commentsHostedByPager,
            ) {
                if (!commentsHostedByPager) {
                    coroutineScope.launch {
                        delay(350)
                        withContext(Dispatchers.Default) {
                            if (isPagerPageActive) {
                                val paginationInfo = viewModel.currentAnswerPaginationInfo()
                                val pageNavigator = currentAnswerNavigator()
                                pageNavigator?.onPageSettled(
                                    article.id,
                                    direction = null,
                                    paginationInfo,
                                ) { id ->
                                    pageNavigator.prefetchPrevious(id)
                                    pageNavigator.prefetchNext(id)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            viewModel.onCurrentPageReady(environment, warmStart = false)
        }
        viewModel.loadCollections(environment)
        viewModel.loadAigcFlagStatus(environment)
    }

    LaunchedEffect(article.type, article.id, viewModel.content, isPagerPageActive) {
        if (isPagerPageActive && viewModel.content.isNotBlank()) {
            viewModel.updateAigcReadProgress(scrollState.value, scrollState.maxValue)
            delay(15_000)
            viewModel.updateAigcReadProgress(scrollState.value, scrollState.maxValue)
            viewModel.syncAigcReadEventIfEligible(environment)
        }
    }
    LaunchedEffect(scrollState.maxValue, viewModel.content, isPagerPageActive) {
        if (isPagerPageActive && viewModel.content.isNotBlank()) {
            viewModel.updateAigcReadProgress(scrollState.value, scrollState.maxValue)
        }
    }

    fun navigateToAdjacentAnswer(isNext: Boolean) {
        if (!answerSwitchPagerEnabled && answerSwitchMode != "off") {
            if (isNext) {
                pagerNavigateToNext?.invoke()
            } else {
                pagerNavigateToPrevious?.invoke()
            }
        }
    }

    val navigateToNext: () -> Unit = {
        navigateToAdjacentAnswer(isNext = true)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainContent() {
        val scrollBehavior = rememberPreferCollapsedExitUntilCollapsedScrollBehavior()
        // 记录历史最大滚动范围，避免顶栏展开/收起时 maxValue 短暂变化导致 scrollBehavior 抖动。
        var scrollStateMaxValue by remember { mutableIntStateOf(0) }
        LaunchedEffect(scrollState.maxValue) {
            if (scrollState.maxValue != Int.MAX_VALUE) {
                scrollStateMaxValue = max(scrollState.maxValue, scrollStateMaxValue)
            }
        }
        LaunchedEffect(pageTurnChannel) {
            pageTurnChannel.collect { direction ->
                if (pageTurnModalDepth.intValue > 0) return@collect
                val state = scrollBehavior.state
                if (direction > 0 || direction == Int.MAX_VALUE) {
                    state.heightOffset = state.heightOffsetLimit
                } else if (direction == Int.MIN_VALUE) {
                    state.heightOffset = 0f
                } else if (direction < 0 && scrollState.value == 0) {
                    state.heightOffset = 0f
                }
            }
        }
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isImmersiveMode) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier),
            topBar = if (isImmersiveMode) {
                {}
            } else {
                {
                    Box(
                        modifier = Modifier
                            .onSizeChanged {
                                topBarHeightPx = it.height.toFloat()
                                if (it.height >= 10) topBarHeight = it.height
                            }.graphicsLayer {
                                translationY = topBarOffset.value
                                alpha = if (topBarHeightPx > 0f) 1f + (topBarOffset.value / topBarHeightPx) else 1f
                            },
                    ) {
                        ZhihuTwoRowsTopAppBar(
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        articleHost?.articleNavController?.popBackStack()
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            },
                            actions = {
                                if (useDuo3ArticleActions) {
                                    IconButton(
                                        onClick = { showActionsMenu = true },
                                    ) {
                                        Icon(
                                            Icons.Filled.MoreVert,
                                            contentDescription = "更多选项",
                                        )
                                    }
                                }
                            },
                            title = { expanded ->
                                Text(
                                    text = viewModel.title,
                                    modifier = Modifier
                                        .padding(if (expanded) PaddingValues(end = 16.dp) else PaddingValues())
                                        .let {
                                            if (article.type == ArticleType.Answer) {
                                                it.clickable {
                                                    navigator.onNavigate(Question(viewModel.questionId, viewModel.title))
                                                }
                                            } else {
                                                it
                                            }
                                        },
                                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            subtitle = { expanded ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(if (expanded) PaddingValues(vertical = 16.dp) else PaddingValues(top = 2.dp, bottom = 8.dp))
                                        .padding(end = 16.dp)
                                        .fillMaxWidth()
                                        .clickable {
                                            navigator.onNavigate(
                                                com.github.zly2006.zhihu.navigation.Person(
                                                    id = viewModel.authorId,
                                                    urlToken = viewModel.authorUrlToken,
                                                    name = viewModel.authorName,
                                                ),
                                            )
                                        },
                                ) {
                                    if (viewModel.authorAvatarSrc.isNotEmpty()) {
                                        AsyncImage(
                                            model = viewModel.authorAvatarSrc,
                                            contentDescription = "作者头像",
                                            modifier = Modifier
                                                .size(if (expanded) 40.dp else 20.dp)
                                                .clip(CircleShape),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(if (expanded) 40.dp else 20.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(if (expanded) 8.dp else 4.dp))

                                    Column(
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = viewModel.authorName,
                                                style = if (expanded) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                                                color = if (expanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            if (viewModel.authorBadge != null) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                AuthorBadge(
                                                    badge = viewModel.authorBadge,
                                                    compact = !expanded,
                                                )
                                            }
                                        }
                                        if (viewModel.authorBio.isNotEmpty() && expanded) {
                                            Text(
                                                text = viewModel.authorBio,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            },
                            scrollBehavior = if (scrollStateMaxValue > 0) scrollBehavior else null,
                            colors = TopAppBarDefaults.topAppBarColors().copy(
                                scrolledContainerColor = if (MaterialTheme.colorScheme.surfaceContainer != MaterialTheme.colorScheme.background) {
                                    MaterialTheme.colorScheme.surfaceContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            ),
                        )
                    }
                }
            },
            bottomBar = if (isImmersiveMode) {
                {}
            } else {
                @Composable {
                    // 防止在导航动画和预测性返回手势过程中，底部操作栏闪烁。
                    val showBottomBarCondition = backStackEntry?.hasRoute(Article::class) == true || articleHost == null

                    // 操作栏内容的共享组合，按 useDuo3ArticleActions 切换两套视觉。
                    @Composable
                    fun ActionBarContent() {
                        if (!useDuo3ArticleActions) {
                            // ── 主视觉：按钮式投票与操作区 ────────────────────────
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 8.dp)
                                    .height(36.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            color = if (viewModel.voteUpState == VoteUpState.Neutral) {
                                                voteUpNeutralContent().copy(alpha = 0.1f)
                                            } else {
                                                voteUpNeutralContent()
                                            },
                                        ),
                                    horizontalArrangement = Arrangement.Start,
                                ) {
                                    when (viewModel.voteUpState) {
                                        VoteUpState.Neutral -> {
                                            Button(
                                                onClick = { viewModel.toggleVoteUp(environment, VoteUpState.Up) },
                                                colors = voteUpNeutralButtonColors(),
                                                shape = RectangleShape,
                                                contentPadding = PaddingValues(horizontal = 0.dp),
                                            ) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(painterResource(Res.drawable.ic_vote_up_24dp), "赞同")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = viewModel.voteUpCount.toString())
                                            }
                                            Button(
                                                onClick = { viewModel.toggleVoteUp(environment, VoteUpState.Down) },
                                                colors = voteUpNeutralButtonColors(),
                                                shape = RectangleShape,
                                                modifier = Modifier.height(ButtonDefaults.MinHeight).width(ButtonDefaults.MinHeight),
                                                contentPadding = PaddingValues(horizontal = 0.dp),
                                            ) {
                                                Icon(painterResource(Res.drawable.ic_vote_down_24dp), "反对")
                                            }
                                        }

                                        VoteUpState.Up -> {
                                            Button(
                                                onClick = { viewModel.toggleVoteUp(environment, VoteUpState.Neutral) },
                                                colors = voteUpActiveButtonColors(),
                                                shape = RectangleShape,
                                                contentPadding = PaddingValues(horizontal = 0.dp),
                                            ) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(painterResource(Res.drawable.ic_vote_up_24dp), "赞同")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = viewModel.voteUpCount.toString())
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                        }

                                        VoteUpState.Down -> {
                                            Button(
                                                onClick = { viewModel.toggleVoteUp(environment, VoteUpState.Neutral) },
                                                colors = voteUpActiveButtonColors(),
                                                shape = RectangleShape,
                                                modifier = Modifier.height(ButtonDefaults.MinHeight),
                                                contentPadding = PaddingValues(horizontal = 0.dp),
                                            ) {
                                                Icon(painterResource(Res.drawable.ic_vote_down_24dp), "反对")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("反对")
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.End) {
                                    IconButton(
                                        onClick = { showCollectionDialog = true },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = if (viewModel.isFavorited) Color(0xFFF57C00) else MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = if (viewModel.isFavorited) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                        ),
                                    ) {
                                        Icon(if (viewModel.isFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = "收藏")
                                    }
                                    val ttsState = articleHost?.articleTtsState
                                    if (ttsState?.isSpeaking == true) {
                                        IconButton(
                                            onClick = {
                                                articleHost.stopArticleSpeaking()
                                                userMessages.showMessage("已停止朗读")
                                            },
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "停止朗读")
                                        }
                                    }
                                    Button(
                                        onClick = { openComments() },
                                        contentPadding = PaddingValues(start = 8.dp, end = 12.dp),
                                        colors = voteUpNeutralButtonColors(),
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "${viewModel.commentCount}")
                                    }

                                    IconButton(
                                        onClick = { showActionsMenu = true },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                    ) {
                                        Icon(
                                            Icons.Filled.MoreVert,
                                            contentDescription = "更多选项",
                                        )
                                    }
                                }
                            }
                        } else {
                            // ── duo3：药丸式动画投票与操作区 ────────────────────
                            Row(
                                modifier = Modifier
                                    .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AnimatedVisibility(
                                        visible = viewModel.voteUpState == VoteUpState.Neutral || viewModel.voteUpState == VoteUpState.Up,
                                    ) {
                                        val upBgColor by animateColorAsState(
                                            targetValue = if (viewModel.voteUpState == VoteUpState.Up) voteUpNeutralContentDuo3() else MaterialTheme.colorScheme.surfaceContainer,
                                        )
                                        val upContentColor by animateColorAsState(
                                            targetValue = if (viewModel.voteUpState == VoteUpState.Up) Color.White else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(upBgColor)
                                                .clickable {
                                                    viewModel.toggleVoteUp(
                                                        environment,
                                                        if (viewModel.voteUpState == VoteUpState.Up) VoteUpState.Neutral else VoteUpState.Up,
                                                    )
                                                }.padding(6.dp, 8.dp, 12.dp, 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_vote_up_24dp),
                                                contentDescription = "赞同",
                                                tint = upContentColor,
                                                modifier = Modifier.size(24.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = viewModel.voteUpCount.toString(),
                                                color = upContentColor,
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = viewModel.voteUpState == VoteUpState.Neutral) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    AnimatedVisibility(
                                        visible = viewModel.voteUpState == VoteUpState.Neutral || viewModel.voteUpState == VoteUpState.Down,
                                    ) {
                                        val downBgColor by animateColorAsState(
                                            targetValue = if (viewModel.voteUpState == VoteUpState.Down) voteUpNeutralContentDuo3() else MaterialTheme.colorScheme.surfaceContainer,
                                        )
                                        val downContentColor by animateColorAsState(
                                            targetValue = if (viewModel.voteUpState == VoteUpState.Down) Color.White else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(downBgColor)
                                                .clickable {
                                                    viewModel.toggleVoteUp(
                                                        environment,
                                                        if (viewModel.voteUpState == VoteUpState.Down) VoteUpState.Neutral else VoteUpState.Down,
                                                    )
                                                }.padding(6.dp, 8.dp, 8.dp, 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            AnimatedVisibility(visible = viewModel.voteUpState != VoteUpState.Down) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                            }
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_vote_down_24dp),
                                                contentDescription = "反对",
                                                tint = downContentColor,
                                                modifier = Modifier.size(24.dp),
                                            )
                                            AnimatedVisibility(visible = viewModel.voteUpState == VoteUpState.Down) {
                                                Row {
                                                    Text(
                                                        text = "反对",
                                                        color = downContentColor,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        modifier = Modifier.padding(horizontal = 4.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        .padding(end = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    IconButton(
                                        onClick = { showCollectionDialog = true },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = if (viewModel.isFavorited) {
                                                Color(0xFFF57C00).harmonize(MaterialTheme.colorScheme.primary)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainer
                                            },
                                            contentColor = if (viewModel.isFavorited) {
                                                Color.White.copy(alpha = 0.87f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                        ),
                                    ) {
                                        Icon(
                                            if (viewModel.isFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                            contentDescription = "收藏",
                                        )
                                    }

                                    val ttsState = articleHost?.articleTtsState
                                    AnimatedVisibility(visible = ttsState?.isSpeaking == true) {
                                        IconButton(
                                            onClick = {
                                                articleHost?.stopArticleSpeaking()
                                                userMessages.showMessage("已停止朗读")
                                            },
                                            enabled = ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing, null),
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = Color(0xFF4CAF50).harmonize(MaterialTheme.colorScheme.primary),
                                                contentColor = Color.White,
                                            ),
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "停止朗读")
                                        }
                                    }

                                    Button(
                                        onClick = { openComments() },
                                        contentPadding = PaddingValues(start = 8.dp, end = 12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                        ),
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "${viewModel.commentCount}", style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }

                    if (showBottomBarCondition) {
                        Box(
                            modifier = Modifier
                                .onSizeChanged { bottomBarHeightPx = it.height.toFloat() }
                                .graphicsLayer {
                                    translationY = bottomBarOffset.value
                                    alpha = if (bottomBarHeightPx > 0f) 1f - (bottomBarOffset.value / bottomBarHeightPx) else 1f
                                },
                        ) {
                            ActionBarContent()
                        }
                    }
                }
            },
        ) { innerPadding ->
            CompositionLocalProvider(LocalBringIntoViewSpec provides articleBringIntoViewSpec) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalPagerScrollGate(
                            scrollState = scrollState,
                            enabled = isPagerPageActive,
                        ).onSizeChanged { pageKeyViewportHeight = it.height },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(scrollState)
                            .padding(innerPadding)
                            .padding(top = 8.dp),
                    ) {
                        @Suppress("UnusedReceiverParameter") // 确保竖式布局
                        @Composable
                        fun ColumnScope.DateTexts() {
                            Text(
                                "发布于 " + formatArticleDateTime(viewModel.createdAt),
                                color = Color.Gray,
                                fontSize = 11.sp,
                            )
                            if (viewModel.createdAt != viewModel.updatedAt) {
                                Text(
                                    "编辑于 " + formatArticleDateTime(viewModel.updatedAt),
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                )
                            }
                        }

                        @Composable
                        fun ColumnScope.ArticleVotersSocialCredit() {
                            val contentLabel = when (article.type) {
                                ArticleType.Answer -> "回答"
                                ArticleType.Article -> "文章"
                            }
                            val hasVotersSocialCredit = viewModel.votersTotal > 0
                            if (!hasVotersSocialCredit && viewModel.aigcSupportVoterCount <= 0) return
                            Spacer(modifier = Modifier.height(8.dp))
                            if (hasVotersSocialCredit) {
                                val text = viewModel.votersSocialText.ifBlank {
                                    "${formatCompactCount(viewModel.votersTotal)} 人赞同了该$contentLabel"
                                }
                                val votersTextModifier = if (article.type == ArticleType.Answer) {
                                    Modifier.clickable {
                                        showVoters = true
                                        if (viewModel.voters.isEmpty()) {
                                            viewModel.loadMoreVoters(environment, reset = true)
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = votersTextModifier,
                                )
                            }
                            if (viewModel.aigcSupportVoterCount > 0) {
                                if (hasVotersSocialCredit) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(
                                    text = "有 ${formatCompactCount(viewModel.aigcSupportVoterCount)} 人认为此${contentLabel}包含AIGC内容",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        if (viewModel.content.isNotEmpty() || viewModel.attachment != null) {
                            val hasPinnedDate = pinAnswerDate
                            val hasSocialCredit = viewModel.votersTotal > 0 || viewModel.aigcSupportVoterCount > 0
                            val endorsements = viewModel.endorsements
                            val hasEndorsements = endorsements.isNotEmpty()
                            if (hasPinnedDate || hasSocialCredit || hasEndorsements) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    if (hasPinnedDate) {
                                        DateTexts()
                                    }
                                    ArticleVotersSocialCredit()
                                    if (hasEndorsements) {
                                        if (hasPinnedDate || hasSocialCredit) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            endorsements.forEach { endorsement ->
                                                AnswerEndorsementChip(endorsement)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (articleSettings.useWebView) {
                                // WebView 正文渲染已经废弃，只保留为紧急回退路径；正文外 UI 不再为它单独分支。
                                ArticleWebViewContent(
                                    article = article,
                                    html = viewModel.content,
                                    title = viewModel.title,
                                    scrollState = scrollState,
                                    rememberedScrollY = viewModel.rememberedScrollY,
                                    rememberedScrollYSync = viewModel.rememberedScrollYSync,
                                    onRememberedScrollYSyncChange = { viewModel.rememberedScrollYSync = it },
                                    onImageLoadFailed = { userMessages.showMessage("图片加载失败，请向开发者反馈") },
                                    onDoubleTap = ::handleAnswerDoubleTap,
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End,
                                ) {
                                    if (!pinAnswerDate) {
                                        DateTexts()
                                    }
                                    if (viewModel.ipInfo != null) {
                                        Text(
                                            "IP属地：${viewModel.ipInfo}",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height((16 + 36).dp))
                            } else {
                                RenderMarkdown(
                                    html = viewModel.content,
                                    modifier = Modifier.articleMarkdownSelectionWorkaround(),
                                    selectable = true,
                                    enableScroll = false,
                                    header = {},
                                    footer = {
                                        ArticleVideoAttachmentContent(viewModel.attachment)
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.End,
                                        ) {
                                            if (!pinAnswerDate) {
                                                DateTexts()
                                            }
                                            if (viewModel.ipInfo != null) {
                                                Text(
                                                    "IP属地：${viewModel.ipInfo}",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp,
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height((16 + 36).dp))
                                    },
                                    onOpenSegmentComment = { segmentCommentTarget = it },
                                )
                            }
                        } else if (article.type == ArticleType.Answer && viewModel.title.isNotEmpty()) {
                            AnswerContentSkeleton(modifier = Modifier.fillMaxWidth())
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
    } // MainContent 结束。

    val progressBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
    val progressBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 96.dp

    Box(
        modifier = Modifier.fillMaxSize().then(answerDoubleTapModifier),
    ) {
        MainContent()

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

        if (showPageTurnGuide) {
            val visibleTopBar = (topBarHeightPx + topBarOffset.value).coerceAtLeast(0f)
            val visibleBottomBar = (bottomBarHeightPx - bottomBarOffset.value).coerceAtLeast(0f)
            PageTurnGuideOverlay(
                pageTurnPercent,
                topInsetPx = visibleTopBar,
                bottomInsetPx = visibleBottomBar,
                lastDirection = lastPageTurnDirection,
            )
        }
    }

    // 全屏菜单
    ArticleActionsMenu(
        article = article,
        viewModel = viewModel,
        showMenu = showActionsMenu && isPagerPageActive,
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
        showDialog = showSummaryDialog && isPagerPageActive,
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

    PlatformBackHandler(showActionsMenu && isPagerPageActive) {
        showActionsMenu = false
    }

    AigcFlagSheet(
        showDialog = showAigcFlagSheet && isPagerPageActive,
        viewModel = viewModel,
        onDismissRequest = { showAigcFlagSheet = false },
        onSubmitRequest = { viewModel.submitAigcFlag(environment) },
    )

    // 使用新的收藏夹对话框组件
    CollectionDialogComponent(
        showDialog = showCollectionDialog && isPagerPageActive,
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

    if (!commentsHostedByPager) {
        CommentScreenComponent(
            showComments = showComments && isPagerPageActive,
            onDismiss = { showComments = false },
            content = article,
        )
    }
    segmentCommentTarget?.let { target ->
        CommentScreenComponent(
            showComments = isPagerPageActive,
            onDismiss = { segmentCommentTarget = null },
            content = target,
        )
    }
    VotersSheet(
        show = showVoters && isPagerPageActive,
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
    if (showDoubleTapActionDialog && isPagerPageActive) {
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
                        openComments()
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
        showDialog = showExportDialog && isPagerPageActive,
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
