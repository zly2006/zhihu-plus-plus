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
 * 分数与二项式相关命令：\frac, \dfrac, \tfrac, \cfrac, \binom, \tbinom, \dbinom
 */
internal fun CommandRegistry.installFractionHandlers() {
    // 分数
    register("frac") { _, ctx, _ ->
        val numerator = ctx.parseArgument() ?: LatexNode.Text("")
        val denominator = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Fraction(
            numerator = numerator,
            denominator = denominator,
            style = LatexNode.Fraction.FractionStyle.NORMAL
        )
    }

    register("dfrac") { _, ctx, _ ->
        val numerator = ctx.parseArgument() ?: LatexNode.Text("")
        val denominator = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Fraction(
            numerator = numerator,
            denominator = denominator,
            style = LatexNode.Fraction.FractionStyle.DISPLAY
        )
    }

    register("tfrac") { _, ctx, _ ->
        val numerator = ctx.parseArgument() ?: LatexNode.Text("")
        val denominator = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Fraction(
            numerator = numerator,
            denominator = denominator,
            style = LatexNode.Fraction.FractionStyle.TEXT
        )
    }

    register("cfrac") { _, ctx, _ ->
        val numerator = ctx.parseArgument() ?: LatexNode.Text("")
        val denominator = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Fraction(
            numerator = numerator,
            denominator = denominator,
            style = LatexNode.Fraction.FractionStyle.CONTINUED
        )
    }

    // 二项式系数
    register("binom") { _, ctx, _ ->
        val top = ctx.parseArgument() ?: LatexNode.Text("")
        val bottom = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Binomial(top, bottom, LatexNode.Binomial.BinomialStyle.NORMAL)
    }

    register("tbinom") { _, ctx, _ ->
        val top = ctx.parseArgument() ?: LatexNode.Text("")
        val bottom = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Binomial(top, bottom, LatexNode.Binomial.BinomialStyle.TEXT)
    }

    register("dbinom") { _, ctx, _ ->
        val top = ctx.parseArgument() ?: LatexNode.Text("")
        val bottom = ctx.parseArgument() ?: LatexNode.Text("")
        LatexNode.Binomial(top, bottom, LatexNode.Binomial.BinomialStyle.DISPLAY)
    }

    // mathtools split fractions. The two pieces form a ruleless two-line fraction.
    register("splitfrac", "splitdfrac") { command, ctx, _ ->
        val top = ctx.parseArgument() ?: LatexNode.Text("")
        val bottom = ctx.parseArgument() ?: LatexNode.Text("")
        val fraction = LatexNode.Fraction(
            numerator = top,
            denominator = bottom,
            style = LatexNode.Fraction.FractionStyle.RULELESS
        )
        if (command == "splitdfrac") {
            LatexNode.MathStyle(listOf(fraction), LatexNode.MathStyle.MathStyleType.DISPLAY)
        } else fraction
    }

    // Generalized fraction: \genfrac{left}{right}{rule}{style}{num}{den}
    // Custom non-zero rule thickness is accepted and rendered with the engine's
    // standard rule thickness; a zero thickness produces a ruleless fraction.
    register("genfrac") { _, ctx, _ ->
        val left = ParseUtils.extractDelimiter(ctx.parseArgument() ?: LatexNode.Text(""))
        val right = ParseUtils.extractDelimiter(ctx.parseArgument() ?: LatexNode.Text(""))
        val rule = ParseUtils.extractText(listOf(ctx.parseArgument() ?: LatexNode.Text(""))).trim()
        val styleNumber = ParseUtils.extractText(
            listOf(ctx.parseArgument() ?: LatexNode.Text(""))
        ).trim().toIntOrNull()
        val numerator = ctx.parseArgument() ?: LatexNode.Text("")
        val denominator = ctx.parseArgument() ?: LatexNode.Text("")
        val ruleless = rule.matches(Regex("""[+-]?0+(?:\.0+)?(?:[A-Za-z]+)?"""))
        val fractionStyle = when {
            ruleless -> LatexNode.Fraction.FractionStyle.RULELESS
            styleNumber == 0 -> LatexNode.Fraction.FractionStyle.DISPLAY
            styleNumber == 1 -> LatexNode.Fraction.FractionStyle.TEXT
            else -> LatexNode.Fraction.FractionStyle.NORMAL
        }
        val fraction = LatexNode.Fraction(numerator, denominator, fractionStyle)
        val styled = when (styleNumber) {
            2 -> LatexNode.MathStyle(
                listOf(fraction), LatexNode.MathStyle.MathStyleType.SCRIPT
            )
            3 -> LatexNode.MathStyle(
                listOf(fraction), LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT
            )
            else -> fraction
        }
        if (left.isNotEmpty() || right.isNotEmpty()) {
            LatexNode.Delimited(left, right, listOf(styled))
        } else {
            styled
        }
    }
}
