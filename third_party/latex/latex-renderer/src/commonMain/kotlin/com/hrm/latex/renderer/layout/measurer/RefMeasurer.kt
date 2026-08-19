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
 * 引用测量器 — 处理 \ref{key}, \eqref{key}
 */
internal class RefMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Ref::class,
        LatexNode.EqRef::class
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout = when (node) {
        is LatexNode.Ref -> measureRef(node, context, measureNode)
        is LatexNode.EqRef -> measureEqRef(node, context, measureNode)
        else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
    }

    private fun measureRef(
        node: LatexNode.Ref,
        context: RenderContext,
        measureNode: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        // 优先从编号映射中查找实际编号，找不到则降级为键名
        val displayText = context.equationNumbering?.resolveLabel(node.key) ?: node.key
        return measureNode(LatexNode.Text(displayText), context)
    }

    private fun measureEqRef(
        node: LatexNode.EqRef,
        context: RenderContext,
        measureNode: (LatexNode, RenderContext) -> NodeLayout
    ): NodeLayout {
        // 优先从编号映射中查找实际编号，找不到则降级为键名
        val resolvedNumber = context.equationNumbering?.resolveLabel(node.key) ?: node.key
        return measureNode(LatexNode.Text("($resolvedNumber)"), context)
    }
}
