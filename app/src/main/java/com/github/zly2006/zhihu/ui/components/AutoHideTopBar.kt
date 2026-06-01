/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.components

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.SettingsKeys
import kotlin.math.roundToInt

/**
 * 顶栏自动隐藏包装。[scrollVisible] 由 ZhihuMain 的全局滚动信号驱动（上划 false、下划 true）；
 * 是否真正隐藏由设置项 [SettingsKeys.AUTO_HIDE_TOP_BAR] 控制，关闭时顶栏始终显示。
 *
 * 不用 AnimatedVisibility：它在动画期间增删/重挂载内容并依赖 Scaffold 逐帧重测，
 * 进入首帧会闪一下全高、退出结束会塌一下，与手势不同步导致信息流跳变。
 * 这里用单个 [animateFloatAsState] 驱动一个 0..1 的 fraction：内容始终按全高测量（不增删），
 * 用 Modifier.layout 把上报高度设为 fraction×全高 —— Scaffold 的内容 padding 用的就是这个
 * 上报高度，于是顶栏收缩与内容下移共用同一动画值、逐帧严格同步，无任何突变。
 * 内容锚定顶部，收缩时上半部上移、超出部分被 clip，呈现向上折叠收起、与底栏自动隐藏对称。
 */
@Composable
fun AutoHideTopBar(scrollVisible: Boolean, content: @Composable () -> Unit) {
    val autoHide = LocalContext.current
        .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        .getBoolean(SettingsKeys.AUTO_HIDE_TOP_BAR, false)
    val fraction by animateFloatAsState(
        targetValue = if (!autoHide || scrollVisible) 1f else 0f,
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
