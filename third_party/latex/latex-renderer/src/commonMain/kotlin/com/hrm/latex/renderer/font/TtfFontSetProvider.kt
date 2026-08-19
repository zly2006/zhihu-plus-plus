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

package com.hrm.latex.renderer.font

import androidx.compose.ui.text.font.FontFamily
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.utils.MathConstants

/** 内置 KaTeX TTF 字体集的 TeX 排版度量。 */
internal class TtfFontSetProvider(
    private val fontFamilies: LatexFontFamilies
) : MathFontProvider {
    override fun axisHeight(fontSizePx: Float) =
        fontSizePx * MathConstants.MATH_AXIS_HEIGHT_RATIO

    override fun fractionRuleThickness(fontSizePx: Float) = fontSizePx * 0.04f
    override fun fractionNumeratorDisplayGap(fontSizePx: Float) = fontSizePx * 0.12f
    override fun fractionNumeratorGap(fontSizePx: Float) = fontSizePx * 0.04f
    override fun fractionDenominatorDisplayGap(fontSizePx: Float) = fontSizePx * 0.12f
    override fun fractionDenominatorGap(fontSizePx: Float) = fontSizePx * 0.04f

    override fun fractionNumeratorShiftUp(
        fontSizePx: Float,
        displayStyle: Boolean,
        hasRule: Boolean
    ) = fontSizePx * when {
        displayStyle -> 0.677f
        hasRule -> 0.394f
        else -> 0.444f
    }

    override fun fractionDenominatorShiftDown(fontSizePx: Float, displayStyle: Boolean) =
        fontSizePx * if (displayStyle) 0.686f else 0.345f

    override fun superscriptShiftUp(fontSizePx: Float) = fontSizePx * 0.363f

    override fun superscriptShiftUp(
        fontSizePx: Float,
        displayStyle: Boolean,
        crampedStyle: Boolean
    ) = fontSizePx * when {
        displayStyle -> 0.413f
        crampedStyle -> 0.289f
        else -> 0.363f
    }

    override fun subscriptShiftDown(fontSizePx: Float) = fontSizePx * 0.150f
    override fun subscriptShiftDown(fontSizePx: Float, hasSuperscript: Boolean) =
        fontSizePx * if (hasSuperscript) 0.247f else 0.150f
    override fun superscriptDrop(fontSizePx: Float) = fontSizePx * 0.386f
    override fun subscriptDrop(fontSizePx: Float) = fontSizePx * 0.050f
    override fun xHeight(fontSizePx: Float) = fontSizePx * 0.431f
    override fun spaceAfterScript(fontSizePx: Float) = fontSizePx * 0.05f
    override fun subSuperscriptGapMin(fontSizePx: Float) =
        fractionRuleThickness(fontSizePx) * 4f

    override fun radicalDisplayVerticalGap(fontSizePx: Float) =
        fractionRuleThickness(fontSizePx) + xHeight(fontSizePx) / 4f
    override fun radicalVerticalGap(fontSizePx: Float) =
        fractionRuleThickness(fontSizePx) * 1.25f
    override fun radicalRuleThickness(fontSizePx: Float) = fractionRuleThickness(fontSizePx)
    override fun upperLimitGap(fontSizePx: Float, limitDepthPx: Float) = maxOf(
        fontSizePx * 0.111f,
        fontSizePx * 0.2f - limitDepthPx
    )

    override fun lowerLimitGap(fontSizePx: Float, limitHeightPx: Float) = maxOf(
        fontSizePx * 0.166f,
        fontSizePx * 0.6f - limitHeightPx
    )

    override fun italicCorrection(glyphChar: String, fontSizePx: Float): Float {
        if (glyphChar.isEmpty()) return 0f
        return fontSizePx * KaTeXFontMetrics.italicCorrection(glyphChar.last().code)
    }

    override fun topAccentAttachment(
        glyphChar: String,
        fontSizePx: Float,
        glyphAdvancePx: Float
    ): Float {
        if (glyphChar.isEmpty()) return -1f
        return glyphAdvancePx / 2f + fontSizePx * KaTeXFontMetrics.skew(glyphChar.last().code)
    }

    override fun fontFamilyFor(role: MathFontRole): FontFamily = when (role) {
        MathFontRole.ROMAN -> fontFamilies.main
        MathFontRole.MATH_ITALIC -> fontFamilies.math
        MathFontRole.BLACKBOARD_BOLD -> fontFamilies.ams
        MathFontRole.CALLIGRAPHIC -> fontFamilies.caligraphic
        MathFontRole.FRAKTUR -> fontFamilies.fraktur
        MathFontRole.SCRIPT -> fontFamilies.script
        MathFontRole.SANS_SERIF -> fontFamilies.sansSerif
        MathFontRole.MONOSPACE -> fontFamilies.monospace
        MathFontRole.LARGE_OPERATOR, MathFontRole.DELIMITER -> fontFamilies.main
    }

    override fun fontBytes(role: MathFontRole): ByteArray? = when (role) {
        MathFontRole.ROMAN, MathFontRole.LARGE_OPERATOR, MathFontRole.DELIMITER ->
            fontFamilies.mainBytes
        MathFontRole.MATH_ITALIC -> fontFamilies.mathBytes
        MathFontRole.BLACKBOARD_BOLD -> fontFamilies.amsBytes
        else -> fontFamilies.mainBytes
    }
}
