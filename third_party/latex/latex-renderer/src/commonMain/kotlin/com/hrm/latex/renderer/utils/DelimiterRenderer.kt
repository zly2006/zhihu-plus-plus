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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
import kotlin.math.max

internal data class VerticalDelimiterGeometry(
    val width: Float,
    val height: Float,
    val strokeWidth: Float,
    val lineCenters: List<Float>
)

/** KaTeX Main/Size1…Size4 定界符选择与精确墨迹测量。 */
internal object DelimiterRenderer {
    private data class SizeLevel(
        val font: (LatexFontFamilies) -> FontFamily,
        val bytes: (LatexFontFamilies) -> ByteArray?
    )

    private val sizeLevels = listOf(
        SizeLevel({ it.main }, { it.mainBytes }),
        SizeLevel({ it.size1 }, { it.size1Bytes }),
        SizeLevel({ it.size2 }, { it.size2Bytes }),
        SizeLevel({ it.size3 }, { it.size3Bytes }),
        SizeLevel({ it.size4 }, { it.size4Bytes }),
    )

    fun measureScaled(
        delimiter: String,
        context: RenderContext,
        measurer: TextMeasurer,
        targetHeight: Float,
        density: Density? = null
    ): NodeLayout {
        if (delimiter.isEmpty()) return NodeLayout.EMPTY

        measureVerticalBars(delimiter, context, measurer, targetHeight, density)?.let {
            return it
        }

        val glyph = FontResolver.resolveDelimiterGlyph(delimiter, context.fontFamilies)
        val families = context.fontFamilies
        if (families == null) {
            return measureText(
                glyph,
                FontResolver.delimiterContext(context, delimiter),
                measurer,
                density = density
            )
        }

        var best = NodeLayout.EMPTY
        var bestContext = context
        for (level in sizeLevels) {
            val levelContext = context.copy(
                fontFamily = level.font(families),
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Normal
            )
            val layout = measureText(glyph, levelContext, measurer, level.bytes(families), density)
            best = layout
            bestContext = levelContext
            if (targetHeight <= 0f || layout.height >= targetHeight) return layout
        }

        if (best.height <= 0f) return best
        val scaledContext = bestContext.copy(fontSize = context.fontSize * (targetHeight / best.height))
        return measureText(glyph, scaledContext, measurer, families.size4Bytes, density)
    }

    /**
     * 竖线是可拼装定界符，不能把 Size 字体中的 glyph 0 当作完整字形缩放。
     * 高度按目标值伸展，笔画宽始终保持 TeX rule thickness。
     */
    fun measureVerticalBars(
        delimiter: String,
        context: RenderContext,
        measurer: TextMeasurer,
        targetHeight: Float,
        density: Density? = null
    ): NodeLayout? {
        val lineCount = verticalBarCount(delimiter) ?: return null
        val mainContext = FontResolver.delimiterContext(context, delimiter, scale = 1.0f)
        val baseLayout = measureText(
            delimiter,
            mainContext,
            measurer,
            context.fontFamilies?.mainBytes,
            density
        )
        val fontSizePx = density?.let { with(it) { context.fontSize.toPx() } }
            ?: context.fontSize.value
        val strokeWidth = context.mathFontProvider?.fractionRuleThickness(fontSizePx)
            ?: fontSizePx * MathConstants.FRACTION_RULE_THICKNESS
        val geometry = calculateVerticalDelimiterGeometry(
            lineCount = lineCount,
            advanceWidth = baseLayout.width,
            targetHeight = if (targetHeight > 0f) targetHeight else baseLayout.height,
            fontSizePx = fontSizePx,
            strokeWidth = strokeWidth
        )

        return NodeLayout(
            width = geometry.width,
            height = geometry.height,
            baseline = geometry.height / 2f
        ) { x, y ->
            for (center in geometry.lineCenters) {
                drawLine(
                    color = context.color,
                    start = Offset(x + center, y),
                    end = Offset(x + center, y + geometry.height),
                    strokeWidth = geometry.strokeWidth
                )
            }
        }
    }

    internal fun calculateVerticalDelimiterGeometry(
        lineCount: Int,
        advanceWidth: Float,
        targetHeight: Float,
        fontSizePx: Float,
        strokeWidth: Float
    ): VerticalDelimiterGeometry {
        require(lineCount == 1 || lineCount == 2)
        val lineSeparation = strokeWidth + fontSizePx * DOUBLE_BAR_GAP_EM
        val inkWidth = if (lineCount == 1) strokeWidth else lineSeparation + strokeWidth
        val width = max(advanceWidth, inkWidth)
        val center = width / 2f
        val centers = if (lineCount == 1) {
            listOf(center)
        } else {
            listOf(center - lineSeparation / 2f, center + lineSeparation / 2f)
        }
        return VerticalDelimiterGeometry(
            width = width,
            height = targetHeight.coerceAtLeast(strokeWidth),
            strokeWidth = strokeWidth,
            lineCenters = centers
        )
    }

    private fun verticalBarCount(delimiter: String): Int? = when (delimiter) {
        "|", "∣" -> 1
        "‖", "∥" -> 2
        else -> null
    }

    fun measureText(
        delimiter: String,
        delimiterStyle: RenderContext,
        measurer: TextMeasurer,
        fontBytes: ByteArray? = null,
        density: Density? = null
    ): NodeLayout {
        val result = measurer.measure(AnnotatedString(delimiter), delimiterStyle.textStyle())
        val fontSizePx = density?.let { with(it) { delimiterStyle.fontSize.toPx() } }
            ?: delimiterStyle.fontSize.value
        val precise = fontBytes?.let {
            InkBoundsEstimator.measurePrecise(
                delimiter,
                fontSizePx,
                it,
                result.firstBaseline,
                delimiterStyle.fontWeight?.weight ?: 400
            )
        }
        val topOffset = precise?.inkTopOffset ?: 0f
        return NodeLayout(
            result.size.width.toFloat(),
            precise?.inkHeight ?: result.size.height.toFloat(),
            precise?.inkBaseline ?: result.firstBaseline
        ) { x, y ->
            drawText(result, topLeft = Offset(x, y - topOffset))
        }
    }

    private const val DOUBLE_BAR_GAP_EM = 0.155f
}
