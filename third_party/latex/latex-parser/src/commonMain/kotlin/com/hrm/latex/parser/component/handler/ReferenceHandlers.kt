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
import com.hrm.latex.parser.tokenizer.LatexToken

/**
 * 标签、引用与公式编号命令：\label, \ref, \eqref, \tag, \substack
 */
internal fun CommandRegistry.installReferenceHandlers() {
    register("label") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: return@register LatexNode.Text("\\label")
        val key = when (arg) {
            is LatexNode.Text -> arg.content
            is LatexNode.Group -> ParseUtils.extractText(arg.children)
            else -> ""
        }
        LatexNode.Label(key)
    }

    register("ref") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: return@register LatexNode.Text("\\ref")
        val key = when (arg) {
            is LatexNode.Text -> arg.content
            is LatexNode.Group -> ParseUtils.extractText(arg.children)
            else -> ""
        }
        LatexNode.Ref(key)
    }

    register("eqref") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: return@register LatexNode.Text("\\eqref")
        val key = when (arg) {
            is LatexNode.Text -> arg.content
            is LatexNode.Group -> ParseUtils.extractText(arg.children)
            else -> ""
        }
        LatexNode.EqRef(key)
    }

    register("tag") { _, ctx, stream ->
        // 检查 \tag 后面是否跟着 *
        val starred = stream.peek()?.let { it is LatexToken.Text && it.content.startsWith("*") } == true
        if (starred) {
            val textToken = stream.peek() as LatexToken.Text
            stream.advance()
            val remaining = textToken.content.removePrefix("*")
            if (remaining.isNotEmpty()) {
                // 不太可能出现，但安全起见保留
            }
        }
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Tag(arg, starred)
    }

    register("notag", "nonumber") { _, _, _ ->
        LatexNode.Tag(LatexNode.Group(emptyList()), starred = true)
    }

    register("intertext", "shortintertext") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        val text = when (arg) {
            is LatexNode.Group -> ParseUtils.extractText(arg.children)
            else -> ParseUtils.extractText(listOf(arg))
        }
        LatexNode.TextMode(text)
    }

    register("substack") { _, ctx, stream ->
        if (stream.peek() !is LatexToken.LeftBrace) {
            return@register LatexNode.Text("\\substack")
        }
        stream.advance() // 消费 {

        val rows = mutableListOf<List<LatexNode>>()
        var currentRow = mutableListOf<LatexNode>()

        while (!stream.isEOF()) {
            val token = stream.peek()
            when {
                token is LatexToken.RightBrace -> {
                    stream.advance()
                    break
                }
                token is LatexToken.NewLine -> {
                    stream.advance()
                    rows.add(currentRow.toList())
                    currentRow = mutableListOf()
                }
                else -> {
                    val node = ctx.parseExpression()
                    if (node != null) currentRow.add(node)
                }
            }
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        LatexNode.Substack(rows)
    }
}
