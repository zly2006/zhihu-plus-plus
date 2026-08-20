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

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.DelimiterRenderer
import com.hrm.latex.renderer.utils.FontResolver
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max
import kotlin.reflect.KClass

/**
 * 定界符测量器
 *
 * 负责测量：
 * 1. 自动伸缩的括号 (\left( ... \right))
 * 2. 手动控制大小的括号 (\big, \Big, \bigg, \Bigg)
 */
internal class DelimiterMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Delimited::class,
        LatexNode.ManualSizedDelimiter::class
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        return when (node) {
            is LatexNode.Delimited -> measureDelimited(
                node,
                context,
                measurer,
                density,
                measureGroup
            )

            is LatexNode.ManualSizedDelimiter -> measureManualSizedDelimiter(
                node,
                context,
                measurer,
                density
            )

            else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
        }
    }

    /**
     * 测量自动伸缩的定界符 (\left ... \right)
     *
     * 逻辑：
     * 1. 测量内部内容。
     * 2. 内容的高度决定了括号的高度。
     * 3. 如果括号类型支持绘制（如 () [] {}），则绘制矢量图形。
     * 4. 否则（如文本），回退到普通文本测量。
     */
    private fun measureDelimited(
        node: LatexNode.Delimited,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        val leftStr = node.left
        val rightStr = node.right

        val fontSizePx = with(density) { context.fontSize.toPx() }
        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        val provisionalContent = measureGroup(node.content, context)
        val provisionalHeight = delimiterTargetHeight(provisionalContent, axisHeight, fontSizePx)
        val middleScale = when {
            provisionalHeight <= fontSizePx -> 1.0f
            provisionalHeight <= fontSizePx * 1.2f -> 1.2f
            provisionalHeight <= fontSizePx * 1.8f -> 1.8f
            provisionalHeight <= fontSizePx * 2.4f -> 2.4f
            else -> 3.0f
        }
        val contentLayout = if (node.content.any { it is LatexNode.ManualSizedDelimiter && it.isMiddle }) {
            measureGroup(
                node.content.map {
                    if (it is LatexNode.ManualSizedDelimiter && it.isMiddle) it.copy(size = middleScale) else it
                },
                context
            )
        } else provisionalContent

        // TeX make_left_right / KaTeX: delimiterFactor=901, shortfall=5pt=.5em.
        val delimiterHeight = delimiterTargetHeight(contentLayout, axisHeight, fontSizePx)

        val leftLayout = if (leftStr.isNotEmpty()) {
            centerOnAxis(
                DelimiterRenderer.measureScaled(leftStr, context, measurer, delimiterHeight, density),
                axisHeight
            )
        } else null

        val rightLayout = if (rightStr.isNotEmpty()) {
            centerOnAxis(
                DelimiterRenderer.measureScaled(rightStr, context, measurer, delimiterHeight, density),
                axisHeight
            )
        } else null

        val leftW = leftLayout?.width ?: 0f
        val rightW = rightLayout?.width ?: 0f

        val width = leftW + contentLayout.width + rightW
        val baseline = maxOf(
            contentLayout.baseline,
            leftLayout?.baseline ?: 0f,
            rightLayout?.baseline ?: 0f
        )
        val descent = maxOf(
            contentLayout.height - contentLayout.baseline,
            leftLayout?.let { it.height - it.baseline } ?: 0f,
            rightLayout?.let { it.height - it.baseline } ?: 0f
        )
        val height = baseline + descent

        return NodeLayout(width, height, baseline) { x, y ->
            var curX = x

            if (leftLayout != null) {
                leftLayout.draw(this, curX, y + baseline - leftLayout.baseline)
                curX += leftLayout.width
            }

            contentLayout.draw(this, curX, y + baseline - contentLayout.baseline)
            curX += contentLayout.width

            if (rightLayout != null) {
                rightLayout.draw(this, curX, y + baseline - rightLayout.baseline)
            }
        }
    }

    private fun delimiterTargetHeight(content: NodeLayout, axisHeight: Float, fontSizePx: Float): Float {
        val contentDepth = content.height - content.baseline
        val maxDistanceFromAxis = max(
            content.baseline - axisHeight,
            contentDepth + axisHeight
        )
        return max(
            2f * maxDistanceFromAxis * 0.901f,
            2f * maxDistanceFromAxis - fontSizePx * 0.5f
        )
    }

    /**
     * 测量手动大小的定界符 (\big, \Big, \bigg, \Bigg)
     *
     * KaTeX 策略：直接使用对应的 Size 字体字形，无需放大 fontSize。
     * - \big  (1.2) → Size1
     * - \Big  (1.8) → Size2
     * - \bigg (2.4) → Size3
     * - \Bigg (3.0) → Size4
     */
    private fun measureManualSizedDelimiter(
        node: LatexNode.ManualSizedDelimiter,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density
    ): NodeLayout {
        val delimiter = node.delimiter
        val scaleFactor = node.size

        val fontSizePx = with(density) { context.fontSize.toPx() }
        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        DelimiterRenderer.measureVerticalBars(
            delimiter = delimiter,
            context = context,
            measurer = measurer,
            targetHeight = fontSizePx * scaleFactor,
            density = density
        )?.let { return centerOnAxis(it, axisHeight) }

        val glyph = FontResolver.resolveDelimiterGlyph(delimiter, context.fontFamilies)

        // 直接用对应的 Size 字体，不放大 fontSize
        val fontFamily = FontResolver.manualDelimiterFont(context.fontFamilies, scaleFactor)
            ?: context.fontFamily
        val delimiterStyle = context.copy(
            fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
            fontFamily = fontFamily,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
        )

        val bytes = when {
            scaleFactor <= 1.0f -> context.fontFamilies?.mainBytes
            scaleFactor <= 1.2f -> context.fontFamilies?.size1Bytes
            scaleFactor <= 1.8f -> context.fontFamilies?.size2Bytes
            scaleFactor <= 2.4f -> context.fontFamilies?.size3Bytes
            else -> context.fontFamilies?.size4Bytes
        }
        val measured = DelimiterRenderer.measureText(
            glyph, delimiterStyle, measurer, bytes, density
        )
        return centerOnAxis(measured, axisHeight)
    }

    private fun centerOnAxis(layout: NodeLayout, axisHeight: Float): NodeLayout = NodeLayout(
        width = layout.width,
        height = layout.height,
        baseline = layout.height / 2f + axisHeight,
        draw = layout.draw
    )
}
