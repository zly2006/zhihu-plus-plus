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

import com.hrm.latex.parser.component.LatexParserContext
import com.hrm.latex.parser.component.LatexTokenStream
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.tokenizer.LatexToken

/** Common, deliberately small subsets of the physics and siunitx packages. */
internal fun CommandRegistry.installPackageCommandHandlers() {
    installPhysicsHandlers()
    installSiunitxHandlers()
}

private fun CommandRegistry.installPhysicsHandlers() {
    register("abs") { _, ctx, _ -> delimited(ctx, "|", "|") }
    register("norm") { _, ctx, _ -> delimited(ctx, "‖", "‖") }
    register("bra") { _, ctx, _ -> delimited(ctx, "⟨", "|") }
    register("ket") { _, ctx, _ -> delimited(ctx, "|", "⟩") }
    register("braket") { _, ctx, _ -> delimited(ctx, "⟨", "⟩") }
    register("Bra") { _, ctx, _ -> delimited(ctx, "⟨", "|") }
    register("Ket") { _, ctx, _ -> delimited(ctx, "|", "⟩") }
    register("Braket") { _, ctx, _ -> delimited(ctx, "⟨", "⟩") }

    register("comm", "anticomm") { command, ctx, _ ->
        val first = ctx.parseArgument() ?: LatexNode.Text("")
        val second = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Delimited(
            if (command == "comm") "[" else "{",
            if (command == "comm") "]" else "}",
            listOf(first, LatexNode.Text(","), LatexNode.Space(LatexNode.Space.SpaceType.THIN), second)
        )
    }

    register("eval") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Delimited("", "|", unwrap(arg))
    }

    register("vb") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Style(unwrap(arg), LatexNode.Style.StyleType.BOLD_SYMBOL)
    }
    register("va") { _, ctx, _ ->
        val arg = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Accent(arg, LatexNode.Accent.AccentType.VEC)
    }

    register("dv", "pdv") { command, ctx, stream ->
        val order = parseOptionalNode(ctx, stream)
        val first = ctx.parseArgument() ?: LatexNode.Text("")
        val second = if (stream.peekSkipping { it is LatexToken.Whitespace } is LatexToken.LeftBrace) {
            ctx.parseArgument()
        } else {
            null
        }
        derivative(
            marker = if (command == "pdv") {
                LatexNode.Symbol("partial", "∂")
            } else {
                LatexNode.Style(listOf(LatexNode.Text("d")), LatexNode.Style.StyleType.ROMAN)
            },
            function = second?.let { first },
            variable = second ?: first,
            order = order
        )
    }
}

private fun CommandRegistry.installSiunitxHandlers() {
    register("num") { _, ctx, _ ->
        formatNumber(ctx.parseArgument() ?: LatexNode.Text(""))
    }

    register("si") { _, ctx, _ ->
        formatUnit(ctx.parseArgument() ?: LatexNode.Text(""))
    }

    register("SI") { _, ctx, _ ->
        val number = formatNumber(ctx.parseArgument() ?: LatexNode.Text(""))
        val unit = formatUnit(ctx.parseArgument() ?: LatexNode.Text(""))
        LatexNode.Group(
            listOf(number, LatexNode.Space(LatexNode.Space.SpaceType.THIN), unit)
        )
    }

    register("unit") { _, ctx, _ ->
        formatUnit(ctx.parseArgument() ?: LatexNode.Text(""))
    }

    // siunitx v3 spellings. \qty intentionally belongs to siunitx here; the
    // physics-package parenthesis alias remains available as \quantity only.
    register("qty") { _, ctx, _ ->
        quantity(ctx)
    }
    register("quantity") { _, ctx, _ -> delimited(ctx, "(", ")") }

    register("numrange") { _, ctx, _ ->
        range(
            formatNumber(ctx.parseArgument() ?: LatexNode.Text("")),
            formatNumber(ctx.parseArgument() ?: LatexNode.Text(""))
        )
    }

    register("qtyrange") { _, ctx, _ ->
        val from = formatNumber(ctx.parseArgument() ?: LatexNode.Text(""))
        val to = formatNumber(ctx.parseArgument() ?: LatexNode.Text(""))
        val unit = formatUnit(ctx.parseArgument() ?: LatexNode.Text(""))
        LatexNode.Group(listOf(range(from, to), LatexNode.Space(LatexNode.Space.SpaceType.THIN), unit))
    }

    register("ang") { _, ctx, _ ->
        val value = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Group(listOf(value, LatexNode.Symbol("degree", "°")))
    }

    register("pu") { _, ctx, _ ->
        formatPhysicalUnit(ctx.parseArgument() ?: LatexNode.Text(""))
    }

    register("bond") { _, ctx, _ ->
        val value = ParseUtils.extractText(listOf(ctx.parseArgument() ?: LatexNode.Text(""))).trim()
        val (name, symbol) = when (value) {
            "-", "1" -> "singlebond" to "−"
            "=", "2" -> "doublebond" to "="
            "#", "3" -> "triplebond" to "≡"
            "~", "1.5" -> "aromaticbond" to "∼"
            else -> "bond" to value
        }
        LatexNode.Symbol(name, symbol)
    }

    val unitNames = mapOf(
        "metre" to "m", "meter" to "m", "second" to "s", "gram" to "g",
        "kilogram" to "kg", "ampere" to "A", "kelvin" to "K", "mole" to "mol",
        "candela" to "cd", "hertz" to "Hz", "newton" to "N", "pascal" to "Pa",
        "joule" to "J", "watt" to "W", "coulomb" to "C", "volt" to "V",
        "farad" to "F", "ohm" to "Ω", "siemens" to "S", "weber" to "Wb",
        "tesla" to "T", "henry" to "H", "lumen" to "lm", "lux" to "lx",
        "becquerel" to "Bq", "gray" to "Gy", "sievert" to "Sv", "katal" to "kat",
        "litre" to "L", "liter" to "L", "degreeCelsius" to "°C"
    )
    unitNames.forEach { (command, value) ->
        register(command) { _, _, _ -> LatexNode.Text(value) }
    }
    val prefixes = mapOf(
        "kilo" to "k", "mega" to "M", "giga" to "G", "centi" to "c",
        "milli" to "m", "micro" to "μ", "nano" to "n"
    )
    prefixes.forEach { (command, value) ->
        register(command) { _, _, _ -> LatexNode.Text(value) }
    }
    register("per") { _, _, _ -> LatexNode.Text("/") }
    register("squared") { _, _, _ -> LatexNode.Text("²") }
    register("cubed") { _, _, _ -> LatexNode.Text("³") }
}

private fun quantity(ctx: LatexParserContext): LatexNode {
    val number = formatNumber(ctx.parseArgument() ?: LatexNode.Text(""))
    val unit = formatUnit(ctx.parseArgument() ?: LatexNode.Text(""))
    return LatexNode.Group(listOf(number, LatexNode.Space(LatexNode.Space.SpaceType.THIN), unit))
}

private fun range(from: LatexNode, to: LatexNode): LatexNode = LatexNode.Group(
    listOf(from, LatexNode.Text("–"), to)
)

private fun delimited(ctx: LatexParserContext, left: String, right: String): LatexNode {
    val arg = ctx.parseArgument() ?: LatexNode.Text("")
    return LatexNode.Delimited(left, right, unwrap(arg))
}

private fun derivative(
    marker: LatexNode,
    function: LatexNode?,
    variable: LatexNode,
    order: LatexNode?
): LatexNode.Fraction {
    val numeratorMarker = order?.let { LatexNode.Superscript(marker, it) } ?: marker
    val denominatorVariable = order?.let { LatexNode.Superscript(variable, it) } ?: variable
    val numerator = buildList {
        add(numeratorMarker)
        function?.let(::add)
    }
    return LatexNode.Fraction(
        numerator = LatexNode.Group(numerator),
        denominator = LatexNode.Group(listOf(marker, denominatorVariable))
    )
}

private fun formatNumber(arg: LatexNode): LatexNode {
    val text = ParseUtils.extractText(unwrap(arg)).trim()
    val scientific = SCIENTIFIC_NUMBER.matchEntire(text) ?: return arg
    val mantissa = scientific.groupValues[1]
    val exponent = scientific.groupValues[2].removePrefix("+")
    return LatexNode.Group(
        listOf(
            LatexNode.Text(mantissa),
            LatexNode.Space(LatexNode.Space.SpaceType.THIN),
            LatexNode.Symbol("times", "×"),
            LatexNode.Space(LatexNode.Space.SpaceType.THIN),
            LatexNode.Superscript(LatexNode.Text("10"), LatexNode.Text(exponent))
        )
    )
}

private fun formatUnit(arg: LatexNode): LatexNode {
    val content = unwrap(arg).map { node ->
        node.mapNodes { child ->
            if (child is LatexNode.Text && '.' in child.content) {
                child.copy(content = replaceUnitSeparators(child.content))
            } else {
                child
            }
        }
    }
    return LatexNode.Style(content, LatexNode.Style.StyleType.ROMAN)
}

private fun formatPhysicalUnit(arg: LatexNode): LatexNode {
    val nodes = unwrap(arg)
    val separatorIndex = nodes.indexOfFirst { it is LatexNode.Space }
    val numberNodes = if (separatorIndex >= 0) nodes.take(separatorIndex) else nodes
    val numberText = ParseUtils.extractText(numberNodes).trim()
    val numberMatch = PHYSICAL_QUANTITY_PREFIX.find(numberText)
        ?: return formatUnit(arg)

    val number = formatNumber(LatexNode.Text(numberMatch.value))
    val unitNodes = buildList {
        val suffix = numberText.removeRange(numberMatch.range)
        if (suffix.isNotEmpty()) add(LatexNode.Text(suffix))
        if (separatorIndex >= 0) {
            addAll(nodes.drop(separatorIndex + 1).dropWhile { it is LatexNode.Space })
        }
    }
    if (unitNodes.isEmpty()) return number

    return LatexNode.Group(
        listOf(
            number,
            LatexNode.Space(LatexNode.Space.SpaceType.THIN),
            formatUnit(LatexNode.Group(unitNodes))
        )
    )
}

private fun replaceUnitSeparators(text: String): String = buildString(text.length) {
    text.forEachIndexed { index, char ->
        val isDecimalPoint = char == '.' &&
            text.getOrNull(index - 1)?.isDigit() == true &&
            text.getOrNull(index + 1)?.isDigit() == true
        append(if (char == '.' && !isDecimalPoint) '·' else char)
    }
}

private fun parseOptionalNode(ctx: LatexParserContext, stream: LatexTokenStream): LatexNode? {
    if (stream.peek() !is LatexToken.LeftBracket) return null
    stream.advance()
    val nodes = ParseUtils.parseUntil(ctx, stream) { it is LatexToken.RightBracket }
    if (stream.peek() is LatexToken.RightBracket) stream.advance()
    return nodes.takeIf { it.isNotEmpty() }?.let(LatexNode::Group)
}

private fun unwrap(node: LatexNode): List<LatexNode> =
    if (node is LatexNode.Group) node.children else listOf(node)

private val SCIENTIFIC_NUMBER =
    Regex("""([+-]?(?:\d+(?:\.\d*)?|\.\d+))[eE]([+-]?\d+)""")

private val PHYSICAL_QUANTITY_PREFIX =
    Regex("""^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?""")
