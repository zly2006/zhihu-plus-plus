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

/**
 * LaTeX AST 访问者接口
 * 用于遍历和处理 LaTeX 语法树
 */
interface LatexVisitor<T> {
    fun visitDocument(node: LatexNode.Document): T
    fun visitText(node: LatexNode.Text): T
    fun visitCommand(node: LatexNode.Command): T
    fun visitEnvironment(node: LatexNode.Environment): T
    fun visitGroup(node: LatexNode.Group): T
    fun visitSuperscript(node: LatexNode.Superscript): T
    fun visitSubscript(node: LatexNode.Subscript): T
    fun visitFraction(node: LatexNode.Fraction): T
    fun visitRoot(node: LatexNode.Root): T
    fun visitMatrix(node: LatexNode.Matrix): T
    fun visitArray(node: LatexNode.Array): T
    fun visitSpace(node: LatexNode.Space): T
    fun visitHSpace(node: LatexNode.HSpace): T
    fun visitNewLine(node: LatexNode.NewLine): T
    fun visitSymbol(node: LatexNode.Symbol): T
    fun visitOperator(node: LatexNode.Operator): T
    fun visitDelimited(node: LatexNode.Delimited): T
    fun visitManualSizedDelimiter(node: LatexNode.ManualSizedDelimiter): T
    fun visitAccent(node: LatexNode.Accent): T
    fun visitExtensibleArrow(node: LatexNode.ExtensibleArrow): T
    fun visitStack(node: LatexNode.Stack): T
    fun visitStyle(node: LatexNode.Style): T
    fun visitColor(node: LatexNode.Color): T
    fun visitMathStyle(node: LatexNode.MathStyle): T
    fun visitFontSize(node: LatexNode.FontSize): T
    fun visitBigOperator(node: LatexNode.BigOperator): T
    fun visitAligned(node: LatexNode.Aligned): T
    fun visitCases(node: LatexNode.Cases): T
    fun visitSplit(node: LatexNode.Split): T
    fun visitMultline(node: LatexNode.Multline): T
    fun visitEqnarray(node: LatexNode.Eqnarray): T
    fun visitSubequations(node: LatexNode.Subequations): T
    fun visitBinomial(node: LatexNode.Binomial): T
    fun visitTextMode(node: LatexNode.TextMode): T
    fun visitBoxed(node: LatexNode.Boxed): T
    fun visitEnclose(node: LatexNode.Enclose): T
    fun visitPhantom(node: LatexNode.Phantom): T
    fun visitNewCommand(node: LatexNode.NewCommand): T
    fun visitNegation(node: LatexNode.Negation): T
    fun visitTag(node: LatexNode.Tag): T
    fun visitSubstack(node: LatexNode.Substack): T
    fun visitSmash(node: LatexNode.Smash): T
    fun visitVPhantom(node: LatexNode.VPhantom): T
    fun visitHPhantom(node: LatexNode.HPhantom): T
    fun visitLabel(node: LatexNode.Label): T
    fun visitRef(node: LatexNode.Ref): T
    fun visitEqRef(node: LatexNode.EqRef): T
    fun visitSideSet(node: LatexNode.SideSet): T
    fun visitTensor(node: LatexNode.Tensor): T
    fun visitTabular(node: LatexNode.Tabular): T
    fun visitHLine(node: LatexNode.HLine): T
    fun visitCLine(node: LatexNode.CLine): T
    fun visitMulticolumn(node: LatexNode.Multicolumn): T
    fun visitOperatorName(node: LatexNode.OperatorName): T
    fun visitModOperator(node: LatexNode.ModOperator): T
    fun visitInlineMath(node: LatexNode.InlineMath): T
    fun visitDisplayMath(node: LatexNode.DisplayMath): T
    fun visitError(node: LatexNode.Error): T
    fun visitHyperlink(node: LatexNode.Hyperlink): T
    fun visitColorBox(node: LatexNode.ColorBox): T
    fun visitPrescript(node: LatexNode.Prescript): T
    fun visitMathLap(node: LatexNode.MathLap): T
    fun visitLayout(node: LatexNode.Layout): T
    fun visitNewEnvironment(node: LatexNode.NewEnvironment): T
    fun visitSectionHeading(node: LatexNode.SectionHeading): T
    fun visitTextDirection(node: LatexNode.TextDirection): T
}

/**
 * 默认访问者实现，提供默认行为
 *
 * 每个 visit 方法手动遍历子节点后调用 [defaultVisit]。
 * 适合需要对每个节点类型做精细化处理的场景。
 *
 * 如果只需按类别处理（叶节点 vs 容器节点），推荐使用 [SimpleLatexVisitor]。
 */
abstract class BaseLatexVisitor<T> : LatexVisitor<T> {

    protected abstract fun defaultVisit(node: LatexNode): T

    override fun visitDocument(node: LatexNode.Document): T {
        node.children.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitText(node: LatexNode.Text): T = defaultVisit(node)

    override fun visitCommand(node: LatexNode.Command): T {
        node.arguments.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitEnvironment(node: LatexNode.Environment): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitGroup(node: LatexNode.Group): T {
        node.children.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitSuperscript(node: LatexNode.Superscript): T {
        visit(node.base)
        visit(node.exponent)
        return defaultVisit(node)
    }

    override fun visitSubscript(node: LatexNode.Subscript): T {
        visit(node.base)
        visit(node.index)
        return defaultVisit(node)
    }

    override fun visitFraction(node: LatexNode.Fraction): T {
        visit(node.numerator)
        visit(node.denominator)
        return defaultVisit(node)
    }

    override fun visitRoot(node: LatexNode.Root): T {
        visit(node.content)
        node.index?.let { visit(it) }
        return defaultVisit(node)
    }

    override fun visitMatrix(node: LatexNode.Matrix): T {
        node.rows.forEach { row ->
            row.forEach { cell -> visit(cell) }
        }
        return defaultVisit(node)
    }

    override fun visitArray(node: LatexNode.Array): T {
        node.rows.forEach { row ->
            row.forEach { cell -> visit(cell) }
        }
        return defaultVisit(node)
    }

    override fun visitSpace(node: LatexNode.Space): T = defaultVisit(node)

    override fun visitHSpace(node: LatexNode.HSpace): T = defaultVisit(node)

    override fun visitNewLine(node: LatexNode.NewLine): T = defaultVisit(node)

    override fun visitSymbol(node: LatexNode.Symbol): T = defaultVisit(node)

    override fun visitOperator(node: LatexNode.Operator): T = defaultVisit(node)

    override fun visitDelimited(node: LatexNode.Delimited): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitManualSizedDelimiter(node: LatexNode.ManualSizedDelimiter): T = defaultVisit(node)

    override fun visitAccent(node: LatexNode.Accent): T {
        visit(node.content)
        return defaultVisit(node)
    }

    override fun visitExtensibleArrow(node: LatexNode.ExtensibleArrow): T {
        visit(node.content)
        node.below?.let { visit(it) }
        return defaultVisit(node)
    }

    override fun visitStack(node: LatexNode.Stack): T {
        visit(node.base)
        node.above?.let { visit(it) }
        node.below?.let { visit(it) }
        return defaultVisit(node)
    }

    override fun visitStyle(node: LatexNode.Style): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitColor(node: LatexNode.Color): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitMathStyle(node: LatexNode.MathStyle): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitFontSize(node: LatexNode.FontSize): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitBigOperator(node: LatexNode.BigOperator): T {
        node.subscript?.let { visit(it) }
        node.superscript?.let { visit(it) }
        return defaultVisit(node)
    }

    override fun visitAligned(node: LatexNode.Aligned): T {
        node.rows.forEach { row ->
            row.forEach { cell -> visit(cell) }
        }
        return defaultVisit(node)
    }

    override fun visitCases(node: LatexNode.Cases): T {
        node.cases.forEach { (expr, cond) ->
            visit(expr)
            visit(cond)
        }
        return defaultVisit(node)
    }

    override fun visitSplit(node: LatexNode.Split): T {
        node.rows.forEach { row ->
            row.forEach { cell -> visit(cell) }
        }
        return defaultVisit(node)
    }

    override fun visitMultline(node: LatexNode.Multline): T {
        node.lines.forEach { line -> visit(line) }
        return defaultVisit(node)
    }

    override fun visitEqnarray(node: LatexNode.Eqnarray): T {
        node.rows.forEach { row ->
            row.forEach { cell -> visit(cell) }
        }
        return defaultVisit(node)
    }

    override fun visitSubequations(node: LatexNode.Subequations): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitBinomial(node: LatexNode.Binomial): T {
        visit(node.top)
        visit(node.bottom)
        return defaultVisit(node)
    }

    override fun visitTextMode(node: LatexNode.TextMode): T = defaultVisit(node)

    override fun visitBoxed(node: LatexNode.Boxed): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitEnclose(node: LatexNode.Enclose): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitPhantom(node: LatexNode.Phantom): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitNewCommand(node: LatexNode.NewCommand): T {
        return defaultVisit(node)
    }

    override fun visitNegation(node: LatexNode.Negation): T {
        visit(node.content)
        return defaultVisit(node)
    }

    override fun visitTag(node: LatexNode.Tag): T {
        visit(node.label)
        return defaultVisit(node)
    }

    override fun visitSubstack(node: LatexNode.Substack): T {
        node.rows.forEach { row ->
            row.forEach { cell -> visit(cell) }
        }
        return defaultVisit(node)
    }

    override fun visitSmash(node: LatexNode.Smash): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitVPhantom(node: LatexNode.VPhantom): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitHPhantom(node: LatexNode.HPhantom): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitLabel(node: LatexNode.Label): T = defaultVisit(node)

    override fun visitRef(node: LatexNode.Ref): T = defaultVisit(node)

    override fun visitEqRef(node: LatexNode.EqRef): T = defaultVisit(node)

    override fun visitSideSet(node: LatexNode.SideSet): T {
        node.leftSub?.let { visit(it) }
        node.leftSup?.let { visit(it) }
        node.rightSub?.let { visit(it) }
        node.rightSup?.let { visit(it) }
        visit(node.base)
        return defaultVisit(node)
    }

    override fun visitTensor(node: LatexNode.Tensor): T {
        visit(node.base)
        node.indices.forEach { (_, indexNode) -> visit(indexNode) }
        return defaultVisit(node)
    }

    override fun visitTabular(node: LatexNode.Tabular): T {
        node.rows.forEach { row ->
            row.forEach { cell -> visit(cell) }
        }
        return defaultVisit(node)
    }

    override fun visitHLine(node: LatexNode.HLine): T = defaultVisit(node)

    override fun visitCLine(node: LatexNode.CLine): T = defaultVisit(node)

    override fun visitMulticolumn(node: LatexNode.Multicolumn): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitOperatorName(node: LatexNode.OperatorName): T = defaultVisit(node)

    override fun visitModOperator(node: LatexNode.ModOperator): T {
        node.content?.let { visit(it) }
        return defaultVisit(node)
    }

    override fun visitInlineMath(node: LatexNode.InlineMath): T {
        node.children.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitDisplayMath(node: LatexNode.DisplayMath): T {
        node.children.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitError(node: LatexNode.Error): T {
        node.recovered.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitHyperlink(node: LatexNode.Hyperlink): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitColorBox(node: LatexNode.ColorBox): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitPrescript(node: LatexNode.Prescript): T {
        node.preSuperscript?.let { visit(it) }
        node.preSubscript?.let { visit(it) }
        visit(node.base)
        return defaultVisit(node)
    }

    override fun visitMathLap(node: LatexNode.MathLap): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitLayout(node: LatexNode.Layout): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitNewEnvironment(node: LatexNode.NewEnvironment): T {
        return defaultVisit(node)
    }

    override fun visitSectionHeading(node: LatexNode.SectionHeading): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    override fun visitTextDirection(node: LatexNode.TextDirection): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }

    /**
     * 访问任意节点 — 通过双分派委托到 [LatexNode.accept]
     */
    fun visit(node: LatexNode): T = node.accept(this)
}

/**
 * 简化版访问者基类 — 利用 [LatexNode.children] 自动遍历。
 *
 * 适合不需要按节点类型做精细化处理的场景（如统计、搜索、通用变换）。
 * 新增节点类型时，只需在 [LatexNode] 中正确实现 [LatexNode.children]，
 * [SimpleLatexVisitor] 无需任何修改即可正确遍历。
 *
 * 子类只需实现：
 * - [defaultVisit]：每个节点的默认处理逻辑
 *
 * 所有 visit 方法默认行为：先递归遍历 [LatexNode.children]，再调用 [defaultVisit]。
 * 可选择性覆盖特定 visit 方法以定制特定节点的行为。
 */
abstract class SimpleLatexVisitor<T> : LatexVisitor<T> {

    protected abstract fun defaultVisit(node: LatexNode): T

    /**
     * 遍历节点所有子节点后调用 [defaultVisit]
     */
    protected open fun visitChildren(node: LatexNode): T {
        node.children().forEach { visit(it) }
        return defaultVisit(node)
    }

    /**
     * 访问任意节点 — 通过双分派委托到 [LatexNode.accept]
     */
    fun visit(node: LatexNode): T = node.accept(this)

    // 所有 visit 方法默认委托到 visitChildren（自动遍历 node.children()）
    override fun visitDocument(node: LatexNode.Document): T = visitChildren(node)
    override fun visitText(node: LatexNode.Text): T = visitChildren(node)
    override fun visitCommand(node: LatexNode.Command): T = visitChildren(node)
    override fun visitEnvironment(node: LatexNode.Environment): T = visitChildren(node)
    override fun visitGroup(node: LatexNode.Group): T = visitChildren(node)
    override fun visitSuperscript(node: LatexNode.Superscript): T = visitChildren(node)
    override fun visitSubscript(node: LatexNode.Subscript): T = visitChildren(node)
    override fun visitFraction(node: LatexNode.Fraction): T = visitChildren(node)
    override fun visitRoot(node: LatexNode.Root): T = visitChildren(node)
    override fun visitMatrix(node: LatexNode.Matrix): T = visitChildren(node)
    override fun visitArray(node: LatexNode.Array): T = visitChildren(node)
    override fun visitSpace(node: LatexNode.Space): T = visitChildren(node)
    override fun visitHSpace(node: LatexNode.HSpace): T = visitChildren(node)
    override fun visitNewLine(node: LatexNode.NewLine): T = visitChildren(node)
    override fun visitSymbol(node: LatexNode.Symbol): T = visitChildren(node)
    override fun visitOperator(node: LatexNode.Operator): T = visitChildren(node)
    override fun visitDelimited(node: LatexNode.Delimited): T = visitChildren(node)
    override fun visitManualSizedDelimiter(node: LatexNode.ManualSizedDelimiter): T = visitChildren(node)
    override fun visitAccent(node: LatexNode.Accent): T = visitChildren(node)
    override fun visitExtensibleArrow(node: LatexNode.ExtensibleArrow): T = visitChildren(node)
    override fun visitStack(node: LatexNode.Stack): T = visitChildren(node)
    override fun visitStyle(node: LatexNode.Style): T = visitChildren(node)
    override fun visitColor(node: LatexNode.Color): T = visitChildren(node)
    override fun visitMathStyle(node: LatexNode.MathStyle): T = visitChildren(node)
    override fun visitFontSize(node: LatexNode.FontSize): T = visitChildren(node)
    override fun visitBigOperator(node: LatexNode.BigOperator): T = visitChildren(node)
    override fun visitAligned(node: LatexNode.Aligned): T = visitChildren(node)
    override fun visitCases(node: LatexNode.Cases): T = visitChildren(node)
    override fun visitSplit(node: LatexNode.Split): T = visitChildren(node)
    override fun visitMultline(node: LatexNode.Multline): T = visitChildren(node)
    override fun visitEqnarray(node: LatexNode.Eqnarray): T = visitChildren(node)
    override fun visitSubequations(node: LatexNode.Subequations): T = visitChildren(node)
    override fun visitBinomial(node: LatexNode.Binomial): T = visitChildren(node)
    override fun visitTextMode(node: LatexNode.TextMode): T = visitChildren(node)
    override fun visitBoxed(node: LatexNode.Boxed): T = visitChildren(node)
    override fun visitEnclose(node: LatexNode.Enclose): T = visitChildren(node)
    override fun visitPhantom(node: LatexNode.Phantom): T = visitChildren(node)
    override fun visitNewCommand(node: LatexNode.NewCommand): T = visitChildren(node)
    override fun visitNegation(node: LatexNode.Negation): T = visitChildren(node)
    override fun visitTag(node: LatexNode.Tag): T = visitChildren(node)
    override fun visitSubstack(node: LatexNode.Substack): T = visitChildren(node)
    override fun visitSmash(node: LatexNode.Smash): T = visitChildren(node)
    override fun visitVPhantom(node: LatexNode.VPhantom): T = visitChildren(node)
    override fun visitHPhantom(node: LatexNode.HPhantom): T = visitChildren(node)
    override fun visitLabel(node: LatexNode.Label): T = visitChildren(node)
    override fun visitRef(node: LatexNode.Ref): T = visitChildren(node)
    override fun visitEqRef(node: LatexNode.EqRef): T = visitChildren(node)
    override fun visitSideSet(node: LatexNode.SideSet): T = visitChildren(node)
    override fun visitTensor(node: LatexNode.Tensor): T = visitChildren(node)
    override fun visitTabular(node: LatexNode.Tabular): T = visitChildren(node)
    override fun visitHLine(node: LatexNode.HLine): T = visitChildren(node)
    override fun visitCLine(node: LatexNode.CLine): T = visitChildren(node)
    override fun visitMulticolumn(node: LatexNode.Multicolumn): T = visitChildren(node)
    override fun visitOperatorName(node: LatexNode.OperatorName): T = visitChildren(node)
    override fun visitModOperator(node: LatexNode.ModOperator): T = visitChildren(node)
    override fun visitInlineMath(node: LatexNode.InlineMath): T = visitChildren(node)
    override fun visitDisplayMath(node: LatexNode.DisplayMath): T = visitChildren(node)
    override fun visitError(node: LatexNode.Error): T = visitChildren(node)
    override fun visitHyperlink(node: LatexNode.Hyperlink): T = visitChildren(node)
    override fun visitColorBox(node: LatexNode.ColorBox): T = visitChildren(node)
    override fun visitPrescript(node: LatexNode.Prescript): T = visitChildren(node)
    override fun visitMathLap(node: LatexNode.MathLap): T = visitChildren(node)
    override fun visitLayout(node: LatexNode.Layout): T = visitChildren(node)
    override fun visitNewEnvironment(node: LatexNode.NewEnvironment): T = visitChildren(node)
    override fun visitSectionHeading(node: LatexNode.SectionHeading): T = visitChildren(node)
    override fun visitTextDirection(node: LatexNode.TextDirection): T = visitChildren(node)
}
