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
 * 空格命令：\,  \:  \;  \quad  \qquad  \!  \hspace 及常用别名
 */
internal fun CommandRegistry.installSpaceHandlers() {
    register(" ") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.NORMAL) }
    register("space") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.NORMAL) }
    register(",") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.THIN) }
    register("thinspace") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.THIN) }
    register(":") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.MEDIUM) }
    register(">") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.MEDIUM) }
    register("medspace") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.MEDIUM) }
    register(";") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.THICK) }
    register("thickspace") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.THICK) }
    register("quad") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.QUAD) }
    register("qquad") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.QQUAD) }
    register("!") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.NEGATIVE_THIN) }
    register("negthinspace") { _, _, _ -> LatexNode.Space(LatexNode.Space.SpaceType.NEGATIVE_THIN) }
    register("enspace") { _, _, _ -> LatexNode.HSpace("0.5em") }
    register("enskip") { _, _, _ -> LatexNode.HSpace("0.5em") }
    register("negmedspace") { _, _, _ -> LatexNode.HSpace("-0.222em") }
    register("negthickspace") { _, _, _ -> LatexNode.HSpace("-0.277em") }

    register("hspace") { _, ctx, _ ->
        val dimension = when (val arg = ctx.parseArgument()) {
            is LatexNode.Text -> arg.content
            is LatexNode.Group -> ParseUtils.extractText(arg.children)
            else -> "0pt"
        }
        LatexNode.HSpace(dimension)
    }

    register("kern", "mkern") { _, ctx, stream ->
        LatexNode.HSpace(ParseUtils.parseDimension(ctx, stream))
    }
    register("allowbreak") { _, _, _ -> LatexNode.HSpace("0pt") }
    register("cr") { _, _, _ -> LatexNode.NewLine() }
}
