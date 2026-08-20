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


package com.hrm.latex.renderer.model

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.font.MathFont
import com.hrm.latex.renderer.font.MathFontProvider
import com.hrm.latex.renderer.font.MathFontProviderFactory
import com.hrm.latex.renderer.utils.MathConstants
import com.hrm.latex.renderer.utils.parseColor
import kotlin.ConsistentCopyVisibility

/**
 * 子表达式高亮配置
 *
 * 支持按 LaTeX 子串模式或 AST 节点位置索引 来指定高亮区域。
 * 高亮在 Draw 阶段以半透明背景矩形叠加渲染。
 *
 * @param ranges 高亮范围列表
 */
data class HighlightConfig(
    val ranges: List<HighlightRange> = emptyList()
)

/**
 * 单个高亮范围
 *
 * @param pattern LaTeX 子串模式（精确匹配渲染时的文本内容）。
 *        为 null 时通过 [nodeIndices] 指定。
 * @param nodeIndices 文档根节点 children 的索引范围（从 0 开始，含首尾）。
 *        为 null 时通过 [pattern] 指定。
 * @param color 高亮背景色
 * @param borderColor 高亮边框色（null 则不绘制边框）
 */
data class HighlightRange(
    val pattern: String? = null,
    val nodeIndices: IntRange? = null,
    val color: Color = Color(0x3300AAFF),
    val borderColor: Color? = null
)

/**
 * LaTeX 单一色板。
 */
data class LatexThemeColors(
    val color: Color,
    val backgroundColor: Color = Color.Transparent
)

/**
 * LaTeX 主题。
 */
sealed interface LatexTheme {
    @ConsistentCopyVisibility
    data class Adaptive internal constructor(
        internal val light: LatexThemeColors,
        internal val dark: LatexThemeColors
    ) : LatexTheme

    @ConsistentCopyVisibility
    data class Fixed internal constructor(
        internal val colors: LatexThemeColors
    ) : LatexTheme

    companion object {
        internal val DefaultLightColors = LatexThemeColors(
            color = Color.Black,
            backgroundColor = Color.Transparent
        )

        internal val DefaultDarkColors = LatexThemeColors(
            color = Color.White,
            backgroundColor = Color.Transparent
        )

        fun auto(
            light: LatexThemeColors = DefaultLightColors,
            dark: LatexThemeColors = DefaultDarkColors
        ): LatexTheme = Adaptive(
            light = light,
            dark = dark
        )

        fun light(
            color: Color = DefaultLightColors.color,
            backgroundColor: Color = DefaultLightColors.backgroundColor
        ): LatexTheme = Fixed(
            colors = LatexThemeColors(
                color = color,
                backgroundColor = backgroundColor
            )
        )

        fun dark(
            color: Color = DefaultDarkColors.color,
            backgroundColor: Color = DefaultDarkColors.backgroundColor
        ): LatexTheme = Fixed(
            colors = LatexThemeColors(
                color = color,
                backgroundColor = backgroundColor
            )
        )

        /**
         * 将 Material 3 当前 [ColorScheme] 映射为固定的 LaTeX 主题。
         */
        fun material3(colorScheme: ColorScheme): LatexTheme = Fixed(
            colors = LatexThemeColors(
                color = colorScheme.onSurface,
                backgroundColor = colorScheme.surface
            )
        )

        /**
         * 使用当前 Compose Material 3 [MaterialTheme.colorScheme] 生成 LaTeX 主题。
         *
         * 这会让 LaTeX 的前景色和背景色跟随当前 Material 3 主题。
         */
        @Composable
        fun material3(): LatexTheme = material3(MaterialTheme.colorScheme)
    }
}

/**
 * LaTeX 渲染配置（用户外部设置）。
 *
 * 主题统一通过 [theme] 控制，推荐使用以下入口：
 * - [LatexTheme.auto]：跟随当前环境的深浅色
 * - [LatexTheme.light]：固定浅色主题
 * - [LatexTheme.dark]：固定深色主题
 * - [LatexTheme.material3]：跟随当前 Material 3 `ColorScheme`
 *
 * 使用示例：
 * ```kotlin
 * LatexConfig(theme = LatexTheme.auto())
 * LatexConfig(theme = LatexTheme.light())
 * LatexConfig(theme = LatexTheme.dark())
 * LatexConfig(theme = LatexTheme.material3())
 * ```
 */
data class LatexConfig(
    val fontSize: TextUnit = 20.sp,
    val mathFont: MathFont = MathFont.Default,
    val theme: LatexTheme = LatexTheme.auto(),
    val lineBreaking: LineBreakingConfig = LineBreakingConfig(),
    val highlight: HighlightConfig = HighlightConfig(),
    val accessibilityEnabled: Boolean = false,
    /**
     * 交互式公式：点击子表达式时的回调。
     * 传入被点击节点的 LaTeX 源码范围（起始和结束偏移量）。
     * 为 null 时不启用点击交互。
     *
     * 使用示例：
     * ```kotlin
     * LatexConfig(
     *     onNodeClick = { startOffset, endOffset, latex ->
     *         println("Clicked: ${latex.substring(startOffset, endOffset)}")
     *     }
     * )
     * ```
     */
    val onNodeClick: ((startOffset: Int, endOffset: Int, latex: String) -> Unit)? = null,
    /**
     * 超链接点击回调：点击 \href 或 \url 节点时触发，直接返回 URL 字符串。
     * 为 null 时不启用超链接专用点击（仍可通过 onNodeClick 处理）。
     *
     * 当 onHyperlinkClick 和 onNodeClick 同时配置时，点击超链接节点
     * 会**同时**触发两个回调（先触发 onHyperlinkClick，再触发 onNodeClick）。
     *
     * 使用示例：
     * ```kotlin
     * LatexConfig(
     *     onHyperlinkClick = { url ->
     *         uriHandler.openUri(url)
     *     }
     * )
     * ```
     */
    val onHyperlinkClick: ((url: String) -> Unit)? = null,
    /**
     * 是否启用 NodeLayout 缓存。
     *
     * 启用后，相同 AST 子树 + 相同 RenderContext 的测量结果会被缓存复用，
     * 避免重复测量，适用于长公式、编辑器实时预览等频繁重绘场景。
     *
     * 默认关闭。建议在 WYSIWYG 编辑器或增量输入场景中开启。
     *
     * 使用示例：
     * ```kotlin
     * LatexConfig(enableLayoutCache = true)
     * ```
     */
    val enableLayoutCache: Boolean = false,
)

/**
 * line breaking configuration
 *
 * @property enabled whether automatic line breaking is enabled
 * @property maxWidth maximum line width in pixels, null means no limit
 */
data class LineBreakingConfig(
    val enabled: Boolean = false,
    val maxWidth: Float? = null
)

/**
 * 数学样式模式 (TeXbook §702)
 *
 * 影响字号缩放和间距行为：
 * - DISPLAY/TEXT: 全尺寸，MEDIUM/THICK 间距生效
 * - SCRIPT: 0.7x 缩放，MEDIUM/THICK 间距降为 0
 * - SCRIPT_SCRIPT: 0.5x 缩放，MEDIUM/THICK 间距降为 0
 */
enum class MathStyle(val scriptLevel: Int) {
    DISPLAY(0),
    TEXT(0),
    SCRIPT(1),
    SCRIPT_SCRIPT(2);

    val isScript get() = scriptLevel > 0

    fun scaleFactor(): Float = when (this) {
        DISPLAY, TEXT -> 1.0f
        SCRIPT -> MathConstants.SCRIPT_SCALE
        SCRIPT_SCRIPT -> MathConstants.SCRIPT_SCRIPT_SCALE
    }

    /** 进入上下标时的样式转换 */
    fun toScript(): MathStyle = when (this) {
        DISPLAY, TEXT -> SCRIPT
        SCRIPT, SCRIPT_SCRIPT -> SCRIPT_SCRIPT
    }

    /** 分数子式的样式转换 (TeXbook §694) */
    fun toFractionChild(): MathStyle = when (this) {
        DISPLAY -> TEXT
        TEXT -> SCRIPT
        SCRIPT -> SCRIPT_SCRIPT
        SCRIPT_SCRIPT -> SCRIPT_SCRIPT
    }

    /** 大型运算符上下限的样式转换 */
    fun toLimit(): MathStyle = toScript()
}

/**
 * 字体变体类型
 */
enum class FontVariant {
    NORMAL,
    BLACKBOARD_BOLD,
    CALLIGRAPHIC,
    FRAKTUR,
    SCRIPT
}

/**
 * 文本方向
 */
enum class TextDirection {
    /** 从左到右（默认） */
    LTR,
    /** 从右到左（阿拉伯语、希伯来语等） */
    RTL
}

/**
 * 布局提示：父级向子级传递的单向布局通信通道。
 *
 * 这些字段不属于渲染样式状态，而是 measureGroup 阶段的临时参数。
 * 独立为数据类后，RenderContext 的 copy() 更清晰：
 * 样式变换（字体、颜色、缩放）不会意外携带布局提示。
 */
internal data class LayoutHints(
    /** 大型运算符（积分等）的目标高度提示，由 measureGroup 注入 */
    val bigOpHeightHint: Float? = null,
    /** 自动断行的最大行宽（px），null 表示不限制 */
    val maxLineWidth: Float? = null,
    /** 是否启用自动断行 */
    val lineBreakingEnabled: Boolean = false,
)

/**
 * 内部渲染上下文（渲染树遍历过程中的状态）
 *
 * 使用 data class 以便利的 copy() 操作。
 * 注意：不应将 RenderContext 作为 remember 的 key —— 应使用
 * 产生它的输入（LatexConfig + isDark 等）作为 key。
 *
 * 职责划分：
 * - 字体状态：fontFamily, fontFamilies, fontWeight, fontStyle, fontVariant, isVariantFontFamily
 * - 样式状态：fontSize, color, errorColor, mathStyle
 * - 布局提示：[layoutHints]（父→子的单向通信，与样式无关）
 * - 排版参数：[mathFontProvider]（KaTeX 字体度量与 TeX 排版参数源）
 */
internal data class RenderContext(
    // ── 样式状态 ──
    val fontSize: TextUnit,
    val baseFontSize: TextUnit = fontSize,
    val color: Color,
    val errorColor: Color = Color(0xFFCC0000),
    val mathStyle: MathStyle = MathStyle.DISPLAY,
    // ── 字体状态 ──
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val fontFamily: FontFamily? = null,
    val fontVariant: FontVariant = FontVariant.NORMAL,
    val fontFamilies: LatexFontFamilies? = null,
    val isVariantFontFamily: Boolean = false,
    // ── 布局提示 ──
    val layoutHints: LayoutHints = LayoutHints(),
    // ── 排版参数 ──
    val mathFontProvider: MathFontProvider? = null,
    // ── 公式编号 ──
    val equationNumbering: com.hrm.latex.renderer.layout.EquationNumberingState? = null,
    // ── 文本方向 ──
    val textDirection: TextDirection = TextDirection.LTR,
) {
    /** Per-context axis measurement. It is never shared globally or included in data-class equality. */
    internal var cachedAxisHeightDensity: Float = Float.NaN
    internal var cachedAxisHeightFontScale: Float = Float.NaN
    internal var cachedAxisHeight: Float = Float.NaN

    /**
     * 缓存的 TextStyle，避免每次 textStyle() 调用都创建新对象。
     * 定义在 body 中，不参与 data class 的 equals/hashCode。
     * copy() 产生新实例时会重新惰性计算。
     */
    val cachedTextStyle: TextStyle by lazy {
        TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily
        )
    }
}

/**
 * 从外部配置创建初始上下文
 *
 * @param isDark 当前环境是否为深色模式，仅在 `LatexTheme.auto(...)` 时参与主题解析
 * @param fontFamilies 内置 KaTeX TTF 字体家族
 * @param provider 预创建的 MathFontProvider（由调用方缓存，避免每次重组创建新实例）
 */
internal fun LatexConfig.toContext(
    isDark: Boolean,
    fontFamilies: LatexFontFamilies,
    provider: MathFontProvider? = null
): RenderContext {
    val resolvedTheme = theme.resolve(isDark)
    val resolvedColor = when {
        resolvedTheme.color != Color.Unspecified -> resolvedTheme.color
        isDark -> LatexTheme.DefaultDarkColors.color
        else -> LatexTheme.DefaultLightColors.color
    }

    val resolvedErrorColor = if (isDark) Color(0xFFFF6666) else Color(0xFFCC0000)

    // 使用调用方传入的 provider（已缓存），或按需创建（兼容旧调用路径）
    val resolvedProvider = provider ?: MathFontProviderFactory.create(fontFamilies)

    return RenderContext(
        fontSize = fontSize,
        baseFontSize = fontSize,
        color = resolvedColor,
        errorColor = resolvedErrorColor,
        fontFamily = fontFamilies.main,
        fontFamilies = fontFamilies,
        isVariantFontFamily = false,
        layoutHints = LayoutHints(
            maxLineWidth = if (lineBreaking.enabled) lineBreaking.maxWidth else null,
            lineBreakingEnabled = lineBreaking.enabled,
        ),
        mathFontProvider = resolvedProvider
    )
}

internal fun LatexTheme.resolve(systemInDarkTheme: Boolean): LatexThemeColors = when (this) {
    is LatexTheme.Adaptive -> if (systemInDarkTheme) dark else light
    is LatexTheme.Fixed -> colors
}

internal fun LatexConfig.resolveThemeColors(isDark: Boolean): LatexThemeColors = theme.resolve(isDark)

internal fun RenderContext.textStyle(): TextStyle = cachedTextStyle

/**
 * 进入上下标时的样式转换：使用 MathStyle 状态机自动决定字号
 */
internal fun RenderContext.toScriptStyle(): RenderContext {
    val newStyle = mathStyle.toScript()
    return copy(
        fontSize = fontSize * (newStyle.scaleFactor() / mathStyle.scaleFactor()),
        mathStyle = newStyle
    )
}

/**
 * 进入分数子式时的样式转换
 */
internal fun RenderContext.toFractionChildStyle(): RenderContext {
    val newStyle = mathStyle.toFractionChild()
    return copy(
        fontSize = fontSize * (newStyle.scaleFactor() / mathStyle.scaleFactor()),
        mathStyle = newStyle
    )
}

/**
 * 进入大型运算符上下限时的样式转换
 */
internal fun RenderContext.toLimitStyle(): RenderContext {
    val newStyle = mathStyle.toLimit()
    return copy(
        fontSize = fontSize * (newStyle.scaleFactor() / mathStyle.scaleFactor()),
        mathStyle = newStyle
    )
}

internal fun RenderContext.shrink(factor: Float): RenderContext = copy(fontSize = fontSize * factor)
internal fun RenderContext.grow(factor: Float): RenderContext = copy(fontSize = fontSize * factor)

internal fun RenderContext.withColor(colorString: String): RenderContext =
    parseColor(colorString)?.let {
        copy(color = it)
    } ?: this

internal fun RenderContext.applyStyle(styleType: LatexNode.Style.StyleType): RenderContext {
    val families = fontFamilies

    return when (styleType) {
        LatexNode.Style.StyleType.BOLD -> copy(
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Normal,
            fontFamily = families?.main ?: fontFamily,
            isVariantFontFamily = false
        )

        LatexNode.Style.StyleType.BOLD_SYMBOL -> copy(fontWeight = FontWeight.Bold)

        LatexNode.Style.StyleType.ITALIC -> copy(
            fontStyle = FontStyle.Italic,
            fontFamily = families?.main ?: fontFamily,
            isVariantFontFamily = false
        )
        LatexNode.Style.StyleType.ROMAN -> copy(
            fontStyle = FontStyle.Normal,
            fontFamily = families?.main ?: fontFamily,
            isVariantFontFamily = false
        )

        LatexNode.Style.StyleType.SANS_SERIF -> copy(
            fontFamily = families?.sansSerif ?: fontFamily,
            fontStyle = FontStyle.Normal,
            isVariantFontFamily = false
        )

        LatexNode.Style.StyleType.MONOSPACE -> copy(
            fontFamily = families?.monospace ?: fontFamily,
            fontStyle = FontStyle.Normal,
            isVariantFontFamily = false
        )

        LatexNode.Style.StyleType.BLACKBOARD_BOLD -> {
            val variantFamily = families?.ams
            copy(
                fontVariant = FontVariant.BLACKBOARD_BOLD,
                fontFamily = variantFamily ?: fontFamily,
                fontStyle = FontStyle.Normal,
                isVariantFontFamily = variantFamily != null
            )
        }

        LatexNode.Style.StyleType.CALLIGRAPHIC -> {
            val variantFamily = families?.caligraphic
            copy(
                fontVariant = FontVariant.CALLIGRAPHIC,
                fontFamily = variantFamily ?: fontFamily,
                fontStyle = FontStyle.Normal,
                isVariantFontFamily = variantFamily != null
            )
        }

        LatexNode.Style.StyleType.FRAKTUR -> {
            val variantFamily = families?.fraktur
            copy(
                fontVariant = FontVariant.FRAKTUR,
                fontFamily = variantFamily ?: fontFamily,
                fontStyle = FontStyle.Normal,
                isVariantFontFamily = variantFamily != null
            )
        }

        LatexNode.Style.StyleType.SCRIPT -> {
            val variantFamily = families?.script
            copy(
                fontVariant = FontVariant.SCRIPT,
                fontFamily = variantFamily ?: fontFamily,
                fontStyle = FontStyle.Normal,
                isVariantFontFamily = variantFamily != null
            )
        }
    }
}

/**
 * 应用数学模式（内部命令触发）
 */
internal fun RenderContext.applyMathStyle(mathStyleType: LatexNode.MathStyle.MathStyleType): RenderContext {
    val newMode = when (mathStyleType) {
        LatexNode.MathStyle.MathStyleType.DISPLAY -> MathStyle.DISPLAY
        LatexNode.MathStyle.MathStyleType.TEXT -> MathStyle.TEXT
        LatexNode.MathStyle.MathStyleType.SCRIPT -> MathStyle.SCRIPT
        LatexNode.MathStyle.MathStyleType.SCRIPT_SCRIPT -> MathStyle.SCRIPT_SCRIPT
    }

    val scaleFactor = newMode.scaleFactor() / mathStyle.scaleFactor()

    return copy(
        fontSize = fontSize * scaleFactor,
        mathStyle = newMode
    )
}

internal fun RenderContext.applyFontSize(sizeType: LatexNode.FontSize.SizeType): RenderContext {
    val scale = when (sizeType) {
        LatexNode.FontSize.SizeType.TINY -> 0.5f
        LatexNode.FontSize.SizeType.SCRIPT_SIZE -> 0.7f
        LatexNode.FontSize.SizeType.FOOTNOTE_SIZE -> 0.8f
        LatexNode.FontSize.SizeType.SMALL -> 0.9f
        LatexNode.FontSize.SizeType.NORMAL_SIZE -> 1.0f
        LatexNode.FontSize.SizeType.LARGE -> 1.2f
        LatexNode.FontSize.SizeType.LARGE_2 -> 1.44f
        LatexNode.FontSize.SizeType.LARGE_3 -> 1.728f
        LatexNode.FontSize.SizeType.HUGE -> 2.074f
        LatexNode.FontSize.SizeType.HUGE_2 -> 2.488f
    }
    return copy(fontSize = baseFontSize * scale * mathStyle.scaleFactor())
}
