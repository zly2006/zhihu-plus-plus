/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun <T> MiuixMultiSelectExpandable(
    title: String,
    options: List<T>,
    selectedOptions: Set<T>,
    onSelectionChange: (Set<T>) -> Unit,
    optionLabel: (T) -> String,
    lockedOptions: Set<T> = emptySet(),
    minSelection: Int = 1,
    emptySummary: String = "未选择",
) {
    val context = LocalContext.current
    val expanded = rememberSaveable { mutableStateOf(false) }

    val summary = if (selectedOptions.isEmpty()) {
        emptySummary
    } else {
        options.filter { it in selectedOptions }.joinToString("、") { optionLabel(it) }
    }

    Column {
        ArrowPreference(
            title = title,
            summary = summary,
            onClick = { expanded.value = !expanded.value },
        )
        AnimatedVisibility(
            visible = expanded.value,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                options.forEach { option ->
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    )
                    val isLocked = option in lockedOptions
                    val isChecked = isLocked || option in selectedOptions

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable(enabled = !isLocked) {
                                val newSet = if (isChecked) selectedOptions - option
                                else selectedOptions + option
                                if (newSet.size >= minSelection) {
                                    onSelectionChange(newSet)
                                } else {
                                    Toast.makeText(context, "至少保留 $minSelection 项", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = optionLabel(option),
                            modifier = Modifier.weight(1f),
                            style = MiuixTheme.textStyles.body1,
                            color = if (isLocked) MiuixTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            else MiuixTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.width(8.dp))
                        Checkbox(
                            state = if (isChecked) ToggleableState.On else ToggleableState.Off,
                            onClick = {},
                            enabled = !isLocked,
                        )
                    }
                }
            }
        }
    }
}
