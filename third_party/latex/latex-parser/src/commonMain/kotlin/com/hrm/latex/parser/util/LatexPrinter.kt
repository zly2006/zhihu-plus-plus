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


package com.hrm.latex.parser.util

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.visitor.BaseLatexVisitor

/**
 * LaTeX AST 打印器
 * 用于调试和可视化语法树
 */
class LatexPrinter : BaseLatexVisitor<String>() {
    private var indent = 0
    private val output = StringBuilder()

    override fun defaultVisit(node: LatexNode): String {
        return ""
    }

    private fun printIndent() {
        output.append("  ".repeat(indent))
    }

    override fun visitDocument(node: LatexNode.Document): String {
        output.append("Document\n")
        indent++
        node.children.forEach {
            printIndent()
            visit(it)
            output.append("\n")
        }
        indent--
        return output.toString()
    }

    override fun visitText(node: LatexNode.Text): String {
        output.append("Text('${node.content}')")
        return ""
    }

    override fun visitFontSize(node: LatexNode.FontSize): String {
        output.append("FontSize(${node.sizeType})")
        if (node.content.isNotEmpty()) {
            output.append("\n")
            indent++
            node.content.forEach {
                printIndent()
                visit(it)
                output.append("\n")
            }
            indent--
        }
        return ""
    }

    override fun visitFraction(node: LatexNode.Fraction): String {
        output.append("Fraction")
        if (node.style != LatexNode.Fraction.FractionStyle.NORMAL) {
            output.append("(style=${node.style})")
        }
        output.append("\n")
        indent++
        printIndent()
        output.append("numerator: ")
        visit(node.numerator)
        output.append("\n")
        printIndent()
        output.append("denominator: ")
        visit(node.denominator)
        indent--
        return ""
    }

    override fun visitRoot(node: LatexNode.Root): String {
        output.append("Root")
        if (node.index != null) {
            output.append("\n")
            indent++
            printIndent()
            output.append("index: ")
            visit(node.index)
            output.append("\n")
            printIndent()
            output.append("content: ")
            visit(node.content)
            indent--
        } else {
            output.append("(")
            visit(node.content)
            output.append(")")
        }
        return ""
    }

    override fun visitSymbol(node: LatexNode.Symbol): String {
        output.append("Symbol(${node.symbol} → ${node.unicode})")
        return ""
    }

    override fun visitBigOperator(node: LatexNode.BigOperator): String {
        output.append("BigOperator(${node.operator})")
        if (node.subscript != null || node.superscript != null) {
            output.append("\n")
            indent++
            if (node.subscript != null) {
                printIndent()
                output.append("subscript: ")
                visit(node.subscript)
                output.append("\n")
            }
            if (node.superscript != null) {
                printIndent()
                output.append("superscript: ")
                visit(node.superscript)
            }
            indent--
        }
        return ""
    }

    override fun visitMatrix(node: LatexNode.Matrix): String {
        output.append(
            "Matrix(${node.type}${if (node.isSmall) ", small" else ""}" +
                "${node.alignment?.let { ", alignment=$it" } ?: ""})\n"
        )
        indent++
        node.rows.forEachIndexed { i, row ->
            printIndent()
            output.append("row $i: [")
            row.forEachIndexed { j, cell ->
                if (j > 0) output.append(", ")
                visit(cell)
            }
            output.append("]\n")
        }
        indent--
        return ""
    }

    override fun visitArray(node: LatexNode.Array): String {
        output.append("Array(alignment=${node.alignment})\n")
        indent++
        node.rows.forEachIndexed { i, row ->
            printIndent()
            output.append("row $i: [")
            row.forEachIndexed { j, cell ->
                if (j > 0) output.append(", ")
                visit(cell)
            }
            output.append("]\n")
        }
        indent--
        return ""
    }

    override fun visitHLine(node: LatexNode.HLine): String {
        output.append("HLine")
        return ""
    }

    override fun visitCLine(node: LatexNode.CLine): String {
        output.append("CLine(${node.startCol}-${node.endCol})")
        return ""
    }

    override fun visitMulticolumn(node: LatexNode.Multicolumn): String {
        output.append("Multicolumn(${node.columnCount}, ${node.alignment})")
        indent++
        output.append("\n")
        printIndent()
        output.append("content: ")
        node.content.forEachIndexed { i, child ->
            if (i > 0) output.append(", ")
            visit(child)
        }
        indent--
        return ""
    }

    override fun visitEnclose(node: LatexNode.Enclose): String {
        val notationText = node.notations.joinToString(",") { it.mathMlName }
        output.append("Enclose($notationText)")
        if (node.attributes.isNotEmpty()) {
            output.append("(attributes=${node.attributes})")
        }
        output.append("\n")
        indent++
        printIndent()
        output.append("content: ")
        node.content.forEachIndexed { i, child ->
            if (i > 0) output.append(", ")
            visit(child)
        }
        indent--
        return ""
    }

    override fun visitSpace(node: LatexNode.Space): String {
        output.append("Space(${node.type})")
        return ""
    }

    override fun visitHSpace(node: LatexNode.HSpace): String {
        output.append("HSpace(${node.dimension})")
        return ""
    }

    override fun visitGroup(node: LatexNode.Group): String {
        output.append("Group(")
        node.children.forEachIndexed { i, child ->
            if (i > 0) output.append(", ")
            visit(child)
        }
        output.append(")")
        return ""
    }

    override fun visitError(node: LatexNode.Error): String {
        output.append("Error('${node.message}')")
        if (node.recovered.isNotEmpty()) {
            output.append("\n")
            indent++
            printIndent()
            output.append("recovered: ")
            node.recovered.forEachIndexed { i, child ->
                if (i > 0) output.append(", ")
                visit(child)
            }
            indent--
        }
        return ""
    }

    override fun visitPrescript(node: LatexNode.Prescript): String {
        output.append("Prescript\n")
        indent++
        if (node.preSuperscript != null) {
            printIndent()
            output.append("preSup: ")
            visit(node.preSuperscript)
            output.append("\n")
        }
        if (node.preSubscript != null) {
            printIndent()
            output.append("preSub: ")
            visit(node.preSubscript)
            output.append("\n")
        }
        printIndent()
        output.append("base: ")
        visit(node.base)
        indent--
        return ""
    }

    override fun visitMathLap(node: LatexNode.MathLap): String {
        output.append("MathLap(${node.lapType})\n")
        indent++
        printIndent()
        output.append("content: ")
        node.content.forEachIndexed { i, child ->
            if (i > 0) output.append(", ")
            visit(child)
        }
        indent--
        return ""
    }

    override fun visitLayout(node: LatexNode.Layout): String {
        output.append("Layout(${node.layoutType}")
        node.width?.let { output.append(", width=$it") }
        node.height?.let { output.append(", height=$it") }
        node.shift?.let { output.append(", shift=$it") }
        output.append(")")
        if (node.content.isNotEmpty()) {
            output.append("\n")
            indent++
            node.content.forEach { child ->
                printIndent()
                visit(child)
            }
            indent--
        }
        return ""
    }

    override fun visitNewEnvironment(node: LatexNode.NewEnvironment): String {
        output.append("NewEnvironment(${node.envName}[${node.numArgs}])")
        return ""
    }

    override fun visitSectionHeading(node: LatexNode.SectionHeading): String {
        output.append("SectionHeading(${node.level}${if (node.starred) "*" else ""})\n")
        indent++
        printIndent()
        output.append("content: ")
        node.content.forEachIndexed { i, child ->
            if (i > 0) output.append(", ")
            visit(child)
        }
        indent--
        return ""
    }

    fun print(node: LatexNode): String {
        output.clear()
        indent = 0
        visit(node)
        return output.toString()
    }
}
