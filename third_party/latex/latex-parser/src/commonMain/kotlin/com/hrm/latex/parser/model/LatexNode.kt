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


package com.hrm.latex.parser.model

import com.hrm.latex.parser.visitor.LatexVisitor
import kotlin.js.JsName

/**
 * LaTeX 抽象语法树节点
 *
 * 每个节点可选地携带 [sourceRange]，表示该节点在原始 LaTeX 输入字符串中的位置范围。
 * 默认为 null，表示位置未知（例如宏展开生成的节点）。
 *
 * ## 自描述协议
 * 每个节点实现四个自描述方法，消除外部对节点结构的重复 when 表达式：
 * - [children]：返回所有直接子节点（有序列表）
 * - [withSourceRange]：创建带有新 sourceRange 的副本
 * - [withChildren]：用新子节点列表重建自身（结构不变，子节点替换）
 * - [accept]：双分派 Visitor 调用，消除 Visitor 基类中的 when 表达式
 */
sealed class LatexNode {
    abstract val sourceRange: SourceRange?

    /** 返回所有直接子节点（有序）。叶节点返回 emptyList()。 */
    @JsName("getChildren")
    abstract fun children(): List<LatexNode>

    /** 用新 sourceRange 创建副本 */
    abstract fun withSourceRange(range: SourceRange): LatexNode

    /**
     * 用新子节点列表重建自身。
     * 子节点的顺序和数量必须与 [children] 返回值一致。
     * 叶节点返回自身。
     */
    abstract fun withChildren(newChildren: List<LatexNode>): LatexNode

    /** 双分派：将访问者分派到对应的 visit 方法 */
    abstract fun <T> accept(visitor: LatexVisitor<T>): T

    // ====================================================================
    // 函数式遍历 API
    // ====================================================================

    /**
     * 深度优先折叠：对自身及所有后代节点应用 [combine]
     *
     * 用法示例：
     * ```kotlin
     * val nodeCount = doc.fold(0) { acc, _ -> acc + 1 }
     * ```
     */
    fun <T> fold(initial: T, combine: (T, LatexNode) -> T): T {
        var acc = combine(initial, this)
        for (child in children()) {
            acc = child.fold(acc, combine)
        }
        return acc
    }

    /**
     * 深度优先映射：对每个节点应用 [transform]，自底向上重建树
     *
     * 用法示例：
     * ```kotlin
     * val cleaned = doc.mapNodes { if (it is LatexNode.Space) LatexNode.Text(" ") else it }
     * ```
     */
    fun mapNodes(transform: (LatexNode) -> LatexNode): LatexNode {
        val mappedChildren = children().map { it.mapNodes(transform) }
        val rebuilt = if (mappedChildren.isEmpty()) this else withChildren(mappedChildren)
        return transform(rebuilt)
    }

    // ====================================================================
    // 行列结构辅助
    // ====================================================================

    companion object {
        /**
         * 按原始行结构重建 rows 的通用实现。
         * 供 Matrix/Array/Aligned/Split/Eqnarray/Tabular/Substack 共用。
         */
        fun rebuildRows(
            originalRows: List<List<LatexNode>>,
            newChildren: List<LatexNode>
        ): List<List<LatexNode>> {
            val result = mutableListOf<List<LatexNode>>()
            var idx = 0
            for (row in originalRows) {
                result.add(List(row.size) { newChildren[idx++] })
            }
            return result
        }
    }

    // ====================================================================
    // 节点类型
    // ====================================================================

    data class Document(
        val children: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = children
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(children = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitDocument(this)
    }

    /**
     * 文本节点
     */
    data class Text(
        val content: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitText(this)
    }

    /**
     * 命令节点（如 \frac, \sqrt 等）
     */
    data class Command(
        val name: String,
        val arguments: List<LatexNode> = emptyList(),
        val options: List<String> = emptyList(),
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = arguments
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(arguments = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitCommand(this)
    }

    /**
     * 环境节点（如 \begin{equation}...\end{equation}）
     */
    data class Environment(
        val name: String,
        val content: List<LatexNode>,
        val options: List<String> = emptyList(),
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitEnvironment(this)
    }

    /**
     * 分组节点（花括号包围的内容）
     */
    data class Group(
        val children: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = children
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(children = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitGroup(this)
    }

    /**
     * 上标节点（^）
     */
    data class Superscript(
        val base: LatexNode,
        val exponent: LatexNode,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOf(base, exponent)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(base = newChildren[0], exponent = newChildren[1])

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitSuperscript(this)
    }

    /**
     * 下标节点（_）
     */
    data class Subscript(
        val base: LatexNode,
        val index: LatexNode,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOf(base, index)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(base = newChildren[0], index = newChildren[1])

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitSubscript(this)
    }

    /**
     * 分数节点
     */
    data class Fraction(
        val numerator: LatexNode,
        val denominator: LatexNode,
        val style: FractionStyle = FractionStyle.NORMAL,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class FractionStyle {
            NORMAL, DISPLAY, TEXT, CONTINUED, RULELESS
        }

        override fun children() = listOf(numerator, denominator)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(numerator = newChildren[0], denominator = newChildren[1])

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitFraction(this)
    }

    /**
     * 根号节点
     */
    data class Root(
        val content: LatexNode,
        val index: LatexNode? = null,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOfNotNull(content, index)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newContent = newChildren[0]
            val newIndex = if (index != null) newChildren.getOrNull(1) else null
            return copy(content = newContent, index = newIndex)
        }

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitRoot(this)
    }

    /**
     * 矩阵节点
     */
    data class RowGap(
        val number: Double,
        val unit: String
    )

    data class Matrix(
        val rows: List<List<LatexNode>>,
        val type: MatrixType = MatrixType.PLAIN,
        val isSmall: Boolean = false,
        override val sourceRange: SourceRange? = null,
        val rowGaps: List<RowGap?> = emptyList(),
        /** Optional alignment from mathtools starred matrix environments. */
        val alignment: String? = null
    ) : LatexNode() {
        enum class MatrixType {
            PLAIN, PAREN, BRACKET, BRACE, VBAR, DOUBLE_VBAR
        }

        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(rows = rebuildRows(rows, newChildren))

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitMatrix(this)
    }

    /**
     * 数组节点（array环境，更通用的表格）
     */
    data class Array(
        val rows: List<List<LatexNode>>,
        val alignment: String,
        override val sourceRange: SourceRange? = null,
        val rowGaps: List<RowGap?> = emptyList()
    ) : LatexNode() {
        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(rows = rebuildRows(rows, newChildren))

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitArray(this)
    }

    /**
     * 空格节点
     */
    data class Space(
        val type: SpaceType,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class SpaceType {
            THIN, MEDIUM, THICK, QUAD, QQUAD, NORMAL, NEGATIVE_THIN
        }

        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitSpace(this)
    }

    /**
     * 自定义水平空格节点 (\hspace)
     */
    data class HSpace(
        val dimension: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitHSpace(this)
    }

    /**
     * 换行节点
     */
    data class NewLine(
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitNewLine(this)
    }

    /**
     * 特殊符号节点（包括希腊字母、运算符符号、数学符号等）
     * 例如：α, β, ×, ÷, ≤, →
     */
    data class Symbol(
        val symbol: String,
        val unicode: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitSymbol(this)
    }

    /**
     * 数学运算符节点
     * 注意：当前解析器将运算符符号（如 \times, \div）解析为 Symbol
     * 此类型保留用于未来可能的语义区分需求
     */
    data class Operator(
        val op: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitOperator(this)
    }

    /**
     * 括号节点（自动伸缩）
     *
     * 使用场景：
     * 1. 自动伸缩：`\left( ... \right)` - scalable=true
     * 2. 不对称分隔符：`\left. ... \right|` - left="" 或 right=""
     *
     * @param left 左分隔符（空字符串表示不显示）
     * @param right 右分隔符（空字符串表示不显示）
     * @param content 括号内的内容
     */
    data class Delimited(
        val left: String,
        val right: String,
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitDelimited(this)
    }

    /**
     * 手动大小分隔符节点
     *
     * 与 Delimited 不同，这是一个独立的符号，不包裹内容
     *
     * 使用场景：
     * - `\big(` - 生成一个 1.2x 大小的左括号符号
     * - `\Big[` - 生成一个 1.8x 大小的左方括号符号
     * - `\bigg\{` - 生成一个 2.4x 大小的左花括号符号
     *
     * @param delimiter 分隔符符号（如 "(", "[", "|" 等）
     * @param size 缩放因子（1.2f, 1.8f, 2.4f, 3.0f）
     */
    data class ManualSizedDelimiter(
        val delimiter: String,
        val size: Float,
        /** True for `\middle`; the renderer matches it to the surrounding pair. */
        val isMiddle: Boolean = false,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitManualSizedDelimiter(this)
    }

    /**
     * 装饰节点（如上划线、下划线、箭头等）
     */
    data class Accent(
        val content: LatexNode,
        val accentType: AccentType,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class AccentType {
            HAT, TILDE, BAR, DOT, DDOT, DDDOT, VEC, OVERLINE, UNDERLINE, OVERBRACE, UNDERBRACE,
            WIDEHAT, OVERRIGHTARROW, OVERLEFTARROW, CANCEL, BCANCEL, XCANCEL,
            GRAVE, ACUTE, CHECK, BREVE, RING,
            OVERBRACKET, UNDERBRACKET,
            WIDECHECK, OVERLEFTRIGHTARROW, UNDERLEFTARROW, UNDERRIGHTARROW,
            OVERPAREN, UNDERPAREN
        }

        override fun children() = listOf(content)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren[0])
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitAccent(this)
    }

    /**
     * 可扩展箭头节点（箭头上方或下方可显示文字）
     */
    data class ExtensibleArrow(
        val content: LatexNode,
        val below: LatexNode?,
        val direction: Direction,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class Direction {
            RIGHT, LEFT, BOTH, HOOK_RIGHT, HOOK_LEFT,
            RIGHT_DOUBLE, LEFT_DOUBLE, BOTH_DOUBLE, MAPSTO, EQUAL
        }

        override fun children() = listOfNotNull(content, below)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newContent = newChildren[0]
            val newBelow = if (below != null) newChildren.getOrNull(1) else null
            return copy(content = newContent, below = newBelow)
        }

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitExtensibleArrow(this)
    }

    /**
     * 堆叠节点（在基础内容上方或下方添加内容）
     */
    data class Stack(
        val base: LatexNode,
        val above: LatexNode?,
        val below: LatexNode?,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOfNotNull(base, above, below)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            var idx = 0
            val newBase = newChildren[idx++]
            val newAbove = if (above != null) newChildren.getOrNull(idx++) else null
            val newBelow = if (below != null) newChildren.getOrNull(idx) else null
            return copy(base = newBase, above = newAbove, below = newBelow)
        }

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitStack(this)
    }

    /**
     * 字体样式节点
     */
    data class Style(
        val content: List<LatexNode>,
        val styleType: StyleType,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class StyleType {
            BOLD, BOLD_SYMBOL, ITALIC, ROMAN, SANS_SERIF, MONOSPACE,
            BLACKBOARD_BOLD, FRAKTUR, SCRIPT, CALLIGRAPHIC
        }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitStyle(this)
    }

    /**
     * 颜色节点
     */
    data class Color(
        val content: List<LatexNode>,
        val color: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitColor(this)
    }

    /**
     * 数学模式节点（控制公式大小）
     *
     * @property content 内容
     * @property mathStyleType 数学模式类型
     */
    data class MathStyle(
        val content: List<LatexNode>,
        val mathStyleType: MathStyleType,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class MathStyleType {
            DISPLAY, TEXT, SCRIPT, SCRIPT_SCRIPT
        }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitMathStyle(this)
    }

    /**
     * 字号节点
     *
     * @property content 内容
     * @property sizeType 字号类型
     */
    data class FontSize(
        val content: List<LatexNode>,
        val sizeType: SizeType,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class SizeType {
            TINY, SCRIPT_SIZE, FOOTNOTE_SIZE, SMALL, NORMAL_SIZE,
            LARGE, LARGE_2, LARGE_3, HUGE, HUGE_2
        }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitFontSize(this)
    }

    /**
     * 大型运算符（求和、积分、乘积等）
     */
    data class BigOperator(
        val operator: String,
        val subscript: LatexNode? = null,
        val superscript: LatexNode? = null,
        val limitsMode: LimitsMode = LimitsMode.AUTO,
        /** Whether AUTO mode places limits above/below in display style. */
        val limitsInDisplay: Boolean = true,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class LimitsMode { AUTO, LIMITS, NOLIMITS }

        override fun children() = listOfNotNull(subscript, superscript)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            var idx = 0
            val newSub = if (subscript != null) newChildren.getOrNull(idx++) else null
            val newSup = if (superscript != null) newChildren.getOrNull(idx) else null
            return copy(subscript = newSub, superscript = newSup)
        }

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitBigOperator(this)
    }

    /**
     * 对齐环境
     * @param envName 原始环境名（如 "align", "align*", "aligned", "gather" 等），
     *   用于公式编号自动计算时区分星号和非星号变体
     */
    data class Aligned(
        val rows: List<List<LatexNode>>,
        val alignType: AlignType = AlignType.CENTER,
        val envName: String? = null,
        override val sourceRange: SourceRange? = null,
        val rowGaps: List<RowGap?> = emptyList()
    ) : LatexNode() {
        enum class AlignType { LEFT, CENTER, RIGHT }

        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(rows = rebuildRows(rows, newChildren))

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitAligned(this)
    }

    /**
     * 案例环境（cases / dcases / rcases）
     * @param cases 每个 case 是 (表达式, 条件) 对
     * @param style 变体风格：NORMAL=cases, DISPLAY=dcases(displaystyle), RIGHT=rcases(右花括号)
     */
    data class Cases(
        val cases: List<Pair<LatexNode, LatexNode>>,
        val style: CasesStyle = CasesStyle.NORMAL,
        override val sourceRange: SourceRange? = null,
        val rowGaps: List<RowGap?> = emptyList()
    ) : LatexNode() {
        enum class CasesStyle { NORMAL, DISPLAY, RIGHT }

        override fun children() = cases.flatMap { listOf(it.first, it.second) }
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newCases = mutableListOf<Pair<LatexNode, LatexNode>>()
            var idx = 0
            for (_case in cases) {
                newCases.add(newChildren[idx++] to newChildren[idx++])
            }
            return copy(cases = newCases)
        }

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitCases(this)
    }

    /**
     * Split 环境（用于单个方程内的多行分割）
     */
    data class Split(
        val rows: List<List<LatexNode>>,
        override val sourceRange: SourceRange? = null,
        val rowGaps: List<RowGap?> = emptyList()
    ) : LatexNode() {
        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(rows = rebuildRows(rows, newChildren))

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitSplit(this)
    }

    /**
     * Multline 环境（多行单个方程）
     * @param envName 原始环境名（如 "multline", "multline*"），
     *   用于公式编号自动计算时区分星号和非星号变体
     */
    data class Multline(
        val lines: List<LatexNode>,
        val envName: String? = null,
        override val sourceRange: SourceRange? = null,
        val rowGaps: List<RowGap?> = emptyList()
    ) : LatexNode() {
        override fun children() = lines
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(lines = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitMultline(this)
    }

    /**
     * Eqnarray 环境（旧式方程数组）
     * @param envName 原始环境名（如 "eqnarray", "eqnarray*"），
     *   用于公式编号自动计算时区分星号和非星号变体
     */
    data class Eqnarray(
        val rows: List<List<LatexNode>>,
        val envName: String? = null,
        override val sourceRange: SourceRange? = null,
        val rowGaps: List<RowGap?> = emptyList()
    ) : LatexNode() {
        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(rows = rebuildRows(rows, newChildren))

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitEqnarray(this)
    }

    /**
     * Subequations 环境（子方程编号）
     */
    data class Subequations(
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitSubequations(this)
    }

    /**
     * 二项式系数 \binom{n}{k}
     */
    data class Binomial(
        val top: LatexNode,
        val bottom: LatexNode,
        val style: BinomialStyle = BinomialStyle.NORMAL,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class BinomialStyle { NORMAL, TEXT, DISPLAY }

        override fun children() = listOf(top, bottom)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(top = newChildren[0], bottom = newChildren[1])

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitBinomial(this)
    }

    /**
     * 文本模式（在数学公式中插入普通文本）
     */
    data class TextMode(
        val text: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitTextMode(this)
    }

    /**
     * 方框节点（\boxed, \fbox）
     * 在内容周围绘制边框
     * @param content 内容
     * @param boxStyle 边框样式
     */
    data class Boxed(
        val content: List<LatexNode>,
        val boxStyle: BoxStyle = BoxStyle.NORMAL,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class BoxStyle { NORMAL, FBOX }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitBoxed(this)
    }

    /**
     * 通用围框/删除线节点（\enclose）
     * 对应 MathML <menclose>，用于统一表示边框、圆圈、删除线等装饰。
     *
     * @param content 被包围的内容
     * @param notations menclose notation 列表
     * @param attributes 可选属性（如 mathcolor/mathbackground）
     */
    data class Enclose(
        val content: List<LatexNode>,
        val notations: List<Notation>,
        val attributes: Map<String, String> = emptyMap(),
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        companion object {
            internal const val BBOX_ATTRIBUTE = "\u0000bbox"
        }

        enum class Notation(val mathMlName: String) {
            LONGDIV("longdiv"),
            ACTUARIAL("actuarial"),
            BOX("box"),
            ROUNDEDBOX("roundedbox"),
            CIRCLE("circle"),
            LEFT("left"),
            RIGHT("right"),
            TOP("top"),
            BOTTOM("bottom"),
            UPDIAGONALSTRIKE("updiagonalstrike"),
            DOWNDIAGONALSTRIKE("downdiagonalstrike"),
            VERTICALSTRIKE("verticalstrike"),
            HORIZONTALSTRIKE("horizontalstrike"),
            MADRUWB("madruwb"),
            UPDIAGONALARROW("updiagonalarrow"),
            PHASORANGLE("phasorangle");

            companion object {
                fun fromMathMlName(name: String): Notation? =
                    entries.firstOrNull { it.mathMlName == name.trim().lowercase() }
            }
        }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitEnclose(this)
    }

    /**
     * 幻影节点（\phantom）
     * 占据空间但不显示内容，用于对齐
     */
    data class Phantom(
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitPhantom(this)
    }

    /**
     * 自定义命令定义节点（\newcommand）
     * 该节点不参与渲染，仅用于记录命令定义
     * @param commandName 命令名（不含反斜杠）
     * @param numArgs 参数个数（0-9）
     * @param definition 命令定义（AST 节点列表）
     * @param defaultArg 可选参数的默认值（对应 \newcommand{\cmd}[2][default]{body} 中的 default）
     */
    data class NewCommand(
        val commandName: String,
        val numArgs: Int,
        val definition: List<LatexNode>,
        val defaultArg: String? = null,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = definition
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(definition = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitNewCommand(this)
    }

    /**
     * 否定修饰节点（\not）
     * 在关系符号上叠加斜线表示否定
     * 例如：\not= → ≠, \not\in → ∉
     */
    data class Negation(
        val content: LatexNode,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOf(content)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren[0])
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitNegation(this)
    }

    /**
     * 公式编号标签节点（\tag, \tag*）
     * @param label 标签内容
     * @param starred true = \tag*（不加括号），false = \tag（加括号）
     */
    data class Tag(
        val label: LatexNode,
        val starred: Boolean = false,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOf(label)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(label = newChildren[0])
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitTag(this)
    }

    /**
     * 多行下标条件节点（\substack）
     * 用于大型运算符上下限排列多行条件
     * @param rows 每行是一个节点列表
     */
    data class Substack(
        val rows: List<List<LatexNode>>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(rows = rebuildRows(rows, newChildren))

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitSubstack(this)
    }

    /**
     * Smash 节点（\smash）
     * 将内容的高度或深度视为零，用于间距微调
     * @param content 内容
     * @param smashType 压缩类型：BOTH=全部，TOP=只压顶部，BOTTOM=只压底部
     */
    data class Smash(
        val content: List<LatexNode>,
        val smashType: SmashType = SmashType.BOTH,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class SmashType { BOTH, TOP, BOTTOM }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitSmash(this)
    }

    /**
     * 垂直幻影节点（\vphantom）
     * 只保留高度/基线，宽度为零
     */
    data class VPhantom(
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitVPhantom(this)
    }

    /**
     * 水平幻影节点（\hphantom）
     * 只保留宽度，高度/基线使用最小值
     */
    data class HPhantom(
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitHPhantom(this)
    }

    /**
     * 标签定义节点（\label{key}）
     * 该节点不参与渲染，仅记录标签定义
     * @param key 标签键名
     */
    data class Label(
        val key: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitLabel(this)
    }

    /**
     * 引用节点（\ref{key}）
     * 渲染时显示引用的标签编号
     * @param key 引用的标签键名
     */
    data class Ref(
        val key: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitRef(this)
    }

    /**
     * 公式引用节点（\eqref{key}）
     * 渲染时显示带括号的引用标签编号
     * @param key 引用的标签键名
     */
    data class EqRef(
        val key: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitEqRef(this)
    }

    /**
     * 四角标注节点（\sideset{_a^b}{_c^d}{\sum}）
     * 在大型运算符的四个角放置上下标
     * @param leftSub 左下标
     * @param leftSup 左上标
     * @param rightSub 右下标
     * @param rightSup 右上标
     * @param base 基础运算符
     */
    data class SideSet(
        val leftSub: LatexNode?,
        val leftSup: LatexNode?,
        val rightSub: LatexNode?,
        val rightSup: LatexNode?,
        val base: LatexNode,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOfNotNull(leftSub, leftSup, rightSub, rightSup, base)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            var idx = 0
            val newLeftSub = if (leftSub != null) newChildren[idx++] else null
            val newLeftSup = if (leftSup != null) newChildren[idx++] else null
            val newRightSub = if (rightSub != null) newChildren[idx++] else null
            val newRightSup = if (rightSup != null) newChildren[idx++] else null
            val newBase = newChildren[idx]
            return copy(
                leftSub = newLeftSub,
                leftSup = newLeftSup,
                rightSub = newRightSub,
                rightSup = newRightSup,
                base = newBase
            )
        }

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitSideSet(this)
    }

    /**
     * 张量/指标节点（\tensor{T}{^a_b^c}）
     * 物理学中用于排列多个上下标指标
     * @param base 基础符号
     * @param indices 指标列表（每个是 Pair<Boolean, LatexNode>，Boolean=true 表示上标）
     */
    data class Tensor(
        val base: LatexNode,
        val indices: List<Pair<Boolean, LatexNode>>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children(): List<LatexNode> {
            val result = ArrayList<LatexNode>(1 + indices.size)
            result.add(base)
            for ((_, indexNode) in indices) {
                result.add(indexNode)
            }
            return result
        }
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newBase = newChildren[0]
            val newIndices = indices.mapIndexed { i, (isSuper, _) ->
                isSuper to newChildren[i + 1]
            }
            return copy(base = newBase, indices = newIndices)
        }

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitTensor(this)
    }

    /**
     * 表格环境节点（\begin{tabular}{ccc}...）
     * 文本模式下的表格
     * @param rows 行列表，每行是单元格列表
     * @param alignment 列对齐方式字符串（如 "ccc", "lcr", "|c|c|c|"）
     */
    data class Tabular(
        val rows: List<List<LatexNode>>,
        val alignment: String,
        override val sourceRange: SourceRange? = null,
        val rowGaps: List<RowGap?> = emptyList()
    ) : LatexNode() {
        override fun children() = rows.flatten()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) =
            copy(rows = rebuildRows(rows, newChildren))

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitTabular(this)
    }

    /**
     * 水平线节点（\hline）
     * 用于表格中绘制整行水平线
     */
    data class HLine(
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitHLine(this)
    }

    /**
     * 部分水平线节点（\cline{start-end}）
     * 用于表格中绘制跨指定列的水平线
     * @param startCol 起始列（从1开始）
     * @param endCol 结束列（从1开始）
     */
    data class CLine(
        val startCol: Int,
        val endCol: Int,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitCLine(this)
    }

    /**
     * 合并单元格节点（\multicolumn{num}{align}{content}）
     * @param columnCount 合并的列数
     * @param alignment 对齐方式字符串（如 "c", "|c|"）
     * @param content 单元格内容
     */
    data class Multicolumn(
        val columnCount: Int,
        val alignment: String,
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitMulticolumn(this)
    }

    /**
     * 自定义运算符名称节点（\operatorname{Tr}）
     * 以正体渲染运算符名，可带上下标
     * @param name 运算符名称
     */
    data class OperatorName(
        val name: String,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = emptyList<LatexNode>()
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = this
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitOperatorName(this)
    }

    /**
     * 取模运算符节点
     * @param content 可选的参数内容（如 \pmod{n} 中的 n）
     * @param modStyle 取模风格
     */
    data class ModOperator(
        val content: LatexNode?,
        val modStyle: ModStyle,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class ModStyle { BMOD, PMOD, MOD }

        override fun children() = listOfNotNull(content)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val newContent = if (content != null) newChildren.firstOrNull() else null
            return copy(content = newContent)
        }

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitModOperator(this)
    }

    /**
     * 行内数学模式节点（$...$）
     * 用于在文本中嵌入行内数学公式
     * @param children 数学内容
     */
    data class InlineMath(
        val children: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = children
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(children = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitInlineMath(this)
    }

    /**
     * 展示数学模式节点（$$...$$）
     * 用于独立行的数学公式
     * @param children 数学内容
     */
    data class DisplayMath(
        val children: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = children
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(children = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitDisplayMath(this)
    }

    /**
     * 错误恢复节点
     *
     * 当解析器遇到无法识别的输入时，将其包装为 Error 节点而非静默丢弃。
     * 渲染层可用 errorColor 渲染此节点，使用户能看到问题所在。
     *
     * @param message 错误描述
     * @param recovered 错误恢复后解析出的部分内容（可能为空）
     */
    data class Error(
        val message: String,
        val recovered: List<LatexNode> = emptyList(),
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = recovered
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(recovered = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitError(this)
    }

    /**
     * 超链接节点（\href{url}{text} 或 \url{url}）
     * @param url 链接地址
     * @param content 显示内容（\url 时为空，显示 url 本身）
     */
    data class Hyperlink(
        val url: String,
        val content: List<LatexNode>,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitHyperlink(this)
    }

    /**
     * 前置上下标节点（\prescript{sup}{sub}{base}）
     * 在基础符号的左前方放置上下标
     * @param preSuperscript 前置上标
     * @param preSubscript 前置下标
     * @param base 基础符号
     */
    data class Prescript(
        val preSuperscript: LatexNode?,
        val preSubscript: LatexNode?,
        val base: LatexNode,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = listOfNotNull(preSuperscript, preSubscript, base)
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            var idx = 0
            val newPreSup = if (preSuperscript != null) newChildren[idx++] else null
            val newPreSub = if (preSubscript != null) newChildren[idx++] else null
            val newBase = newChildren[idx]
            return copy(preSuperscript = newPreSup, preSubscript = newPreSub, base = newBase)
        }

        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitPrescript(this)
    }

    /**
     * 零宽叠加节点（\mathclap, \mathllap, \mathrlap）
     * 绘制内容但宽度为零（或单侧为零）
     * @param content 内容
     * @param lapType 叠加类型
     */
    data class MathLap(
        val content: List<LatexNode>,
        val lapType: LapType,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class LapType { CLAP, LLAP, RLAP }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitMathLap(this)
    }

    /**
     * Fine layout primitives used by \raisebox and \rule.
     * Dimensions are retained in TeX form and resolved by the renderer so em/ex
     * values continue to follow the active math style.
     */
    data class Layout(
        val layoutType: LayoutType,
        val content: List<LatexNode> = emptyList(),
        val width: String? = null,
        val height: String? = null,
        val shift: String? = null,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class LayoutType { RAISE_BOX, RULE }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitLayout(this)
    }

    /**
     * 背景色节点（\colorbox{color}{text} 或 \fcolorbox{borderColor}{bgColor}{text}）
     * @param content 内容
     * @param backgroundColor 背景颜色名
     * @param borderColor 边框颜色名（null 表示 \colorbox，非 null 表示 \fcolorbox）
     */
    data class ColorBox(
        val content: List<LatexNode>,
        val backgroundColor: String,
        val borderColor: String? = null,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitColorBox(this)
    }

    /**
     * 自定义环境定义节点（\newenvironment）
     * 该节点不参与渲染，仅用于记录环境定义
     * @param envName 环境名
     * @param numArgs 参数个数（0-9）
     * @param beginDef 环境开始定义（AST 节点列表）
     * @param endDef 环境结束定义（AST 节点列表）
     * @param defaultArg 可选参数的默认值
     */
    data class NewEnvironment(
        val envName: String,
        val numArgs: Int,
        val beginDef: List<LatexNode>,
        val endDef: List<LatexNode>,
        val defaultArg: String? = null,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        override fun children() = beginDef + endDef
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>): LatexNode {
            val beginSize = beginDef.size
            return copy(
                beginDef = newChildren.subList(0, beginSize),
                endDef = newChildren.subList(beginSize, newChildren.size)
            )
        }
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitNewEnvironment(this)
    }

    /**
     * 章节标题节点（\section, \subsection, \subsubsection, \paragraph, \subparagraph）
     * @param content 标题内容
     * @param level 标题层级
     * @param starred 是否为星号变体（不编号）
     */
    data class SectionHeading(
        val content: List<LatexNode>,
        val level: HeadingLevel,
        val starred: Boolean = false,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class HeadingLevel {
            SECTION, SUBSECTION, SUBSUBSECTION, PARAGRAPH, SUBPARAGRAPH
        }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitSectionHeading(this)
    }

    /**
     * 文本方向节点（RTL/LTR 切换）
     *
     * 支持以下 LaTeX 命令和环境：
     * - `\RLE{...}` — 强制从右到左
     * - `\LRE{...}` — 强制从左到右（嵌套在 RTL 中使用）
     * - `\textarabic{...}` — 阿拉伯语文本（RTL）
     * - `\texthebrew{...}` — 希伯来语文本（RTL）
     * - `\begin{RTL}...\end{RTL}` — RTL 环境
     * - `\begin{LTR}...\end{LTR}` — LTR 环境（嵌套在 RTL 中使用）
     *
     * @param content 子内容节点列表
     * @param direction 文本方向
     */
    data class TextDirection(
        val content: List<LatexNode>,
        val direction: Direction,
        override val sourceRange: SourceRange? = null
    ) : LatexNode() {
        enum class Direction { RTL, LTR }

        override fun children() = content
        override fun withSourceRange(range: SourceRange) = copy(sourceRange = range)
        override fun withChildren(newChildren: List<LatexNode>) = copy(content = newChildren)
        override fun <T> accept(visitor: LatexVisitor<T>) = visitor.visitTextDirection(this)
    }
}
