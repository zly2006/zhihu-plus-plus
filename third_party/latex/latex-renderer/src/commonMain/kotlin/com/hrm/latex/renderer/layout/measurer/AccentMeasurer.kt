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


package com.hrm.latex.renderer.layout.measurer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.LatexNode.Accent.AccentType
import com.hrm.latex.renderer.font.KaTeXFontMetrics
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.MathConstants
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * 装饰符号测量器
 *
 * 负责测量重音符号（如 \hat, \vec）和可伸缩的宽装饰（如 \widehat, \overline, \underbrace）。
 */
internal class AccentMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Accent::class
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        node as LatexNode.Accent
        val contentLayout = measureGroup(listOf(node.content), context)

        // 判断是否是宽装饰（需要横向拉伸）
        val isWideAccent = when (node.accentType) {
            AccentType.WIDEHAT, AccentType.OVERRIGHTARROW, AccentType.OVERLEFTARROW,
            AccentType.OVERLINE, AccentType.UNDERLINE,
            AccentType.OVERBRACE, AccentType.UNDERBRACE,
            AccentType.OVERBRACKET, AccentType.UNDERBRACKET,
            AccentType.WIDECHECK, AccentType.OVERLEFTRIGHTARROW,
            AccentType.UNDERLEFTARROW, AccentType.UNDERRIGHTARROW,
            AccentType.OVERPAREN, AccentType.UNDERPAREN,
            AccentType.CANCEL, AccentType.BCANCEL, AccentType.XCANCEL -> true

            else -> false
        }

        if (isWideAccent) {
            return measureWideAccent(node, contentLayout, context, density)
        }

        // KaTeX renders \vec with a dedicated SVG. A standalone text arrow has
        // a normal text baseline, so clipping its line box makes it sink into
        // the accented glyph. Draw the compact arrow independently instead.
        if (node.accentType == AccentType.VEC) {
            return measureVectorAccent(node.content, contentLayout, context, density)
        }

        // 普通字符装饰
        val accentChar = when (node.accentType) {
            AccentType.HAT -> "^"
            AccentType.TILDE -> "~"
            AccentType.BAR -> "ˉ"
            AccentType.DOT -> "˙"
            AccentType.DDOT -> "¨"
            AccentType.DDDOT -> "⃛"
            AccentType.GRAVE -> "ˋ"
            AccentType.ACUTE -> "ˊ"
            AccentType.CHECK -> "ˇ"
            AccentType.BREVE -> "˘"
            AccentType.RING -> "˚"
            else -> ""
        }

        val accentContext = context.copy(
            fontFamily = context.fontFamilies?.main ?: context.fontFamily,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal
        )
        val result = measurer.measure(AnnotatedString(accentChar), accentContext.textStyle())
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val accentMetrics = KaTeXFontMetrics.mainAccentVerticalMetrics(accentChar.first().code)
        val accentHeight = fontSizePx * accentMetrics.height
        val accentDepth = fontSizePx * accentMetrics.depth
        val accentBoxHeight = accentHeight + accentDepth

        val accentLayout = NodeLayout(
            result.size.width.toFloat(),
            accentBoxHeight,
            accentHeight
        ) { x, y ->
            drawText(result, topLeft = Offset(x, y + accentHeight - result.firstBaseline))
        }

        val baseChar = singleBaseCharacter(node.content)
        val glyphAdvance = (contentLayout.width - contentLayout.italicCorrection).coerceAtLeast(0f)
        val attachment = if (baseChar != null) {
            context.mathFontProvider?.topAccentAttachment(baseChar, fontSizePx, glyphAdvance)
                ?.takeIf { it >= 0f } ?: glyphAdvance / 2f
        } else {
            contentLayout.width / 2f
        }
        val rawAccentLeft = attachment - accentLayout.width / 2f
        val left = min(0f, rawAccentLeft)
        val right = max(contentLayout.width, rawAccentLeft + accentLayout.width)
        val width = right - left
        val contentX = -left
        val accentX = rawAccentLeft - left
        val vertical = calculateKaTeXAccentVerticalPlacement(
            baseHeight = contentLayout.baseline,
            baseDepth = contentLayout.height - contentLayout.baseline,
            accentHeight = accentHeight,
            accentDepth = accentDepth,
            xHeight = context.mathFontProvider?.xHeight(fontSizePx) ?: fontSizePx * 0.431f
        )

        return NodeLayout(
            width,
            vertical.totalHeight,
            vertical.baseline
        ) { x, y ->
            accentLayout.draw(this, x + accentX, y + vertical.accentY)
            contentLayout.draw(this, x + contentX, y + vertical.contentY)
        }
    }

    private fun measureVectorAccent(
        contentNode: LatexNode,
        contentLayout: NodeLayout,
        context: RenderContext,
        density: Density
    ): NodeLayout {
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val strokeWidth = max(with(density) { 1f.dp.toPx() }, fontSizePx * 0.04f)
        val strokeHalf = strokeWidth / 2f
        val arrowWidth = fontSizePx * 0.471f
        val arrowInkHeight = fontSizePx * 0.16f
        val arrowHeadLength = fontSizePx * 0.12f
        val gap = fontSizePx * 0.04f

        val glyphAdvance = (contentLayout.width - contentLayout.italicCorrection).coerceAtLeast(0f)
        val baseChar = singleBaseCharacter(contentNode)
        val attachment = baseChar?.let {
            context.mathFontProvider?.topAccentAttachment(it, fontSizePx, glyphAdvance)
                ?.takeIf { value -> value >= 0f }
        } ?: glyphAdvance / 2f

        val rawArrowLeft = attachment - arrowWidth / 2f
        val left = min(0f, rawArrowLeft - strokeHalf)
        val right = max(contentLayout.width, rawArrowLeft + arrowWidth + strokeHalf)
        val width = right - left
        val contentX = -left
        val arrowLeft = rawArrowLeft - left
        val contentY = arrowInkHeight + gap + strokeWidth
        val height = contentY + contentLayout.height
        val baseline = contentY + contentLayout.baseline

        return NodeLayout(width, height, baseline) { x, y ->
            val centerY = y + strokeHalf + arrowInkHeight / 2f
            val startX = x + arrowLeft + strokeHalf
            val endX = x + arrowLeft + arrowWidth - strokeHalf
            drawLine(
                color = context.color,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = context.color,
                start = Offset(endX - arrowHeadLength, centerY - arrowInkHeight / 2f),
                end = Offset(endX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = context.color,
                start = Offset(endX - arrowHeadLength, centerY + arrowInkHeight / 2f),
                end = Offset(endX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            contentLayout.draw(this, x + contentX, y + contentY)
        }
    }

    private fun singleBaseCharacter(node: LatexNode): String? = when (node) {
        is LatexNode.Text -> node.content.takeIf { it.length == 1 }
        is LatexNode.Symbol -> node.unicode.ifEmpty { node.symbol }.takeIf { it.length == 1 }
        is LatexNode.Group -> node.children.singleOrNull()?.let(::singleBaseCharacter)
        else -> null
    }

    /**
     * 测量宽装饰（自绘图形）
     *
     * 根据内容宽度，动态绘制横线、大括号、箭头或宽帽子。
     * Path 以 (0,0) 为原点构建，draw 时通过 translate 偏移。
     * NodeLayout 尺寸包含 Stroke 半宽。
     */
    private fun measureWideAccent(
        node: LatexNode.Accent,
        contentLayout: NodeLayout,
        context: RenderContext,
        density: Density
    ): NodeLayout {
        val isUnder = node.accentType == AccentType.UNDERLINE ||
                node.accentType == AccentType.UNDERBRACE ||
                node.accentType == AccentType.UNDERBRACKET ||
                node.accentType == AccentType.UNDERLEFTARROW ||
                node.accentType == AccentType.UNDERRIGHTARROW ||
                node.accentType == AccentType.UNDERPAREN

        val isArrowAccent = node.accentType == AccentType.OVERRIGHTARROW ||
                node.accentType == AccentType.OVERLEFTARROW ||
                node.accentType == AccentType.OVERLEFTRIGHTARROW ||
                node.accentType == AccentType.UNDERLEFTARROW ||
                node.accentType == AccentType.UNDERRIGHTARROW

        val strokeWidth = when (node.accentType) {
            AccentType.OVERBRACE, AccentType.UNDERBRACE -> with(density) { 1.2f.dp.toPx() }
            AccentType.OVERBRACKET, AccentType.UNDERBRACKET -> with(density) { 1.2f.dp.toPx() }
            AccentType.WIDEHAT, AccentType.WIDECHECK,
            AccentType.OVERPAREN, AccentType.UNDERPAREN -> with(density) { 1.5f.dp.toPx() }
            AccentType.OVERLINE, AccentType.UNDERLINE,
            AccentType.OVERRIGHTARROW, AccentType.OVERLEFTARROW,
            AccentType.OVERLEFTRIGHTARROW, AccentType.UNDERLEFTARROW,
            AccentType.UNDERRIGHTARROW -> with(density) { 1.5f.dp.toPx() }

            AccentType.CANCEL, AccentType.BCANCEL, AccentType.XCANCEL -> with(density) { 1.5f.dp.toPx() }
            else -> with(density) { 1.5f.dp.toPx() }
        }
        val strokeHalf = strokeWidth / 2f

        val accentHeight = when (node.accentType) {
            AccentType.OVERLINE, AccentType.UNDERLINE -> with(density) { 2f.dp.toPx() }
            AccentType.OVERRIGHTARROW, AccentType.OVERLEFTARROW,
            AccentType.OVERLEFTRIGHTARROW, AccentType.UNDERLEFTARROW,
            AccentType.UNDERRIGHTARROW ->
                with(density) { (context.fontSize * MathConstants.WIDE_ACCENT_ARROW_HEIGHT).toPx() }

            else -> with(density) { (context.fontSize * MathConstants.WIDE_ACCENT_DEFAULT_HEIGHT).toPx() }
        }
        val gap = when {
            isArrowAccent -> with(density) { (context.fontSize * MathConstants.WIDE_ACCENT_ARROW_GAP).toPx() }
            else -> with(density) { (context.fontSize * MathConstants.WIDE_ACCENT_DEFAULT_GAP).toPx() }
        }

        val width = contentLayout.width
        // 总高度包含 Stroke 半宽
        val totalHeight = contentLayout.height + accentHeight + gap + strokeHalf

        // Measure 阶段：以 (0,0) 为原点预构建 Path
        val accentPath: Path? = when (node.accentType) {
            AccentType.OVERBRACE -> {
                val leftX = 0f
                val rightX = width
                val centerX = width / 2
                val topY = 0f
                val bottomY = accentHeight
                val tipHeight = accentHeight * 0.25f
                val shoulderY = topY + tipHeight
                val curveWidth = min(width / 2, accentHeight)

                Path().apply {
                    moveTo(leftX, bottomY)
                    cubicTo(
                        leftX,
                        bottomY - (bottomY - shoulderY) * 0.6f,
                        leftX + curveWidth * 0.4f,
                        shoulderY,
                        leftX + curveWidth,
                        shoulderY
                    )
                    lineTo(centerX - curveWidth, shoulderY)
                    cubicTo(
                        centerX - curveWidth * 0.4f,
                        shoulderY,
                        centerX,
                        shoulderY - tipHeight * 0.6f,
                        centerX,
                        topY
                    )
                    cubicTo(
                        centerX,
                        shoulderY - tipHeight * 0.6f,
                        centerX + curveWidth * 0.4f,
                        shoulderY,
                        centerX + curveWidth,
                        shoulderY
                    )
                    lineTo(rightX - curveWidth, shoulderY)
                    cubicTo(
                        rightX - curveWidth * 0.4f,
                        shoulderY,
                        rightX,
                        bottomY - (bottomY - shoulderY) * 0.6f,
                        rightX,
                        bottomY
                    )
                }
            }

            AccentType.UNDERBRACE -> {
                val leftX = 0f
                val rightX = width
                val centerX = width / 2
                val topY = 0f
                val bottomY = accentHeight
                val tipHeight = accentHeight * 0.25f
                val shoulderY = bottomY - tipHeight
                val curveWidth = min(width / 2, accentHeight)

                Path().apply {
                    moveTo(leftX, topY)
                    cubicTo(
                        leftX,
                        topY + (shoulderY - topY) * 0.6f,
                        leftX + curveWidth * 0.4f,
                        shoulderY,
                        leftX + curveWidth,
                        shoulderY
                    )
                    lineTo(centerX - curveWidth, shoulderY)
                    cubicTo(
                        centerX - curveWidth * 0.4f,
                        shoulderY,
                        centerX,
                        shoulderY + tipHeight * 0.6f,
                        centerX,
                        bottomY
                    )
                    cubicTo(
                        centerX,
                        shoulderY + tipHeight * 0.6f,
                        centerX + curveWidth * 0.4f,
                        shoulderY,
                        centerX + curveWidth,
                        shoulderY
                    )
                    lineTo(rightX - curveWidth, shoulderY)
                    cubicTo(
                        rightX - curveWidth * 0.4f,
                        shoulderY,
                        rightX,
                        topY + (shoulderY - topY) * 0.6f,
                        rightX,
                        topY
                    )
                }
            }

            AccentType.OVERBRACKET -> {
                // 方括号形状：┌──────┐
                Path().apply {
                    moveTo(0f, accentHeight)
                    lineTo(0f, 0f)
                    lineTo(width, 0f)
                    lineTo(width, accentHeight)
                }
            }

            AccentType.UNDERBRACKET -> {
                // 方括号形状：└──────┘
                Path().apply {
                    moveTo(0f, 0f)
                    lineTo(0f, accentHeight)
                    lineTo(width, accentHeight)
                    lineTo(width, 0f)
                }
            }

            AccentType.WIDEHAT -> {
                val bottomY = accentHeight
                Path().apply {
                    moveTo(0f, bottomY)
                    lineTo(width / 2, 0f)
                    lineTo(width, bottomY)
                }
            }

            AccentType.WIDECHECK -> Path().apply {
                moveTo(0f, 0f)
                lineTo(width / 2, accentHeight)
                lineTo(width, 0f)
            }

            AccentType.OVERPAREN -> Path().apply {
                moveTo(0f, accentHeight)
                cubicTo(width * 0.2f, 0f, width * 0.8f, 0f, width, accentHeight)
            }

            AccentType.UNDERPAREN -> Path().apply {
                moveTo(0f, 0f)
                cubicTo(width * 0.2f, accentHeight, width * 0.8f, accentHeight, width, 0f)
            }

            AccentType.OVERRIGHTARROW, AccentType.UNDERRIGHTARROW -> {
                val arrowHeadSize = with(density) { 4f.dp.toPx() }
                Path().apply {
                    moveTo(width, accentHeight / 2)
                    lineTo(width - arrowHeadSize, accentHeight / 2 - arrowHeadSize / 2)
                    lineTo(width - arrowHeadSize, accentHeight / 2 + arrowHeadSize / 2)
                    close()
                }
            }

            AccentType.OVERLEFTARROW, AccentType.UNDERLEFTARROW -> {
                val arrowHeadSize = with(density) { 4f.dp.toPx() }
                Path().apply {
                    moveTo(0f, accentHeight / 2)
                    lineTo(arrowHeadSize, accentHeight / 2 - arrowHeadSize / 2)
                    lineTo(arrowHeadSize, accentHeight / 2 + arrowHeadSize / 2)
                    close()
                }
            }

            AccentType.OVERLEFTRIGHTARROW -> {
                val arrowHeadSize = with(density) { 4f.dp.toPx() }
                Path().apply {
                    moveTo(0f, accentHeight / 2)
                    lineTo(arrowHeadSize, accentHeight / 2 - arrowHeadSize / 2)
                    lineTo(arrowHeadSize, accentHeight / 2 + arrowHeadSize / 2)
                    close()
                    moveTo(width, accentHeight / 2)
                    lineTo(width - arrowHeadSize, accentHeight / 2 - arrowHeadSize / 2)
                    lineTo(width - arrowHeadSize, accentHeight / 2 + arrowHeadSize / 2)
                    close()
                }
            }

            else -> null
        }

        return NodeLayout(
            width,
            totalHeight,
            contentLayout.baseline + (if (isUnder) 0f else accentHeight + gap)
        ) { x, y ->
            val accentY = if (isUnder) y + contentLayout.height + gap else y
            val contentY = if (isUnder) y else y + accentHeight + gap

            when (node.accentType) {
                AccentType.OVERLINE, AccentType.UNDERLINE -> {
                    drawLine(
                        color = context.color,
                        start = Offset(x, accentY + strokeHalf),
                        end = Offset(x + width, accentY + strokeHalf),
                        strokeWidth = strokeWidth
                    )
                }

                AccentType.OVERBRACE, AccentType.UNDERBRACE,
                AccentType.OVERBRACKET, AccentType.UNDERBRACKET,
                AccentType.WIDEHAT, AccentType.WIDECHECK,
                AccentType.OVERPAREN, AccentType.UNDERPAREN -> {
                    accentPath?.let { path ->
                        withTransform({ translate(left = x, top = accentY) }) {
                            drawPath(
                                path = path,
                                color = context.color,
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }
                }

                AccentType.OVERRIGHTARROW, AccentType.OVERLEFTARROW,
                AccentType.OVERLEFTRIGHTARROW, AccentType.UNDERLEFTARROW,
                AccentType.UNDERRIGHTARROW -> {
                    drawLine(
                        color = context.color,
                        start = Offset(x, accentY + accentHeight / 2),
                        end = Offset(x + width, accentY + accentHeight / 2),
                        strokeWidth = strokeWidth
                    )
                    accentPath?.let { path ->
                        withTransform({ translate(left = x, top = accentY) }) {
                            drawPath(path = path, color = context.color)
                        }
                    }
                }

                AccentType.CANCEL -> {
                    drawLine(
                        color = context.color,
                        start = Offset(x, contentY + contentLayout.height),
                        end = Offset(x + width, contentY),
                        strokeWidth = strokeWidth
                    )
                }

                AccentType.BCANCEL -> {
                    // 反向取消线：从左上到右下
                    drawLine(
                        color = context.color,
                        start = Offset(x, contentY),
                        end = Offset(x + width, contentY + contentLayout.height),
                        strokeWidth = strokeWidth
                    )
                }

                AccentType.XCANCEL -> {
                    // 交叉取消线：两条对角线
                    drawLine(
                        color = context.color,
                        start = Offset(x, contentY + contentLayout.height),
                        end = Offset(x + width, contentY),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = context.color,
                        start = Offset(x, contentY),
                        end = Offset(x + width, contentY + contentLayout.height),
                        strokeWidth = strokeWidth
                    )
                }

                else -> {}
            }

            contentLayout.draw(this, x, contentY)
        }
    }
}

/** KaTeX `firstBaseline` vlist placement for a non-stretchy accent. */
internal data class AccentVerticalPlacement(
    val totalHeight: Float,
    val baseline: Float,
    val contentY: Float,
    val accentY: Float
)

internal fun calculateKaTeXAccentVerticalPlacement(
    baseHeight: Float,
    baseDepth: Float,
    accentHeight: Float,
    accentDepth: Float,
    xHeight: Float
): AccentVerticalPlacement {
    val clearance = min(baseHeight, xHeight)
    val accentExtentAboveBaseline = baseHeight - clearance + accentHeight + accentDepth
    val heightAboveBaseline = max(baseHeight, accentExtentAboveBaseline)
    return AccentVerticalPlacement(
        totalHeight = heightAboveBaseline + baseDepth,
        baseline = heightAboveBaseline,
        contentY = heightAboveBaseline - baseHeight,
        accentY = heightAboveBaseline - accentExtentAboveBaseline
    )
}
