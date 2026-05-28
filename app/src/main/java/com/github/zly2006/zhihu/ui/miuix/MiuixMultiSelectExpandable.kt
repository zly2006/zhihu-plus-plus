/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixMultiSelectExpandable(
    title: String,
    options: List<String>,
    labels: Map<String, String>,
    selectedOptions: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        ArrowPreference(
            title = title,
            summary = "建议 3-5 项 (已选 ${selectedOptions.size})",
            onClick = { expanded = !expanded },
        )

        if (expanded) {
            HorizontalDivider(
                color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                thickness = 0.5.dp,
            )
            options.forEach { key ->
                val checked = key in selectedOptions
                Row(
                    Modifier.fillMaxWidth().clickable {
                        val newSet = if (checked) selectedOptions - key else selectedOptions + key
                        if (newSet.size in 3..5) onSelectionChange(newSet)
                    }.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(labels[key] ?: key, color = MiuixTheme.colorScheme.onSurface)
                    Checkbox(
                        state = if (checked) androidx.compose.ui.state.ToggleableState.On
                        else androidx.compose.ui.state.ToggleableState.Off,
                        onClick = {},
                    )
                }
                HorizontalDivider(
                    color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    thickness = 0.5.dp,
                )
            }
        }
    }
}
