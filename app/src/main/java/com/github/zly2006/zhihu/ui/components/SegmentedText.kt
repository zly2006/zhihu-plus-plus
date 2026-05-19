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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.SegmentInfoMeta
import com.github.zly2006.zhihu.navigation.SegmentCommentHolder
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.subscreens.PREF_FONT_SIZE
import com.github.zly2006.zhihu.ui.subscreens.PREF_LINE_HEIGHT
import com.github.zly2006.zhihu.util.SegmentHighlightSpan
import com.github.zly2006.zhihu.util.SegmentTextPart
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * 在可选中的 Markdown 子树外承载划线评论弹窗。
 *
 * `SegmentedText` 作为 Markdown 的原生块渲染，通常处在 Markdown 级别的文本选择容器内。
 * 如果它直接从这个子树里打开评论弹窗，Compose 文本选择工具栏可能会跨弹窗窗口换算坐标，
 * 触发 `IllegalArgumentException: layouts are not part of the same hierarchy`。
 */
internal val LocalSegmentCommentHost = staticCompositionLocalOf<(SegmentCommentHolder) -> Unit> {
    error("LocalSegmentCommentHost is not provided")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedText(
    parts: List<SegmentTextPart>,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = segmentedTextStyle(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val metaStates = remember(parts) { mutableStateMapOf<String, SegmentInfoMeta>() }
    var selectedHighlight by remember(parts) { mutableStateOf<SegmentHighlightSpan?>(null) }
    val openSegmentComments = LocalSegmentCommentHost.current

    val onHighlightClick = remember(parts) { { highlight: SegmentHighlightSpan -> selectedHighlight = highlight } }

    SegmentTextRenderer(
        parts = parts,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        style = style,
        onHighlightClick = onHighlightClick,
    )

    selectedHighlight?.let { highlight ->
        val key = highlightKey(highlight)
        val currentMeta = metaStates[key] ?: highlight.meta
        SegmentActionSheet(
            highlight = highlight.copy(meta = currentMeta),
            onDismiss = { selectedHighlight = null },
            onLikeClick = {
                coroutineScope.launch {
                    val updatedMeta = runCatching {
                        toggleSegmentLike(
                            context = context,
                            highlight = highlight.copy(meta = currentMeta),
                        )
                    }.getOrElse { currentMeta }
                    metaStates[key] = updatedMeta
                }
            },
            onCommentClick = {
                selectedHighlight = null
                highlight.copy(meta = currentMeta).toSegmentCommentHolder()?.let { target ->
                    openSegmentComments(target)
                }
            },
            onCopyClick = {
                context.clipboardManager.setPrimaryClip(
                    android.content.ClipData.newPlainText("segment_text", highlight.text),
                )
                selectedHighlight = null
            },
        )
    }
}

@Composable
private fun SegmentTextRenderer(
    parts: List<SegmentTextPart>,
    modifier: Modifier,
    maxLines: Int,
    overflow: TextOverflow,
    style: TextStyle,
    onHighlightClick: (SegmentHighlightSpan) -> Unit,
) {
    val highlightTextColor = MaterialTheme.colorScheme.onSurface
    val highlightUnderlineColor = MaterialTheme.colorScheme.outlineVariant
    var textLayoutResult by remember(parts) { mutableStateOf<TextLayoutResult?>(null) }

    val annotatedText = remember(parts, highlightTextColor, onHighlightClick) {
        buildSegmentAnnotatedText(
            parts = parts,
            highlightTextColor = highlightTextColor,
            onHighlightClick = onHighlightClick,
        )
    }

    BasicText(
        text = annotatedText,
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
    parts.forEach { part ->
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
        }
    }
}

@Composable
fun segmentedTextStyle(): TextStyle {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE)
    }
    val fontSizePercent = preferences.getInt(PREF_FONT_SIZE, 100)
    val lineHeightPercent = preferences.getInt(PREF_LINE_HEIGHT, 160)
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

private suspend fun toggleSegmentLike(
    context: android.content.Context,
    highlight: SegmentHighlightSpan,
): SegmentInfoMeta {
    val contentId = highlight.contentId ?: return highlight.meta
    val targetType = highlight.contentType ?: return highlight.meta
    val url = "https://www.zhihu.com/api/v4/reaction/${targetType}s/$contentId/segment_reaction"

    return if (highlight.meta.isLike) {
        AccountData.httpClient(context).delete(url) {
            signFetchRequest()
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("seg_ids", highlight.meta.segIds.joinToString(","))
                }.toString(),
            )
        }
        highlight.meta.copy(
            isLike = false,
            likeCount = (highlight.meta.likeCount - 1).coerceAtLeast(0),
        )
    } else {
        val response = AccountData.fetchPost(context, url) {
            signFetchRequest()
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
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
                }.toString(),
            )
        }
        val segId = response
            ?.get("payload")
            ?.jsonObject
            ?.get("segId")
            ?.jsonPrimitive
            ?.content
            ?.split(',')
            ?.filter(String::isNotEmpty)
            ?: highlight.meta.segIds
        highlight.meta.copy(
            segIds = segId,
            isLike = true,
            likeCount = highlight.meta.likeCount + 1,
        )
    }
}
