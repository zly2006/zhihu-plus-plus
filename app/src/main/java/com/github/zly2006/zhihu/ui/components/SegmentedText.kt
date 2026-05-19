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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ThumbUp
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.subscreens.PREF_FONT_SIZE
import com.github.zly2006.zhihu.ui.subscreens.PREF_LINE_HEIGHT
import com.github.zly2006.zhihu.util.SegmentHighlightSpan
import com.github.zly2006.zhihu.util.SegmentTextParagraph
import com.github.zly2006.zhihu.util.SegmentTextPart
import com.github.zly2006.zhihu.util.clipboardManager

private const val SEGMENT_TAG = "segment-highlight"

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
    var selectedHighlight by remember(parts) { mutableStateOf<SegmentHighlightSpan?>(null) }
    val likedStates = remember(parts) { mutableStateMapOf<String, Pair<Boolean, Int>>() }
    val highlightBackground = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    val highlightTextColor = MaterialTheme.colorScheme.onSurface

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
                    background = highlightBackground,
                    color = highlightTextColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium,
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
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
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
        val currentLike = likedStates[key]?.first ?: currentHighlight.meta.isLike
        val currentLikeCount = likedStates[key]?.second ?: currentHighlight.meta.likeCount
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
                            likedStates[key] = (!currentLike) to (currentLikeCount + if (currentLike) -1 else 1)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ThumbUp,
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
                            actions.onCommentClick?.invoke(currentHighlight)
                        },
                        enabled = actions.onCommentClick != null,
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
