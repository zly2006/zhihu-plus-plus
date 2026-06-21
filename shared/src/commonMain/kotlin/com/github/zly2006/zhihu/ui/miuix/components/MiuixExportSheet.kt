/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import kotlin.math.roundToInt

/**
 * 导出文章弹层的 miuix 版本，复用 M3 [com.github.zly2006.zhihu.ui.components.ExportDialogComponent] 的回调契约，
 * 仅把 Dialog/Card/Slider 换成 miuix [WindowBottomSheet] + [Slider]，配色走 [MiuixTheme]。
 * 不可用的「导出 PDF」用描边空心样式，与其余可用项的填充样式相反，明确表达禁用。
 */
@Composable
fun MiuixExportSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    onExportHtml: suspend (includeAppAttribution: Boolean, onComplete: (Boolean) -> Unit) -> Unit,
    onExportImage: suspend (includeAppAttribution: Boolean, onComplete: (Boolean) -> Unit) -> Unit,
    onExportMarkdown: () -> Unit,
    onExportImageWithComments: suspend (commentCount: Int, includeAppAttribution: Boolean, onComplete: (Boolean) -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var commentCount by remember { mutableIntStateOf(3) }
    var isExporting by remember { mutableStateOf(false) }
    var includeAppAttribution by remember { mutableStateOf(true) }

    fun runExport(block: suspend ((Boolean) -> Unit) -> Unit) {
        if (isExporting) return
        isExporting = true
        scope.launch {
            block { success ->
                isExporting = false
                if (success) onDismiss()
            }
        }
    }

    WindowBottomSheet(show = show, title = "导出文章", onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExportActionRow(Icons.Filled.PictureAsPdf, "导出为 PDF", "暂时禁用，后续切第三方库实现", enabled = false) {}
            ExportActionRow(Icons.Filled.GetApp, "导出为 HTML", "图片会内联为 data URL，便于离线保存", enabled = !isExporting) {
                runExport { onComplete -> onExportHtml(includeAppAttribution, onComplete) }
            }
            ExportActionRow(Icons.Filled.Image, "导出为图片", enabled = !isExporting) {
                runExport { onComplete -> onExportImage(includeAppAttribution, onComplete) }
            }
            ExportActionRow(Icons.Filled.ContentCopy, "复制 Markdown", enabled = !isExporting) {
                onExportMarkdown()
                onDismiss()
            }

            SmallTitle(text = "带评论导出")
            Text(
                "包含评论数量: $commentCount 条",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 4.dp),
            )
            Slider(
                value = commentCount.toFloat(),
                onValueChange = { commentCount = it.roundToInt() },
                valueRange = 0f..10f,
                modifier = Modifier.fillMaxWidth(),
            )
            ExportActionRow(Icons.Filled.GetApp, "导出图片（含评论）", enabled = !isExporting) {
                runExport { onComplete -> onExportImageWithComments(commentCount, includeAppAttribution, onComplete) }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("在导出底部加入知乎++开源项目说明", color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Switch(checked = includeAppAttribution, onCheckedChange = { includeAppAttribution = it }, enabled = !isExporting)
            }

            if (isExporting) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("导出中...", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun ExportActionRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    // 可用项=填充背景；不可用项=描边空心 + 内容淡化（与可用项相反，明确禁用态）。
    val base = if (enabled) {
        Modifier.background(MiuixTheme.colorScheme.secondaryContainer, shape)
    } else {
        Modifier.border(1.dp, MiuixTheme.colorScheme.dividerLine, shape)
    }
    val contentColor = if (enabled) {
        MiuixTheme.colorScheme.onSecondaryContainer
    } else {
        MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(base)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = contentColor)
            if (summary != null) {
                Text(summary, color = contentColor, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
