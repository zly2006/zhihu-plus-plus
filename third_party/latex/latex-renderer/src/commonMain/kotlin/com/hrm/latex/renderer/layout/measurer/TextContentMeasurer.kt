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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.model.FontVariant
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle
import com.hrm.latex.renderer.utils.FontResolver
import com.hrm.latex.renderer.utils.InkBoundsEstimator
import com.hrm.latex.renderer.utils.MathConstants
import com.hrm.latex.renderer.utils.MathFontUtils
import com.hrm.latex.renderer.utils.parseDimension
import com.hrm.latex.renderer.utils.spaceWidthPx
import kotlin.reflect.KClass

/**
 * 文本内容测量器
 */
internal class TextContentMeasurer : NodeMeasurer {

    override val handledNodeTypes: Set<KClass<out LatexNode>> = setOf(
        LatexNode.Text::class,
        LatexNode.TextMode::class,
        LatexNode.Symbol::class,
        LatexNode.Operator::class,
        LatexNode.OperatorName::class,
        LatexNode.ModOperator::class,
        LatexNode.Command::class,
        LatexNode.Space::class,
        LatexNode.HSpace::class
    )

    override fun measure(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        measureNode: (LatexNode, RenderContext) -> NodeLayout,
        measureGroup: (List<LatexNode>, RenderContext) -> NodeLayout
    ): NodeLayout {
        return when (node) {
            is LatexNode.Text -> measureText(node.content, context, measurer, density)
            is LatexNode.TextMode -> measureTextMode(node.text, context, measurer)
            is LatexNode.Symbol -> measureSymbol(node, context, measurer, density)
            is LatexNode.Operator -> {
                val operatorGap =
                    with(density) { (context.fontSize * MathConstants.OPERATOR_RIGHT_GAP).toPx() }
                val layout = measureText(
                    node.op,
                    context.copy(
                        fontStyle = FontStyle.Normal,
                        fontFamily = context.fontFamilies?.main ?: context.fontFamily
                    ),
                    measurer,
                    density
                )
                NodeLayout(layout.width + operatorGap, layout.height, layout.baseline, draw = layout.draw)
            }

            is LatexNode.Command -> {
                // 用 errorColor 标记未识别的命令，展示为 \commandName
                val errorContext = context.copy(color = context.errorColor)
                measureText("\\${node.name}", errorContext, measurer, density)
            }
            is LatexNode.OperatorName -> {
                val operatorGap =
                    with(density) { (context.fontSize * MathConstants.OPERATOR_RIGHT_GAP).toPx() }
                val layout = measureText(
                    node.name,
                    context.copy(
                        fontStyle = FontStyle.Normal,
                        fontFamily = context.fontFamilies?.main ?: context.fontFamily
                    ),
                    measurer,
                    density
                )
                NodeLayout(layout.width + operatorGap, layout.height, layout.baseline, draw = layout.draw)
            }
            is LatexNode.ModOperator -> measureModOperator(node, context, measurer, density)
            is LatexNode.Space -> measureSpace(node.type, context, density)
            is LatexNode.HSpace -> measureHSpace(node, context, density)
            is LatexNode.NewLine -> NodeLayout(0f, 0f, 0f) { _, _ -> }
            else -> throw IllegalArgumentException("Unsupported node type: ${node::class.simpleName}")
        }
    }

    private fun measureText(
        text: String, context: RenderContext, measurer: TextMeasurer, density: Density
    ): NodeLayout {
        // 字体变体处理逻辑：
        // - 如果设置了变体字体家族(如 blackboardBold, calligraphic 等)，优先使用字体文件自身的字形
        // - 否则，使用 Unicode 数学字母作为降级方案
        val transformedText = if (context.isVariantFontFamily) {
            // 直接使用原始字符，让字体文件提供正确的字形
            text
        } else {
            // 使用 Unicode 数学字母映射作为降级方案
            applyFontVariant(text, context.fontVariant)
        }

        val resolvedStyle =
            if (context.fontStyle == null && context.fontVariant == FontVariant.NORMAL) {
                // KaTeX: mathnormal 字母走 Math-Italic，数字和标点走 Main-Regular。
                return measureKaTeXMathText(transformedText, context, measurer, density)
            } else {
                context
            }

        return measureAnnotatedText(transformedText, resolvedStyle, measurer, density)
    }

    private fun measureSymbol(
        node: LatexNode.Symbol, context: RenderContext, measurer: TextMeasurer, density: Density
    ): NodeLayout {
        // 1. 通过 FontResolver 获取符号的字体路由信息
        //    优先用命令名查找，失败时用 unicode 字符反向查找
        val symbolInfo = FontResolver.resolveSymbol(node.symbol, context.fontFamilies)
            ?: FontResolver.resolveSymbol(node.unicode, context.fontFamilies)

        if (symbolInfo != null) {
            // 使用 KaTeX 字体渲染（标准 Unicode 编码）
            val fontFamily = FontResolver.getFontForSymbol(symbolInfo, context.fontFamilies)
            val resolvedStyle = context.copy(
                fontStyle = symbolInfo.fontStyle,
                fontFamily = fontFamily ?: context.fontFamily
            )

            val isMathItalic = symbolInfo.fontCategory == com.hrm.latex.renderer.utils.FontCategory.MATH_ITALIC
            val correction = if (isMathItalic) {
                context.mathFontProvider?.italicCorrection(symbolInfo.texGlyph, with(density) { context.fontSize.toPx() }) ?: 0f
            } else 0f
            val bytes = when (symbolInfo.fontCategory) {
                com.hrm.latex.renderer.utils.FontCategory.MATH_ITALIC ->
                    context.fontFamilies?.mathBytes
                com.hrm.latex.renderer.utils.FontCategory.BLACKBOARD_BOLD ->
                    context.fontFamilies?.amsBytes
                com.hrm.latex.renderer.utils.FontCategory.EXTENSION ->
                    context.fontFamilies?.size1Bytes
                else -> context.fontFamilies?.mainBytes
            }
            return measureAnnotatedText(
                symbolInfo.texGlyph, resolvedStyle, measurer, density, bytes, correction
            )
        }

        // 2. 回退：FontResolver 未覆盖的符号，使用 Unicode 字符直接渲染
        val text = node.unicode.ifEmpty { node.symbol }

        var resolvedStyle = if (context.fontStyle == null) {
            when {
                isLowercaseGreek(node.symbol) -> context.copy(fontStyle = FontStyle.Italic)
                isVarUppercaseGreek(node.symbol) -> context.copy(fontStyle = FontStyle.Italic)
                isUppercaseGreek(node.symbol) -> context.copy(fontStyle = FontStyle.Normal)
                else -> context
            }
        } else {
            context
        }

        return measureAnnotatedText(text, resolvedStyle, measurer, density)
    }

    private fun measureKaTeXMathText(
        text: String,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density
    ): NodeLayout {
        if (text.isEmpty()) return NodeLayout.EMPTY

        val families = context.fontFamilies ?: return measureAnnotatedText(
            text, context.copy(fontStyle = FontStyle.Italic), measurer, density
        )
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val layouts = mutableListOf<NodeLayout>()
        var start = 0
        while (start < text.length) {
            val mathRun = text[start].isLetter()
            var end = start + 1
            while (end < text.length && text[end].isLetter() == mathRun) end++
            val run = text.substring(start, end)
            if (mathRun) {
                val correction = context.mathFontProvider?.italicCorrection(run, fontSizePx) ?: 0f
                layouts += measureAnnotatedText(
                    run,
                    context.copy(fontFamily = families.math, fontStyle = FontStyle.Italic),
                    measurer,
                    density,
                    families.mathBytes,
                    correction
                )
            } else {
                layouts += measureAnnotatedText(
                    run,
                    context.copy(fontFamily = families.main, fontStyle = FontStyle.Normal),
                    measurer,
                    density,
                    families.mainBytes
                )
            }
            start = end
        }
        return combineRuns(layouts)
    }

    private fun combineRuns(layouts: List<NodeLayout>): NodeLayout {
        if (layouts.size == 1) return layouts.first()
        val baseline = layouts.maxOf { it.baseline }
        val descent = layouts.maxOf { it.height - it.baseline }
        val width = layouts.sumOf { it.width.toDouble() }.toFloat()
        return NodeLayout(width, baseline + descent, baseline, layouts.last().italicCorrection) { x, y ->
            var currentX = x
            for (layout in layouts) {
                layout.draw(this, currentX, y + baseline - layout.baseline)
                currentX += layout.width
            }
        }
    }

    private fun measureAnnotatedText(
        text: String,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density,
        fontBytes: ByteArray? = null,
        italicCorrection: Float = 0f
    ): NodeLayout {
        val result = measurer.measure(AnnotatedString(text), context.textStyle())
        val baseWidth = result.size.width.toFloat()
        val fontSizePx = with(density) { context.fontSize.toPx() }
        val precise = fontBytes?.let {
            InkBoundsEstimator.measurePrecise(
                text = text,
                fontSizePx = fontSizePx,
                fontBytes = it,
                baseline = result.firstBaseline,
                fontWeightValue = context.fontWeight?.weight ?: 400
            )
        }
        val topOffset = precise?.inkTopOffset ?: 0f
        val height = precise?.inkHeight ?: result.size.height.toFloat()
        val baseline = precise?.inkBaseline ?: result.firstBaseline

        return NodeLayout(
            width = baseWidth + italicCorrection,
            height = height,
            baseline = baseline,
            italicCorrection = italicCorrection
        ) { x, y ->
            drawText(result, topLeft = Offset(x, y - topOffset))
        }
    }

    /**
     * 应用字体变体的降级方案（Unicode 映射）
     *
     * 注意：这是降级方案，仅在无法加载字体时使用。
     * 正常情况下应该直接使用对应的字体系列（blackboardBold, calligraphic等）。
     */
    private fun applyFontVariant(text: String, variant: FontVariant): String {
        return when (variant) {
            FontVariant.BLACKBOARD_BOLD -> MathFontUtils.toBlackboardBold(text)
            FontVariant.CALLIGRAPHIC -> MathFontUtils.toCalligraphic(text)
            else -> text
        }
    }

    private fun isLowercaseGreek(symbol: String): Boolean {
        return symbol in LOWERCASE_GREEK_SYMBOLS
    }

    private fun isUppercaseGreek(symbol: String): Boolean {
        return symbol in UPPERCASE_GREEK_SYMBOLS
    }

    private fun isVarUppercaseGreek(symbol: String): Boolean {
        return symbol in VAR_UPPERCASE_GREEK_SYMBOLS
    }

    companion object {
        /** 小写希腊字母命令名集合（编译时常量，避免每次调用重新创建） */
        private val LOWERCASE_GREEK_SYMBOLS = setOf(
            "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa",
            "lambda", "mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi",
            "chi", "psi", "omega",
            "varpi", "varrho", "varsigma", "vartheta", "varphi", "varepsilon"
        )

        /** 大写希腊字母命令名集合 */
        private val UPPERCASE_GREEK_SYMBOLS = setOf(
            "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa",
            "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi",
            "Chi", "Psi", "Omega"
        )

        /** 大写希腊字母斜体变量变体命令名集合 */
        private val VAR_UPPERCASE_GREEK_SYMBOLS = setOf(
            "varGamma", "varDelta", "varTheta", "varLambda", "varXi",
            "varPi", "varSigma", "varUpsilon", "varPhi", "varPsi", "varOmega"
        )

    }

    private fun measureTextMode(
        text: String, context: RenderContext, measurer: TextMeasurer
    ): NodeLayout {
        val textStyle = context.copy(
            fontStyle = FontStyle.Normal,
            fontFamily = context.fontFamilies?.main ?: context.fontFamily,
            fontWeight = context.fontWeight ?: FontWeight.Normal
        ).textStyle()
        val result = measurer.measure(AnnotatedString(text), textStyle)

        return NodeLayout(
            result.size.width.toFloat(),
            result.size.height.toFloat(),
            result.firstBaseline
        ) { x, y ->
            drawText(result, topLeft = Offset(x, y))
        }
    }

    private fun measureModOperator(
        node: LatexNode.ModOperator,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density
    ): NodeLayout {
        val romanContext = context.copy(
            fontStyle = FontStyle.Normal,
            fontFamily = context.fontFamilies?.main ?: context.fontFamily
        )
        val fontSizePx = with(density) { context.fontSize.toPx() }

        return when (node.modStyle) {
            LatexNode.ModOperator.ModStyle.BMOD -> {
                // \bmod → " mod " (二元运算符间距)
                val modLayout = measureAnnotatedText("mod", romanContext, measurer, density)
                val gap = fontSizePx * MathConstants.THICK_SPACE
                val totalWidth = gap + modLayout.width + gap
                NodeLayout(totalWidth, modLayout.height, modLayout.baseline) { x, y ->
                    modLayout.draw(this, x + gap, y)
                }
            }
            LatexNode.ModOperator.ModStyle.PMOD -> {
                // \pmod{n} → " (mod n)"
                val thinGap = fontSizePx * MathConstants.THIN_SPACE
                val leftParenLayout = measureAnnotatedText("(", romanContext, measurer, density)
                val modLayout = measureAnnotatedText("mod", romanContext, measurer, density)
                val rightParenLayout = measureAnnotatedText(")", romanContext, measurer, density)

                val contentLayout = if (node.content != null) {
                    measureGlobalRef(node.content!!, context, measurer, density)
                } else null

                val spaceBeforeMod = fontSizePx * MathConstants.QUAD_SPACE
                var totalWidth = spaceBeforeMod + leftParenLayout.width + modLayout.width
                if (contentLayout != null) {
                    totalWidth += thinGap + contentLayout.width
                }
                totalWidth += rightParenLayout.width

                val maxAscent = maxOf(
                    leftParenLayout.baseline, modLayout.baseline,
                    rightParenLayout.baseline,
                    contentLayout?.baseline ?: 0f
                )
                val maxDescent = maxOf(
                    leftParenLayout.height - leftParenLayout.baseline,
                    modLayout.height - modLayout.baseline,
                    rightParenLayout.height - rightParenLayout.baseline,
                    (contentLayout?.height ?: 0f) - (contentLayout?.baseline ?: 0f)
                )
                val height = maxAscent + maxDescent

                NodeLayout(totalWidth, height, maxAscent) { x, y ->
                    var cx = x + spaceBeforeMod
                    leftParenLayout.draw(this, cx, y + maxAscent - leftParenLayout.baseline)
                    cx += leftParenLayout.width
                    modLayout.draw(this, cx, y + maxAscent - modLayout.baseline)
                    cx += modLayout.width
                    if (contentLayout != null) {
                        cx += thinGap
                        contentLayout.draw(this, cx, y + maxAscent - contentLayout.baseline)
                        cx += contentLayout.width
                    }
                    rightParenLayout.draw(this, cx, y + maxAscent - rightParenLayout.baseline)
                }
            }
            LatexNode.ModOperator.ModStyle.MOD -> {
                // \mod → "  mod " (大间距)
                val modLayout = measureAnnotatedText("mod", romanContext, measurer, density)
                val gap = fontSizePx * MathConstants.QUAD_SPACE
                val totalWidth = gap + modLayout.width + gap
                NodeLayout(totalWidth, modLayout.height, modLayout.baseline) { x, y ->
                    modLayout.draw(this, x + gap, y)
                }
            }
        }
    }

    /**
     * 通用测量辅助：避免类型系统限制
     */
    private fun measureGlobalRef(
        node: LatexNode,
        context: RenderContext,
        measurer: TextMeasurer,
        density: Density
    ): NodeLayout {
        // 直接用 measureAnnotatedText 作为简化处理
        val text = when (node) {
            is LatexNode.Text -> node.content
            is LatexNode.Group -> {
                node.children.joinToString("") {
                    when (it) {
                        is LatexNode.Text -> it.content
                        is LatexNode.Symbol -> it.unicode
                        else -> ""
                    }
                }
            }
            is LatexNode.Symbol -> node.unicode
            else -> ""
        }
        return measureAnnotatedText(text, context, measurer, density)
    }

    private fun measureSpace(
        type: LatexNode.Space.SpaceType, context: RenderContext, density: Density
    ): NodeLayout {
        val width = spaceWidthPx(context, type, density)
        return NodeLayout(width, 0f, 0f) { _, _ -> }
    }

    private fun measureHSpace(
        node: LatexNode.HSpace, context: RenderContext, density: Density
    ): NodeLayout {
        val width = parseDimension(node.dimension, context, density)
        return NodeLayout(width, 0f, 0f) { _, _ -> }
    }
}
