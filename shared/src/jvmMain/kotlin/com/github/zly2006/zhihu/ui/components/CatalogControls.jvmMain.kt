package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun LiquidSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier, enabled: Boolean): Unit = error("此平台不支持 Liquid Glass 开关")

@Composable
internal actual fun CatalogButton(onClick: () -> Unit, modifier: Modifier, enabled: Boolean, containerColor: Color, contentColor: Color, content: @Composable RowScope.() -> Unit): Unit = error("此平台不支持 Liquid Glass 按钮")

@Composable
internal actual fun CatalogSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier, enabled: Boolean, valueRange: ClosedFloatingPointRange<Float>, steps: Int, onValueChangeFinished: (() -> Unit)?): Unit = error("此平台不支持 Liquid Glass 滑杆")
