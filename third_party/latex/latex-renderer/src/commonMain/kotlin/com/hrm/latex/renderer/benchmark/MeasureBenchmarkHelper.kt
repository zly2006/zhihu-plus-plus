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

package com.hrm.latex.renderer.benchmark

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.LatexRenderer
import com.hrm.latex.renderer.layout.NodeLayout
import com.hrm.latex.renderer.layout.measureGroup
import com.hrm.latex.renderer.layout.measureNode
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.toContext

/**
 * Benchmark 辅助类：将 internal 的 measure API 暴露给 benchmark 模块使用。
 *
 * 本类仅用于性能基准测试，不应在业务代码中使用。
 */
internal object MeasureBenchmarkHelper {

    /**
     * 创建 RenderContext（封装 internal 的 toContext）
     */
    fun createContext(
        fontSize: TextUnit = 20.sp,
        fontFamilies: LatexFontFamilies? = null,
        isDark: Boolean = false
    ): RenderContext {
        val families = fontFamilies ?: createDefaultFontFamilies()
        return LatexConfig(fontSize = fontSize).toContext(isDark, families)
    }

    /**
     * 执行 measureGroup（封装 internal 的 measureGroup）
     */
    fun measureGroup(
        nodes: List<LatexNode>,
        context: RenderContext,
        textMeasurer: TextMeasurer,
        density: Density
    ): NodeLayout {
        return measureGroup(nodes, context, textMeasurer, density, null)
    }

    /**
     * 执行 measureNode（封装 internal 的 measureNode）
     */
    fun measureNode(
        node: LatexNode,
        context: RenderContext,
        textMeasurer: TextMeasurer,
        density: Density
    ): NodeLayout {
        return measureNode(node, context, textMeasurer, density)
    }

    /**
     * 执行完整的 LatexRenderer.measure 流程
     *
     * @return Triple(canvasWidth, canvasHeight, measureTimeOnly)
     */
    fun measureFull(
        children: List<LatexNode>,
        context: RenderContext,
        textMeasurer: TextMeasurer,
        density: Density
    ): MeasureResult {
        val result = LatexRenderer.measure(children, context, textMeasurer, density)
        return MeasureResult(
            width = result.canvasWidth,
            height = result.canvasHeight,
            layoutWidth = result.layout.width,
            layoutHeight = result.layout.height,
            baseline = result.layout.baseline
        )
    }

    /**
     * 创建使用系统默认字体的 LatexFontFamilies（不需要 Compose 资源加载）
     */
    fun createDefaultFontFamilies(): LatexFontFamilies {
        return LatexFontFamilies(
            main = FontFamily.Default,
            math = FontFamily.Default,
            ams = FontFamily.Default,
            sansSerif = FontFamily.SansSerif,
            monospace = FontFamily.Monospace,
            caligraphic = FontFamily.Default,
            fraktur = FontFamily.Default,
            script = FontFamily.Default,
            size1 = FontFamily.Default,
            size2 = FontFamily.Default,
            size3 = FontFamily.Default,
            size4 = FontFamily.Default
        )
    }
}

/**
 * Measure 结果数据
 */
data class MeasureResult(
    val width: Float,
    val height: Float,
    val layoutWidth: Float,
    val layoutHeight: Float,
    val baseline: Float
)
