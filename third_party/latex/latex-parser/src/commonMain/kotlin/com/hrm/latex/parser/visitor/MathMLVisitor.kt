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

import com.hrm.latex.parser.model.LatexNode
import kotlin.math.round

private val bboxMathMlDimensionPattern =
    Regex("""(-?\d+(?:\.\d+)?)(px|pt|em|ex|mm|cm|in)""", RegexOption.IGNORE_CASE)

/**
 * LaTeX AST → MathML 转换访问者
 *
 * 将 LaTeX 语法树转换为 Presentation MathML 标记语言，
 * 用于提升 Web 可访问性和跨平台数学公式交换。
 *
 * 用法示例：
 * ```kotlin
 * val mathml = MathMLVisitor.convert(document)
 * // 生成完整的 <math> 元素
 * ```
 *
 * 支持的 MathML 元素：
 * - `<mi>` 标识符（变量）
 * - `<mn>` 数字
 * - `<mo>` 运算符
 * - `<mfrac>` 分数
 * - `<msqrt>` / `<mroot>` 根号
 * - `<msup>` / `<msub>` / `<msubsup>` 上下标
 * - `<mover>` / `<munder>` / `<munderover>` 上下装饰
 * - `<mtable>` / `<mtr>` / `<mtd>` 矩阵
 * - `<mrow>` 行分组
 * - `<mtext>` 文本
 * - `<mstyle>` 样式
 * - `<menclose>` 围框/取消线
 * - `<mphantom>` 幻影
 */
class MathMLVisitor : BaseLatexVisitor<String>() {
    private var currentFontScale: Float = 1.0f

    companion object {
        /**
         * 将 LaTeX AST 转换为完整的 MathML 字符串
         * @param node AST 根节点
         * @param displayMode 是否为 display 模式（block 或 inline）
         * @return 完整的 <math> 元素字符串
         */
        fun convert(node: LatexNode, displayMode: Boolean = true): String {
            val visitor = MathMLVisitor()
            val body = visitor.visit(node)
            val display = if (displayMode) "block" else "inline"
            return "<math xmlns=\"http://www.w3.org/1998/Math/MathML\" display=\"$display\">$body</math>"
        }

        /**
         * 将 LaTeX Document 转换为 MathML
         */
        fun convert(document: LatexNode.Document, displayMode: Boolean = true): String {
            return convert(document as LatexNode, displayMode)
        }
    }

    override fun defaultVisit(node: LatexNode): String = ""

    override fun visitDocument(node: LatexNode.Document): String {
        return mrow(node.children.joinToString("") { visit(it) })
    }

    override fun visitText(node: LatexNode.Text): String {
        return node.content.map { ch ->
            when {
                ch.isDigit() -> "<mn>$ch</mn>"
                ch.isLetter() -> "<mi>${escapeXml(ch.toString())}</mi>"
                ch == '+' || ch == '-' || ch == '=' || ch == '<' || ch == '>' ||
                    ch == ',' || ch == '.' || ch == '!' || ch == ':' || ch == ';' ->
                    "<mo>${escapeXml(ch.toString())}</mo>"
                ch == ' ' -> ""
                else -> "<mo>${escapeXml(ch.toString())}</mo>"
            }
        }.joinToString("")
    }

    override fun visitGroup(node: LatexNode.Group): String {
        return mrow(node.children.joinToString("") { visit(it) })
    }

    override fun visitSuperscript(node: LatexNode.Superscript): String {
        return "<msup>${visit(node.base)}${visit(node.exponent)}</msup>"
    }

    override fun visitSubscript(node: LatexNode.Subscript): String {
        return "<msub>${visit(node.base)}${visit(node.index)}</msub>"
    }

    override fun visitFraction(node: LatexNode.Fraction): String {
        val lineThickness = if (node.style == LatexNode.Fraction.FractionStyle.RULELESS) {
            " linethickness=\"0\""
        } else {
            ""
        }
        val frac = "<mfrac$lineThickness>${visit(node.numerator)}${visit(node.denominator)}</mfrac>"
        return when (node.style) {
            LatexNode.Fraction.FractionStyle.DISPLAY,
            LatexNode.Fraction.FractionStyle.CONTINUED ->
                "<mstyle displaystyle=\"true\">$frac</mstyle>"

            LatexNode.Fraction.FractionStyle.TEXT ->
                "<mstyle displaystyle=\"false\">$frac</mstyle>"

            LatexNode.Fraction.FractionStyle.RULELESS,
            LatexNode.Fraction.FractionStyle.NORMAL ->
                frac
        }
    }

    override fun visitRoot(node: LatexNode.Root): String {
        return if (node.index != null) {
            "<mroot>${visit(node.content)}${visit(node.index)}</mroot>"
        } else {
            "<msqrt>${visit(node.content)}</msqrt>"
        }
    }

    override fun visitMatrix(node: LatexNode.Matrix): String {
        val (open, close) = when (node.type) {
            LatexNode.Matrix.MatrixType.PAREN -> "(" to ")"
            LatexNode.Matrix.MatrixType.BRACKET -> "[" to "]"
            LatexNode.Matrix.MatrixType.BRACE -> "{" to "}"
            LatexNode.Matrix.MatrixType.VBAR -> "|" to "|"
            LatexNode.Matrix.MatrixType.DOUBLE_VBAR -> "‖" to "‖"
            LatexNode.Matrix.MatrixType.PLAIN -> "" to ""
        }
        val table = buildTable(node.rows)
        return if (open.isNotEmpty()) {
            mrow("<mo>$open</mo>$table<mo>$close</mo>")
        } else {
            table
        }
    }

    override fun visitArray(node: LatexNode.Array): String {
        return buildTable(node.rows)
    }

    override fun visitSymbol(node: LatexNode.Symbol): String {
        val unicode = node.unicode
        return if (unicode.length == 1 && unicode[0].isLetter()) {
            "<mi>${escapeXml(unicode)}</mi>"
        } else {
            "<mo>${escapeXml(unicode)}</mo>"
        }
    }

    override fun visitOperator(node: LatexNode.Operator): String {
        return "<mo>${escapeXml(node.op)}</mo>"
    }

    override fun visitSpace(node: LatexNode.Space): String {
        val width = when (node.type) {
            LatexNode.Space.SpaceType.THIN -> "thinmathspace"
            LatexNode.Space.SpaceType.MEDIUM -> "mediummathspace"
            LatexNode.Space.SpaceType.THICK -> "thickmathspace"
            LatexNode.Space.SpaceType.QUAD -> "1em"
            LatexNode.Space.SpaceType.QQUAD -> "2em"
            LatexNode.Space.SpaceType.NEGATIVE_THIN -> "negativethinmathspace"
            LatexNode.Space.SpaceType.NORMAL -> "0.25em"
        }
        return "<mspace width=\"$width\"/>"
    }

    override fun visitHSpace(node: LatexNode.HSpace): String {
        return "<mspace width=\"${escapeXml(node.dimension)}\"/>"
    }

    override fun visitNewLine(node: LatexNode.NewLine): String = ""

    override fun visitDelimited(node: LatexNode.Delimited): String {
        val content = node.content.joinToString("") { visit(it) }
        val left = if (node.left.isNotEmpty() && node.left != ".")
            "<mo stretchy=\"true\">${escapeXml(node.left)}</mo>" else ""
        val right = if (node.right.isNotEmpty() && node.right != ".")
            "<mo stretchy=\"true\">${escapeXml(node.right)}</mo>" else ""
        return mrow("$left$content$right")
    }

    override fun visitManualSizedDelimiter(node: LatexNode.ManualSizedDelimiter): String {
        return "<mo stretchy=\"true\">${escapeXml(node.delimiter)}</mo>"
    }

    override fun visitAccent(node: LatexNode.Accent): String {
        val content = visit(node.content)
        return when (node.accentType) {
            LatexNode.Accent.AccentType.HAT -> "<mover>$content<mo>^</mo></mover>"
            LatexNode.Accent.AccentType.TILDE -> "<mover>$content<mo>~</mo></mover>"
            LatexNode.Accent.AccentType.BAR -> "<mover>$content<mo>¯</mo></mover>"
            LatexNode.Accent.AccentType.DOT -> "<mover>$content<mo>˙</mo></mover>"
            LatexNode.Accent.AccentType.DDOT -> "<mover>$content<mo>¨</mo></mover>"
            LatexNode.Accent.AccentType.VEC -> "<mover>$content<mo>→</mo></mover>"
            LatexNode.Accent.AccentType.OVERLINE -> "<mover>$content<mo>¯</mo></mover>"
            LatexNode.Accent.AccentType.UNDERLINE -> "<munder>$content<mo>_</mo></munder>"
            LatexNode.Accent.AccentType.OVERBRACE -> "<mover>$content<mo>⏞</mo></mover>"
            LatexNode.Accent.AccentType.UNDERBRACE -> "<munder>$content<mo>⏟</mo></munder>"
            LatexNode.Accent.AccentType.WIDEHAT -> "<mover>$content<mo>^</mo></mover>"
            LatexNode.Accent.AccentType.OVERRIGHTARROW -> "<mover>$content<mo>→</mo></mover>"
            LatexNode.Accent.AccentType.OVERLEFTARROW -> "<mover>$content<mo>←</mo></mover>"
            LatexNode.Accent.AccentType.CANCEL -> renderMenclose(content, listOf(LatexNode.Enclose.Notation.UPDIAGONALSTRIKE))
            LatexNode.Accent.AccentType.BCANCEL -> renderMenclose(content, listOf(LatexNode.Enclose.Notation.DOWNDIAGONALSTRIKE))
            LatexNode.Accent.AccentType.XCANCEL -> renderMenclose(
                content,
                listOf(
                    LatexNode.Enclose.Notation.UPDIAGONALSTRIKE,
                    LatexNode.Enclose.Notation.DOWNDIAGONALSTRIKE
                )
            )
            LatexNode.Accent.AccentType.DDDOT -> "<mover>$content<mo>\u20DB</mo></mover>"
            LatexNode.Accent.AccentType.GRAVE -> "<mover>$content<mo>`</mo></mover>"
            LatexNode.Accent.AccentType.ACUTE -> "<mover>$content<mo>´</mo></mover>"
            LatexNode.Accent.AccentType.CHECK -> "<mover>$content<mo>ˇ</mo></mover>"
            LatexNode.Accent.AccentType.BREVE -> "<mover>$content<mo>˘</mo></mover>"
            LatexNode.Accent.AccentType.RING -> "<mover>$content<mo>˚</mo></mover>"
            LatexNode.Accent.AccentType.OVERBRACKET -> "<mover>$content<mo>⎴</mo></mover>"
            LatexNode.Accent.AccentType.UNDERBRACKET -> "<munder>$content<mo>⎵</mo></munder>"
            LatexNode.Accent.AccentType.WIDECHECK -> "<mover>$content<mo>ˇ</mo></mover>"
            LatexNode.Accent.AccentType.OVERLEFTRIGHTARROW ->
                "<mover>$content<mo>↔</mo></mover>"
            LatexNode.Accent.AccentType.UNDERLEFTARROW ->
                "<munder>$content<mo>←</mo></munder>"
            LatexNode.Accent.AccentType.UNDERRIGHTARROW ->
                "<munder>$content<mo>→</mo></munder>"
            LatexNode.Accent.AccentType.OVERPAREN -> "<mover>$content<mo>⏜</mo></mover>"
            LatexNode.Accent.AccentType.UNDERPAREN -> "<munder>$content<mo>⏝</mo></munder>"
        }
    }

    override fun visitExtensibleArrow(node: LatexNode.ExtensibleArrow): String {
        val arrowChar = when (node.direction) {
            LatexNode.ExtensibleArrow.Direction.RIGHT -> "→"
            LatexNode.ExtensibleArrow.Direction.LEFT -> "←"
            LatexNode.ExtensibleArrow.Direction.BOTH -> "↔"
            LatexNode.ExtensibleArrow.Direction.HOOK_RIGHT -> "↪"
            LatexNode.ExtensibleArrow.Direction.HOOK_LEFT -> "↩"
            LatexNode.ExtensibleArrow.Direction.RIGHT_DOUBLE -> "⇒"
            LatexNode.ExtensibleArrow.Direction.LEFT_DOUBLE -> "⇐"
            LatexNode.ExtensibleArrow.Direction.BOTH_DOUBLE -> "⇔"
            LatexNode.ExtensibleArrow.Direction.MAPSTO -> "↦"
            LatexNode.ExtensibleArrow.Direction.EQUAL -> "="
        }
        val arrow = "<mo stretchy=\"true\">$arrowChar</mo>"
        val above = visit(node.content)
        val below = node.below?.let { visit(it) }
        return if (below != null) {
            "<munderover>$arrow${mrow(below)}${mrow(above)}</munderover>"
        } else {
            "<mover>$arrow${mrow(above)}</mover>"
        }
    }

    override fun visitStack(node: LatexNode.Stack): String {
        val base = visit(node.base)
        val above = node.above?.let { visit(it) }
        val below = node.below?.let { visit(it) }
        return when {
            above != null && below != null -> "<munderover>$base${mrow(below)}${mrow(above)}</munderover>"
            above != null -> "<mover>$base${mrow(above)}</mover>"
            below != null -> "<munder>$base${mrow(below)}</munder>"
            else -> base
        }
    }

    override fun visitStyle(node: LatexNode.Style): String {
        val content = node.content.joinToString("") { visit(it) }
        val attr = when (node.styleType) {
            LatexNode.Style.StyleType.BOLD, LatexNode.Style.StyleType.BOLD_SYMBOL ->
                " mathvariant=\"bold\""
            LatexNode.Style.StyleType.ITALIC -> " mathvariant=\"italic\""
            LatexNode.Style.StyleType.ROMAN -> " mathvariant=\"normal\""
            LatexNode.Style.StyleType.SANS_SERIF -> " mathvariant=\"sans-serif\""
            LatexNode.Style.StyleType.MONOSPACE -> " mathvariant=\"monospace\""
            LatexNode.Style.StyleType.BLACKBOARD_BOLD -> " mathvariant=\"double-struck\""
            LatexNode.Style.StyleType.FRAKTUR -> " mathvariant=\"fraktur\""
            LatexNode.Style.StyleType.SCRIPT -> " mathvariant=\"script\""
            LatexNode.Style.StyleType.CALLIGRAPHIC -> " mathvariant=\"script\""
        }
        return "<mstyle$attr>$content</mstyle>"
    }

    override fun visitColor(node: LatexNode.Color): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mstyle mathcolor=\"${escapeXml(node.color)}\">$content</mstyle>"
    }

    override fun visitMathStyle(node: LatexNode.MathStyle): String {
        val content = node.content.joinToString("") { visit(it) }
        val size = when (node.mathStyleType) {
            LatexNode.MathStyle.MathStyleType.DISPLAY -> "normal"
            LatexNode.MathStyle.MathStyleType.TEXT -> "normal"
            LatexNode.MathStyle.MathStyleType.SCRIPT -> "scriptlevel=\"1\""
            LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT -> "scriptlevel=\"2\""
        }
        val display = when (node.mathStyleType) {
            LatexNode.MathStyle.MathStyleType.DISPLAY -> " displaystyle=\"true\""
            LatexNode.MathStyle.MathStyleType.TEXT -> " displaystyle=\"false\""
            else -> ""
        }
        return "<mstyle$display>$content</mstyle>"
    }

    override fun visitFontSize(node: LatexNode.FontSize): String {
        val inheritedScale = currentFontScale
        val targetScale = node.sizeType.scaleFactor()
        currentFontScale = targetScale
        val content = try {
            node.content.joinToString("") { visit(it) }
        } finally {
            currentFontScale = inheritedScale
        }
        val size = formatPercent(targetScale / inheritedScale)
        return "<mstyle mathsize=\"$size\">$content</mstyle>"
    }

    private fun LatexNode.FontSize.SizeType.scaleFactor(): Float = when (this) {
        LatexNode.FontSize.SizeType.TINY -> 0.5f
        LatexNode.FontSize.SizeType.SCRIPT_SIZE -> 0.7f
        LatexNode.FontSize.SizeType.FOOTNOTE_SIZE -> 0.8f
        LatexNode.FontSize.SizeType.SMALL -> 0.9f
        LatexNode.FontSize.SizeType.NORMAL_SIZE -> 1.0f
        LatexNode.FontSize.SizeType.LARGE -> 1.2f
        LatexNode.FontSize.SizeType.LARGE_2 -> 1.44f
        LatexNode.FontSize.SizeType.LARGE_3 -> 1.728f
        LatexNode.FontSize.SizeType.HUGE -> 2.074f
        LatexNode.FontSize.SizeType.HUGE_2 -> 2.488f
    }

    private fun formatPercent(scale: Float): String {
        val percent = round(scale * 100_000f) / 1_000f
        return if (percent % 1f == 0f) {
            "${percent.toInt()}%"
        } else {
            "${percent.toString().trimEnd('0').trimEnd('.')}%"
        }
    }

    override fun visitBigOperator(node: LatexNode.BigOperator): String {
        val op = "<mo>${escapeXml(node.operator)}</mo>"
        val sub = node.subscript?.let { visit(it) }
        val sup = node.superscript?.let { visit(it) }
        return when {
            sub != null && sup != null -> "<munderover>$op${mrow(sub)}${mrow(sup)}</munderover>"
            sub != null -> "<munder>$op${mrow(sub)}</munder>"
            sup != null -> "<mover>$op${mrow(sup)}</mover>"
            else -> op
        }
    }

    override fun visitAligned(node: LatexNode.Aligned): String {
        return buildTable(node.rows)
    }

    override fun visitCases(node: LatexNode.Cases): String {
        val rows = node.cases.map { (expr, cond) -> listOf(expr, cond) }
        return mrow("<mo>{</mo>${buildTable(rows)}")
    }

    override fun visitSplit(node: LatexNode.Split): String {
        return buildTable(node.rows)
    }

    override fun visitMultline(node: LatexNode.Multline): String {
        val rows = node.lines.map { listOf(it) }
        return buildTable(rows)
    }

    override fun visitEqnarray(node: LatexNode.Eqnarray): String {
        return buildTable(node.rows)
    }

    override fun visitSubequations(node: LatexNode.Subequations): String {
        return node.content.joinToString("") { visit(it) }
    }

    override fun visitBinomial(node: LatexNode.Binomial): String {
        return mrow("<mo>(</mo><mfrac linethickness=\"0\">${visit(node.top)}${visit(node.bottom)}</mfrac><mo>)</mo>")
    }

    override fun visitTextMode(node: LatexNode.TextMode): String {
        return "<mtext>${escapeXml(node.text)}</mtext>"
    }

    override fun visitNegation(node: LatexNode.Negation): String {
        return renderMenclose(visit(node.content), listOf(LatexNode.Enclose.Notation.UPDIAGONALSTRIKE))
    }

    override fun visitTag(node: LatexNode.Tag): String {
        val label = visit(node.label)
        return if (node.starred) label else mrow("<mo>(</mo>$label<mo>)</mo>")
    }

    override fun visitSubstack(node: LatexNode.Substack): String {
        return buildTable(node.rows)
    }

    override fun visitSmash(node: LatexNode.Smash): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mpadded height=\"0\" depth=\"0\">$content</mpadded>"
    }

    override fun visitVPhantom(node: LatexNode.VPhantom): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mphantom>$content</mphantom>"
    }

    override fun visitHPhantom(node: LatexNode.HPhantom): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mphantom>$content</mphantom>"
    }

    override fun visitLabel(node: LatexNode.Label): String = ""

    override fun visitRef(node: LatexNode.Ref): String {
        return "<mtext>${escapeXml(node.key)}</mtext>"
    }

    override fun visitEqRef(node: LatexNode.EqRef): String {
        return mrow("<mo>(</mo><mtext>${escapeXml(node.key)}</mtext><mo>)</mo>")
    }

    override fun visitSideSet(node: LatexNode.SideSet): String {
        val base = visit(node.base)
        // 用 mmultiscripts 表示四角标注
        val leftSub = node.leftSub?.let { visit(it) } ?: "<none/>"
        val leftSup = node.leftSup?.let { visit(it) } ?: "<none/>"
        val rightSub = node.rightSub?.let { visit(it) } ?: "<none/>"
        val rightSup = node.rightSup?.let { visit(it) } ?: "<none/>"
        return "<mmultiscripts>$base$rightSub$rightSup<mprescripts/>$leftSub$leftSup</mmultiscripts>"
    }

    override fun visitTensor(node: LatexNode.Tensor): String {
        val base = visit(node.base)
        if (node.indices.isEmpty()) return base
        // 简化处理：配对为 sub/sup
        var result = base
        var i = 0
        while (i < node.indices.size) {
            val (isUpper, indexNode) = node.indices[i]
            val idx = visit(indexNode)
            if (isUpper) {
                // 检查下一个是否为下标
                if (i + 1 < node.indices.size && !node.indices[i + 1].first) {
                    val subIdx = visit(node.indices[i + 1].second)
                    result = "<msubsup>$result${mrow(subIdx)}${mrow(idx)}</msubsup>"
                    i += 2
                } else {
                    result = "<msup>$result${mrow(idx)}</msup>"
                    i++
                }
            } else {
                if (i + 1 < node.indices.size && node.indices[i + 1].first) {
                    val supIdx = visit(node.indices[i + 1].second)
                    result = "<msubsup>$result${mrow(idx)}${mrow(supIdx)}</msubsup>"
                    i += 2
                } else {
                    result = "<msub>$result${mrow(idx)}</msub>"
                    i++
                }
            }
        }
        return result
    }

    override fun visitTabular(node: LatexNode.Tabular): String {
        return buildTable(node.rows)
    }

    override fun visitHLine(node: LatexNode.HLine): String = ""

    override fun visitCLine(node: LatexNode.CLine): String = ""

    override fun visitMulticolumn(node: LatexNode.Multicolumn): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mtd columnspan=\"${node.columnCount}\">$content</mtd>"
    }

    override fun visitBoxed(node: LatexNode.Boxed): String {
        val content = node.content.joinToString("") { visit(it) }
        return renderMenclose(content, listOf(LatexNode.Enclose.Notation.BOX))
    }

    override fun visitEnclose(node: LatexNode.Enclose): String {
        val content = node.content.joinToString("") { visit(it) }
        if (node.attributes[LatexNode.Enclose.BBOX_ATTRIBUTE] == "true") {
            return renderBBox(content, node.attributes)
        }
        return renderMenclose(content, node.notations, node.attributes)
    }

    override fun visitPhantom(node: LatexNode.Phantom): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mphantom>$content</mphantom>"
    }

    override fun visitNewCommand(node: LatexNode.NewCommand): String = ""

    override fun visitError(node: LatexNode.Error): String {
        val content = node.recovered.joinToString("") { visit(it) }
        return if (content.isNotEmpty()) {
            "<merror><mtext>${escapeXml(node.message)}</mtext>$content</merror>"
        } else {
            "<merror><mtext>${escapeXml(node.message)}</mtext></merror>"
        }
    }

    override fun visitHyperlink(node: LatexNode.Hyperlink): String {
        val content = if (node.content.isNotEmpty()) {
            node.content.joinToString("") { visit(it) }
        } else {
            "<mtext>${escapeXml(node.url)}</mtext>"
        }
        return "<mrow href=\"${escapeXml(node.url)}\">$content</mrow>"
    }

    override fun visitColorBox(node: LatexNode.ColorBox): String {
        val content = node.content.joinToString("") { visit(it) }
        val bgStyle = "background-color:${escapeXml(node.backgroundColor)}"
        val borderStyle = node.borderColor?.let { ";border:1px solid ${escapeXml(it)}" } ?: ""
        return "<mstyle mathbackground=\"${escapeXml(node.backgroundColor)}\">$content</mstyle>"
    }

    override fun visitPrescript(node: LatexNode.Prescript): String {
        val base = visit(node.base)
        val preSub = node.preSubscript?.let { visit(it) } ?: "<none/>"
        val preSup = node.preSuperscript?.let { visit(it) } ?: "<none/>"
        return "<mmultiscripts>$base<none/><none/><mprescripts/>$preSub$preSup</mmultiscripts>"
    }

    override fun visitMathLap(node: LatexNode.MathLap): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mpadded width=\"0\">$content</mpadded>"
    }

    override fun visitLayout(node: LatexNode.Layout): String = when (node.layoutType) {
        LatexNode.Layout.LayoutType.RAISE_BOX -> {
            val content = node.content.joinToString("") { visit(it) }
            "<mpadded voffset=\"${escapeXml(node.shift.orEmpty())}\">$content</mpadded>"
        }
        LatexNode.Layout.LayoutType.RULE ->
            "<mspace width=\"${escapeXml(node.width.orEmpty())}\" height=\"${escapeXml(node.height.orEmpty())}\" mathbackground=\"currentColor\"/>"
    }

    override fun visitNewEnvironment(node: LatexNode.NewEnvironment): String = ""

    override fun visitSectionHeading(node: LatexNode.SectionHeading): String {
        val content = node.content.joinToString("") { visit(it) }
        return "<mtext>${escapeXml(content)}</mtext>"
    }

    override fun visitTextDirection(node: LatexNode.TextDirection): String {
        val content = node.content.joinToString("") { visit(it) }
        val dir = when (node.direction) {
            LatexNode.TextDirection.Direction.RTL -> "rtl"
            LatexNode.TextDirection.Direction.LTR -> "ltr"
        }
        return "<mrow dir=\"$dir\">$content</mrow>"
    }

    override fun visitOperatorName(node: LatexNode.OperatorName): String {
        return "<mo>${escapeXml(node.name)}</mo>"
    }

    override fun visitModOperator(node: LatexNode.ModOperator): String {
        val arg = node.content?.let { visit(it) } ?: ""
        return when (node.modStyle) {
            LatexNode.ModOperator.ModStyle.BMOD -> "<mo>mod</mo>"
            LatexNode.ModOperator.ModStyle.PMOD -> mrow("<mo>(</mo><mo>mod</mo><mspace width=\"mediummathspace\"/>${arg}<mo>)</mo>")
            LatexNode.ModOperator.ModStyle.MOD -> mrow("<mo>mod</mo><mspace width=\"mediummathspace\"/>${arg}")
        }
    }

    // ========== 辅助方法 ==========

    private fun mrow(content: String): String = "<mrow>$content</mrow>"

    private fun renderMenclose(
        content: String,
        notations: List<LatexNode.Enclose.Notation>,
        attributes: Map<String, String> = emptyMap()
    ): String {
        val notationAttr = if (notations.isEmpty()) {
            LatexNode.Enclose.Notation.LONGDIV.mathMlName
        } else {
            notations.joinToString(" ") { it.mathMlName }
        }
        val extraAttrs = attributes.entries.joinToString("") { (key, value) ->
            " ${escapeXml(key)}=\"${escapeXml(value)}\""
        }
        return "<menclose notation=\"$notationAttr\"$extraAttrs>$content</menclose>"
    }

    private fun renderBBox(content: String, attributes: Map<String, String>): String {
        val padding = attributes["padding"]
        val paddingAttributes = padding?.let {
            val doubled = bboxMathMlDimensionPattern.matchEntire(it)?.let { match ->
                val value = match.groupValues[1].toDouble() * 2
                "${value.toString().removeSuffix(".0")}${match.groupValues[2]}"
            } ?: it
            """ lspace="${escapeXml(it)}" width="+${escapeXml(doubled)}"""" +
                """ height="+${escapeXml(it)}" depth="+${escapeXml(it)}""""
        } ?: ""
        val styles = buildList {
            attributes["mathbackground"]?.let { add("background-color: $it") }
            attributes["border"]?.let { add("border: $it") }
        }.joinToString("; ")
        val styleAttribute = if (styles.isEmpty()) "" else """ style="${escapeXml(styles)}""""
        return "<mpadded$paddingAttributes$styleAttribute>$content</mpadded>"
    }

    private fun buildTable(rows: List<List<LatexNode>>): String {
        val sb = StringBuilder("<mtable>")
        for (row in rows) {
            sb.append("<mtr>")
            for (cell in row) {
                sb.append("<mtd>")
                sb.append(visit(cell))
                sb.append("</mtd>")
            }
            sb.append("</mtr>")
        }
        sb.append("</mtable>")
        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        // 快速路径：大多数字符串不含特殊字符，直接返回
        var needsEscape = false
        for (ch in text) {
            if (ch == '&' || ch == '<' || ch == '>' || ch == '"') {
                needsEscape = true
                break
            }
        }
        if (!needsEscape) return text

        // 慢路径：单遍扫描构建结果
        val sb = StringBuilder(text.length + 8)
        for (ch in text) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
