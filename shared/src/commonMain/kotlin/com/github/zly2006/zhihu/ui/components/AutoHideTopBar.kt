/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.roundToInt

/**
 * 顶栏目标可见性，由 ZhihuMain 提供（`!autoHideTopBar || isBottomBarVisible`）。
 * 与底栏共用同一个 [com.github.zly2006.zhihu.ui.ZhihuMain] 的滚动信号，因此顶栏与底栏
 * 的收起/展开严格同步：底栏全收时顶栏全收、底栏全展时顶栏全展。
 */
val LocalAutoHideTopBarVisible = compositionLocalOf { true }
val LocalAutoHideTopBarScrollFraction = compositionLocalOf<Float?> { null }
val LocalAutoHideTopBarHeightChanged = compositionLocalOf<(Int) -> Unit> { {} }

/**
 * 顶栏自动隐藏包装。可见性由 [LocalAutoHideTopBarVisible] 驱动，动画曲线与底栏
 * `AnimatedVisibility` 完全一致（[tween] 200ms），两者逐帧同速。
 *
 * 不用 AnimatedVisibility：它在动画期间增删/重挂载内容并依赖 Scaffold 逐帧重测，
 * 进入首帧会闪一下全高、退出结束会塌一下，与底栏不同步导致信息流跳变。
 * 这里用单个 [Animatable] 驱动一个 0..1 的 fraction：内容始终按全高测量，
 * 但包装层只向 Scaffold 上报当前可见高度，让顶栏和内容 inset 使用同一个速度。
 * 收起时顶栏内容在裁切区域内向上平移，松手结算也沿同一 fraction 连续过渡。
 */
@Composable
fun AutoHideTopBar(content: @Composable () -> Unit) {
    val visible = LocalAutoHideTopBarVisible.current
    val scrollFraction = LocalAutoHideTopBarScrollFraction.current
    val onHeightChanged by rememberUpdatedState(LocalAutoHideTopBarHeightChanged.current)
    val fraction = remember { Animatable(if (visible) 1f else 0f) }
    LaunchedEffect(scrollFraction, visible) {
        val dragFraction = scrollFraction
        if (dragFraction != null) {
            fraction.snapTo(dragFraction)
        } else {
            fraction.animateTo(if (visible) 1f else 0f, tween(200))
        }
    }
    Box(
        Modifier
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val visibleHeight = (placeable.height * fraction.value).roundToInt().coerceIn(0, placeable.height)
                layout(placeable.width, visibleHeight) {
                    val y = visibleHeight - placeable.height
                    placeable.place(0, y)
                }
            },
    ) {
        Box(Modifier.onSizeChanged { onHeightChanged(it.height) }) {
            content()
        }
    }
}
