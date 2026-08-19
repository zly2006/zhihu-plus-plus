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

package com.hrm.latex.renderer.layout

import com.hrm.latex.parser.model.LatexNode

/**
 * line breaking engine for latex formulas
 *
 * uses penalty-based algorithm inspired by mathjax/katex:
 * - spaces have top priority (best break points)
 * - relations (=, <, >) have high priority
 * - binary operators (+, -) have medium priority
 * - multiplicative (×, ÷) have lower priority
 * - atomic structures (fractions, matrices) are unbreakable
 *
 * break position: operators go to the NEW line (latex convention)
 *
 * optimizations:
 * - stores index ranges instead of copying lists (zero-copy)
 * - O(1) break position lookup
 * - minimal allocations during line breaking
 */
internal class LineBreaker(private val maxWidth: Float) {

    companion object {
        private const val NOBREAK = Int.MAX_VALUE / 2
        private const val PENALTY_SPACE = -100
        private const val PENALTY_RELATION = 0
        private const val PENALTY_ADDITIVE = 100
        private const val PENALTY_MULTIPLICATIVE = 200
        private const val PENALTY_OTHER_OP = 300
        private const val PENALTY_DEPTH_FACTOR = 400
    }

    /**
     * break state using index ranges to avoid list copying
     */
    private class BreakState {
        val lineRanges = ArrayList<IntRange>(4)
        var lineStart = 0
        var currentPos = 0
        var currentWidth = 0f
        var bestBreakPos = -1
        var bestBreakPenalty = NOBREAK
        var widthAtBestBreak = 0f

        fun commitBreak(): Boolean {
            if (bestBreakPos < 0 || currentPos <= lineStart || bestBreakPos <= lineStart) return false

            // break BEFORE the operator - operator goes to new line (latex convention)
            lineRanges.add(lineStart until bestBreakPos)
            lineStart = bestBreakPos

            // O(1) width recalc: widthAtBestBreak is width BEFORE break point
            // so remaining width = currentWidth - widthAtBestBreak
            currentWidth -= widthAtBestBreak

            bestBreakPos = -1
            bestBreakPenalty = NOBREAK
            widthAtBestBreak = 0f

            return true
        }

        fun forceBreak() {
            if (currentPos > lineStart) {
                lineRanges.add(lineStart until currentPos)
                lineStart = currentPos
            }
            currentWidth = 0f
            bestBreakPos = -1
            bestBreakPenalty = NOBREAK
            widthAtBestBreak = 0f
        }

        fun finalize(): List<IntRange> {
            if (currentPos > lineStart) {
                lineRanges.add(lineStart until currentPos)
            }
            return lineRanges
        }

        fun recordBreakCandidate(globalIndex: Int, penalty: Int, widthBefore: Float) {
            if (penalty < bestBreakPenalty) {
                bestBreakPos = globalIndex
                bestBreakPenalty = penalty
                widthAtBestBreak = widthBefore
            }
        }
    }

    fun breakIntoLines(
        nodes: List<LatexNode>,
        widths: FloatArray
    ): List<List<Int>> {
        if (nodes.isEmpty()) return listOf(emptyList())

        var totalWidth = 0f
        for (w in widths) totalWidth += w
        if (totalWidth <= maxWidth) {
            return listOf(nodes.indices.toList())
        }

        val state = BreakState()
        var depth = 0

        for (i in nodes.indices) {
            val node = nodes[i]
            val width = widths[i]

            depth += getDepthDelta(node, entering = true)

            if (state.currentWidth + width > maxWidth && state.currentPos > state.lineStart) {
                if (!state.commitBreak()) {
                    state.forceBreak()
                }
            }

            val widthBeforeThis = state.currentWidth
            state.currentWidth += width
            state.currentPos = i + 1

            val penalty = calculatePenalty(node, depth)
            if (penalty < NOBREAK) {
                state.recordBreakCandidate(i, penalty, widthBeforeThis)
            }

            depth += getDepthDelta(node, entering = false)
        }

        val ranges = state.finalize()
        return ranges.map { range -> range.toList() }
    }

    private fun getDepthDelta(node: LatexNode, entering: Boolean): Int {
        val delta = when (node) {
            is LatexNode.Group,
            is LatexNode.Delimited,
            is LatexNode.Style,
            is LatexNode.Color,
            is LatexNode.MathStyle,
            is LatexNode.FontSize -> 1

            else -> 0
        }
        return if (entering) delta else -delta
    }

    private fun calculatePenalty(node: LatexNode, depth: Int): Int {
        val basePenalty = when (node) {
            is LatexNode.Operator -> getOperatorPenalty(node.op)
            is LatexNode.Symbol -> getSymbolPenalty(node.unicode)
            is LatexNode.Text -> getTextPenalty(node.content)
            is LatexNode.Space -> PENALTY_SPACE

            is LatexNode.Fraction,
            is LatexNode.Root,
            is LatexNode.Matrix,
            is LatexNode.Array,
            is LatexNode.Delimited,
            is LatexNode.BigOperator,
            is LatexNode.Accent,
            is LatexNode.Binomial,
            is LatexNode.Cases,
            is LatexNode.Superscript,
            is LatexNode.Subscript,
            is LatexNode.Stack,
            is LatexNode.ExtensibleArrow,
            is LatexNode.ManualSizedDelimiter -> NOBREAK

            is LatexNode.Group -> {
                if (node.children.size == 1) {
                    calculatePenalty(node.children[0], depth)
                } else {
                    NOBREAK
                }
            }

            else -> NOBREAK
        }

        return if (basePenalty >= NOBREAK) {
            NOBREAK
        } else {
            basePenalty + depth * PENALTY_DEPTH_FACTOR
        }
    }

    private fun getOperatorPenalty(op: String): Int = when (op) {
        "=", "≠", "≈", "≡", "∼", "≃", "≅", "<", ">", "≤", "≥", "≪", "≫",
        "⊂", "⊃", "⊆", "⊇", "∈", "∋", "≺", "≻", "∝", "≔", "≕", "∷",
        "eq", "ne", "neq", "approx", "equiv", "sim", "le", "leq", "ge", "geq",
        "ll", "gg", "subset", "supset", "subseteq", "supseteq", "in", "ni",
        "prec", "succ", "propto", "varpropto",
        "coloneqq", "coloneq", "eqqcolon", "eqcolon", "dblcolon" -> PENALTY_RELATION

        "+", "-", "−", "±", "∓",
        "pm", "mp", "plus", "minus" -> PENALTY_ADDITIVE

        "×", "÷", "·", "∗", "⋅", "∘",
        "times", "div", "cdot", "ast", "star", "circ" -> PENALTY_MULTIPLICATIVE

        else -> PENALTY_OTHER_OP
    }

    private fun getSymbolPenalty(unicode: String): Int = when (unicode) {
        "=", "≠", "≈", "≡", "∼", "≃", "≅", "<", ">", "≤", "≥", "≪", "≫",
        "⊂", "⊃", "⊆", "⊇", "∈", "∋", "≺", "≻", "∝", "≔", "≕", "∷" -> PENALTY_RELATION

        "+", "-", "−", "±", "∓" -> PENALTY_ADDITIVE

        "×", "÷", "·", "∗", "⋅", "∘" -> PENALTY_MULTIPLICATIVE

        else -> NOBREAK
    }

    private fun getTextPenalty(content: String): Int {
        val c = content.trim()
        return when {
            c.length != 1 -> NOBREAK
            c[0] in "=<>" -> PENALTY_RELATION
            c[0] in "+-" -> PENALTY_ADDITIVE
            else -> NOBREAK
        }
    }
}
