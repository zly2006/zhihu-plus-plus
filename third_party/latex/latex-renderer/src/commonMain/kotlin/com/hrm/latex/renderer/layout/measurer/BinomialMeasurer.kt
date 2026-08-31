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
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.toFractionChildStyle
import com.hrm.latex.renderer.utils.DelimiterRenderer
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max
import kotlin.reflect.KClass

/**
 * 二项式测量器 — 处理 \binom{n}{k}
 *
 * 布局类似分数，但无横线，左右包裹圆括号。
 * 使用 DelimiterRenderer 统一渲染括号。
 */
internal class BinomialMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Binomial::class
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        node as LatexNode.Binomial
        val childStyle = context.toFractionChildStyle()
        val numLayout = measureGroup(listOf(node.top), childStyle)
        val denLayout = measureGroup(listOf(node.bottom), childStyle)

        val fontSizePx = with(density) { context.fontSize.toPx() }
        val provider = context.mathFontProvider
        val defaultRuleThickness = provider?.fractionRuleThickness(fontSizePx)
            ?: fontSizePx * 0.04f
        val displayStyle = context.mathStyle == MathStyle.DISPLAY
        var numeratorShift = provider?.fractionNumeratorShiftUp(
            fontSizePx, displayStyle, hasRule = false
        ) ?: fontSizePx * if (displayStyle) 0.677f else 0.444f
        var denominatorShift = provider?.fractionDenominatorShiftDown(fontSizePx, displayStyle)
            ?: fontSizePx * if (displayStyle) 0.686f else 0.345f
        val clearance = defaultRuleThickness * if (displayStyle) 7f else 3f
        val currentClearance = (numeratorShift - (numLayout.height - numLayout.baseline)) -
            (denLayout.baseline - denominatorShift)
        if (currentClearance < clearance) {
            val adjustment = (clearance - currentClearance) / 2f
            numeratorShift += adjustment
            denominatorShift += adjustment
        }

        val contentWidth = max(numLayout.width, denLayout.width)
        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        val contentTop = minOf(
            -numeratorShift - numLayout.baseline,
            denominatorShift - denLayout.baseline
        )
        val contentBottom = maxOf(
            -numeratorShift + numLayout.height - numLayout.baseline,
            denominatorShift + denLayout.height - denLayout.baseline
        )
        val contentBaseline = -contentTop
        val contentHeight = contentBottom - contentTop

        val delimiterTarget = fontSizePx * if (displayStyle) 2.39f else 1.01f
        fun centered(layout: NodeLayout) = NodeLayout(
            layout.width, layout.height, layout.height / 2f + axisHeight, draw = layout.draw
        )
        val leftLayout = centered(
            DelimiterRenderer.measureScaled("(", context, measurer, delimiterTarget, density)
        )
        val rightLayout = centered(
            DelimiterRenderer.measureScaled(")", context, measurer, delimiterTarget, density)
        )

        val width = leftLayout.width + contentWidth + rightLayout.width
        val baseline = maxOf(contentBaseline, leftLayout.baseline, rightLayout.baseline)
        val descent = maxOf(
            contentHeight - contentBaseline,
            leftLayout.height - leftLayout.baseline,
            rightLayout.height - rightLayout.baseline
        )
        val height = baseline + descent

        return NodeLayout(width, height, baseline) { x, y ->
            leftLayout.draw(this, x, y + baseline - leftLayout.baseline)

            val numX = x + leftLayout.width + (contentWidth - numLayout.width) / 2
            val denX = x + leftLayout.width + (contentWidth - denLayout.width) / 2
            numLayout.draw(
                this, numX, y + baseline - numeratorShift - numLayout.baseline
            )
            denLayout.draw(
                this, denX, y + baseline + denominatorShift - denLayout.baseline
            )

            rightLayout.draw(
                this,
                x + leftLayout.width + contentWidth,
                y + baseline - rightLayout.baseline
            )
        }
    }
}
