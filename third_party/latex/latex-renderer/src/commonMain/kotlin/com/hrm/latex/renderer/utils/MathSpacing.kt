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

package com.hrm.latex.renderer.utils

import com.hrm.latex.parser.model.LatexNode

/**
 * TeX 原子类型，用于确定元素间的数学间距
 *
 * 参考 TeXbook Chapter 18 的间距规则表。
 */
enum class AtomType {
    ORD,    // 普通符号：变量、数字、希腊字母
    OP,     // 大型运算符：\sum, \int, \prod, \lim
    BIN,    // 二元运算符：+, -, \times, \div, \cdot
    REL,    // 关系符号：=, <, >, \leq, \geq, \approx
    OPEN,   // 开定界符：(, [, {
    CLOSE,  // 闭定界符：), ], }
    PUNCT,  // 标点：, ; :
    INNER   // 内部结构：\frac 等
}

/**
 * 间距规则枚举
 */
private enum class SpaceRule {
    NONE,   // 0mu
    THIN,   // 3mu (约 0.167em)
    MEDIUM, // 4mu (约 0.222em)，仅在非 script 样式下
    THICK   // 5mu (约 0.278em)，仅在非 script 样式下
}

/**
 * TeX 标准数学间距系统
 *
 * 基础间距单位是 mu (math unit)，1em = 18mu。
 * 间距定义为 fontSize 的比例系数。
 */
object MathSpacing {

    private const val THIN = 3f / 18f      // \, = 3mu ≈ 0.167em
    private const val MEDIUM = 4f / 18f    // \: = 4mu ≈ 0.222em
    private const val THICK = 5f / 18f     // \; = 5mu ≈ 0.278em

    /**
     * TeX 标准间距表 (简化版)
     *
     * 行 = 左原子类型，列 = 右原子类型
     * null 表示该组合不应出现（按 NONE 处理）
     */
    private val SPACING_TABLE: Array<Array<SpaceRule?>> = arrayOf(
        //              ORD        OP         BIN        REL        OPEN       CLOSE      PUNCT      INNER
        /* ORD   */ arrayOf(
            SpaceRule.NONE,
            SpaceRule.THIN,
            SpaceRule.MEDIUM,
            SpaceRule.THICK,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.THIN
        ),
        /* OP    */
        arrayOf(
            SpaceRule.THIN,
            SpaceRule.THIN,
            null,
            SpaceRule.THICK,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.THIN
        ),
        /* BIN   */
        arrayOf(
            SpaceRule.MEDIUM,
            SpaceRule.MEDIUM,
            null,
            null,
            SpaceRule.MEDIUM,
            null,
            null,
            SpaceRule.MEDIUM
        ),
        /* REL   */
        arrayOf(
            SpaceRule.THICK,
            SpaceRule.THICK,
            null,
            SpaceRule.NONE,
            SpaceRule.THICK,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.THICK
        ),
        /* OPEN  */
        arrayOf(
            SpaceRule.NONE,
            SpaceRule.NONE,
            null,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.NONE
        ),
        /* CLOSE */
        arrayOf(
            SpaceRule.NONE,
            SpaceRule.THIN,
            SpaceRule.MEDIUM,
            SpaceRule.THICK,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.NONE,
            SpaceRule.THIN
        ),
        /* PUNCT */
        arrayOf(
            SpaceRule.THIN,
            SpaceRule.THIN,
            null,
            SpaceRule.THIN,
            SpaceRule.THIN,
            SpaceRule.THIN,
            SpaceRule.THIN,
            SpaceRule.THIN
        ),
        /* INNER */
        arrayOf(
            SpaceRule.THIN,
            SpaceRule.THIN,
            SpaceRule.MEDIUM,
            SpaceRule.THICK,
            SpaceRule.THIN,
            SpaceRule.NONE,
            SpaceRule.THIN,
            SpaceRule.THIN
        )
    )

    /**
     * 根据左右原子类型和当前数学样式，返回间距比例系数
     *
     * @return fontSize 的比例系数（乘以 fontSizePx 得到像素值）
     */
    fun spaceBetween(left: AtomType, right: AtomType, isScript: Boolean): Float {
        val rule = SPACING_TABLE[left.ordinal][right.ordinal] ?: return 0f
        return when (rule) {
            SpaceRule.NONE -> 0f
            SpaceRule.THIN -> THIN
            SpaceRule.MEDIUM -> if (isScript) 0f else MEDIUM
            SpaceRule.THICK -> if (isScript) 0f else THICK
        }
    }

    fun effectiveAtomType(nodes: List<LatexNode>, index: Int): AtomType {
        val type = classifyNode(nodes[index])
        if (type != AtomType.BIN) return type

        val prevType = previousNonSpaceAtomType(nodes, index)
        val nextType = nextNonSpaceAtomType(nodes, index)

        if (prevType == null || prevType in setOf(
                AtomType.BIN, AtomType.OP, AtomType.REL, AtomType.OPEN, AtomType.PUNCT
            )
        ) {
            return AtomType.ORD
        }

        if (nextType == null || nextType in setOf(
                AtomType.BIN, AtomType.REL, AtomType.CLOSE, AtomType.PUNCT
            )
        ) {
            return AtomType.ORD
        }

        return AtomType.BIN
    }

    /**
     * 已知的二元运算符符号名
     */
    private val BINARY_OPERATORS = setOf(
        "plus", "minus", "times", "div", "cdot",
        "pm", "mp", "ast", "star", "circ",
        "oplus", "ominus", "otimes", "oslash",
        "cup", "cap", "setminus", "wedge", "vee"
    )

    /**
     * 已知的关系符号名
     */
    private val RELATION_SYMBOLS = setOf(
        "equals", "neq", "approx", "equiv", "sim", "simeq", "cong",
        "leq", "geq", "ll", "gg", "le", "ge",
        "subset", "supset", "subseteq", "supseteq",
        "in", "ni", "notin",
        "rightarrow", "leftarrow", "leftrightarrow",
        "Rightarrow", "Leftarrow", "Leftrightarrow",
        "longrightarrow", "longleftarrow", "longleftrightarrow",
        "Longleftarrow", "Longrightarrow", "Longleftrightarrow",
        "implies", "iff",
        "mapsto", "to", "longmapsto",
        "propto", "varpropto", "perp", "parallel", "mid",
        "prec", "succ", "preceq", "succeq",
        "vdash", "dashv",
        "coloneqq", "coloneq", "eqqcolon", "eqcolon", "dblcolon"
    )

    /**
     * 标点符号的 Unicode
     */
    private val PUNCTUATION_CHARS = setOf(',', ';', ':')

    /**
     * 开定界符
     */
    private val OPEN_DELIMITERS = setOf("(", "[", "{", "\\{", "\\langle", "\\lfloor", "\\lceil")

    /**
     * 闭定界符
     */
    private val CLOSE_DELIMITERS = setOf(")", "]", "}", "\\}", "\\rangle", "\\rfloor", "\\rceil")

    /**
     * 将 LatexNode 分类为 AtomType
     *
     * 部分节点类型需要根据具体内容做更细粒度的分类（如 Text 的字符、Symbol 的名称），
     * 其余节点使用默认分类。
     */
    fun classifyNode(node: LatexNode): AtomType {
        return when (node) {
            is LatexNode.Text -> classifyText(node.content)
            is LatexNode.Symbol -> classifySymbol(node.symbol, node.unicode)
            is LatexNode.Operator -> classifyOperatorText()
            is LatexNode.BigOperator -> AtomType.OP
            is LatexNode.Fraction, is LatexNode.Binomial -> AtomType.INNER
            is LatexNode.Root -> AtomType.ORD
            is LatexNode.Superscript, is LatexNode.Subscript -> AtomType.ORD
            is LatexNode.Delimited -> AtomType.INNER
            is LatexNode.ManualSizedDelimiter -> classifyDelimiterString(node.delimiter)
            is LatexNode.Space, is LatexNode.HSpace -> AtomType.ORD
            is LatexNode.Group -> AtomType.ORD
            is LatexNode.Accent -> AtomType.ORD
            is LatexNode.Stack -> AtomType.ORD
            is LatexNode.ExtensibleArrow -> AtomType.REL
            is LatexNode.TextMode -> AtomType.ORD
            is LatexNode.Boxed -> AtomType.INNER
            is LatexNode.Matrix -> AtomType.INNER
            is LatexNode.OperatorName -> AtomType.OP
            is LatexNode.ModOperator -> AtomType.BIN
            else -> AtomType.ORD
        }
    }

    private fun classifyText(content: String): AtomType {
        if (content.length == 1) {
            val ch = content[0]
            if (ch in PUNCTUATION_CHARS) return AtomType.PUNCT
            if (ch == '(' || ch == '[') return AtomType.OPEN
            if (ch == ')' || ch == ']') return AtomType.CLOSE
            if (ch == '+' || ch == '-' || ch == '*') return AtomType.BIN
            if (ch == '=' || ch == '<' || ch == '>') return AtomType.REL
        }
        return AtomType.ORD
    }

    private fun classifySymbol(symbol: String, unicode: String): AtomType {
        if (symbol in BINARY_OPERATORS) return AtomType.BIN
        if (symbol in RELATION_SYMBOLS) return AtomType.REL
        if (unicode.length == 1) {
            val ch = unicode[0]
            if (ch == '+' || ch == '-' || ch == '×' || ch == '÷' || ch == '·') return AtomType.BIN
            if (ch == '=' || ch == '<' || ch == '>' || ch == '≤' || ch == '≥' || ch == '≈' ||
                ch == '≡' || ch == '≠' || ch == '≔' || ch == '≕' || ch == '∷'
            ) return AtomType.REL
            if (ch in PUNCTUATION_CHARS) return AtomType.PUNCT
        }
        return AtomType.ORD
    }

    private fun classifyOperatorText(): AtomType {
        // Operator node 通常是命名运算符如 sin, cos, lim 等
        return AtomType.OP
    }

    private fun classifyDelimiterString(delimiter: String): AtomType {
        return when {
            delimiter in OPEN_DELIMITERS || delimiter.length == 1 && delimiter[0] in "([{" -> AtomType.OPEN
            delimiter in CLOSE_DELIMITERS || delimiter.length == 1 && delimiter[0] in ")]}" -> AtomType.CLOSE
            else -> AtomType.ORD
        }
    }

    private fun previousNonSpaceAtomType(nodes: List<LatexNode>, index: Int): AtomType? {
        for (i in index - 1 downTo 0) {
            val node = nodes[i]
            if (node is LatexNode.Space || node is LatexNode.HSpace) continue
            return classifyNode(node)
        }
        return null
    }

    private fun nextNonSpaceAtomType(nodes: List<LatexNode>, index: Int): AtomType? {
        for (i in index + 1 until nodes.size) {
            val node = nodes[i]
            if (node is LatexNode.Space || node is LatexNode.HSpace) continue
            return classifyNode(node)
        }
        return null
    }
}
