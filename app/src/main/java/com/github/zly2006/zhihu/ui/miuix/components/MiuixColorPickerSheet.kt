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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.HsvColorPicker
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

private val DefaultPresetColors = listOf(
    Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFF44336),
    Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFF607D8B),
)

@Composable
fun MiuixColorPickerSheet(
    show: MutableState<Boolean>,
    title: String,
    initialColor: Color,
    presetColors: List<Color> = DefaultPresetColors,
    onConfirm: (Color) -> Unit,
) {
    var selectedColor by remember { mutableStateOf(initialColor) }

    LaunchedEffect(show.value) {
        if (show.value) selectedColor = initialColor
    }

    WindowBottomSheet(
        show = show.value,
        onDismissRequest = { show.value = false },
        title = title,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            HsvColorPicker(
                color = selectedColor,
                onColorChanged = { selectedColor = it },
                modifier = Modifier.fillMaxWidth(),
            )

            if (presetColors.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "预设颜色",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(8.dp))
                ColorPalette(
                    color = selectedColor,
                    onColorChanged = { selectedColor = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    text = "取消",
                    onClick = { show.value = false },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { onConfirm(selectedColor) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("确定")
                }
            }
        }
    }
}
