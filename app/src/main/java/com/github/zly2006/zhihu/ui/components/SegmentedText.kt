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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.SegmentInfoMeta
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.subscreens.PREF_FONT_SIZE
import com.github.zly2006.zhihu.ui.subscreens.PREF_LINE_HEIGHT
import com.github.zly2006.zhihu.util.SegmentHighlightSpan
import com.github.zly2006.zhihu.util.SegmentTextParagraph
import com.github.zly2006.zhihu.util.SegmentTextPart
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jsoup.Jsoup

private const val SEGMENT_TAG = "segment-highlight"

private sealed interface SegmentCommentsState {
    data object Loading : SegmentCommentsState

    data class Ready(
        val highlight: SegmentHighlightSpan,
        val totalCount: Int,
        val placeholder: String,
        val comments: List<DataHolder.Comment>,
    ) : SegmentCommentsState

    data class Error(
        val message: String,
    ) : SegmentCommentsState
}

data class SegmentHighlightActions(
    val onCommentClick: ((SegmentHighlightSpan) -> Unit)? = null,
)

val LocalSegmentHighlightActions = staticCompositionLocalOf { SegmentHighlightActions() }

@Composable
fun SegmentedTextParagraphs(
    paragraphs: List<SegmentTextParagraph>,
    modifier: Modifier = Modifier,
    maxParagraphs: Int = paragraphs.size,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = segmentedTextStyle(),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        paragraphs.take(maxParagraphs).forEach { paragraph ->
            SegmentedText(
                parts = paragraph.parts,
                maxLines = maxLines,
                overflow = overflow,
                style = style,
            )
        }
    }
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
    val actions = LocalSegmentHighlightActions.current
    val coroutineScope = rememberCoroutineScope()
    var selectedHighlight by remember(parts) { mutableStateOf<SegmentHighlightSpan?>(null) }
    var commentsState by remember(parts) { mutableStateOf<SegmentCommentsState?>(null) }
    var textLayoutResult by remember(parts) { mutableStateOf<TextLayoutResult?>(null) }
    val metaStates = remember(parts) { mutableStateMapOf<String, SegmentInfoMeta>() }
    val highlightTextColor = MaterialTheme.colorScheme.onSurface
    val highlightUnderlineColor = MaterialTheme.colorScheme.outlineVariant

    val annotatedText = buildAnnotatedString {
        parts.forEach { part ->
            val start = length
            append(part.text)
            val end = length
            val highlight = part.highlight ?: return@forEach
            val key = highlightKey(highlight)
            addStringAnnotation(
                tag = SEGMENT_TAG,
                annotation = key,
                start = start,
                end = end,
            )
            addStyle(
                SpanStyle(
                    color = highlightTextColor,
                ),
                start = start,
                end = end,
            )
        }
    }

    val highlightIndex = remember(parts) {
        parts
            .mapNotNull { part ->
                part.highlight?.let { highlightKey(it) to it }
            }.toMap()
    }

    ClickableText(
        text = annotatedText,
        modifier = modifier.drawBehind {
            val layout = textLayoutResult ?: return@drawBehind
            val strokeWidth = 1.dp.toPx()
            val dashWidth = 6.dp.toPx()
            val gapWidth = 4.dp.toPx()
            annotatedText.getStringAnnotations(SEGMENT_TAG, 0, annotatedText.length).forEach { annotation ->
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
        onClick = { offset ->
            annotatedText
                .getStringAnnotations(SEGMENT_TAG, offset, offset)
                .firstOrNull()
                ?.let { selected ->
                    selectedHighlight = highlightIndex[selected.item]
                }
        },
    )

    val currentHighlight = selectedHighlight
    if (currentHighlight != null) {
        val key = highlightKey(currentHighlight)
        val currentMeta = metaStates[key] ?: currentHighlight.meta
        val currentLike = currentMeta.isLike
        val currentLikeCount = currentMeta.likeCount
        ModalBottomSheet(
            onDismissRequest = { selectedHighlight = null },
        ) {
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
                    text = "“${currentHighlight.text}”",
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                val updatedMeta = runCatching {
                                    toggleSegmentLike(
                                        context = context,
                                        highlight = currentHighlight.copy(meta = currentMeta),
                                    )
                                }.getOrElse { currentMeta }
                                metaStates[key] = updatedMeta
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = if (currentLike) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = null,
                        )
                        Text(
                            text = currentLikeCount.toString(),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            selectedHighlight = null
                            if (actions.onCommentClick != null) {
                                actions.onCommentClick.invoke(currentHighlight)
                            } else {
                                commentsState = SegmentCommentsState.Loading
                                coroutineScope.launch {
                                    commentsState = runCatching {
                                        loadSegmentComments(
                                            context = context,
                                            highlight = currentHighlight.copy(meta = currentMeta),
                                        )
                                    }.fold(
                                        onSuccess = { state -> state },
                                        onFailure = { SegmentCommentsState.Error(it.message ?: "评论加载失败") },
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Comment,
                            contentDescription = null,
                        )
                        Text(
                            text = currentHighlight.meta.commentCount.toString(),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    IconButton(
                        onClick = {
                            context.clipboardManager.setPrimaryClip(
                                android.content.ClipData.newPlainText("segment_text", currentHighlight.text),
                            )
                            selectedHighlight = null
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "复制内容",
                        )
                    }
                }
            }
        }
    }

    val currentCommentsState = commentsState
    if (currentCommentsState != null) {
        ModalBottomSheet(
            onDismissRequest = { commentsState = null },
        ) {
            when (currentCommentsState) {
                SegmentCommentsState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                    }
                }

                is SegmentCommentsState.Error -> {
                    Text(
                        text = currentCommentsState.message,
                        modifier = Modifier.padding(20.dp),
                    )
                }

                is SegmentCommentsState.Ready -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "${currentCommentsState.totalCount} 条评论",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = currentCommentsState.highlight.text,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        currentCommentsState.comments.forEach { comment ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = comment.author.name,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Text(
                                    text = Jsoup.parse(comment.content).text(),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = buildString {
                                        append(comment.commentTag.firstOrNull()?.text ?: "")
                                        if (comment.likeCount > 0) {
                                            if (isNotEmpty()) append(" · ")
                                            append("赞 ")
                                            append(comment.likeCount)
                                        }
                                        if (comment.childCommentCount > 0) {
                                            if (isNotEmpty()) append(" · ")
                                            append("回复 ")
                                            append(comment.childCommentCount)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = currentCommentsState.placeholder,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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

private suspend fun loadSegmentComments(
    context: android.content.Context,
    highlight: SegmentHighlightSpan,
): SegmentCommentsState.Ready {
    val contentId = highlight.contentId ?: error("missing contentId")
    val targetType = highlight.contentType ?: error("missing contentType")
    val segmentId = highlight.meta.segIds.joinToString(",")
    val rootComments = AccountData.fetchGet(
        context,
        "https://www.zhihu.com/api/v4/comment_v5/${targetType}s/$contentId/segment/root_comment?segment_id=$segmentId&order_by=score&limit=20&offset=",
    ) {
        signFetchRequest()
    } ?: error("root comments missing")
    val config = AccountData.fetchGet(
        context,
        "https://www.zhihu.com/api/v4/comment_v5/${targetType}s/$contentId/segment/config?show_ai_comment=&segment_id=$segmentId",
    ) {
        signFetchRequest()
    }
    val comments = rootComments["data"]
        ?.jsonArray
        ?.map { AccountData.decodeJson<DataHolder.Comment>(it) }
        .orEmpty()
    val totalCount = rootComments["counts"]
        ?.jsonObject
        ?.get("total_counts")
        ?.jsonPrimitive
        ?.int
        ?: comments.size
    val placeholder = config
        ?.get("place_holder")
        ?.jsonPrimitive
        ?.content
        ?: "理性发言，友善互动"
    return SegmentCommentsState.Ready(
        highlight = highlight,
        totalCount = totalCount,
        placeholder = placeholder,
        comments = comments,
    )
}
