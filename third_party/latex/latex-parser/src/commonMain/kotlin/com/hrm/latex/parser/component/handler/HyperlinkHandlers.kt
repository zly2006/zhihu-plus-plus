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
 * 超链接命令：\href, \url
 */
internal fun CommandRegistry.installHyperlinkHandlers() {
    // \href{url}{text}
    register("href") { _, ctx, _ ->
        val urlArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val url = ParseUtils.extractText(
            when (urlArg) {
                is LatexNode.Group -> urlArg.children
                else -> listOf(urlArg)
            }
        )

        val contentArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val content = when (contentArg) {
            is LatexNode.Group -> contentArg.children
            else -> listOf(contentArg)
        }

        LatexNode.Hyperlink(url, content)
    }

    // \url{url} — 显示 URL 文本本身
    register("url") { _, ctx, _ ->
        val urlArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val url = ParseUtils.extractText(
            when (urlArg) {
                is LatexNode.Group -> urlArg.children
                else -> listOf(urlArg)
            }
        )

        // \url 没有显示文本参数，内容为空列表，渲染时显示 URL 本身
        LatexNode.Hyperlink(url, emptyList())
    }
}
