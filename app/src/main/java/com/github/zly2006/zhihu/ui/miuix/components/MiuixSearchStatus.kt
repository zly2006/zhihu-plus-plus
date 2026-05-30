/*
 * Based on KernelSU's SearchStatus.kt (GPL-3.0-only)
 *   https://github.com/tiann/KernelSU
 * Adapted for zhihu-plus-plus under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 搜索框状态机。4 态：
 *   COLLAPSED  折叠（假搜索框，显示 feed）
 *   EXPANDING  展开动画中
 *   EXPANDED   完全展开（搜索浮层 + 键盘）
 *   COLLAPSING 收起动画中
 */
@Stable
data class SearchStatus(
    val label: String,
    val searchText: String = "",
    val current: Status = Status.COLLAPSED,
    val offsetY: Dp = 0.dp,
    val resultStatus: ResultStatus = ResultStatus.DEFAULT,
) {
    fun isExpand() = current == Status.EXPANDED
    fun isCollapsed() = current == Status.COLLAPSED
    fun shouldExpand() = current == Status.EXPANDED || current == Status.EXPANDING
    fun shouldCollapsed() = current == Status.COLLAPSED || current == Status.COLLAPSING
    fun isAnimatingExpand() = current == Status.EXPANDING

    fun onAnimationComplete(): SearchStatus = when (current) {
        Status.EXPANDING -> copy(current = Status.EXPANDED)
        Status.COLLAPSING -> copy(searchText = "", current = Status.COLLAPSED)
        else -> this
    }

    /**
     * TopAppBar 消失/出现动画（抄 KernelSU）。
     * 折叠态 alpha=1 显示，展开态 alpha=0 隐藏（带过渡即回弹观感）。
     * 背景层始终绘制，避免内容透出。
     */
    @Composable
    fun TopAppBarAnim(
        modifier: Modifier = Modifier,
        visible: Boolean = shouldCollapsed(),
        backgroundColor: Color = colorScheme.surface,
        content: @Composable () -> Unit,
    ) {
        Box(modifier = modifier) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(backgroundColor),
            )
            Box(
                modifier = Modifier.graphicsLayer { alpha = if (visible) 1f else 0f },
            ) { content() }
        }
    }

    enum class Status { EXPANDED, EXPANDING, COLLAPSED, COLLAPSING }
    enum class ResultStatus { DEFAULT, EMPTY, LOAD, SHOW }
}
