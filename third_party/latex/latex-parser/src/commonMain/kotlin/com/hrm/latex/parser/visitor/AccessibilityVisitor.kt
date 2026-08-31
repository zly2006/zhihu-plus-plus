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

package com.hrm.latex.parser.visitor

import com.hrm.latex.parser.SymbolMap
import com.hrm.latex.parser.model.LatexNode

/**
 * 可访问性访问者：将 LaTeX AST 转换为屏幕阅读器友好的文本描述
 *
 * 遵循 MathSpeak 风格的读法规则，为视觉障碍用户提供数学公式的自然语言描述。
 *
 * 用法示例：
 * ```kotlin
 * val description = AccessibilityVisitor.describe(document)
 * // "fraction: 1 over 2" for \frac{1}{2}
 * ```
 */
class AccessibilityVisitor : BaseLatexVisitor<String>() {

    companion object {
        /** 预编译的空白折叠正则 — 避免每次 collapseSpaces() 调用时重新编译 */
        private val WHITESPACE_REGEX = Regex("\\s+")

        /**
         * 将 LaTeX AST 转换为可访问性文本描述
         */
        fun describe(node: LatexNode): String {
            return AccessibilityVisitor().visit(node).trim()
        }

        /**
         * 将 LaTeX 文档转换为可访问性文本描述
         */
        fun describe(document: LatexNode.Document): String {
            return AccessibilityVisitor().visitDocument(document).trim()
        }
    }

    override fun defaultVisit(node: LatexNode): String = ""

    override fun visitDocument(node: LatexNode.Document): String {
        return node.children.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitText(node: LatexNode.Text): String = node.content

    override fun visitGroup(node: LatexNode.Group): String {
        return node.children.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitSuperscript(node: LatexNode.Superscript): String {
        val base = visit(node.base)
        val exp = visit(node.exponent)
        return if (exp == "2") "$base squared"
        else if (exp == "3") "$base cubed"
        else "$base to the power of $exp"
    }

    override fun visitSubscript(node: LatexNode.Subscript): String {
        val base = visit(node.base)
        val idx = visit(node.index)
        return "$base sub $idx"
    }

    override fun visitFraction(node: LatexNode.Fraction): String {
        val num = visit(node.numerator)
        val den = visit(node.denominator)
        val prefix = when (node.style) {
            LatexNode.Fraction.FractionStyle.DISPLAY -> "display fraction"
            LatexNode.Fraction.FractionStyle.TEXT -> "text fraction"
            LatexNode.Fraction.FractionStyle.CONTINUED -> "continued fraction"
            LatexNode.Fraction.FractionStyle.RULELESS -> "stacked expression"
            LatexNode.Fraction.FractionStyle.NORMAL -> "fraction"
        }
        return "$prefix: $num over $den"
    }

    override fun visitRoot(node: LatexNode.Root): String {
        val content = visit(node.content)
        val index = node.index?.let { visit(it) }
        return if (index != null) "$index-th root of $content"
        else "square root of $content"
    }

    override fun visitMatrix(node: LatexNode.Matrix): String {
        val rows = node.rows.size
        val cols = node.rows.firstOrNull()?.size ?: 0
        val bracket = when (node.type) {
            LatexNode.Matrix.MatrixType.PAREN -> "parenthesized "
            LatexNode.Matrix.MatrixType.BRACKET -> "bracketed "
            LatexNode.Matrix.MatrixType.BRACE -> "braced "
            LatexNode.Matrix.MatrixType.VBAR -> "determinant "
            LatexNode.Matrix.MatrixType.DOUBLE_VBAR -> "norm "
            else -> ""
        }
        val body = node.rows.mapIndexed { i, row ->
            "row ${i + 1}: " + row.joinToString(", ") { visit(it) }
        }.joinToString("; ")
        return "${bracket}$rows by $cols matrix: $body"
    }

    override fun visitArray(node: LatexNode.Array): String {
        val body = node.rows.mapIndexed { i, row ->
            "row ${i + 1}: " + row.joinToString(", ") { visit(it) }
        }.joinToString("; ")
        return "array: $body"
    }

    override fun visitSymbol(node: LatexNode.Symbol): String {
        return symbolName(node.symbol, node.unicode)
    }

    override fun visitOperator(node: LatexNode.Operator): String = node.op

    override fun visitSpace(node: LatexNode.Space): String = " "

    override fun visitHSpace(node: LatexNode.HSpace): String = " "

    override fun visitNewLine(node: LatexNode.NewLine): String = "; "

    override fun visitDelimited(node: LatexNode.Delimited): String {
        val left = delimiterName(node.left, isLeft = true)
        val right = delimiterName(node.right, isLeft = false)
        val content = node.content.joinToString(" ") { visit(it) }.collapseSpaces()
        return "$left $content $right"
    }

    override fun visitManualSizedDelimiter(node: LatexNode.ManualSizedDelimiter): String {
        return delimiterName(node.delimiter, isLeft = true)
    }

    override fun visitAccent(node: LatexNode.Accent): String {
        val content = visit(node.content)
        val accent = when (node.accentType) {
            LatexNode.Accent.AccentType.HAT -> "hat"
            LatexNode.Accent.AccentType.TILDE -> "tilde"
            LatexNode.Accent.AccentType.BAR -> "bar"
            LatexNode.Accent.AccentType.DOT -> "dot"
            LatexNode.Accent.AccentType.DDOT -> "double dot"
            LatexNode.Accent.AccentType.VEC -> "vector"
            LatexNode.Accent.AccentType.OVERLINE -> "overline"
            LatexNode.Accent.AccentType.UNDERLINE -> "underline"
            LatexNode.Accent.AccentType.OVERBRACE -> "overbrace"
            LatexNode.Accent.AccentType.UNDERBRACE -> "underbrace"
            LatexNode.Accent.AccentType.WIDEHAT -> "wide hat"
            LatexNode.Accent.AccentType.OVERRIGHTARROW -> "right arrow over"
            LatexNode.Accent.AccentType.OVERLEFTARROW -> "left arrow over"
            LatexNode.Accent.AccentType.CANCEL -> "canceled"
            LatexNode.Accent.AccentType.BCANCEL -> "back-canceled"
            LatexNode.Accent.AccentType.XCANCEL -> "cross-canceled"
            LatexNode.Accent.AccentType.DDDOT -> "triple dot"
            LatexNode.Accent.AccentType.GRAVE -> "grave"
            LatexNode.Accent.AccentType.ACUTE -> "acute"
            LatexNode.Accent.AccentType.CHECK -> "check"
            LatexNode.Accent.AccentType.BREVE -> "breve"
            LatexNode.Accent.AccentType.RING -> "ring"
            LatexNode.Accent.AccentType.OVERBRACKET -> "overbracket"
            LatexNode.Accent.AccentType.UNDERBRACKET -> "underbracket"
            LatexNode.Accent.AccentType.WIDECHECK -> "wide check"
            LatexNode.Accent.AccentType.OVERLEFTRIGHTARROW -> "left right arrow over"
            LatexNode.Accent.AccentType.UNDERLEFTARROW -> "left arrow under"
            LatexNode.Accent.AccentType.UNDERRIGHTARROW -> "right arrow under"
            LatexNode.Accent.AccentType.OVERPAREN -> "overparen"
            LatexNode.Accent.AccentType.UNDERPAREN -> "underparen"
        }
        return "$content $accent"
    }

    override fun visitExtensibleArrow(node: LatexNode.ExtensibleArrow): String {
        val above = visit(node.content)
        val below = node.below?.let { visit(it) }
        val dir = when (node.direction) {
            LatexNode.ExtensibleArrow.Direction.RIGHT -> "right arrow"
            LatexNode.ExtensibleArrow.Direction.LEFT -> "left arrow"
            LatexNode.ExtensibleArrow.Direction.BOTH -> "bidirectional arrow"
            LatexNode.ExtensibleArrow.Direction.HOOK_RIGHT -> "hook right arrow"
            LatexNode.ExtensibleArrow.Direction.HOOK_LEFT -> "hook left arrow"
            LatexNode.ExtensibleArrow.Direction.RIGHT_DOUBLE -> "double right arrow"
            LatexNode.ExtensibleArrow.Direction.LEFT_DOUBLE -> "double left arrow"
            LatexNode.ExtensibleArrow.Direction.BOTH_DOUBLE -> "double bidirectional arrow"
            LatexNode.ExtensibleArrow.Direction.MAPSTO -> "maps to"
            LatexNode.ExtensibleArrow.Direction.EQUAL -> "long equals"
        }
        return buildString {
            append(dir)
            if (above.isNotBlank()) append(" labeled $above")
            if (below != null && below.isNotBlank()) append(" with $below below")
        }
    }

    override fun visitStack(node: LatexNode.Stack): String {
        val base = visit(node.base)
        val above = node.above?.let { visit(it) }
        val below = node.below?.let { visit(it) }
        return buildString {
            append(base)
            if (above != null) append(" with $above above")
            if (below != null) append(" with $below below")
        }
    }

    override fun visitStyle(node: LatexNode.Style): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitColor(node: LatexNode.Color): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitMathStyle(node: LatexNode.MathStyle): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitFontSize(node: LatexNode.FontSize): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitBigOperator(node: LatexNode.BigOperator): String {
        val op = bigOperatorName(node.operator)
        val sub = node.subscript?.let { "from ${visit(it)}" }
        val sup = node.superscript?.let { "to ${visit(it)}" }
        return listOfNotNull(op, sub, sup).joinToString(" ")
    }

    override fun visitAligned(node: LatexNode.Aligned): String {
        return node.rows.joinToString("; ") { row ->
            row.joinToString(" ") { visit(it) }.collapseSpaces()
        }
    }

    override fun visitCases(node: LatexNode.Cases): String {
        val body = node.cases.joinToString("; ") { (expr, cond) ->
            "${visit(expr)} if ${visit(cond)}"
        }
        return "cases: $body"
    }

    override fun visitSplit(node: LatexNode.Split): String {
        return node.rows.joinToString("; ") { row ->
            row.joinToString(" ") { visit(it) }.collapseSpaces()
        }
    }

    override fun visitMultline(node: LatexNode.Multline): String {
        return node.lines.joinToString("; ") { visit(it) }
    }

    override fun visitEqnarray(node: LatexNode.Eqnarray): String {
        return node.rows.joinToString("; ") { row ->
            row.joinToString(" ") { visit(it) }.collapseSpaces()
        }
    }

    override fun visitSubequations(node: LatexNode.Subequations): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitBinomial(node: LatexNode.Binomial): String {
        val top = visit(node.top)
        val bottom = visit(node.bottom)
        return "$top choose $bottom"
    }

    override fun visitTextMode(node: LatexNode.TextMode): String = node.text

    override fun visitNegation(node: LatexNode.Negation): String {
        return "not ${visit(node.content)}"
    }

    override fun visitTag(node: LatexNode.Tag): String {
        return "(${visit(node.label)})"
    }

    override fun visitSubstack(node: LatexNode.Substack): String {
        return node.rows.joinToString(", ") { row ->
            row.joinToString(" ") { visit(it) }.collapseSpaces()
        }
    }

    override fun visitSmash(node: LatexNode.Smash): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitVPhantom(node: LatexNode.VPhantom): String = ""
    override fun visitHPhantom(node: LatexNode.HPhantom): String = ""

    override fun visitLabel(node: LatexNode.Label): String = ""

    override fun visitRef(node: LatexNode.Ref): String = "reference ${node.key}"

    override fun visitEqRef(node: LatexNode.EqRef): String = "(${node.key})"

    override fun visitSideSet(node: LatexNode.SideSet): String {
        val base = visit(node.base)
        val parts = mutableListOf<String>()
        node.leftSub?.let { parts.add("left sub ${visit(it)}") }
        node.leftSup?.let { parts.add("left super ${visit(it)}") }
        node.rightSub?.let { parts.add("right sub ${visit(it)}") }
        node.rightSup?.let { parts.add("right super ${visit(it)}") }
        return "$base with ${parts.joinToString(", ")}"
    }

    override fun visitTensor(node: LatexNode.Tensor): String {
        val base = visit(node.base)
        val indices = node.indices.joinToString(" ") { (isUpper, indexNode) ->
            val prefix = if (isUpper) "upper" else "lower"
            "$prefix ${visit(indexNode)}"
        }
        return "$base $indices"
    }

    override fun visitTabular(node: LatexNode.Tabular): String {
        val rows = node.rows.size
        val cols = node.rows.firstOrNull()?.size ?: 0
        val body = node.rows.mapIndexed { i, row ->
            "row ${i + 1}: " + row.joinToString(", ") { visit(it) }
        }.joinToString("; ")
        return "$rows by $cols table: $body"
    }

    override fun visitHLine(node: LatexNode.HLine): String = "horizontal line"

    override fun visitCLine(node: LatexNode.CLine): String =
        "horizontal line from column ${node.startCol} to ${node.endCol}"

    override fun visitMulticolumn(node: LatexNode.Multicolumn): String {
        val content = node.content.joinToString(" ") { visit(it) }.collapseSpaces()
        return "multicolumn spanning ${node.columnCount}: $content"
    }

    override fun visitOperatorName(node: LatexNode.OperatorName): String = node.name

    override fun visitModOperator(node: LatexNode.ModOperator): String {
        val arg = node.content?.let { visit(it) }
        return when (node.modStyle) {
            LatexNode.ModOperator.ModStyle.BMOD -> "mod"
            LatexNode.ModOperator.ModStyle.PMOD -> if (arg != null) "(mod $arg)" else "(mod)"
            LatexNode.ModOperator.ModStyle.MOD -> if (arg != null) "mod $arg" else "mod"
        }
    }

    override fun visitBoxed(node: LatexNode.Boxed): String {
        return "boxed: " + node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitEnclose(node: LatexNode.Enclose): String {
        val content = node.content.joinToString(" ") { visit(it) }.collapseSpaces()
        val notation = node.notations.joinToString(", ") { encloseNotationName(it) }
        return if (notation.isBlank()) {
            "enclosed: $content"
        } else {
            "enclosed with $notation: $content"
        }
    }

    override fun visitPhantom(node: LatexNode.Phantom): String = ""

    override fun visitNewCommand(node: LatexNode.NewCommand): String = ""

    override fun visitError(node: LatexNode.Error): String {
        val recovered = node.recovered.joinToString(" ") { visit(it) }.collapseSpaces()
        return if (recovered.isNotBlank()) "error: $recovered" else "error: ${node.message}"
    }

    override fun visitHyperlink(node: LatexNode.Hyperlink): String {
        val text = node.content.joinToString(" ") { visit(it) }.collapseSpaces()
        return if (text.isNotBlank()) "link: $text" else "link: ${node.url}"
    }

    override fun visitColorBox(node: LatexNode.ColorBox): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitPrescript(node: LatexNode.Prescript): String {
        val base = visit(node.base)
        val parts = mutableListOf<String>()
        node.preSuperscript?.let { parts.add("pre-superscript ${visit(it)}") }
        node.preSubscript?.let { parts.add("pre-subscript ${visit(it)}") }
        return "$base with ${parts.joinToString(", ")}"
    }

    override fun visitMathLap(node: LatexNode.MathLap): String {
        return node.content.joinToString(" ") { visit(it) }.collapseSpaces()
    }

    override fun visitLayout(node: LatexNode.Layout): String = when (node.layoutType) {
        LatexNode.Layout.LayoutType.RAISE_BOX ->
            node.content.joinToString(" ") { visit(it) }.collapseSpaces()
        LatexNode.Layout.LayoutType.RULE -> "rule"
    }

    override fun visitNewEnvironment(node: LatexNode.NewEnvironment): String = ""

    override fun visitSectionHeading(node: LatexNode.SectionHeading): String {
        val content = node.content.joinToString(" ") { visit(it) }.collapseSpaces()
        val level = when (node.level) {
            LatexNode.SectionHeading.HeadingLevel.SECTION -> "section"
            LatexNode.SectionHeading.HeadingLevel.SUBSECTION -> "subsection"
            LatexNode.SectionHeading.HeadingLevel.SUBSUBSECTION -> "subsubsection"
            LatexNode.SectionHeading.HeadingLevel.PARAGRAPH -> "paragraph"
            LatexNode.SectionHeading.HeadingLevel.SUBPARAGRAPH -> "subparagraph"
        }
        return "$level: $content"
    }

    override fun visitTextDirection(node: LatexNode.TextDirection): String {
        val content = node.content.joinToString(" ") { visit(it) }.collapseSpaces()
        val dir = when (node.direction) {
            LatexNode.TextDirection.Direction.RTL -> "right-to-left"
            LatexNode.TextDirection.Direction.LTR -> "left-to-right"
        }
        return "$dir: $content"
    }

    // ========== 辅助方法 ==========

    private fun symbolName(symbol: String, unicode: String): String {
        return SymbolMap.getAccessibleName(symbol)
            ?: SymbolMap.getAccessibleNameByUnicode(unicode)
            ?: unicode
    }

    private fun bigOperatorName(operator: String): String {
        return when (operator) {
            "∑", "\\sum", "sum" -> "sum"
            "∏", "\\prod", "prod" -> "product"
            "∫", "\\int", "int" -> "integral"
            "∮", "\\oint", "oint" -> "contour integral"
            "∬", "\\iint", "iint" -> "double integral"
            "∭", "\\iiint", "iiint" -> "triple integral"
            "⋃", "\\bigcup", "bigcup" -> "union"
            "⋂", "\\bigcap", "bigcap" -> "intersection"
            "⊕", "\\bigoplus", "bigoplus" -> "direct sum"
            "⊗", "\\bigotimes", "bigotimes" -> "tensor product"
            "⋁", "\\bigvee", "bigvee" -> "disjunction"
            "⋀", "\\bigwedge", "bigwedge" -> "conjunction"
            "∐", "\\coprod", "coprod" -> "coproduct"
            "⊔", "\\bigsqcup", "bigsqcup" -> "square union"
            "⊙", "\\bigodot", "bigodot" -> "circle dot"
            "⊎", "\\biguplus", "biguplus" -> "disjoint union"
            "lim" -> "limit"
            "max" -> "maximum"
            "min" -> "minimum"
            "sup" -> "supremum"
            "inf" -> "infimum"
            "limsup" -> "limit superior"
            "liminf" -> "limit inferior"
            "det" -> "determinant"
            "gcd" -> "greatest common divisor"
            else -> operator
        }
    }

    private fun delimiterName(delim: String, isLeft: Boolean): String {
        if (delim.isEmpty() || delim == ".") return ""
        return when (delim) {
            "(", ")" -> if (isLeft) "open paren" else "close paren"
            "[", "]" -> if (isLeft) "open bracket" else "close bracket"
            "{", "}", "\\{", "\\}" -> if (isLeft) "open brace" else "close brace"
            "|" -> "vertical bar"
            "‖", "\\|" -> "double vertical bar"
            "⟨", "\\langle" -> "left angle bracket"
            "⟩", "\\rangle" -> "right angle bracket"
            "⌊", "\\lfloor" -> "floor"
            "⌋", "\\rfloor" -> "end floor"
            "⌈", "\\lceil" -> "ceiling"
            "⌉", "\\rceil" -> "end ceiling"
            else -> delim
        }
    }

    private fun encloseNotationName(notation: LatexNode.Enclose.Notation): String {
        return when (notation) {
            LatexNode.Enclose.Notation.LONGDIV -> "long division"
            LatexNode.Enclose.Notation.ACTUARIAL -> "actuarial"
            LatexNode.Enclose.Notation.BOX -> "box"
            LatexNode.Enclose.Notation.ROUNDEDBOX -> "rounded box"
            LatexNode.Enclose.Notation.CIRCLE -> "circle"
            LatexNode.Enclose.Notation.LEFT -> "left line"
            LatexNode.Enclose.Notation.RIGHT -> "right line"
            LatexNode.Enclose.Notation.TOP -> "top line"
            LatexNode.Enclose.Notation.BOTTOM -> "bottom line"
            LatexNode.Enclose.Notation.UPDIAGONALSTRIKE -> "up diagonal strike"
            LatexNode.Enclose.Notation.DOWNDIAGONALSTRIKE -> "down diagonal strike"
            LatexNode.Enclose.Notation.VERTICALSTRIKE -> "vertical strike"
            LatexNode.Enclose.Notation.HORIZONTALSTRIKE -> "horizontal strike"
            LatexNode.Enclose.Notation.MADRUWB -> "madruwb"
            LatexNode.Enclose.Notation.UPDIAGONALARROW -> "up diagonal arrow"
            LatexNode.Enclose.Notation.PHASORANGLE -> "phasor angle"
        }
    }

    private fun String.collapseSpaces(): String {
        return replace(WHITESPACE_REGEX, " ").trim()
    }
}
