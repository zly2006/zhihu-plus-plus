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

private val bboxDimensionPattern =
    Regex("""(-?\d+(?:\.\d+)?)(px|pt|em|ex|mm|cm|in)""", RegexOption.IGNORE_CASE)
private val bboxColorPattern = Regex("""[A-Za-z]+|#[0-9A-Fa-f]{3,8}""")
private val bboxBorderPattern =
    Regex(
        """(-?\d+(?:\.\d+)?(?:px|pt|em|ex|mm|cm|in))\s+""" +
            """(solid|dashed|dotted)\s+([A-Za-z]+|#[0-9A-Fa-f]{3,8})""",
        RegexOption.IGNORE_CASE
    )

/**
 * 特殊效果命令：\boxed, \enclose, \phantom, \smash, \vphantom, \hphantom, \not
 */
internal fun CommandRegistry.installSpecialEffectHandlers() {
    register("mathstrut") { _, _, _ ->
        // TeX's \mathstrut has the height/depth of "(" but no horizontal width.
        LatexNode.VPhantom(listOf(LatexNode.Text("(")))
    }

    register("circled") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Enclose(
            content = unwrapContent(arg),
            notations = listOf(LatexNode.Enclose.Notation.CIRCLE)
        )
    }

    register("boxed", "Aboxed") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        val content = unwrapContent(arg)
        LatexNode.Boxed(content)
    }

    register("enclose") { _, ctx, stream ->
        val notationArg = ctx.parseArgument() ?: LatexNode.Text("")
        val notationText = ParseUtils.extractColorName(notationArg)
        val notations = parseEncloseNotations(notationText)

        val attributes = parseAttributeList(parseOptionalBracketText(stream))

        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Enclose(
            content = unwrapContent(arg),
            notations = notations,
            attributes = attributes
        )
    }

    // MathJax \bbox[padding,border]{content}, used by Zhihu's equation service.
    register("bbox") { _, ctx, stream ->
        val (notations, attributes) = parseBBoxOptions(parseOptionalBracketText(stream))
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Enclose(
            content = unwrapContent(arg),
            notations = notations,
            attributes = attributes
        )
    }

    register("phantom") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Phantom(unwrapContent(arg))
    }

    register("smash") { _, ctx, stream ->
        val smashType = if (stream.peek() is LatexToken.LeftBracket) {
            stream.advance() // consume [
            val typeStr = StringBuilder()
            while (!stream.isEOF() && stream.peek() !is LatexToken.RightBracket) {
                val t = stream.peek()
                if (t is LatexToken.Text) {
                    typeStr.append(t.content)
                }
                stream.advance()
            }
            if (stream.peek() is LatexToken.RightBracket) {
                stream.advance() // consume ]
            }
            when (typeStr.toString().trim()) {
                "t" -> LatexNode.Smash.SmashType.TOP
                "b" -> LatexNode.Smash.SmashType.BOTTOM
                else -> LatexNode.Smash.SmashType.BOTH
            }
        } else {
            LatexNode.Smash.SmashType.BOTH
        }

        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Smash(unwrapContent(arg), smashType)
    }

    register("vphantom") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.VPhantom(unwrapContent(arg))
    }

    register("hphantom") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.HPhantom(unwrapContent(arg))
    }

    // 否定修饰 \not
    register("not") { _, ctx, stream ->
        val next = if (!stream.isEOF()) {
            ctx.parseFactor() ?: LatexNode.Text("")
        } else {
            LatexNode.Text("")
        }
        LatexNode.Negation(next)
    }

    // \fbox — 文本模式方框，行为同 \boxed 但使用 FBOX 样式
    register("fbox") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Boxed(unwrapContent(arg), LatexNode.Boxed.BoxStyle.FBOX)
    }

    // \mathclap — 零宽居中叠加
    register("mathclap", "clap") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.MathLap(unwrapContent(arg), LatexNode.MathLap.LapType.CLAP)
    }

    // \mathllap — 零宽左叠加（内容向左扩展）
    register("mathllap", "llap") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.MathLap(unwrapContent(arg), LatexNode.MathLap.LapType.LLAP)
    }

    // \mathrlap — 零宽右叠加（内容向右扩展）
    register("mathrlap", "rlap") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.MathLap(unwrapContent(arg), LatexNode.MathLap.LapType.RLAP)
    }

    register("raisebox") { _, ctx, stream ->
        val shift = ParseUtils.extractText(
            listOf(ctx.parseArgument() ?: LatexNode.Text("0pt"))
        ).trim()
        repeat(2) {
            if (stream.peek() is LatexToken.LeftBracket) {
                stream.advance()
                ParseUtils.parseUntil(ctx, stream) { token -> token is LatexToken.RightBracket }
                if (stream.peek() is LatexToken.RightBracket) stream.advance()
            }
        }
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Layout(
            layoutType = LatexNode.Layout.LayoutType.RAISE_BOX,
            content = unwrapContent(arg),
            shift = shift
        )
    }

    register("rule") { _, ctx, stream ->
        val raise = if (stream.peek() is LatexToken.LeftBracket) {
            stream.advance()
            val nodes = ParseUtils.parseUntil(ctx, stream) { token -> token is LatexToken.RightBracket }
            if (stream.peek() is LatexToken.RightBracket) stream.advance()
            ParseUtils.extractText(nodes).trim()
        } else "0pt"
        val width = ParseUtils.extractText(
            listOf(ctx.parseArgument() ?: LatexNode.Text("0pt"))
        ).trim()
        val height = ParseUtils.extractText(
            listOf(ctx.parseArgument() ?: LatexNode.Text("0pt"))
        ).trim()
        LatexNode.Layout(
            layoutType = LatexNode.Layout.LayoutType.RULE,
            width = width,
            height = height,
            shift = raise
        )
    }

    // mathtools alignment helper: default shift is two ems to the left.
    register("MoveEqLeft") { _, ctx, stream ->
        val amount = if (stream.peek() is LatexToken.LeftBracket) {
            stream.advance()
            val nodes = ParseUtils.parseUntil(ctx, stream) { it is LatexToken.RightBracket }
            if (stream.peek() is LatexToken.RightBracket) stream.advance()
            ParseUtils.extractText(nodes).trim().toFloatOrNull() ?: 2f
        } else 2f
        LatexNode.HSpace("${-amount}em")
    }
}

private fun unwrapContent(arg: LatexNode): List<LatexNode> {
    return when (arg) {
        is LatexNode.Group -> arg.children
        else -> listOf(arg)
    }
}

private fun parseOptionalBracketText(stream: com.hrm.latex.parser.component.LatexTokenStream): String? {
    if (stream.peek() !is LatexToken.LeftBracket) return null
    stream.advance()
    val builder = StringBuilder()
    while (!stream.isEOF() && stream.peek() !is LatexToken.RightBracket) {
        val token = stream.peek()
        when (token) {
            is LatexToken.Text -> builder.append(token.content)
            is LatexToken.Command -> builder.append("\\").append(token.name)
            is LatexToken.Whitespace -> builder.append(token.content)
            is LatexToken.LeftBrace -> builder.append("{")
            is LatexToken.RightBrace -> builder.append("}")
            is LatexToken.LeftBracket -> builder.append("[")
            is LatexToken.RightBracket -> builder.append("]")
            is LatexToken.Superscript -> builder.append("^")
            is LatexToken.Prime -> builder.append("'")
            is LatexToken.Subscript -> builder.append("_")
            is LatexToken.Ampersand -> builder.append("&")
            is LatexToken.NewLine -> builder.append("\\\\")
            is LatexToken.MathShift -> builder.append("$".repeat(token.count))
            else -> Unit
        }
        stream.advance()
    }
    if (stream.peek() is LatexToken.RightBracket) {
        stream.advance()
    }
    return builder.toString().trim().ifEmpty { null }
}

private fun parseEncloseNotations(text: String): List<LatexNode.Enclose.Notation> {
    return text.split(',', ' ')
        .mapNotNull { LatexNode.Enclose.Notation.fromMathMlName(it) }
        .distinct()
}

private fun parseAttributeList(raw: String?): Map<String, String> {
    if (raw.isNullOrBlank()) return emptyMap()
    val regex = Regex("""([A-Za-z][A-Za-z0-9_-]*)\s*=\s*"([^"]*)"""")
    return regex.findAll(raw).associate { match ->
        match.groupValues[1] to match.groupValues[2]
    }
}

private fun parseBBoxOptions(raw: String?): Pair<List<LatexNode.Enclose.Notation>, Map<String, String>> {
    if (raw.isNullOrBlank()) {
        return emptyList<LatexNode.Enclose.Notation>() to
            mapOf(LatexNode.Enclose.BBOX_ATTRIBUTE to "true")
    }

    val attributes = mutableMapOf(LatexNode.Enclose.BBOX_ATTRIBUTE to "true")
    var hasBorder = false
    raw.split(',').map(String::trim).filter(String::isNotEmpty).forEach { option ->
        when {
            option.startsWith("border:", ignoreCase = true) -> {
                val border = option.substringAfter(':').trim()
                val borderMatch = bboxBorderPattern.matchEntire(border)
                when {
                    borderMatch != null -> {
                        attributes["border"] = borderMatch.value
                        attributes["mathcolor"] = borderMatch.groupValues[3]
                        hasBorder = true
                    }

                    bboxDimensionPattern.matches(border) -> {
                        attributes["border"] = "$border solid currentColor"
                        hasBorder = true
                    }
                }
            }

            bboxDimensionPattern.matches(option) -> attributes["padding"] = option

            bboxColorPattern.matches(option) -> attributes["mathbackground"] = option
        }
    }
    val notations = if (hasBorder) listOf(LatexNode.Enclose.Notation.BOX) else emptyList()
    return notations to attributes
}
