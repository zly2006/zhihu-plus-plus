package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.zhihuArticleUrl
import com.github.zly2006.zhihu.navigation.zhihuQuestionAnswerUrl
import com.github.zly2006.zhihu.shared.article.CachedAnswerContent
import com.github.zly2006.zhihu.shared.data.OfficialBadge
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.ui.components.AuthorBadge
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.materialkolor.ktx.harmonize
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val SCROLL_THRESHOLD = 10
val ScrollThresholdDp = SCROLL_THRESHOLD.dp

fun articleActionText(
    article: Article,
    questionId: Long,
    title: String,
    authorName: String,
): String =
    when (article.type) {
        ArticleType.Answer -> {
            "${zhihuQuestionAnswerUrl(questionId, article.id)}\n【$title - $authorName 的回答】"
        }

        ArticleType.Article -> {
            "${zhihuArticleUrl(article.id)}\n【$title - $authorName 的文章】"
        }
    }

fun articleSpeechText(
    title: String,
    content: String,
    maxContentLength: Int = 50_000,
): String =
    buildString {
        append(title)
        append("。")
        if (content.isNotEmpty()) {
            val contentToProcess =
                if (content.length > maxContentLength) {
                    content.substring(0, maxContentLength) + "..."
                } else {
                    content
                }
            append(Ksoup.parse(contentToProcess).text())
        }
    }

/**
 * 修复 noscript 标签中的图片加载问题。
 * 提取为独立函数，确保主 WebView 和预览 WebView 使用相同的文档处理。
 */
fun prepareContentDocument(
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

@OptIn(ExperimentalTime::class)
fun formatArticleDateTime(seconds: Long): String {
    val dateTime = Instant
        .fromEpochSeconds(seconds)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(dateTime.year.toString().padStart(4, '0'))
        append('-')
        append((dateTime.month.ordinal + 1).toString().padStart(2, '0'))
        append('-')
        append(dateTime.day.toString().padStart(2, '0'))
        append(' ')
        append(dateTime.hour.toString().padStart(2, '0'))
        append(':')
        append(dateTime.minute.toString().padStart(2, '0'))
        append(':')
        append(dateTime.second.toString().padStart(2, '0'))
    }
}

@Composable
fun ArticleAuthorRow(
    expanded: Boolean,
    authorId: String,
    authorUrlToken: String,
    authorName: String,
    authorAvatarSrc: String,
    authorBio: String,
    authorBadge: OfficialBadge?,
) {
    val navigator = LocalNavigator.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(if (expanded) PaddingValues(vertical = 16.dp) else PaddingValues(top = 2.dp, bottom = 8.dp))
            .padding(end = 16.dp)
            .fillMaxWidth()
            .clickable {
                navigator.onNavigate(
                    com.github.zly2006.zhihu.navigation.Person(
                        id = authorId,
                        urlToken = authorUrlToken,
                        name = authorName,
                    ),
                )
            },
    ) {
        if (authorAvatarSrc.isNotEmpty()) {
            AsyncImage(
                model = authorAvatarSrc,
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
                    text = authorName,
                    style = if (expanded) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                    color = if (expanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (authorBadge != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    AuthorBadge(
                        badge = authorBadge,
                        compact = !expanded,
                    )
                }
            }
            if (authorBio.isNotEmpty() && expanded) {
                Text(
                    text = authorBio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ArticleMenuActionButton(
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
fun ArticleMenuActionButton(
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    ArticleMenuActionButton(
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
fun ArticleActionsMenuContent(
    ttsState: TtsState,
    onToggleSpeech: () -> Unit,
    onShareRequest: () -> Unit,
    onSummaryRequest: () -> Unit,
    onCopyLinkRequest: () -> Unit,
    onExportRequest: () -> Unit,
    onOpenInBrowserRequest: () -> Unit,
) {
    ArticleMenuActionButton(
        icon = {
            when (ttsState) {
                TtsState.Initializing, TtsState.Uninitialized -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
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
        onClick = onToggleSpeech,
    )

    Spacer(modifier = Modifier.height(12.dp))

    ArticleMenuActionButton(
        icon = Icons.Filled.Share,
        text = "分享",
        onClick = onShareRequest,
    )

    Spacer(modifier = Modifier.height(12.dp))

    ArticleMenuActionButton(
        icon = Icons.AutoMirrored.Filled.Comment,
        text = "总结本文",
        onClick = onSummaryRequest,
    )

    Spacer(modifier = Modifier.height(12.dp))

    ArticleMenuActionButton(
        icon = Icons.Filled.ContentCopy,
        text = "复制链接",
        onClick = onCopyLinkRequest,
    )

    Spacer(modifier = Modifier.height(12.dp))

    ArticleMenuActionButton(
        icon = Icons.Filled.GetApp,
        text = "导出文章 (Markdown、图片、HTML、PDF)",
        onClick = onExportRequest,
    )

    Spacer(modifier = Modifier.height(12.dp))

    ArticleMenuActionButton(
        icon = Icons.Outlined.DesktopWindows,
        text = "在电脑中打开（我计划使用浏览器插件实现，还在写，点击后请手动前往收藏夹打开）",
        onClick = onOpenInBrowserRequest,
    )

    Spacer(modifier = Modifier.height(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleActionsMenuSheet(
    showMenu: Boolean,
    ttsState: TtsState,
    onDismissRequest: () -> Unit,
    onToggleSpeech: () -> Unit,
    onShareRequest: () -> Unit,
    onSummaryRequest: () -> Unit,
    onCopyLinkRequest: () -> Unit,
    onExportRequest: () -> Unit,
    onOpenInBrowserRequest: () -> Unit,
) {
    if (!showMenu) return
    MyModalBottomSheet(onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            ArticleActionsMenuContent(
                ttsState = ttsState,
                onToggleSpeech = onToggleSpeech,
                onShareRequest = onShareRequest,
                onSummaryRequest = onSummaryRequest,
                onCopyLinkRequest = onCopyLinkRequest,
                onExportRequest = onExportRequest,
                onOpenInBrowserRequest = onOpenInBrowserRequest,
            )
        }
    }
}

@Composable
fun CachedAnswerPreviewContent(
    cached: CachedAnswerContent,
    voteUpIcon: @Composable () -> Unit,
    content: @Composable (String) -> Unit,
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
                            voteUpIcon()
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
            if (cached.content.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                content(cached.content)
            }
            Spacer(modifier = Modifier.height((16 + 36).dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerDoubleTapActionDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onActionSelected: (AnswerDoubleTapAction) -> Unit,
) {
    if (!showDialog) return
    MyModalBottomSheet(
        onDismissRequest = onDismissRequest,
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
                onClick = { onActionSelected(AnswerDoubleTapAction.None) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("设为无操作")
            }
            Button(
                onClick = { onActionSelected(AnswerDoubleTapAction.VoteUp) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("设为点赞")
            }
            Button(
                onClick = { onActionSelected(AnswerDoubleTapAction.OpenComments) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("设为打开评论区")
            }
        }
    }
}

@Composable
fun rememberBottomBarAvoidingBringIntoViewSpec(
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
fun ArticleSummarySheet(
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
fun TwoRowsTopAppBar(
    title: @Composable (expanded: Boolean) -> Unit,
    subtitle: (@Composable (expanded: Boolean) -> Unit)?,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    collapsedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = {
            Column(
                horizontalAlignment = titleHorizontalAlignment,
            ) {
                title(false)
                subtitle?.invoke(false)
            }
        },
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight = maxOf(collapsedHeight, expandedHeight),
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}
