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
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.shrink
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * 四角标注测量器 — 处理 \sideset, \tensor, \prescript
 */
internal class SideSetTensorMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.SideSet::class,
        LatexNode.Tensor::class,
        LatexNode.Prescript::class
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout = when (node) {
        is LatexNode.SideSet -> measureSideSet(node, context, density, measureNode)
        is LatexNode.Tensor -> measureTensor(node, context, density, measureNode)
        is LatexNode.Prescript -> measurePrescript(node, context, density, measureNode)
        else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
    }

    private fun measureSideSet(
        node: LatexNode.SideSet,
        context: RenderContext,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val baseLayout = measureNode(node.base, context)
        val scriptContext = context.shrink(MathConstants.SCRIPT_SCALE)

        val leftSubLayout = node.leftSub?.let { measureNode(it, scriptContext) }
        val leftSupLayout = node.leftSup?.let { measureNode(it, scriptContext) }
        val rightSubLayout = node.rightSub?.let { measureNode(it, scriptContext) }
        val rightSupLayout = node.rightSup?.let { measureNode(it, scriptContext) }

        val leftWidth = max(leftSubLayout?.width ?: 0f, leftSupLayout?.width ?: 0f)
        val rightWidth = max(rightSubLayout?.width ?: 0f, rightSupLayout?.width ?: 0f)

        val fontSizePx = with(density) { context.fontSize.toPx() }
        val scriptGap = fontSizePx * 0.05f

        val totalWidth = leftWidth + scriptGap + baseLayout.width + scriptGap + rightWidth

        // 优先使用 MathFontProvider 的精确参数，回退到 MathConstants
        val provider = context.mathFontProvider
        val supShift = provider?.superscriptShiftUp(fontSizePx)
            ?: (fontSizePx * MathConstants.SUPERSCRIPT_SHIFT)
        val subShift = provider?.subscriptShiftDown(fontSizePx)
            ?: (fontSizePx * MathConstants.SUBSCRIPT_SHIFT)

        val topExtent = max(
            leftSupLayout?.let { supShift + it.height } ?: 0f,
            rightSupLayout?.let { supShift + it.height } ?: 0f
        )
        val aboveBase = max(baseLayout.baseline, topExtent)

        val belowBase = max(
            baseLayout.height - baseLayout.baseline,
            max(
                leftSubLayout?.let { subShift + it.height } ?: 0f,
                rightSubLayout?.let { subShift + it.height } ?: 0f
            )
        )

        val totalHeight = aboveBase + belowBase
        val baseline = aboveBase

        val baseRelX = leftWidth + scriptGap
        val baseRelY = aboveBase - baseLayout.baseline

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            baseLayout.draw(this, x + baseRelX, y + baseRelY)

            leftSupLayout?.let {
                val lsX = x + leftWidth - it.width
                val lsY = y + aboveBase - supShift - it.height
                it.draw(this, lsX, lsY)
            }
            leftSubLayout?.let {
                val lsX = x + leftWidth - it.width
                val lsY = y + aboveBase + subShift
                it.draw(this, lsX, lsY)
            }
            rightSupLayout?.let {
                val rsX = x + baseRelX + baseLayout.width + scriptGap
                val rsY = y + aboveBase - supShift - it.height
                it.draw(this, rsX, rsY)
            }
            rightSubLayout?.let {
                val rsX = x + baseRelX + baseLayout.width + scriptGap
                val rsY = y + aboveBase + subShift
                it.draw(this, rsX, rsY)
            }
        }
    }

    private fun measureTensor(
        node: LatexNode.Tensor,
        context: RenderContext,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val baseLayout = measureNode(node.base, context)
        val scriptStyle = context.shrink(MathConstants.SCRIPT_SCALE)

        if (node.indices.isEmpty()) {
            return baseLayout
        }

        val fontSizePx = with(density) { context.fontSize.toPx() }
        // 优先使用 MathFontProvider 的精确参数，回退到 MathConstants
        val provider = context.mathFontProvider
        val superscriptShift = provider?.superscriptShiftUp(fontSizePx)
            ?: (fontSizePx * MathConstants.SUPERSCRIPT_SHIFT)
        val subscriptShift = provider?.subscriptShiftDown(fontSizePx)
            ?: (fontSizePx * MathConstants.SUBSCRIPT_SHIFT)
        val scriptKern = with(density) { MathConstants.SCRIPT_KERN_DP.dp.toPx() }

        data class IndexColumn(
            var supLayout: NodeLayout? = null,
            var subLayout: NodeLayout? = null
        )

        val columns = mutableListOf<IndexColumn>()
        var i = 0
        val indexLayouts = node.indices.map { (isUpper, indexNode) ->
            Pair(isUpper, measureNode(indexNode, scriptStyle))
        }

        while (i < indexLayouts.size) {
            val (isUpper, layout) = indexLayouts[i]
            val col = IndexColumn()
            if (isUpper) {
                col.supLayout = layout
                if (i + 1 < indexLayouts.size && !indexLayouts[i + 1].first) {
                    col.subLayout = indexLayouts[i + 1].second
                    i += 2
                } else {
                    i++
                }
            } else {
                col.subLayout = layout
                if (i + 1 < indexLayouts.size && indexLayouts[i + 1].first) {
                    col.supLayout = indexLayouts[i + 1].second
                    i += 2
                } else {
                    i++
                }
            }
            columns.add(col)
        }

        val superRelY = -superscriptShift
        val subRelY = subscriptShift

        val baseTopRel = -baseLayout.baseline
        val baseBottomRel = baseLayout.height - baseLayout.baseline

        var minTopRel = baseTopRel
        var maxBottomRel = baseBottomRel

        for (col in columns) {
            col.supLayout?.let { sup ->
                minTopRel = min(minTopRel, superRelY - sup.baseline)
                maxBottomRel = max(maxBottomRel, superRelY + (sup.height - sup.baseline))
            }
            col.subLayout?.let { sub ->
                minTopRel = min(minTopRel, subRelY - sub.baseline)
                maxBottomRel = max(maxBottomRel, subRelY + (sub.height - sub.baseline))
            }
        }

        val totalHeight = maxBottomRel - minTopRel
        val baseline = -minTopRel

        val colWidths = columns.map { col ->
            max(col.supLayout?.width ?: 0f, col.subLayout?.width ?: 0f)
        }

        val scriptX = baseLayout.width + scriptKern
        val colRelXList = mutableListOf<Float>()
        var cx = scriptX
        for (idx in columns.indices) {
            colRelXList.add(cx)
            cx += colWidths[idx]
        }

        val totalWidth = cx

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            baseLayout.draw(this, x, y + baseline - baseLayout.baseline)

            for (idx in columns.indices) {
                val col = columns[idx]
                val colX = x + colRelXList[idx]

                col.supLayout?.let { sup ->
                    sup.draw(this, colX, y + baseline + superRelY - sup.baseline)
                }
                col.subLayout?.let { sub ->
                    sub.draw(this, colX, y + baseline + subRelY - sub.baseline)
                }
            }
        }
    }

    /**
     * 测量 \prescript{上标}{下标}{base} — 前置上下标
     * 类似 SideSet 但只有左侧标注
     */
    private fun measurePrescript(
        node: LatexNode.Prescript,
        context: RenderContext,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        val baseLayout = measureNode(node.base, context)
        val scriptContext = context.shrink(MathConstants.SCRIPT_SCALE)

        val preSupLayout = node.preSuperscript?.let { measureNode(it, scriptContext) }
        val preSubLayout = node.preSubscript?.let { measureNode(it, scriptContext) }

        val preWidth = max(preSupLayout?.width ?: 0f, preSubLayout?.width ?: 0f)

        val fontSizePx = with(density) { context.fontSize.toPx() }
        val scriptGap = fontSizePx * 0.05f

        val totalWidth = preWidth + scriptGap + baseLayout.width

        // 使用 MathFontProvider 的精确参数，回退到 MathConstants
        val provider = context.mathFontProvider
        val supShift = provider?.superscriptShiftUp(fontSizePx)
            ?: (fontSizePx * MathConstants.SUPERSCRIPT_SHIFT)
        val subShift = provider?.subscriptShiftDown(fontSizePx)
            ?: (fontSizePx * MathConstants.SUBSCRIPT_SHIFT)

        val topExtent = preSupLayout?.let { supShift + it.height } ?: 0f
        val aboveBase = max(baseLayout.baseline, topExtent)

        val belowBase = max(
            baseLayout.height - baseLayout.baseline,
            preSubLayout?.let { subShift + it.height } ?: 0f
        )

        val totalHeight = aboveBase + belowBase
        val baseline = aboveBase

        val baseRelX = preWidth + scriptGap
        val baseRelY = aboveBase - baseLayout.baseline

        return NodeLayout(totalWidth, totalHeight, baseline) { x, y ->
            baseLayout.draw(this, x + baseRelX, y + baseRelY)

            preSupLayout?.let {
                val lsX = x + preWidth - it.width
                val lsY = y + aboveBase - supShift - it.height
                it.draw(this, lsX, lsY)
            }
            preSubLayout?.let {
                val lsX = x + preWidth - it.width
                val lsY = y + aboveBase + subShift
                it.draw(this, lsX, lsY)
            }
        }
    }
}
