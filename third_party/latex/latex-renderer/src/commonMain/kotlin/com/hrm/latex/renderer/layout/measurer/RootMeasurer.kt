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
import com.hrm.latex.renderer.utils.DelimiterRenderer
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max
import kotlin.reflect.KClass

/** 使用 KaTeX Size 字体阶梯渲染 `\sqrt[index]{content}`。 */
internal class RootMeasurer : NodeMeasurer {
    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(LatexNode.Root::class)

    private companion object {
        const val SQRT_CHAR = "\u221A"
        const val CONTENT_LEFT_KERN = 0.08f
        const val CONTENT_RIGHT_KERN = 0.05f
    }

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        node as LatexNode.Root
        val indexStyle = context.copy(
            fontSize = context.fontSize *
                (MathStyle.SCRIPT_SCRIPT.scaleFactor() / context.mathStyle.scaleFactor()),
            mathStyle = MathStyle.SCRIPT_SCRIPT
        )
        val content = measureGroup(listOf(node.content), context)
        val index = node.index?.let { measureGroup(listOf(it), indexStyle) }
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val provider = context.mathFontProvider
        val ruleThickness = provider?.radicalRuleThickness(fontSizePx)
            ?: fontSizePx * MathConstants.FRACTION_RULE_THICKNESS
        val gap = if (context.mathStyle == MathStyle.DISPLAY) {
            provider?.radicalDisplayVerticalGap(fontSizePx)
                ?: (ruleThickness + fontSizePx * 0.431f / 4f)
        } else {
            provider?.radicalVerticalGap(fontSizePx) ?: ruleThickness * 1.25f
        }
        val targetHeight = content.height + gap + ruleThickness
        val surd = DelimiterRenderer.measureScaled(
            SQRT_CHAR,
            context,
            measurer,
            targetHeight,
            density
        )
        return layoutRoot(surd, content, index, ruleThickness, fontSizePx, context, targetHeight)
    }

    private fun layoutRoot(
        surd: NodeLayout,
        content: NodeLayout,
        index: NodeLayout?,
        ruleThickness: Float,
        fontSizePx: Float,
        context: RenderContext,
        targetHeight: Float
    ): NodeLayout {
        val bodyHeight = max(targetHeight, surd.height)
        val surdY = bodyHeight - surd.height
        val contentY = bodyHeight - content.height
        val contentX = surd.width + fontSizePx * CONTENT_LEFT_KERN
        val width = contentX + content.width + fontSizePx * CONTENT_RIGHT_KERN
        val indexX = max(0f, surd.width * 0.5f - (index?.width ?: 0f))
        val indexY = bodyHeight * 0.3f - (index?.height ?: 0f)
        val topPadding = max(0f, -indexY)

        return NodeLayout(
            width = width,
            height = bodyHeight + topPadding,
            baseline = topPadding + contentY + content.baseline
        ) { x, y ->
            index?.draw(this, x + indexX, y + topPadding + indexY)
            surd.draw(this, x, y + topPadding + surdY)
            drawLine(
                color = context.color,
                start = Offset(
                    x + surd.width - fontSizePx * 0.02f,
                    y + topPadding + surdY
                ),
                end = Offset(x + width, y + topPadding + surdY),
                strokeWidth = ruleThickness
            )
            content.draw(this, x + contentX, y + topPadding + contentY)
        }
    }
}
