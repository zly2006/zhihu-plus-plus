package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
internal actual fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    contentPadding: PaddingValues,
    content: @Composable RowScope.() -> Unit,
): Unit = error("JVM 平台暂不支持 Liquid Glass 按钮渲染")

@Composable
internal actual fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
): Unit = error("JVM 平台暂不支持 Liquid Glass 开关渲染")

@Composable
internal actual fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChangeFinished: (() -> Unit)?,
): Unit = error("JVM 平台暂不支持 Liquid Glass 滑杆渲染")

@Composable
internal actual fun LiquidNavigationBar(
    items: List<AppNavigationItem>,
): Unit = error("JVM 平台暂不支持 Liquid Glass 底部导航栏渲染")
