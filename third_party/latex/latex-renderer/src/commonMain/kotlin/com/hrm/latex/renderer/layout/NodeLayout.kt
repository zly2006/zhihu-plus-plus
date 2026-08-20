/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.hrm.latex.renderer.layout

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 节点布局结果
 *
 * 使用 class 而非 data class，因为 draw lambda 不参与 equals/hashCode。
 * 若使用 data class，remember(layout) 等基于 equality 的缓存机制会产生误判。
 * 缓存应基于产生 layout 的输入（AST 节点 + 配置），而非 layout 本身。
 *
 * @property width 墨水边界宽度（包含 Stroke 半宽、斜体悬伸等所有可能产生像素的区域）
 * @property height 墨水边界高度
 * @property baseline 基线距离顶部的距离，始终 >= 0
 * @property italicCorrection 字形右侧斜体修正；上标保留，下标会回退该距离
 * @property draw 绘制回调。(x, y) 是元素左上角的绝对画布坐标，由父级传入。
 */
class NodeLayout(
    val width: Float,
    val height: Float,
    val baseline: Float,
    val italicCorrection: Float = 0f,
    val draw: DrawScope.(x: Float, y: Float) -> Unit
) {
    companion object {
        internal val EMPTY: NodeLayout
            get() = NodeLayout(0f, 0f, 0f) { _, _ -> }
    }
}
