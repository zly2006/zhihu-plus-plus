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

package com.github.zly2006.zhihu.theme

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixCircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField

/**
 * 按当前皮肤渲染的基础控件。
 *
 * 登录面板这类页面业务流程只有一份（协议交互、风控分支、倒计时都不该复制），但两套皮肤的控件外观完全不同。
 * 与其为 miuix 复制一整个面板，不如让面板用这里的控件：分支只在这一处，页面本身保持单一实现。
 * 配色和字体走 [AppTokens]，这里只负责控件形态。
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingText: String? = null,
) {
    if (LocalThemeStyle.current == ThemeStyle.Miuix) {
        MiuixTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            // miuix TextField 没有 leadingIcon 槽，把 +86 这类前缀并进 label 里，避免丢信息。
            label = leadingText?.let { "$label（$it）" } ?: label,
            useLabelAsPlaceholder = true,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = { Text(label) },
            leadingIcon = leadingText?.let { { Text(it) } },
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
    }
}

/** 主按钮；[loading] 时显示转圈而不是文字，两套皮肤都用各自的进度指示器。 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    if (LocalThemeStyle.current == ThemeStyle.Miuix) {
        MiuixButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            if (loading) {
                MiuixCircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                MiuixText(text)
            }
        }
    } else {
        Button(onClick = onClick, modifier = modifier, enabled = enabled) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(text)
            }
        }
    }
}

/** 次要按钮（换一张验证码、刷新二维码等）。 */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalThemeStyle.current == ThemeStyle.Miuix) {
        MiuixTextButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            Text(text)
        }
    }
}

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalThemeStyle.current == ThemeStyle.Miuix) {
        MiuixCheckbox(
            state = if (checked) ToggleableState.On else ToggleableState.Off,
            onClick = { onCheckedChange(!checked) },
            modifier = modifier,
        )
    } else {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
    }
}

@Composable
fun AppCircularProgressIndicator(modifier: Modifier = Modifier) {
    if (LocalThemeStyle.current == ThemeStyle.Miuix) {
        MiuixCircularProgressIndicator(modifier = modifier)
    } else {
        CircularProgressIndicator(modifier = modifier)
    }
}
