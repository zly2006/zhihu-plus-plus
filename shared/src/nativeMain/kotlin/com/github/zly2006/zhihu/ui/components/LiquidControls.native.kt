package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// 备注：后续 macOS 和 iOS 会使用系统原生的 Liquid Glass 组件，而非这里的 Android 实现。
@Composable
internal actual fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    contentPadding: PaddingValues,
    content: @Composable RowScope.() -> Unit,
): Unit = error("Native 平台暂不支持 Liquid Glass 按钮渲染")

@Composable
internal actual fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
): Unit = error("Native 平台暂不支持 Liquid Glass 开关渲染")

@Composable
internal actual fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChangeFinished: (() -> Unit)?,
): Unit = error("Native 平台暂不支持 Liquid Glass 滑杆渲染")

@Composable
internal actual fun LiquidNavigationBar(
    items: List<AppNavigationItem>,
): Unit = error("Native 平台暂不支持 Liquid Glass 底部导航栏渲染")
