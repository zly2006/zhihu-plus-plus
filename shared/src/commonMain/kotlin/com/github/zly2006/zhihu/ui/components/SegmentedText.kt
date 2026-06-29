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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.navigation.SegmentCommentHolder
import com.github.zly2006.zhihu.shared.data.SegmentInfoMeta
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.util.SegmentHighlightSpan
import com.github.zly2006.zhihu.shared.util.SegmentTextPart
import com.github.zly2006.zhihu.ui.subscreens.PREF_FONT_SIZE
import com.github.zly2006.zhihu.ui.subscreens.PREF_LINE_HEIGHT
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * 在可选中的 Markdown 子树外承载划线交互弹窗。
 *
 * `SegmentedText` 作为 Markdown 的原生块渲染，通常处在 Markdown 级别的文本选择容器内。
 * 如果它直接从这个子树里打开底部弹窗或评论弹窗，Compose 文本选择工具栏可能会跨弹窗窗口换算坐标，
 * 触发 `IllegalArgumentException: layouts are not part of the same hierarchy`。
 */
private const val COMMENT_BADGE_ID = "comment_badge"

internal val LocalSegmentCommentHost = staticCompositionLocalOf<(SegmentCommentHolder) -> Unit> {
    error("LocalSegmentCommentHost is not provided")
}

internal data class SegmentActionSheetState(
    val highlight: SegmentHighlightSpan,
    val onDismiss: () -> Unit,
    val onLikeClick: () -> Unit,
    val onCommentClick: () -> Unit,
    val onCopyClick: () -> Unit,
)

internal val LocalSegmentActionSheetHost = staticCompositionLocalOf<(SegmentActionSheetState?) -> Unit> {
    error("LocalSegmentActionSheetHost is not provided")
}

data class SegmentedTextRuntime(
    val toggleSegmentLike: suspend (SegmentHighlightSpan) -> SegmentInfoMeta,
)

fun buildSegmentUnlikeBody(highlight: SegmentHighlightSpan): String = buildJsonObject {
    put("seg_ids", highlight.meta.segIds.joinToString(","))
}.toString()

fun buildSegmentLikeBody(highlight: SegmentHighlightSpan): String = buildJsonObject {
    if (highlight.meta.segIds.isNotEmpty()) {
        put("seg_id", highlight.meta.segIds.joinToString(","))
    }
    put("content", highlight.text)
    put(
        "position",
        buildJsonObject {
            put(
                "start",
                buildJsonObject {
                    put("paragraph_id", highlight.paragraphId.orEmpty())
                    put("offset", highlight.startOffset ?: 0)
                },
            )
            put(
                "end",
                buildJsonObject {
                    put("paragraph_id", highlight.paragraphId.orEmpty())
                    put("offset", highlight.endOffset ?: 0)
                },
            )
        },
    )
}.toString()

fun updateSegmentMetaAfterUnlike(highlight: SegmentHighlightSpan): SegmentInfoMeta = highlight.meta.copy(
    isLike = false,
    likeCount = (highlight.meta.likeCount - 1).coerceAtLeast(0),
)

fun updateSegmentMetaAfterLike(
    highlight: SegmentHighlightSpan,
    response: JsonObject?,
): SegmentInfoMeta {
    val segId = response
        ?.get("payload")
        ?.jsonObject
        ?.get("segId")
        ?.jsonPrimitive
        ?.content
        ?.split(',')
        ?.filter(String::isNotEmpty)
        ?: highlight.meta.segIds
    return highlight.meta.copy(
        segIds = segId,
        isLike = true,
        likeCount = highlight.meta.likeCount + 1,
    )
}

@Composable
expect fun rememberSegmentedTextRuntime(): SegmentedTextRuntime

@Composable
fun SegmentedText(
    parts: List<SegmentTextPart>,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = segmentedTextStyle(),
) {
    val runtime = rememberSegmentedTextRuntime()
    val copyPlainText = rememberPlainTextClipboard()
    val coroutineScope = rememberCoroutineScope()
    val metaStates = remember(parts) { mutableStateMapOf<String, SegmentInfoMeta>() }
    var selectedHighlight by remember(parts) { mutableStateOf<SegmentHighlightSpan?>(null) }
    val openSegmentComments = LocalSegmentCommentHost.current
    val showSegmentActionSheet = LocalSegmentActionSheetHost.current

    val onHighlightClick = remember(parts) { { highlight: SegmentHighlightSpan -> selectedHighlight = highlight } }
    val onBadgeClick = remember(parts) {
        { highlight: SegmentHighlightSpan ->
            highlight.toSegmentCommentHolder()?.let { openSegmentComments(it) }
            Unit
        }
    }

    SegmentTextRenderer(
        parts = parts,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        style = style,
        onHighlightClick = onHighlightClick,
        onBadgeClick = onBadgeClick,
    )

    val selected = selectedHighlight
    val selectedKey = selected?.let(::highlightKey)
    val selectedMeta = selectedKey?.let { metaStates[it] } ?: selected?.meta
    LaunchedEffect(selected, selectedMeta) {
        if (selected == null || selectedKey == null || selectedMeta == null) {
            showSegmentActionSheet(null)
            return@LaunchedEffect
        }
        showSegmentActionSheet(
            SegmentActionSheetState(
                highlight = selected.copy(meta = selectedMeta),
                onDismiss = {
                    selectedHighlight = null
                    showSegmentActionSheet(null)
                },
                onLikeClick = {
                    coroutineScope.launch {
                        val updatedMeta = runCatching {
                            runtime.toggleSegmentLike(selected.copy(meta = selectedMeta))
                        }.getOrElse { selectedMeta }
                        metaStates[selectedKey] = updatedMeta
                    }
                },
                onCommentClick = {
                    selectedHighlight = null
                    showSegmentActionSheet(null)
                    selected.copy(meta = selectedMeta).toSegmentCommentHolder()?.let { target ->
                        openSegmentComments(target)
                    }
                },
                onCopyClick = {
                    copyPlainText("segment_text", selected.text)
                    selectedHighlight = null
                    showSegmentActionSheet(null)
                },
            ),
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            showSegmentActionSheet(null)
        }
    }
}

@Composable
internal fun SegmentActionSheet(state: SegmentActionSheetState) {
    SegmentActionSheet(
        highlight = state.highlight,
        onDismiss = state.onDismiss,
        onLikeClick = state.onLikeClick,
        onCommentClick = state.onCommentClick,
        onCopyClick = state.onCopyClick,
    )
}

@Composable
private fun SegmentTextRenderer(
    parts: List<SegmentTextPart>,
    modifier: Modifier,
    maxLines: Int,
    overflow: TextOverflow,
    style: TextStyle,
    onHighlightClick: (SegmentHighlightSpan) -> Unit,
    onBadgeClick: (SegmentHighlightSpan) -> Unit,
) {
    val highlightTextColor = MaterialTheme.colorScheme.onSurface
    val commentCountColor = MaterialTheme.colorScheme.primary
    val highlightUnderlineColor = MaterialTheme.colorScheme.outlineVariant
    var textLayoutResult by remember(parts) { mutableStateOf<TextLayoutResult?>(null) }

    val scale = style.fontSize.value / 16f
    val badgeIconDp = (16 * scale).dp
    val badgeFontSp = (12 * scale).sp
    val badgeHeight = (16 * scale).sp

    val annotatedText = remember(parts, highlightTextColor, onHighlightClick) {
        buildSegmentAnnotatedText(
            parts = parts,
            highlightTextColor = highlightTextColor,
            onHighlightClick = onHighlightClick,
        )
    }

    val commentBadgeContent = remember(parts, commentCountColor, scale, onBadgeClick) {
        buildMap {
            parts.forEachIndexed { index, part ->
                val highlight = part.highlight ?: return@forEachIndexed
                if (highlight.meta.commentCount <= 0) return@forEachIndexed
                val digits = highlight.meta.commentCount
                    .toString()
                    .length
                val badgeWidth = ((18 + 8 * digits) * scale).sp
                put(
                    "${COMMENT_BADGE_ID}_$index",
                    InlineTextContent(
                        placeholder = Placeholder(
                            width = badgeWidth,
                            height = badgeHeight,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().clickable(
                                interactionSource = null,
                                indication = null,
                            ) { onBadgeClick(highlight) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Comment,
                                contentDescription = null,
                                modifier = Modifier.size(badgeIconDp),
                                tint = commentCountColor,
                            )
                            Text(
                                text = highlight.meta.commentCount.toString(),
                                color = commentCountColor,
                                fontSize = badgeFontSp,
                                lineHeight = badgeFontSp,
                            )
                        }
                    },
                )
            }
        }
    }

    BasicText(
        text = annotatedText,
        inlineContent = commentBadgeContent,
        modifier = modifier.drawBehind {
            val layout = textLayoutResult ?: return@drawBehind
            val strokeWidth = 1.dp.toPx()
            val dashWidth = 6.dp.toPx()
            val gapWidth = 4.dp.toPx()
            annotatedText.getLinkAnnotations(0, annotatedText.length).forEach { annotation ->
                highlightedLineRects(layout, annotation.start, annotation.end).forEach { rect ->
                    val y = rect.bottom - 2.dp.toPx()
                    val path = Path().apply {
                        moveTo(rect.left, y)
                        lineTo(rect.right, y)
                    }
                    drawPath(
                        path = path,
                        color = highlightUnderlineColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, gapWidth)),
                        ),
                    )
                }
            }
        },
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { textLayoutResult = it },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentActionSheet(
    highlight: SegmentHighlightSpan,
    onDismiss: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onCopyClick: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "划线片段",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "“${highlight.text}”",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = onLikeClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = if (highlight.meta.isLike) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = null,
                    )
                    Text(
                        text = highlight.meta.likeCount.toString(),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                FilledTonalButton(
                    onClick = onCommentClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Comment,
                        contentDescription = null,
                    )
                    Text(
                        text = highlight.meta.commentCount.toString(),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                IconButton(onClick = onCopyClick) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "复制内容",
                    )
                }
            }
        }
    }
}

private fun buildSegmentAnnotatedText(
    parts: List<SegmentTextPart>,
    highlightTextColor: androidx.compose.ui.graphics.Color,
    onHighlightClick: (SegmentHighlightSpan) -> Unit,
): AnnotatedString = buildAnnotatedString {
    parts.forEachIndexed { index, part ->
        val highlight = part.highlight
        if (highlight == null) {
            append(part.text)
        } else {
            withLink(
                LinkAnnotation.Clickable(
                    tag = highlightKey(highlight),
                    styles = TextLinkStyles(style = SpanStyle(color = highlightTextColor)),
                    linkInteractionListener = LinkInteractionListener { onHighlightClick(highlight) },
                ),
            ) {
                append(part.text)
            }
            if (highlight.meta.commentCount > 0) {
                val badgeKey = "${COMMENT_BADGE_ID}_$index"
                appendInlineContent(badgeKey, index.toString())
            }
        }
    }
}

@Composable
fun segmentedTextStyle(settings: SettingsStore = rememberSettingsStore()): TextStyle {
    val fontSizePercent = settings.getInt(PREF_FONT_SIZE, 100)
    val lineHeightPercent = settings.getInt(PREF_LINE_HEIGHT, 160)
    return MaterialTheme.typography.bodyLarge.copy(
        fontSize = 16.sp * fontSizePercent / 100,
        lineHeight = 16.sp * fontSizePercent / 100 * lineHeightPercent / 100,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

private fun highlightKey(highlight: SegmentHighlightSpan): String =
    buildString {
        append(highlight.meta.segIds.joinToString(","))
        append('|')
        append(highlight.text)
    }

private fun SegmentHighlightSpan.toSegmentCommentHolder(): SegmentCommentHolder? {
    val contentId = contentId ?: return null
    val contentType = contentType ?: return null
    val segmentId = meta.segIds.joinToString(",").takeIf { it.isNotBlank() } ?: return null
    return SegmentCommentHolder(
        contentId = contentId,
        contentType = contentType,
        segmentId = segmentId,
    )
}

private fun highlightedLineRects(
    layout: TextLayoutResult,
    start: Int,
    end: Int,
): List<Rect> {
    if (start >= end) return emptyList()

    val result = mutableListOf<Rect>()
    var current: Rect? = null
    for (offset in start until end) {
        val box = layout.getBoundingBox(offset)
        current = if (current == null) {
            box
        } else if (
            kotlin.math.abs(box.top - current.top) < 0.5f &&
            kotlin.math.abs(box.bottom - current.bottom) < 0.5f &&
            box.left <= current.right + 1f
        ) {
            Rect(
                left = current.left,
                top = current.top,
                right = box.right,
                bottom = current.bottom,
            )
        } else {
            result += current
            box
        }
    }
    current?.let(result::add)
    return result
}
