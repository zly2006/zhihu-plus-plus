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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.data.SegmentInfoMeta
import com.github.zly2006.zhihu.navigation.SegmentCommentHolder
import com.github.zly2006.zhihu.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.util.SegmentHighlightSpan
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.deleteSigned
import com.github.zly2006.zhihu.viewmodel.postSigned
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.SegmentHighlight
import com.hrm.markdown.renderer.LocalOnSegmentHighlightClick
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.math.roundToInt

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
internal fun SegmentHighlightInteractionHost(
    document: Document,
    content: @Composable () -> Unit,
) {
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val copyPlainText = rememberPlainTextClipboard()
    val coroutineScope = rememberCoroutineScope()
    val metaStates = remember { mutableStateMapOf<String, SegmentInfoMeta>() }
    var selectedHighlight by remember { mutableStateOf<Pair<String, SegmentHighlightSpan>?>(null) }
    val openSegmentComments = LocalSegmentCommentHost.current
    val showSegmentActionSheet = LocalSegmentActionSheetHost.current
    val displayTexts = remember(document) { document.segmentDisplayTexts() }
    val onSegmentHighlightClick: (SegmentHighlight) -> Unit = remember(displayTexts) {
        { node ->
            selectedHighlight = node.interactionKey to node.toSegmentHighlightSpan(
                displayText = displayTexts[node.segmentThreadKey()] ?: node.text,
            )
        }
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
                            toggleSegmentLike(environment, selected.copy(meta = selectedMeta))
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
                    copyPlainText("segment_text", selected.displayText)
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

private suspend fun toggleSegmentLike(
    environment: PaginationEnvironment,
    highlight: SegmentHighlightSpan,
): SegmentInfoMeta {
    val contentId = highlight.contentId ?: return highlight.meta
    val targetType = highlight.contentType ?: return highlight.meta
    val url = "https://www.zhihu.com/api/v4/reaction/${targetType}s/$contentId/segment_reaction"
    if (environment.authenticatedCookies()["d_c0"] == null) return highlight.meta

    return if (highlight.meta.isLike) {
        environment.deleteSigned(url) {
            contentType(ContentType.Application.Json)
            setBody(buildSegmentUnlikeBody(highlight))
        }
        updateSegmentMetaAfterUnlike(highlight)
    } else {
        val response = environment.postSigned(url) {
            contentType(ContentType.Application.Json)
            setBody(buildSegmentLikeBody(highlight))
        }
        updateSegmentMetaAfterLike(
            highlight,
            if (response.status == HttpStatusCode.NoContent) null else response.body<JsonElement>() as? JsonObject,
        )
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
    val sheetState = rememberModalBottomSheetState()
    val textScrollState = rememberScrollState()
    val overflowTolerance = with(LocalDensity.current) { 1.dp.roundToPx() }
    val hasMeasuredOverflow = textScrollState.viewportSize > 0 &&
        textScrollState.maxValue > overflowTolerance
    val showTopDivider = hasMeasuredOverflow && textScrollState.value > overflowTolerance
    val showBottomDivider = hasMeasuredOverflow &&
        textScrollState.value < textScrollState.maxValue - overflowTolerance
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Layout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            content = {
                Text(
                    text = "划线片段",
                    modifier = Modifier.layoutId("title"),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (showTopDivider) {
                    HorizontalDivider(
                        modifier = Modifier
                            .layoutId("topDivider")
                            .testTag("segment_action_sheet_top_divider"),
                    )
                }
                Text(
                    text = "“${highlight.displayText}”",
                    modifier = Modifier
                        .layoutId("text")
                        .verticalScroll(textScrollState),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                )
                if (showBottomDivider) {
                    HorizontalDivider(
                        modifier = Modifier
                            .layoutId("bottomDivider")
                            .testTag("segment_action_sheet_bottom_divider"),
                    )
                }
                Row(
                    modifier = Modifier
                        .layoutId("actions")
                        .fillMaxWidth(),
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
            },
        ) { measurables, constraints ->
            val spacing = 12.dp.roundToPx()
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val title = measurables.first { it.layoutId == "title" }.measure(looseConstraints)
            val textMeasurable = measurables.first { it.layoutId == "text" }
            val topDivider = measurables
                .firstOrNull { it.layoutId == "topDivider" }
                ?.measure(looseConstraints)
            val bottomDivider = measurables
                .firstOrNull { it.layoutId == "bottomDivider" }
                ?.measure(looseConstraints)
            val actions = measurables.first { it.layoutId == "actions" }.measure(looseConstraints)
            val naturalTextHeight = textMeasurable
                .maxIntrinsicHeight(constraints.maxWidth)
                .coerceAtMost(constraints.maxHeight)
            val naturalHeight = title.height + naturalTextHeight + actions.height + spacing * 2
            val needsPartialExpansion = naturalHeight > constraints.maxHeight / 2
            val layoutHeight = if (needsPartialExpansion) constraints.maxHeight else naturalHeight
            val visibleHeight = if (needsPartialExpansion) {
                val sheetOffset = runCatching { sheetState.requireOffset() }
                    .getOrDefault(constraints.maxHeight.toFloat())
                (constraints.maxHeight - sheetOffset.roundToInt()).coerceIn(
                    minimumValue = title.height + actions.height + spacing * 2,
                    maximumValue = constraints.maxHeight,
                )
            } else {
                layoutHeight
            }
            val textTop = title.height + spacing
            val actionsTop = visibleHeight - actions.height
            val textBottom = actionsTop - spacing
            val textHeight = (textBottom - textTop).coerceAtLeast(0)
            val text = textMeasurable.measure(
                looseConstraints.copy(maxHeight = textHeight),
            )

            layout(constraints.maxWidth, layoutHeight) {
                title.placeRelative(0, 0)
                text.placeRelative(0, textTop)
                topDivider?.placeRelative(0, textTop - topDivider.height)
                bottomDivider?.placeRelative(0, textBottom - bottomDivider.height)
                actions.placeRelative(0, actionsTop)
            }
        }
    }
}

private fun SegmentHighlight.toSegmentHighlightSpan(displayText: String): SegmentHighlightSpan = SegmentHighlightSpan(
    text = text,
    displayText = displayText,
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

private fun SegmentHighlight.segmentThreadKey(): String = listOf(
    attributes["data-highlight-content-type"],
    attributes["data-highlight-content-id"],
    attributes["data-highlight-id"],
).joinToString("|") { it.orEmpty() }

private fun Document.segmentDisplayTexts(): Map<String, String> {
    val displayTexts = mutableMapOf<String, String>()

    fun collect(node: Node) {
        if (node is SegmentHighlight) {
            node.attributes["data-highlight-display-text"]?.let {
                displayTexts[node.segmentThreadKey()] = it
            }
        }
        if (node is ContainerNode) node.children.forEach(::collect)
    }
    collect(this)
    return displayTexts
}

private fun SegmentHighlightSpan.toSegmentCommentHolder(): SegmentCommentHolder? {
    val contentId = contentId ?: return null
    val contentType = contentType ?: return null
    val segmentId = meta.segIds.joinToString(",").takeIf { it.isNotBlank() } ?: return null
    return SegmentCommentHolder(
        contentId = contentId,
        contentType = contentType,
        segmentId = segmentId,
        segmentContent = text,
        paragraphId = paragraphId ?: return null,
        startOffset = startOffset ?: return null,
        endOffset = endOffset ?: return null,
    )
}
