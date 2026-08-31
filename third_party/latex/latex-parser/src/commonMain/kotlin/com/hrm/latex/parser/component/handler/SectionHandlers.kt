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
 * 章节结构命令：\section, \subsection, \subsubsection, \paragraph, \subparagraph
 *
 * 支持星号变体（\section*{...}）：tokenizer 将 \section 和 * 分为两个 token，
 * 因此在 handler 内部窥探下一个 token 检测是否为星号。
 */
internal fun CommandRegistry.installSectionHandlers() {
    val sectionLevels = mapOf(
        "section" to LatexNode.SectionHeading.HeadingLevel.SECTION,
        "subsection" to LatexNode.SectionHeading.HeadingLevel.SUBSECTION,
        "subsubsection" to LatexNode.SectionHeading.HeadingLevel.SUBSUBSECTION,
        "paragraph" to LatexNode.SectionHeading.HeadingLevel.PARAGRAPH,
        "subparagraph" to LatexNode.SectionHeading.HeadingLevel.SUBPARAGRAPH,
    )

    for ((cmdName, level) in sectionLevels) {
        register(cmdName) { _, ctx, stream ->
            // 检查后续是否为星号 *
            val starred = isNextStar(stream)
            if (starred) {
                stream.advance() // 消耗 * token
            }

            // 解析标题内容 {title}
            val arg = ctx.parseArgument() ?: return@register LatexNode.Text("\\$cmdName")
            val content = when (arg) {
                is LatexNode.Group -> arg.children
                else -> listOf(arg)
            }

            LatexNode.SectionHeading(content, level, starred)
        }
    }
}

/**
 * 检查下一个 token 是否为星号 "*"
 */
private fun isNextStar(stream: com.hrm.latex.parser.component.LatexTokenStream): Boolean {
    val next = stream.peek() ?: return false
    return next is LatexToken.Text && next.content == "*"
}
