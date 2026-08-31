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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.toFractionChildStyle
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max
import kotlin.reflect.KClass

/**
 * 分数测量器 — 处理 \frac{num}{den}
 *
 * 布局模型：
 * ```
 *   ┌─── numerator (居中) ───┐
 *   │         gap             │
 * ```
 *   ├───── fraction line ─────┤  ← baseline = lineY + ruleThickness/2 + axisHeight
 *   │         gap             │
 *   └─── denominator (居中) ──┘
 * ```
 *
 * 使用 MathStyle 状态机决定子式字号：
 * - DISPLAY → TEXT (子式)
 * - TEXT → SCRIPT
 * - SCRIPT → SCRIPT_SCRIPT
 */
internal class FractionMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Fraction::class
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        node as LatexNode.Fraction
        // \dfrac/\tfrac/\cfrac should override the effective style of this fraction only.
        // In TeX, \dfrac forces displaystyle fraction in inline math; \tfrac forces textstyle.
        val effectiveContext = when (node.style) {
            LatexNode.Fraction.FractionStyle.DISPLAY,
            LatexNode.Fraction.FractionStyle.CONTINUED ->
                context.copy(mathStyle = MathStyle.DISPLAY)

            LatexNode.Fraction.FractionStyle.TEXT ->
                context.copy(mathStyle = MathStyle.TEXT)

            LatexNode.Fraction.FractionStyle.RULELESS,
            LatexNode.Fraction.FractionStyle.NORMAL ->
                context
        }

        val childStyle = effectiveContext.toFractionChildStyle()
        val numeratorLayout = measureGroup(listOf(node.numerator), childStyle)
        val denominatorLayout = measureGroup(listOf(node.denominator), childStyle)

        val fontSizePx = with(density) { effectiveContext.fontSize.toPx() }
        val provider = effectiveContext.mathFontProvider
        val hasRule = node.style != LatexNode.Fraction.FractionStyle.RULELESS
        val defaultRuleThickness = provider?.fractionRuleThickness(fontSizePx)
            ?: (fontSizePx * MathConstants.FRACTION_RULE_THICKNESS)
        val ruleThickness = if (hasRule) defaultRuleThickness else 0f
        val displayStyle = effectiveContext.mathStyle == MathStyle.DISPLAY
        val numeratorClearance = if (displayStyle) {
            provider?.fractionNumeratorDisplayGap(fontSizePx) ?: defaultRuleThickness * 3f
        } else {
            provider?.fractionNumeratorGap(fontSizePx) ?: defaultRuleThickness
        }
        val denominatorClearance = if (displayStyle) {
            provider?.fractionDenominatorDisplayGap(fontSizePx) ?: defaultRuleThickness * 3f
        } else {
            provider?.fractionDenominatorGap(fontSizePx) ?: defaultRuleThickness
        }
        val nullDelimiterSpace = fontSizePx * 0.12f
        val width = max(numeratorLayout.width, denominatorLayout.width) + nullDelimiterSpace * 2f
        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)

        var numeratorShift = provider?.fractionNumeratorShiftUp(
            fontSizePx, displayStyle, hasRule
        ) ?: fontSizePx * if (displayStyle) 0.677f else if (hasRule) 0.394f else 0.444f
        var denominatorShift = provider?.fractionDenominatorShiftDown(fontSizePx, displayStyle)
            ?: fontSizePx * if (displayStyle) 0.686f else 0.345f

        val numeratorDepth = numeratorLayout.height - numeratorLayout.baseline
        val denominatorHeight = denominatorLayout.baseline

        if (hasRule) {
            val halfRule = ruleThickness / 2f
            val numeratorGap = (numeratorShift - numeratorDepth) - (axisHeight + halfRule)
            if (numeratorGap < numeratorClearance) {
                numeratorShift += numeratorClearance - numeratorGap
            }
            val denominatorGap = (axisHeight - halfRule) - (denominatorHeight - denominatorShift)
            if (denominatorGap < denominatorClearance) {
                denominatorShift += denominatorClearance - denominatorGap
            }
        } else {
            val clearance = if (displayStyle) defaultRuleThickness * 7f else defaultRuleThickness * 3f
            val current = (numeratorShift - numeratorDepth) -
                (denominatorHeight - denominatorShift)
            if (current < clearance) {
                val adjustment = (clearance - current) / 2f
                numeratorShift += adjustment
                denominatorShift += adjustment
            }
        }

        val numeratorTopRel = -numeratorShift - numeratorLayout.baseline
        val numeratorBottomRel = -numeratorShift + numeratorDepth
        val denominatorTopRel = denominatorShift - denominatorLayout.baseline
        val denominatorBottomRel = denominatorShift +
            (denominatorLayout.height - denominatorLayout.baseline)
        val ruleTopRel = -axisHeight - ruleThickness / 2f
        val ruleBottomRel = -axisHeight + ruleThickness / 2f
        val top = minOf(numeratorTopRel, denominatorTopRel, if (hasRule) ruleTopRel else 0f)
        val bottom = maxOf(numeratorBottomRel, denominatorBottomRel, if (hasRule) ruleBottomRel else 0f)
        val height = bottom - top
        val baseline = -top

        return NodeLayout(width, height, baseline) { x, y ->
            val numeratorX = x + (width - numeratorLayout.width) / 2f
            numeratorLayout.draw(
                this, numeratorX, y + baseline - numeratorShift - numeratorLayout.baseline
            )

            if (hasRule) {
                drawLine(
                    color = effectiveContext.color,
                    start = Offset(x + nullDelimiterSpace, y + baseline - axisHeight),
                    end = Offset(x + width - nullDelimiterSpace, y + baseline - axisHeight),
                    strokeWidth = ruleThickness
                )
            }

            val denominatorX = x + (width - denominatorLayout.width) / 2f
            denominatorLayout.draw(
                this, denominatorX, y + baseline + denominatorShift - denominatorLayout.baseline
            )
        }
    }
}
