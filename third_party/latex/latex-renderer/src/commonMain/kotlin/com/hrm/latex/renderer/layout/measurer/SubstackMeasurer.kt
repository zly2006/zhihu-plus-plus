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
import kotlin.reflect.KClass

/**
 * Substack 测量器 — 处理 \substack{line1 \\\\ line2}
 */
internal class SubstackMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Substack::class
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        node as LatexNode.Substack
        if (node.rows.isEmpty()) {
            return NodeLayout(0f, 0f, 0f) { _, _ -> }
        }

        val substackContext = context.shrink(MathConstants.SCRIPT_SCALE)
        val fontSizePx = with(density) { substackContext.fontSize.toPx() }
        val rowSpacing = fontSizePx * 0.15f

        val rowLayouts = node.rows.map { measureGroup(it, substackContext) }
        val maxWidth = rowLayouts.maxOf { it.width }

        var totalHeight = 0f
        val positions = rowLayouts.map { layout ->
            val y = totalHeight
            totalHeight += layout.height + rowSpacing
            y
        }
        if (positions.isNotEmpty()) totalHeight -= rowSpacing

        val baseline = totalHeight / 2f

        return NodeLayout(maxWidth, totalHeight, baseline) { x, y ->
            rowLayouts.forEachIndexed { i, layout ->
                val rowX = x + (maxWidth - layout.width) / 2f
                layout.draw(this, rowX, y + positions[i])
            }
        }
    }
}
