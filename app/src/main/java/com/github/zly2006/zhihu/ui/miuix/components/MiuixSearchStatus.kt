/*
 * Based on KernelSU's SearchStatus.kt (GPL-3.0-only)
 *   https://github.com/tiann/KernelSU
 * Adapted for zhihu-plus-plus under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 搜索框状态机。4 态：
 *   COLLAPSED  折叠（假搜索框，显示 feed）
 *   EXPANDING  展开动画中
 *   EXPANDED   完全展开（搜索浮层 + 键盘）
 *   COLLAPSING 收起动画中
 *
 * 状态推进：点假框 → EXPANDING →(动画完成)→ EXPANDED
 *           点取消/返回 → COLLAPSING →(动画完成)→ COLLAPSED（清空搜索词）
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

    /** 动画完成后推进状态：EXPANDING→EXPANDED；COLLAPSING→COLLAPSED 并清空搜索词。 */
    fun onAnimationComplete(): SearchStatus = when (current) {
        Status.EXPANDING -> copy(current = Status.EXPANDED)
        Status.COLLAPSING -> copy(searchText = "", current = Status.COLLAPSED)
        else -> this
    }

    enum class Status { EXPANDED, EXPANDING, COLLAPSED, COLLAPSING }
    enum class ResultStatus { DEFAULT, EMPTY, LOAD, SHOW }
}
