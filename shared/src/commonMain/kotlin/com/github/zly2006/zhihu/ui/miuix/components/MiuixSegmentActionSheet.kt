/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.util.SegmentHighlightSpan
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 划线片段操作弹层的 miuix 版本，对标 M3 `SegmentActionSheet`。
 *
 * M3 版整层用的是 `MaterialTheme.colorScheme`；miuix 主题下 `FilledTonalButton` 会拿到
 * miuix 的 `onSecondaryContainer`（偏灰），点赞/评论数字几乎看不见。这里换成 miuix 组件与配色，
 * 片段正文放进 miuix `Card`（16dp 圆角，与设置页、`MiuixSheetActionRow` 一致）。
 */
@Composable
fun MiuixSegmentActionSheet(
    highlight: SegmentHighlightSpan,
    onDismiss: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onCopyClick: () -> Unit,
) {
    WindowBottomSheet(
        cornerRadius = miuixSheetCornerRadius(),
        show = true,
        title = "划线片段",
        insideMargin = DpSize(16.dp, 0.dp),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().miuixSheetBottomInsets().padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    text = "“${highlight.displayText}”",
                    modifier = Modifier
                        .fillMaxWidth()
                        // 长片段不能把弹层顶满，超出部分自己滚动。
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SegmentCountButton(
                    modifier = Modifier.weight(1f),
                    count = highlight.meta.likeCount,
                    onClick = onLikeClick,
                    liked = highlight.meta.isLike,
                )
                SegmentCountButton(
                    modifier = Modifier.weight(1f),
                    count = highlight.meta.commentCount,
                    onClick = onCommentClick,
                    liked = null,
                )
                IconButton(onClick = onCopyClick) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "复制内容",
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** [liked] 为 null 表示评论按钮，非 null 表示点赞按钮及其是否已赞。 */
@Composable
private fun SegmentCountButton(
    count: Int,
    liked: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = if (liked == true) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(),
    ) {
        Icon(
            imageVector = when {
                liked == null -> Icons.AutoMirrored.Outlined.Comment
                liked -> Icons.Filled.ThumbUp
                else -> Icons.Outlined.ThumbUp
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(count.toString())
    }
}
