/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixColorPickerDialog(
    title: String = "选择颜色",
    initialColor: Color,
    presetColors: List<Color> = listOf(
        Color(0xFF2196F3),
        Color(0xFF4CAF50),
        Color(0xFFF44336),
        Color(0xFFFF9800),
        Color(0xFF9C27B0),
        Color(0xFF607D8B),
    ),
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    var selectedColor by remember { mutableStateOf(initialColor) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.padding(32.dp)) {
            Column(Modifier.padding(24.dp)) {
                SmallTitle(text = title)

                Spacer(Modifier.height(16.dp))

                HsvColorPicker(
                    color = selectedColor,
                    onColorChanged = { selectedColor = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))

                if (presetColors.isNotEmpty()) {
                    Text("预设颜色", style = MiuixTheme.textStyles.body2)
                    Spacer(Modifier.height(8.dp))
                    ColorPalette(
                        color = selectedColor,
                        onColorChanged = { selectedColor = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(text = "取消", onClick = onDismiss)
                    Button(onClick = { onColorSelected(selectedColor) }) { Text("确定") }
                }
            }
        }
    }
}
