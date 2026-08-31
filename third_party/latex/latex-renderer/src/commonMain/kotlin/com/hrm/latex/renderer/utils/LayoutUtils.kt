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

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.textStyle

/**
 * 布局工具类，提供通用的测量与对齐辅助方法
 */
internal object LayoutUtils {

    /**
     * 获取当前字体的数学轴 (Math Axis) 高度。
     * 数学轴是分数线、算子（如 +）垂直居中的参考线。
     *
     * 算法：测量减号 '-'，其垂直中心即为数学轴。
     * 结果按 (fontSize, provider) 缓存，同一渲染周期内不重复测量。
     *
     * @return 数学轴相对于基线的偏移量（向上为正）
     */
    fun getAxisHeight(density: Density, context: RenderContext, measurer: TextMeasurer): Float {
        if (
            context.cachedAxisHeightDensity == density.density &&
            context.cachedAxisHeightFontScale == density.fontScale &&
            !context.cachedAxisHeight.isNaN()
        ) {
            return context.cachedAxisHeight
        }
        val fontSizePx = with(density) { context.fontSize.toPx() }
        return computeAxisHeight(fontSizePx, context, measurer).also { axisHeight ->
            context.cachedAxisHeightDensity = density.density
            context.cachedAxisHeightFontScale = density.fontScale
            context.cachedAxisHeight = axisHeight
        }
    }

    private fun computeAxisHeight(
        fontSizePx: Float,
        context: RenderContext,
        measurer: TextMeasurer
    ): Float {
        // 使用 KaTeX 字体度量中的数学轴高度。
        context.mathFontProvider?.let { provider ->
            val axisFromFont = provider.axisHeight(fontSizePx)
            if (axisFromFont > 0f) return axisFromFont
        }

        // Fallback: 测量减号的垂直中心
        val style = context.textStyle()
        val minusResult = measurer.measure("-", style)
        val axisHeight = minusResult.firstBaseline - (minusResult.size.height / 2f)

        val minReasonable = fontSizePx * 0.1f
        val maxReasonable = fontSizePx * 0.5f

        return if (axisHeight in minReasonable..maxReasonable) {
            axisHeight
        } else {
            fontSizePx * MathConstants.MATH_AXIS_HEIGHT_RATIO
        }
    }

}
