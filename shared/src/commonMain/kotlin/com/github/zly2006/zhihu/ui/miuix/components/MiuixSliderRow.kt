/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

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
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Slider

/**
 * 设置页里展开在 [MiuixExpandableArrowPreference] 下方的滑块行。
 *
 * 拖动过程中用本地状态跟手，同时把每一步都回调出去，让调用方立刻写偏好并刷新摘要文字；
 * [value] 变化会重置本地状态，因此调用方对取值做规整（取整、限幅）不会让滑块跳动。
 */
@Composable
fun MiuixSliderRow(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
) {
    var sliderValue by remember(value) { mutableStateOf(value) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it)
            },
            valueRange = range,
            steps = steps,
            enabled = enabled,
            modifier = modifier.weight(1f),
        )
    }
}
