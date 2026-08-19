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

package com.hrm.latex.parser.component.handler

import com.hrm.latex.parser.model.LatexNode

/**
 * 高级数学命令：\sideset, \tensor, \indices
 */
internal fun CommandRegistry.installAdvancedHandlers() {
    register("sideset") { _, ctx, stream ->
        val (leftSub, leftSup) = ParseUtils.parseScriptGroup(ctx, stream)
        val (rightSub, rightSup) = ParseUtils.parseScriptGroup(ctx, stream)
        val baseArg = ctx.parseArgument() ?: return@register LatexNode.Text("\\sideset")
        LatexNode.SideSet(leftSub, leftSup, rightSub, rightSup, baseArg)
    }

    register("tensor") { _, ctx, stream ->
        val baseArg = ctx.parseArgument() ?: return@register LatexNode.Text("\\tensor")
        val indices = ParseUtils.parseTensorIndicesGroup(ctx, stream)
        LatexNode.Tensor(baseArg, indices)
    }

    register("indices") { _, ctx, stream ->
        val indices = ParseUtils.parseTensorIndicesGroup(ctx, stream)
        LatexNode.Tensor(LatexNode.Text(""), indices)
    }

    // \prescript{上标}{下标}{基础符号}
    register("prescript") { _, ctx, _ ->
        val supArg = ctx.parseArgument() ?: return@register LatexNode.Text("\\prescript")
        val subArg = ctx.parseArgument() ?: return@register LatexNode.Text("\\prescript")
        val baseArg = ctx.parseArgument() ?: return@register LatexNode.Text("\\prescript")

        // 空参数（{}）视为 null
        val preSup = when {
            supArg is LatexNode.Group && supArg.children.isEmpty() -> null
            supArg is LatexNode.Text && supArg.content.isBlank() -> null
            else -> supArg
        }
        val preSub = when {
            subArg is LatexNode.Group && subArg.children.isEmpty() -> null
            subArg is LatexNode.Text && subArg.content.isBlank() -> null
            else -> subArg
        }

        LatexNode.Prescript(preSup, preSub, baseArg)
    }
}
