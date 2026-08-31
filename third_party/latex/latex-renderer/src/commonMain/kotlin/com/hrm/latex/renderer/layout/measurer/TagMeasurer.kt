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
import kotlin.reflect.KClass

/**
 * 标签测量器 — 处理 \tag{label}, \tag*{label}
 */
internal class TagMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Tag::class
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        node as LatexNode.Tag
        if (node.starred && node.label.children().isEmpty()) {
            return NodeLayout(0f, 0f, 0f) { _, _ -> }
        }
        val labelLayout = measureNode(node.label, context)
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val gap = fontSizePx * 1.5f

        if (node.starred) {
            val totalWidth = gap + labelLayout.width
            return NodeLayout(totalWidth, labelLayout.height, labelLayout.baseline) { x, y ->
                labelLayout.draw(this, x + gap, y)
            }
        } else {
            // Parentheses must use the same KaTeX glyph measurement as the
            // label. Drawing raw TextLayoutResults from the same top position
            // mixes a full line box with the label's trimmed ink box and makes
            // the parentheses appear lower.
            val leftParen = measureNode(LatexNode.Text("("), context)
            val rightParen = measureNode(LatexNode.Text(")"), context)

            val baseline = maxOf(leftParen.baseline, labelLayout.baseline, rightParen.baseline)
            val depth = maxOf(
                leftParen.height - leftParen.baseline,
                labelLayout.height - labelLayout.baseline,
                rightParen.height - rightParen.baseline
            )
            val totalWidth = gap + leftParen.width + labelLayout.width + rightParen.width
            val height = baseline + depth

            return NodeLayout(totalWidth, height, baseline) { x, y ->
                leftParen.draw(this, x + gap, y + baseline - leftParen.baseline)
                labelLayout.draw(
                    this,
                    x + gap + leftParen.width,
                    y + baseline - labelLayout.baseline
                )
                rightParen.draw(
                    this,
                    x + gap + leftParen.width + labelLayout.width,
                    y + baseline - rightParen.baseline
                )
            }
        }
    }
}
