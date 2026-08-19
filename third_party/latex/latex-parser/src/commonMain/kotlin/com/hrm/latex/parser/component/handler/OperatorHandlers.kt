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
 * 数学算子 & 运算符名称命令：\sin, \cos, \operatorname, \mathop, \bmod, \pmod, \mod, \dots
 */
internal fun CommandRegistry.installOperatorHandlers() {
    fun normalizeMathOpArgument(node: LatexNode): LatexNode {
        if (node is LatexNode.Group) {
            val significant = node.children.filter {
                it !is LatexNode.Space && it !is LatexNode.NewLine
            }
            if (significant.size == 1) {
                return normalizeMathOpArgument(significant[0])
            }
        }
        return node
    }

    // \dots 判断用的常量集合 — 提取为文件级避免每次调用 \dots 时重新创建
    val dotsTextChars = setOf('+', '-', '*', '=', '<', '>')
    val dotsCmdNames = setOf(
        "times", "cdot", "pm", "mp", "leq", "geq", "le", "ge",
        "neq", "equiv", "approx", "sim", "rightarrow", "leftarrow",
        "Rightarrow", "Leftarrow", "subset", "supset", "subseteq", "supseteq",
        "in", "oplus", "otimes"
    )
    // 标准数学算子（正体渲染）
    val standardOps = arrayOf(
        "sin", "cos", "tan", "cot", "sec", "csc",
        "arcsin", "arccos", "arctan",
        "sinh", "cosh", "tanh", "coth",
        "ln", "log", "exp", "lg", "sgn", "lcm"
    )

    register(*standardOps) { cmdName, _, _ ->
        LatexNode.Operator(cmdName)
    }

    // \operatorname{Name} / \operatorname*{Name}
    register("operatorname") { _, ctx, stream ->
        val starred = stream.peek().let {
            it is LatexToken.Text && it.content == "*"
        }
        if (starred) stream.advance()

        val arg = ctx.parseArgument() ?: return@register LatexNode.Text("\\operatorname")
        val name = when (arg) {
            is LatexNode.Text -> arg.content
            is LatexNode.Group -> ParseUtils.extractText(arg.children)
            else -> ""
        }
        if (name.isEmpty()) return@register LatexNode.Text("\\operatorname")

        val (sub, sup, limitsMode) = ParseUtils.parseScriptsAndLimits(ctx, stream)

        if (sub != null || sup != null) {
            LatexNode.BigOperator(
                operator = name,
                subscript = sub,
                superscript = sup,
                limitsMode = limitsMode,
                limitsInDisplay = starred
            )
        } else {
            LatexNode.OperatorName(name)
        }
    }

    // \mathop{content}
    register("mathop") { _, ctx, stream ->
        val rawArg = ctx.parseArgument() ?: return@register LatexNode.Text("\\mathop")
        val arg = normalizeMathOpArgument(rawArg)
        when (arg) {
            is LatexNode.BigOperator -> {
                val (sub, sup, limitsMode) = ParseUtils.parseScriptsAndLimits(ctx, stream)
                LatexNode.BigOperator(
                    operator = arg.operator,
                    subscript = sub ?: arg.subscript,
                    superscript = sup ?: arg.superscript,
                    limitsMode = if (limitsMode != LatexNode.BigOperator.LimitsMode.AUTO) {
                        limitsMode
                    } else {
                        arg.limitsMode
                    },
                    limitsInDisplay = arg.limitsInDisplay
                )
            }

            else -> {
                val content = when (arg) {
                    is LatexNode.Text -> arg.content
                    is LatexNode.Group -> ParseUtils.extractText(arg.children)
                    is LatexNode.OperatorName -> arg.name
                    is LatexNode.Operator -> arg.op
                    is LatexNode.Symbol -> arg.unicode
                    is LatexNode.Command -> arg.name
                    is LatexNode.TextMode -> arg.text
                    else -> ""
                }
                if (content.isEmpty()) return@register LatexNode.Text("\\mathop")

                val (sub, sup, limitsMode) = ParseUtils.parseScriptsAndLimits(ctx, stream)
                LatexNode.BigOperator(
                    operator = content,
                    subscript = sub,
                    superscript = sup,
                    limitsMode = limitsMode,
                    limitsInDisplay = true
                )
            }
        }
    }

    // 取模运算符
    register("bmod") { _, _, _ ->
        LatexNode.ModOperator(null, LatexNode.ModOperator.ModStyle.BMOD)
    }
    register("pmod") { _, ctx, _ ->
        val arg = ctx.parseArgument()
        LatexNode.ModOperator(arg, LatexNode.ModOperator.ModStyle.PMOD)
    }
    register("mod") { _, _, _ ->
        LatexNode.ModOperator(null, LatexNode.ModOperator.ModStyle.MOD)
    }

    // 自适应省略号 \dots
    register("dots") { _, _, stream ->
        val next = stream.peekSkipping { it is LatexToken.Whitespace }
        val useCdots = when {
            next is LatexToken.Text && next.content.firstOrNull() in dotsTextChars -> true
            next is LatexToken.Command && next.name in dotsCmdNames -> true
            else -> false
        }
        val unicode = if (useCdots) "⋯" else "…"
        val cmdName = if (useCdots) "cdots" else "ldots"
        LatexNode.Symbol(cmdName, unicode)
    }
}
