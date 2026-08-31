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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.grow
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.model.toLimitStyle
import com.hrm.latex.renderer.font.KaTeXFontMetrics
import com.hrm.latex.renderer.font.MathFontRole
import com.hrm.latex.renderer.utils.FontResolver
import com.hrm.latex.renderer.utils.InkBoundsEstimator
import com.hrm.latex.renderer.utils.InkFontCategory
import com.hrm.latex.renderer.utils.LayoutUtils
import com.hrm.latex.renderer.utils.MathConstants
import com.hrm.latex.renderer.utils.mapBigOp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.reflect.KClass

/**
 * 大型运算符测量器 — 处理 \sum, \int, \prod 等
 *
 * 支持两种模式：
 * 1. 侧边模式 (Side): 上下标在符号右侧（积分符号始终使用）
 * 2. 上下模式 (Display): 上下标在符号正上方/正下方（求和等在 DISPLAY 模式使用）
 *
 * 使用 MathConstants 集中管理所有排版参数。
 */
internal class BigOperatorMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.BigOperator::class
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        node as LatexNode.BigOperator
        val symbol = mapBigOp(node.operator)
        val isIntegral = node.operator.contains("int")
        val isNamedOperator = symbol == node.operator && symbol.all { it.isLetter() }

        val renderSymbol = symbol

        val useSideMode = resolveLimitsMode(node, context)
        val limitStyle = context.toLimitStyle()

        val provider = context.mathFontProvider
        val useKaTeXSizeFont = !isNamedOperator
        val opStyle = buildOperatorStyle(
            context, isNamedOperator, useKaTeXSizeFont
        )

        // ── 积分混合拉伸策略 ──
        // 目标：将总高度拉伸分解为 fontSize 均匀放大 + 剩余垂直 Canvas scale
        // fontSize 放大保持自然宽高比和笔画粗细，Canvas scale 仅处理剩余部分
        // 这样非均匀分量很小，避免水平压缩感；同时笔画不会过粗

        // 1. 先用基础字号测量，得到基础墨水高度
        val baseOpResult = measurer.measure(AnnotatedString(renderSymbol), opStyle.textStyle())
        val baseFontCategory = when {
            isNamedOperator -> InkFontCategory.TEXT
            else -> InkFontCategory.EXTENSION
        }
        val baseFontBytes = when {
            isNamedOperator -> context.fontFamilies?.mainBytes
                ?: provider?.fontBytes(MathFontRole.ROMAN)
            useKaTeXSizeFont && context.mathStyle == MathStyle.DISPLAY ->
                context.fontFamilies?.size2Bytes
            useKaTeXSizeFont -> context.fontFamilies?.size1Bytes
            else -> provider?.fontBytes(MathFontRole.LARGE_OPERATOR)
                ?: context.fontFamilies?.mainBytes
        }
        val baseFontSizePx = with(density) { opStyle.fontSize.toPx() }
        val baseFontWeightVal = opStyle.fontWeight?.weight ?: 400

        val baseInkBounds = if (baseFontBytes != null) {
            InkBoundsEstimator.measurePrecise(
                text = renderSymbol,
                fontSizePx = baseFontSizePx,
                fontBytes = baseFontBytes,
                baseline = baseOpResult.firstBaseline,
                fontWeightValue = baseFontWeightVal
            ) ?: InkBoundsEstimator.estimate(baseOpResult, baseFontCategory)
        } else {
            InkBoundsEstimator.estimate(baseOpResult, baseFontCategory)
        }

        // 2. 计算总需要的垂直拉伸倍数（相对于基础墨水高度）
        var totalVerticalScale = 1.0f
        if (isIntegral && context.mathStyle == MathStyle.DISPLAY && !useKaTeXSizeFont) {
            if (context.layoutHints.bigOpHeightHint != null) {
                val targetHeight =
                    context.layoutHints.bigOpHeightHint * MathConstants.INTEGRAL_HEIGHT_HINT_OVERSHOOT
                val currentInkHeight = baseInkBounds.inkHeight
                if (currentInkHeight > 0f && targetHeight > currentInkHeight) {
                    totalVerticalScale = targetHeight / currentInkHeight
                }
            }
            totalVerticalScale =
                totalVerticalScale.coerceAtLeast(MathConstants.INTEGRAL_MIN_VERTICAL_SCALE)
        }

        // 3. 混合分解：fontSize 均匀放大 + 剩余垂直 scale
        //    fontScaleUp = totalVerticalScale ^ ratio  （均匀放大部分）
        //    remainingVerticalScale = totalVerticalScale / fontScaleUp （剩余垂直拉伸）
        //    ratio=0.5 时，两者均为 sqrt(totalVerticalScale)
        val fontScaleUp: Float
        val verticalScale: Float
        if (totalVerticalScale > 1.0f && isIntegral) {
            val ratio = MathConstants.INTEGRAL_FONT_SCALE_RATIO
            fontScaleUp = totalVerticalScale.toDouble().pow(ratio.toDouble()).toFloat()
            verticalScale = totalVerticalScale / fontScaleUp
        } else {
            fontScaleUp = 1.0f
            verticalScale = 1.0f
        }

        // 4. 用放大后的 fontSize 重新测量
        val finalOpStyle = if (fontScaleUp > 1.0f) {
            val weight = FontResolver.compensatedFontWeight(
                MathConstants.BIG_OP_SYMBOL_BASE_WEIGHT,
                fontScaleUp
            )
            opStyle.grow(fontScaleUp).copy(fontWeight = weight)
        } else {
            opStyle
        }
        val opResult = measurer.measure(AnnotatedString(renderSymbol), finalOpStyle.textStyle())

        // 5. 重新测量放大后的墨水边界
        val finalFontSizePx = with(density) { finalOpStyle.fontSize.toPx() }
        val finalFontWeightVal = finalOpStyle.fontWeight?.weight ?: 400

        val finalInkBounds = if (baseFontBytes != null) {
            InkBoundsEstimator.measurePrecise(
                text = renderSymbol,
                fontSizePx = finalFontSizePx,
                fontBytes = baseFontBytes,
                baseline = opResult.firstBaseline,
                fontWeightValue = finalFontWeightVal
            ) ?: InkBoundsEstimator.estimate(opResult, baseFontCategory)
        } else {
            InkBoundsEstimator.estimate(opResult, baseFontCategory)
        }

        val opWidth = opResult.size.width.toFloat()

        // 应用剩余垂直拉伸到墨水尺寸
        // verticalScale 现在仅是 sqrt(totalScale) 级别，非均匀分量很小
        val opInkHeight = finalInkBounds.inkHeight * verticalScale
        val opInkTopOffset = finalInkBounds.inkTopOffset
        val opInkBaseline = finalInkBounds.inkBaseline * verticalScale

        // KaTeX Size1/Size2 integral glyphs have a substantial right italic
        // correction. It is part of the base advance for superscripts; subscripts
        // cancel it again according to TeX Rule 18.
        val opItalicCorrection = if (useKaTeXSizeFont && isIntegral) {
            finalFontSizePx * KaTeXFontMetrics.integralItalicCorrection(
                context.mathStyle == MathStyle.DISPLAY
            )
        } else 0f
        val opLayout = NodeLayout(
            opWidth + opItalicCorrection,
            opInkHeight,
            opInkBaseline,
            opItalicCorrection
        ) { x, y ->
            if (verticalScale > 1.0f) {
                // 剩余垂直拉伸：非均匀分量很小（~sqrt 级别），水平压缩感轻微
                val scaledInkCenter = y + opInkHeight / 2f
                withTransform({
                    scale(1.0f, verticalScale, pivot = Offset(x + opWidth / 2f, scaledInkCenter))
                }) {
                    val textY = scaledInkCenter - finalInkBounds.inkHeight / 2f - opInkTopOffset
                    drawText(opResult, topLeft = Offset(x, textY))
                }
            } else {
                drawText(opResult, topLeft = Offset(x, y - opInkTopOffset))
            }
        }

        val superLayout = node.superscript?.let { measureGroup(listOf(it), limitStyle) }
        val subLayout = node.subscript?.let { measureGroup(listOf(it), limitStyle) }

        return if (useSideMode) {
            layoutSideMode(
                context, density, measurer, opLayout, superLayout, subLayout,
                isIntegral, isNamedOperator
            )
        } else {
            layoutDisplayMode(
                context, density, opLayout, superLayout, subLayout,
                subOverflow = computeLapOverflow(node.subscript, limitStyle, measureGroup),
                superOverflow = computeLapOverflow(node.superscript, limitStyle, measureGroup)
            )
        }
    }

    private fun resolveLimitsMode(
        node: LatexNode.BigOperator,
        context: RenderContext
    ): Boolean = when (node.limitsMode) {
        LatexNode.BigOperator.LimitsMode.LIMITS -> false
        LatexNode.BigOperator.LimitsMode.NOLIMITS -> true
        LatexNode.BigOperator.LimitsMode.AUTO ->
            !node.limitsInDisplay || context.mathStyle != MathStyle.DISPLAY
    }

    private fun buildOperatorStyle(
        context: RenderContext,
        isNamedOperator: Boolean,
        useKaTeXSizeFont: Boolean
    ): RenderContext {
        // 命名运算符使用 Main，符号运算符使用 KaTeX Size1/Size2。
        val role = if (isNamedOperator) MathFontRole.ROMAN else MathFontRole.LARGE_OPERATOR
        val fontFamily = when {
            useKaTeXSizeFont && context.mathStyle == MathStyle.DISPLAY -> context.fontFamilies?.size2
            useKaTeXSizeFont -> context.fontFamilies?.size1
            else -> context.mathFontProvider?.fontFamilyFor(role)
        }
            ?: context.fontFamilies?.main
            ?: context.fontFamily
        return context.copy(
            fontFamily = fontFamily,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Normal
        )
    }

    private fun layoutSideMode(
        context: RenderContext, density: Density, measurer: TextMeasurer,
        opLayout: NodeLayout, superLayout: NodeLayout?, subLayout: NodeLayout?,
        isIntegral: Boolean, isNamedOperator: Boolean
    ): NodeLayout {
        val axisHeight = LayoutUtils.getAxisHeight(density, context, measurer)
        val fontSizePx = with(density) { context.fontSize.toPx() }

        val opVisualWidth = when {
            isIntegral -> fontSizePx * MathConstants.INTEGRAL_VISUAL_WIDTH
            else -> opLayout.width
        }

        val opDrawX = if (isIntegral) max(
            0f,
            (opVisualWidth - opLayout.width) / 2f
        ) else 0f
        val opActualLeft = if (opDrawX == 0f) opLayout.width else opVisualWidth

        // opLayout 已经是墨水高度，不需要额外估算
        val glyphVisualPart = opLayout.height

        // 积分符号定位策略：
        // 用原始（未拉伸）高度以数学轴为中心确定基准位置
        val opCenter = -axisHeight  // 数学轴 y 坐标

        // Symbol operators center on the math axis. Named operators retain
        // their natural Main-Regular baseline, as KaTeX does for `mop` text.
        val opGlyphDrawY = if (isNamedOperator) {
            -opLayout.baseline
        } else {
            opCenter - glyphVisualPart / 2f
        }
        val opTop = opGlyphDrawY
        val opBottom = opGlyphDrawY + glyphVisualPart

        val limitSpacing = when {
            isIntegral -> 0f
            isNamedOperator -> 0f
            else -> with(density) { MathConstants.SCRIPT_KERN_DP.dp.toPx() }
        }

        val superX = opActualLeft + limitSpacing
        val subX = (opActualLeft - opLayout.italicCorrection).coerceAtLeast(0f) + limitSpacing

        val provider = context.mathFontProvider
        val scriptFontSizePx = with(density) { context.toLimitStyle().fontSize.toPx() }
        val xHeight = provider?.xHeight(fontSizePx) ?: fontSizePx * 0.431f
        val baseHeight = -opGlyphDrawY
        val baseDepth = opGlyphDrawY + glyphVisualPart

        var superShift = if (superLayout != null) {
            baseHeight - (provider?.superscriptDrop(scriptFontSizePx) ?: 0f)
        } else 0f
        var subShift = if (subLayout != null) {
            baseDepth + (provider?.subscriptDrop(scriptFontSizePx) ?: 0f)
        } else 0f

        if (superLayout != null) {
            val minimum = provider?.superscriptShiftUp(
                fontSizePx,
                displayStyle = context.mathStyle == MathStyle.DISPLAY,
                crampedStyle = false
            ) ?: (fontSizePx * MathConstants.SUPERSCRIPT_SHIFT)
            val superDepth = superLayout.height - superLayout.baseline
            superShift = maxOf(superShift, minimum, superDepth + 0.25f * xHeight)
        }

        if (subLayout != null) {
            val hasSuper = superLayout != null
            val minimum = provider?.subscriptShiftDown(fontSizePx, hasSuper)
                ?: (fontSizePx * MathConstants.SUBSCRIPT_SHIFT)
            subShift = if (hasSuper) {
                max(subShift, minimum)
            } else {
                maxOf(subShift, minimum, subLayout.baseline - 0.8f * xHeight)
            }
        }

        if (superLayout != null && subLayout != null) {
            val superDepth = superLayout.height - superLayout.baseline
            val minGap = provider?.subSuperscriptGapMin(fontSizePx)
                ?: (fontSizePx * MathConstants.SCRIPT_MIN_GAP)
            val currentGap = (superShift - superDepth) - (subLayout.baseline - subShift)
            if (currentGap < minGap) {
                subShift += minGap - currentGap
                val psi = 0.8f * xHeight - (superShift - superDepth)
                if (psi > 0f) {
                    superShift += psi
                    subShift -= psi
                }
            }
        }

        val superTop = superLayout?.let { -superShift - it.baseline } ?: opTop
        val subTop = subLayout?.let { subShift - it.baseline } ?: opBottom

        // 边界计算：取所有元素的最小顶部和最大底部
        val glyphTop = opGlyphDrawY
        val glyphBottom = opGlyphDrawY + glyphVisualPart
        val minTop = minOf(
            if (superLayout != null) superTop else opTop,
            glyphTop
        )
        val maxBottom = maxOf(
            if (subLayout != null) subTop + subLayout.height else opBottom,
            glyphBottom
        )
        val totalHeight = maxBottom - minTop
        val baseline = -minTop

        val superRightEdge = superX + (superLayout?.width ?: 0f)
        val subRightEdge = subX + (subLayout?.width ?: 0f)
        val scriptSpace = provider?.spaceAfterScript(fontSizePx) ?: fontSizePx * 0.05f
        val width = max(
            max(opActualLeft, opDrawX + opLayout.width) * MathConstants.BIG_OP_WIDTH_OVERFLOW_FACTOR,
            max(superRightEdge, subRightEdge)
        ) + if (superLayout != null || subLayout != null) scriptSpace else 0f

        return NodeLayout(width, totalHeight, baseline) { x, y ->
            opLayout.draw(this, x + opDrawX, y + baseline + opGlyphDrawY)
            superLayout?.draw(this, x + superX, y + baseline + superTop)
            subLayout?.draw(this, x + subX, y + baseline + subTop)
        }
    }

    /**
     * MathLap 节点的溢出信息。
     * @param contentWidth 内容的实际宽度
     * @param lapType 叠加类型（CLAP/LLAP/RLAP），决定溢出方向
     */
    private data class LapOverflow(val contentWidth: Float, val lapType: LatexNode.MathLap.LapType)

    /**
     * 计算 MathLap（\mathclap/\mathllap/\mathrlap）节点的内容溢出信息。
     *
     * 当上标/下标是 MathLap 节点时，其 NodeLayout.width=0，但实际内容可能
     * 远宽于运算符。此函数返回内容的实际宽度和叠加类型，供 layoutDisplayMode 使用，
     * 确保返回的 NodeLayout 尺寸能完整容纳溢出内容。
     *
     * 注意：解析器可能将 `_{\\mathclap{...}}` 解析为 Group 包裹 MathLap 的结构，
     * 因此需要穿透外层 Group 找到内部的 MathLap 节点。
     *
     * @return 溢出信息（非 MathLap 节点返回 null）
     */
    private fun computeLapOverflow(
        scriptNode: LatexNode?,
        limitStyle: RenderContext,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): LapOverflow? {
        // 直接是 MathLap
        if (scriptNode is LatexNode.MathLap) {
            val contentLayout = measureGroup(scriptNode.content, limitStyle)
            return LapOverflow(contentLayout.width, scriptNode.lapType)
        }
        // 穿透 Group 包裹：_{\\mathclap{...}} 解析为 Group([MathLap])
        if (scriptNode is LatexNode.Group && scriptNode.children.size == 1) {
            val inner = scriptNode.children[0]
            if (inner is LatexNode.MathLap) {
                val contentLayout = measureGroup(inner.content, limitStyle)
                return LapOverflow(contentLayout.width, inner.lapType)
            }
        }
        return null
    }

    /**
     * 计算单个 MathLap 溢出在 display 模式下的左右边界。
     *
     * @param overflow 溢出信息（null 表示无溢出）
     * @param anchorX 叠加锚点的 x 坐标（居中于 baseMaxWidth 的中点）
     * @return (leftBound, rightBound) 相对于 x=0 的左右边界
     */
    private fun lapBounds(overflow: LapOverflow?, anchorX: Float): Pair<Float, Float> {
        if (overflow == null) return 0f to 0f
        return when (overflow.lapType) {
            // CLAP: 内容以锚点为中心居中
            LatexNode.MathLap.LapType.CLAP ->
                (anchorX - overflow.contentWidth / 2f) to (anchorX + overflow.contentWidth / 2f)
            // LLAP: 内容向左扩展，右边界在锚点
            LatexNode.MathLap.LapType.LLAP ->
                (anchorX - overflow.contentWidth) to anchorX
            // RLAP: 内容向右扩展，左边界在锚点
            LatexNode.MathLap.LapType.RLAP ->
                anchorX to (anchorX + overflow.contentWidth)
        }
    }

    private fun layoutDisplayMode(
        context: RenderContext, density: Density,
        opLayout: NodeLayout, superLayout: NodeLayout?, subLayout: NodeLayout?,
        subOverflow: LapOverflow? = null,
        superOverflow: LapOverflow? = null
    ): NodeLayout {
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val provider = context.mathFontProvider
        val upperSpacing = superLayout?.let {
            provider?.upperLimitGap(fontSizePx, it.height - it.baseline)
                ?: max(fontSizePx * 0.111f, fontSizePx * 0.2f - (it.height - it.baseline))
        } ?: 0f
        val lowerSpacing = subLayout?.let {
            provider?.lowerLimitGap(fontSizePx, it.baseline)
                ?: max(fontSizePx * 0.166f, fontSizePx * 0.6f - it.baseline)
        } ?: 0f

        // 基础宽度：基于 op/super/sub 报告的 width
        val baseMaxWidth = max(opLayout.width, max(superLayout?.width ?: 0f, subLayout?.width ?: 0f))

        // 处理 MathLap（\mathclap 等）溢出：
        // MathLap 报告 width=0，但内容基于运算符中心绘制（CLAP 居中/LLAP 向左/RLAP 向右），
        // 可能向左右两侧溢出。需要计算溢出范围并扩展总宽度。
        val opCenter = baseMaxWidth / 2f
        val (subLeft, subRight) = lapBounds(subOverflow, opCenter)
        val (superLeft, superRight) = lapBounds(superOverflow, opCenter)

        // 合并所有溢出的左右边界
        val overflowLeft = min(0f, min(subLeft, superLeft))
        val overflowRight = max(baseMaxWidth, max(subRight, superRight))

        // 需要的左移量，确保所有内容在正坐标区域
        val leftShift = if (overflowLeft < 0f) -overflowLeft else 0f

        val maxWidth = overflowRight + leftShift

        // opLayout 已经是墨水高度，不需要额外估算
        val glyphVisualPart = opLayout.height

        // 上限实际底部：使用 height（完整绘制区域），避免 descent 部分与运算符重叠
        val superBottom = superLayout?.height ?: 0f

        // 布局坐标 (y=0 = NodeLayout 顶部):
        val superDrawY = 0f
        val opDrawY = superBottom + upperSpacing
        val subDrawY = opDrawY + glyphVisualPart + lowerSpacing
        val totalHeight = subDrawY + (subLayout?.height ?: 0f)

        // baseline：保持运算符字体自身的基线。
        val baseline = opDrawY + opLayout.baseline

        return NodeLayout(maxWidth, totalHeight, baseline) { x, y ->
            // leftShift 确保溢出内容不在负坐标区域
            opLayout.draw(this, x + leftShift + (baseMaxWidth - opLayout.width) / 2, y + opDrawY)
            superLayout?.draw(this, x + leftShift + (baseMaxWidth - superLayout.width) / 2, y + superDrawY)
            subLayout?.draw(this, x + leftShift + (baseMaxWidth - subLayout.width) / 2, y + subDrawY)
        }
    }
}
