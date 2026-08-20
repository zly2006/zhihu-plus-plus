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
 * 颜色命令：\color, \textcolor
 */
internal fun CommandRegistry.installColorHandlers() {
    val colorHandler = CommandHandler { _, ctx, _ ->
        val colorArg = ctx.parseArgument() ?: return@CommandHandler LatexNode.Text("")
        val colorName = ParseUtils.extractColorName(colorArg)

        val contentArg = ctx.parseArgument() ?: return@CommandHandler LatexNode.Text("")
        val content = when (contentArg) {
            is LatexNode.Group -> contentArg.children
            else -> listOf(contentArg)
        }

        LatexNode.Color(content, colorName)
    }

    register("color", "textcolor", handler = colorHandler)

    // \colorbox{color}{text}
    register("colorbox") { _, ctx, _ ->
        val colorArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val colorName = ParseUtils.extractColorName(colorArg)

        val contentArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val content = when (contentArg) {
            is LatexNode.Group -> contentArg.children
            else -> listOf(contentArg)
        }

        LatexNode.ColorBox(content, colorName)
    }

    // \fcolorbox{borderColor}{bgColor}{text}
    register("fcolorbox") { _, ctx, _ ->
        val borderColorArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val borderColor = ParseUtils.extractColorName(borderColorArg)

        val bgColorArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val bgColor = ParseUtils.extractColorName(bgColorArg)

        val contentArg = ctx.parseArgument() ?: return@register LatexNode.Text("")
        val content = when (contentArg) {
            is LatexNode.Group -> contentArg.children
            else -> listOf(contentArg)
        }

        LatexNode.ColorBox(content, bgColor, borderColor)
    }
}
