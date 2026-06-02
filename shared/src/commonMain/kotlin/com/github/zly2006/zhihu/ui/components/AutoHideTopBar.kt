/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

/**
 * 顶栏目标可见性，由 ZhihuMain 提供（`!autoHideTopBar || isBottomBarVisible`）。
 * 与底栏共用同一个 [com.github.zly2006.zhihu.ui.ZhihuMain] 的滚动信号，因此顶栏与底栏
 * 的收起/展开严格同步：底栏全收时顶栏全收、底栏全展时顶栏全展。
 */
val LocalAutoHideTopBarVisible = compositionLocalOf { true }

/**
 * 顶栏自动隐藏包装。可见性由 [LocalAutoHideTopBarVisible] 驱动，动画曲线与底栏
 * `AnimatedVisibility` 完全一致（[tween] 200ms），两者逐帧同速。
 *
 * 不用 AnimatedVisibility：它在动画期间增删/重挂载内容并依赖 Scaffold 逐帧重测，
 * 进入首帧会闪一下全高、退出结束会塌一下，与底栏不同步导致信息流跳变。
 * 这里用单个 [animateFloatAsState] 驱动一个 0..1 的 fraction：内容始终按全高测量
 * （不增删），用 Modifier.layout 把上报高度设为 fraction×全高 —— Scaffold 的内容
 * padding 用的就是这个上报高度，于是顶栏收缩与内容下移共用同一动画值、逐帧严格同步。
 * 内容锚定顶部，收缩时整体上移、底部被 clip，呈现向上折叠收起、与底栏自动隐藏对称。
 */
@Composable
fun AutoHideTopBar(content: @Composable () -> Unit) {
    val visible = LocalAutoHideTopBarVisible.current
    val fraction by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label = "autoHideTopBar",
    )
    Box(
        Modifier
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val height = (placeable.height * fraction).roundToInt()
                layout(placeable.width, height) {
                    // 锚定顶部：高度收缩时内容整体上移、底部被 clip
                    placeable.place(0, height - placeable.height)
                }
            },
    ) {
        content()
    }
}
