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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.navigation.SegmentCommentHolder
import com.github.zly2006.zhihu.shared.data.SegmentInfoMeta
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.util.SegmentHighlightSpan
import com.hrm.markdown.parser.ast.SegmentHighlight
import com.hrm.markdown.renderer.LocalOnSegmentHighlightClick
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * 在可选中的 Markdown 子树外承载划线交互弹窗。
 *
 * `SegmentHighlight` 作为 Markdown 段落内联节点渲染，处在 Markdown 级别的文本选择容器内。
 * 如果它直接从这个子树里打开底部弹窗或评论弹窗，Compose 文本选择工具栏可能会跨弹窗窗口换算坐标，
 * 触发 `IllegalArgumentException: layouts are not part of the same hierarchy`。
 */
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
internal fun SegmentHighlightInteractionHost(
    content: @Composable () -> Unit,
) {
    val runtime = rememberSegmentedTextRuntime()
    val copyPlainText = rememberPlainTextClipboard()
    val coroutineScope = rememberCoroutineScope()
    val metaStates = remember { mutableStateMapOf<String, SegmentInfoMeta>() }
    var selectedHighlight by remember { mutableStateOf<Pair<String, SegmentHighlightSpan>?>(null) }
    val openSegmentComments = LocalSegmentCommentHost.current
    val showSegmentActionSheet = LocalSegmentActionSheetHost.current
    val onSegmentHighlightClick: (SegmentHighlight) -> Unit = remember {
        { node -> selectedHighlight = node.interactionKey to node.toSegmentHighlightSpan() }
    }

    CompositionLocalProvider(
        LocalOnSegmentHighlightClick provides onSegmentHighlightClick,
        content = content,
    )

    val selectedKey = selectedHighlight?.first
    val selected = selectedHighlight?.second
    val selectedMeta = selectedKey?.let(metaStates::get) ?: selected?.meta
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

private fun SegmentHighlight.toSegmentHighlightSpan(): SegmentHighlightSpan = SegmentHighlightSpan(
    text = text,
    meta = SegmentInfoMeta(
        segIds = attributes["data-highlight-id"]
            .orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty),
        isLike = attributes["data-highlight-is-like"]?.toBoolean() ?: false,
        likeCount = attributes["data-highlight-like-count"]?.toIntOrNull() ?: 0,
        commentCount = attributes["data-highlight-comment-count"]?.toIntOrNull() ?: 0,
        myCommentCount = attributes["data-highlight-my-comment-count"]?.toIntOrNull() ?: 0,
        isSpan = attributes["data-highlight-is-span"]?.toBoolean() ?: false,
    ),
    sourceUrl = attributes["data-highlight-source-url"],
    contentId = attributes["data-highlight-content-id"],
    contentType = attributes["data-highlight-content-type"],
    paragraphId = attributes["data-highlight-pid"],
    startOffset = attributes["data-highlight-start-offset"]?.toIntOrNull(),
    endOffset = attributes["data-highlight-end-offset"]?.toIntOrNull(),
)

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
