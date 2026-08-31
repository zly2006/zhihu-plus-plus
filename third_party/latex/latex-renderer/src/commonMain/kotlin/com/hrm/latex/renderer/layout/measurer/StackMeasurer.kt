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


package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max
import kotlin.reflect.KClass

/**
 * 堆叠渲染器，用于 \overset、\underset、\stackrel 等命令
 */
internal class StackMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Stack::class
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        node as LatexNode.Stack
        // 先对 Stack 结构做规范化 ：\overset{a}{\underset{b}{X}} 会产生 Stack 套 Stack
        // 如果不扁平化，会导致定位/贴合基于"已经堆叠后的盒子"，从而出现上下异常。
        fun unwrapSingleGroup(node0: LatexNode): LatexNode {
            var n = node0
            while (n is LatexNode.Group && n.children.size == 1) {
                n = n.children[0]
            }
            return n
        }

        fun merge(a: LatexNode?, b: LatexNode?): LatexNode? {
            if (a == null) return b
            if (b == null) return a
            return LatexNode.Group(listOf(a, b))
        }

        var baseNode: LatexNode = unwrapSingleGroup(node.base)
        var aboveNode: LatexNode? = node.above?.let { unwrapSingleGroup(it) }
        var belowNode: LatexNode? = node.below?.let { unwrapSingleGroup(it) }

        while (baseNode is LatexNode.Stack) {
            val inner = baseNode
            aboveNode = merge(aboveNode, inner.above?.let { unwrapSingleGroup(it) })
            belowNode = merge(belowNode, inner.below?.let { unwrapSingleGroup(it) })
            baseNode = unwrapSingleGroup(inner.base)
        }

        // 基础内容已是按 KaTeX TTF 真实墨迹边界测量的盒子。
        val baseLayout = measureGroup(listOf(baseNode), context)

        // 上下内容使用较小字体（0.7倍）
        val scriptStyle = context.shrink(MathConstants.STACK_SCRIPT_SCALE)

        // 测量上方内容（如果有）- 包装成List
        val aboveLayout = aboveNode?.let { measureGroup(listOf(it), scriptStyle) }

        // 测量下方内容（如果有）- 包装成List
        val belowLayout = belowNode?.let { measureGroup(listOf(it), scriptStyle) }

        // 计算总宽度（取最宽的元素）
        val totalWidth = max(
            baseLayout.width,
            max(aboveLayout?.width ?: 0f, belowLayout?.width ?: 0f)
        )

        // 严格使用三段盒模型，不再依赖旧行框的经验偏移。
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val gap = fontSizePx * MathConstants.STACK_VERTICAL_GAP
        val aboveY = aboveLayout?.let { 0f }
        val baseY = (aboveLayout?.height ?: 0f) + if (aboveLayout != null) gap else 0f
        val belowY = belowLayout?.let { baseY + baseLayout.height + gap }
        val totalHeight = if (belowLayout != null) {
            baseY + baseLayout.height + gap + belowLayout.height
        } else {
            baseY + baseLayout.height
        }
        val baseline = baseY + baseLayout.baseline

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            // 绘制基础内容（居中对齐）
            val baseX = x + (totalWidth - baseLayout.width) / 2
            baseLayout.draw(this, baseX, y + baseY)

            aboveLayout?.let { layout ->
                val aboveX = x + (totalWidth - layout.width) / 2
                layout.draw(this, aboveX, y + (aboveY ?: 0f))
            }

            belowLayout?.let { layout ->
                val belowX = x + (totalWidth - layout.width) / 2
                layout.draw(this, belowX, y + (belowY ?: 0f))
            }
        }
    }
}
