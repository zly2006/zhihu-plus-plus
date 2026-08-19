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
 * 装饰命令：\hat, \vec, \overline, \cancel, 等
 */
internal fun CommandRegistry.installAccentHandlers() {
    val accentMapping = mapOf(
        "hat" to LatexNode.Accent.AccentType.HAT,
        "tilde" to LatexNode.Accent.AccentType.TILDE,
        "widetilde" to LatexNode.Accent.AccentType.TILDE,
        "bar" to LatexNode.Accent.AccentType.OVERLINE,
        "overline" to LatexNode.Accent.AccentType.OVERLINE,
        "underline" to LatexNode.Accent.AccentType.UNDERLINE,
        "dot" to LatexNode.Accent.AccentType.DOT,
        "ddot" to LatexNode.Accent.AccentType.DDOT,
        "dddot" to LatexNode.Accent.AccentType.DDDOT,
        "vec" to LatexNode.Accent.AccentType.VEC,
        "overbrace" to LatexNode.Accent.AccentType.OVERBRACE,
        "underbrace" to LatexNode.Accent.AccentType.UNDERBRACE,
        "widehat" to LatexNode.Accent.AccentType.WIDEHAT,
        "overrightarrow" to LatexNode.Accent.AccentType.OVERRIGHTARROW,
        "overleftarrow" to LatexNode.Accent.AccentType.OVERLEFTARROW,
        "cancel" to LatexNode.Accent.AccentType.CANCEL,
        "bcancel" to LatexNode.Accent.AccentType.BCANCEL,
        "xcancel" to LatexNode.Accent.AccentType.XCANCEL,
        "grave" to LatexNode.Accent.AccentType.GRAVE,
        "acute" to LatexNode.Accent.AccentType.ACUTE,
        "check" to LatexNode.Accent.AccentType.CHECK,
        "breve" to LatexNode.Accent.AccentType.BREVE,
        "ring" to LatexNode.Accent.AccentType.RING,
        "mathring" to LatexNode.Accent.AccentType.RING,
        "overbracket" to LatexNode.Accent.AccentType.OVERBRACKET,
        "underbracket" to LatexNode.Accent.AccentType.UNDERBRACKET,
        "widecheck" to LatexNode.Accent.AccentType.WIDECHECK,
        "overleftrightarrow" to LatexNode.Accent.AccentType.OVERLEFTRIGHTARROW,
        "underleftarrow" to LatexNode.Accent.AccentType.UNDERLEFTARROW,
        "underrightarrow" to LatexNode.Accent.AccentType.UNDERRIGHTARROW,
        "overparen" to LatexNode.Accent.AccentType.OVERPAREN,
        "underparen" to LatexNode.Accent.AccentType.UNDERPAREN,
    )

    for ((cmd, accentType) in accentMapping) {
        register(cmd) { _, ctx, _ ->
            val content = ctx.parseArgument() ?: LatexNode.Text("")
            LatexNode.Accent(content, accentType)
        }
    }
}
