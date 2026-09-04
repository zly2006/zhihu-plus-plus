/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

/**
 * M3 主题的 AOSP 风格预测性返回转场。
 *
 * miuix-nav 用**同一个** [NavTransition] 驱动前进/返回与预测性返回手势（深度浮点模型，转场是 relativeDepth 的纯函数）。
 * 这里据 `scope.gesture` 是否存在区分两种状态，使预测返回手势呈现 Android 14+ 的 cross-activity 观感，而不改变前进/返回键体验：
 * - **手势进行中（gesture != null）**：关闭中的顶层缩到 ~0.9、加圆角、跟随起手边缘轻微外移（FOLLOW_GESTURE）；
 *   被揭示的下层从 ~0.92 放大回 1。
 * - **无手势（前进 push / 返回键程序化返回）**：标准横向滑动，与默认一致。
 *
 * 注：当设置项“启用预测性返回”关闭时，miuix-nav 不会写入 `gesture`（见 NavDisplay 的 fork patch），手势分支自然不触发，
 * 返回退化为普通滑动。
 */
val AospPredictiveBackTransition: NavTransition = navGraphicsTransition(opaqueDepth = 1f) { scope ->
    val d = scope.relativeDepth
    val width = scope.layoutSize.width.toFloat()
    val gesture = scope.gesture
    if (gesture != null) {
        if (d <= 0f) {
            // 关闭中的顶层：1→0.9 缩放 + 圆角 + 跟随起手边外移 + 淡出。
            val p = (1f + d).coerceIn(0f, 1f) // 1=完整在顶, 0=完全关闭
            val scale = 0.9f + 0.1f * p
            scaleX = scale
            scaleY = scale
            // 左边缘起手→向右移出；右/无边缘→向左移出。
            val edgeSign = if (gesture.swipeEdge == NavSwipeEdge.Left) 1f else -1f
            translationX = edgeSign * (1f - p) * width * 0.06f
            // 关键：让关闭层在收尾前淡到不可见，避免 miuix-nav 弹簧 settle 的慢尾把 0.9 缩放的页面挂在屏上、
            // 收尾时突然 unload 的“卡顿/突变”。淡出后即便弹簧仍在收敛，视觉上返回已完成。
            alpha = (p * 1.6f).coerceIn(0f, 1f)
            clip = true
            shape = RoundedCornerShape(28.dp)
        } else {
            // 被揭示的下层：0.92→1 放大。
            val p = d.coerceIn(0f, 1f) // 1=完全被覆盖, 0=完全揭示
            scaleX = 1f - 0.08f * p
            scaleY = scaleX
        }
    } else {
        // 无手势：标准横向滑动（与全局默认一致），不改变前进/返回键体验。
        val rtl = scope.layoutDirection == LayoutDirection.Rtl
        if (d <= 0f) {
            translationX = (if (rtl) -1f else 1f) * (-d).coerceIn(0f, 1f) * width
        } else {
            translationX = (if (rtl) 1f else -1f) * d.coerceIn(0f, 1f) * width * 0.25f
            alpha = 1f - 0.1f * d.coerceIn(0f, 1f)
        }
    }
}
