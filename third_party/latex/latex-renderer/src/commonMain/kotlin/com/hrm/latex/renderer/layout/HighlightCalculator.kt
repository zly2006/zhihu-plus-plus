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

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.model.HighlightRange
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.utils.MathSpacing

/**
 * 高亮矩形区域
 */
data class HighlightRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * 高亮区域计算器
 *
 * 负责根据高亮配置（pattern 或 nodeIndices）计算每个高亮区域在布局中的边界矩形。
 * 间距计算与 [measureGroup] 使用相同的 [MathSpacing] 逻辑，确保高亮位置与渲染内容精确对齐。
 */
internal object HighlightCalculator {

    /**
     * 计算高亮区域的边界矩形
     *
     * @param children 文档根节点的子节点列表
     * @param ranges 高亮范围配置列表
     * @param context 渲染上下文
     * @param measurer 文本测量器
     * @param density 屏幕密度
     * @param groupLayout 整体布局结果（用于获取总高度）
     * @return 高亮矩形与对应高亮范围的配对列表
     */
    fun computeHighlightRects(
        children: List<LatexNode>,
        ranges: List<HighlightRange>,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        groupLayout: NodeLayout,
        cache: LayoutCache? = null
    ): List<Pair<HighlightRect, HighlightRange>> {
        if (children.isEmpty()) return emptyList()

        // 预测量每个子节点的尺寸，用于计算 x 偏移
        // 如果存在 cache，这些节点在主测量阶段已被缓存，此处直接命中
        val childLayouts = children.map { measureNode(it, context, measurer, density, cache) }
        val fontSizePx = with(density) { context.fontSize.toPx() }

        // 计算基线对齐参数
        var maxAscent = 0f
        var maxDescent = 0f
        for (layout in childLayouts) {
            val ascent = layout.baseline
            val descent = layout.height - layout.baseline
            if (ascent > maxAscent) maxAscent = ascent
            if (descent > maxDescent) maxDescent = descent
        }

        // 计算 TeX 标准原子间距（与 measureGroup 中的逻辑一致）
        val isScript = context.mathStyle == MathStyle.SCRIPT ||
                context.mathStyle == MathStyle.SCRIPT_SCRIPT
        val spacings = FloatArray(children.size) { 0f }
        for (i in 0 until children.size - 1) {
            val leftNode = children[i]
            val rightNode = children[i + 1]
            if (leftNode is LatexNode.Space || leftNode is LatexNode.HSpace ||
                rightNode is LatexNode.Space || rightNode is LatexNode.HSpace
            ) {
                continue
            }
            val leftType = MathSpacing.classifyNode(leftNode)
            val rightType = MathSpacing.classifyNode(rightNode)
            val spacingFactor = MathSpacing.spaceBetween(leftType, rightType, isScript)
            spacings[i] = spacingFactor * fontSizePx
        }

        // 计算每个子节点的 x 偏移
        val xOffsets = FloatArray(children.size)
        var currentX = 0f
        for (i in children.indices) {
            xOffsets[i] = currentX
            currentX += childLayouts[i].width
            if (i < spacings.size) currentX += spacings[i]
        }

        val result = mutableListOf<Pair<HighlightRect, HighlightRange>>()

        for (range in ranges) {
            when {
                range.nodeIndices != null -> {
                    val startIdx = range.nodeIndices.first.coerceIn(0, children.size - 1)
                    val endIdx = range.nodeIndices.last.coerceIn(0, children.size - 1)
                    val x = xOffsets[startIdx]
                    val endX = xOffsets[endIdx] + childLayouts[endIdx].width
                    val rect = HighlightRect(
                        x = x,
                        y = 0f,
                        width = endX - x,
                        height = groupLayout.height
                    )
                    result.add(rect to range)
                }

                range.pattern != null -> {
                    for (i in children.indices) {
                        if (nodeContainsText(children[i], range.pattern)) {
                            val rect = HighlightRect(
                                x = xOffsets[i],
                                y = maxAscent - childLayouts[i].baseline,
                                width = childLayouts[i].width,
                                height = childLayouts[i].height
                            )
                            result.add(rect to range)
                        }
                    }
                }
            }
        }

        return result
    }

    /**
     * 检查 AST 节点是否包含指定文本
     */
    private fun nodeContainsText(node: LatexNode, pattern: String): Boolean {
        return when (node) {
            is LatexNode.Text -> node.content.contains(pattern)
            is LatexNode.Group -> node.children.any { nodeContainsText(it, pattern) }
            is LatexNode.Symbol -> node.symbol.contains(pattern) || node.unicode.contains(pattern)
            is LatexNode.Operator -> node.op.contains(pattern)
            is LatexNode.TextMode -> node.text.contains(pattern)
            is LatexNode.Superscript -> nodeContainsText(node.base, pattern) || nodeContainsText(
                node.exponent,
                pattern
            )

            is LatexNode.Subscript -> nodeContainsText(
                node.base,
                pattern
            ) || nodeContainsText(node.index, pattern)

            is LatexNode.Fraction -> nodeContainsText(node.numerator, pattern) || nodeContainsText(
                node.denominator,
                pattern
            )

            is LatexNode.Style -> node.content.any { nodeContainsText(it, pattern) }
            is LatexNode.Color -> node.content.any { nodeContainsText(it, pattern) }
            is LatexNode.FontSize -> node.content.any { nodeContainsText(it, pattern) }
            else -> false
        }
    }
}
