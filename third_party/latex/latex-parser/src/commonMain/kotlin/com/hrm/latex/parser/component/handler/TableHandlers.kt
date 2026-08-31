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
 * 表格相关命令：\hline, \cline, \multicolumn
 */
internal fun CommandRegistry.installTableHandlers() {
    register("hline") { _, _, _ -> LatexNode.HLine() }

    register("cline") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: return@register LatexNode.Text("\\cline")
        val rangeStr = when (arg) {
            is LatexNode.Text -> arg.content
            is LatexNode.Group -> ParseUtils.extractText(arg.children)
            else -> ""
        }
        val parts = rangeStr.split("-")
        val startCol = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 1
        val endCol = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: startCol
        LatexNode.CLine(startCol, endCol)
    }

    register("multicolumn") { _, ctx, _ ->
        // 第一个参数：列数
        val numArg = ctx.parseArgument() ?: return@register LatexNode.Text("\\multicolumn")
        val numStr = when (numArg) {
            is LatexNode.Text -> numArg.content
            is LatexNode.Group -> ParseUtils.extractText(numArg.children)
            else -> "1"
        }
        val columnCount = numStr.trim().toIntOrNull() ?: 1

        // 第二个参数：对齐方式
        val alignArg = ctx.parseArgument() ?: return@register LatexNode.Text("\\multicolumn")
        val alignment = when (alignArg) {
            is LatexNode.Text -> alignArg.content
            is LatexNode.Group -> ParseUtils.extractText(alignArg.children)
            else -> "c"
        }

        // 第三个参数：内容
        val contentArg = ctx.parseArgument() ?: return@register LatexNode.Text("\\multicolumn")
        val content = when (contentArg) {
            is LatexNode.Group -> contentArg.children
            else -> listOf(contentArg)
        }

        LatexNode.Multicolumn(columnCount, alignment, content)
    }
}
