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

import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.min

/**
 * 字体类别，决定墨水边界的估算策略。
 *
 * 不同字体的 ascent/descent 空白比例差异很大：
 * - KaTeX_Size1~4 (EXTENSION): 大型定界符，可能有较大 descent
 * - KaTeX_Main (SYMBOL): 大型运算符 (∑, ∫ 等)，标准 Unicode 编码
 * - KaTeX_Main/KaTeX_Math (TEXT): 行框基本等于墨水区域
 */
enum class InkFontCategory {
    /** 大型运算符/定界符 — glyph 可能包含较大 descent 空白 */
    EXTENSION,
    /** 普通文本/数学字体 — 行框高度已接近墨水边界 */
    TEXT
}

/**
 * 文字墨水边界结果。
 *
 * 传统 TextLayoutResult.size 返回的是行框高度（含 ascent/descent/leading 空白），
 * 对于大型数学符号字体，行框可能大于实际墨水区域。
 * 本类提供去除空白后的精确墨水度量。
 *
 * @property inkHeight  墨水区域高度（不含空白）
 * @property inkTopOffset  墨水区域顶部相对于行框顶部的偏移量（即顶部空白的大小）
 * @property inkBaseline  基线相对于墨水区域顶部的距离
 */
class InkBounds(
    val inkHeight: Float,
    val inkTopOffset: Float,
    val inkBaseline: Float
)

/**
 * 墨水边界测量器。
 *
 * 优先使用平台原生 API 通过 [measureGlyphBounds] 获取精确的 glyph bounding box，
 * 当字体字节数据不可用时回退到基于 baseline 的启发式估算。
 */
internal object InkBoundsEstimator {

    /** 大型运算符墨水高度估算系数（回退策略） */
    private const val EXTENSION_INK_HEIGHT_RATIO = 0.92f

    /** 大型运算符墨水底部估算系数（回退策略） */
    private const val EXTENSION_INK_BOTTOM_RATIO = 1.02f

    /**
     * 使用平台原生 API 精确测量字形的墨水边界。
     *
     * 通过 [measureGlyphBounds] expect/actual 函数在各平台获取精确的字形 bounding box：
     * - Android: Paint.getTextBounds()
     * - JVM/iOS/JS/WASM: Skia Font.getBounds()
     *
     * @param text 要测量的文本
     * @param fontSizePx 字号大小（像素）
     * @param fontBytes 字体文件字节数据
     * @param baseline TextLayoutResult 的 firstBaseline（用于基线校准）
     * @param fontWeightValue 字重值
     * @return 精确的墨水边界，如果测量失败返回 null
     */
    fun measurePrecise(
        text: String,
        fontSizePx: Float,
        fontBytes: ByteArray,
        baseline: Float,
        fontWeightValue: Int = 400
    ): InkBounds? {
        val glyphBounds = measureGlyphBounds(text, fontSizePx, fontBytes, fontWeightValue) ?: return null

        // GlyphBounds 以 baseline 为 y=0：
        // ascentPx = baseline 以上的墨水高度
        // descentPx = baseline 以下的墨水高度
        // 转换为 InkBounds（以行框顶部为原点）：
        // inkTopOffset = baseline - ascentPx（行框顶部到墨水顶部的距离）
        val inkTopOffset = (baseline - glyphBounds.ascentPx).coerceAtLeast(0f)
        val inkHeight = glyphBounds.inkHeight
        val inkBaseline = glyphBounds.ascentPx

        return InkBounds(inkHeight, inkTopOffset, inkBaseline)
    }

    /**
     * 从 TextLayoutResult 估算墨水边界（回退方案）。
     *
     * 当字体字节数据不可用时使用此方法。
     * 基于 baseline 的启发式估算，跨平台精度有限。
     *
     * @param result TextMeasurer.measure() 的结果
     * @param category 字体类别，决定估算策略
     * @return 墨水边界信息
     */
    fun estimate(
        result: TextLayoutResult,
        category: InkFontCategory = InkFontCategory.EXTENSION
    ): InkBounds {
        val height = result.size.height.toFloat()
        val baseline = result.firstBaseline

        return when (category) {
            InkFontCategory.EXTENSION -> estimateExtension(height, baseline)
            InkFontCategory.TEXT -> InkBounds(
                inkHeight = height,
                inkTopOffset = 0f,
                inkBaseline = baseline
            )
        }
    }

    /**
     * 大型运算符字体的墨水边界估算（回退方案）。
     */
    private fun estimateExtension(height: Float, baseline: Float): InkBounds {
        val inkBottom = min(baseline * EXTENSION_INK_BOTTOM_RATIO, height)
        val inkHeight = min(baseline * EXTENSION_INK_HEIGHT_RATIO, inkBottom)
        val inkTopOffset = inkBottom - inkHeight

        return InkBounds(
            inkHeight = inkHeight,
            inkTopOffset = inkTopOffset,
            inkBaseline = baseline - inkTopOffset
        )
    }
}
