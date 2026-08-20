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
 * 文本方向命令：\RLE, \LRE, \textarabic, \texthebrew
 *
 * 支持 RTL（从右到左）和 LTR（从左到右）文本方向切换。
 * - \RLE{...} — 强制从右到左排列
 * - \LRE{...} — 强制从左到右排列（嵌套在 RTL 中使用）
 * - \textarabic{...} — 阿拉伯语文本（RTL 方向）
 * - \texthebrew{...} — 希伯来语文本（RTL 方向）
 */
internal fun CommandRegistry.installTextDirectionHandlers() {
    // \RLE{content} — Right-to-Left Embedding
    register("RLE") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val content = when (arg) {
            is LatexNode.Group -> arg.children
            else -> listOf(arg)
        }
        LatexNode.TextDirection(content, LatexNode.TextDirection.Direction.RTL)
    }

    // \LRE{content} — Left-to-Right Embedding
    register("LRE") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val content = when (arg) {
            is LatexNode.Group -> arg.children
            else -> listOf(arg)
        }
        LatexNode.TextDirection(content, LatexNode.TextDirection.Direction.LTR)
    }

    // \textarabic{content} — 阿拉伯语文本（RTL）
    register("textarabic") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val content = when (arg) {
            is LatexNode.Group -> arg.children
            else -> listOf(arg)
        }
        LatexNode.TextDirection(content, LatexNode.TextDirection.Direction.RTL)
    }

    // \texthebrew{content} — 希伯来语文本（RTL）
    register("texthebrew") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val content = when (arg) {
            is LatexNode.Group -> arg.children
            else -> listOf(arg)
        }
        LatexNode.TextDirection(content, LatexNode.TextDirection.Direction.RTL)
    }
}
