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

package com.hrm.latex.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.font.MathFontProviderFactory
import com.hrm.latex.renderer.layout.LayoutCache
import com.hrm.latex.renderer.layout.LayoutMap
import com.hrm.latex.renderer.layout.LatexRenderer
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.defaultLatexFontFamilies
import com.hrm.latex.renderer.model.resolveThemeColors
import com.hrm.latex.renderer.model.toContext

/**
 * 编辑器渲染结果信息
 *
 * 提供渲染后的尺寸和内边距信息，供编辑器模块定位光标和处理点击。
 *
 * @property canvasWidth 完整画布宽度（包含内边距）
 * @property canvasHeight 完整画布高度（包含内边距）
 * @property horizontalPadding 水平内边距
 * @property verticalPadding 垂直内边距
 */
data class EditorRenderInfo(
    val canvasWidth: Float,
    val canvasHeight: Float,
    val horizontalPadding: Float,
    val verticalPadding: Float
)

/**
 * LaTeX 编辑器专用渲染组件
 *
 * 与 [Latex] 组件功能类似，但额外支持：
 * - 将布局映射信息写入 [layoutMap]，供编辑器进行光标定位和 hit-testing
 * - 通过 [onRenderInfoChanged] 回调通知渲染尺寸变化
 * - 通过 [overlay] 回调支持在渲染内容上叠加光标/选区等编辑器 UI
 *
 * @param children 解析后的 AST 节点列表（Document.children）
 * @param modifier 修饰符
 * @param config 渲染配置
 * @param isDarkTheme 当前环境是否为深色模式。
 * 仅在 `config.theme = LatexTheme.auto(...)` 时用于选择 light/dark 色板。
 * @param layoutMap 布局映射表，测量阶段会将节点位置信息写入其中
 * @param onRenderInfoChanged 渲染信息变化回调
 * @param overlay 叠加绘制回调（在内容绘制之后调用，可用于绘制光标/选区）
 */
@ExperimentalComposeUiApi
@Composable
fun LatexEditorCanvas(
    children: List<LatexNode>,
    modifier: Modifier = Modifier,
    config: LatexConfig = LatexConfig(),
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    layoutMap: LayoutMap? = null,
    onRenderInfoChanged: ((EditorRenderInfo) -> Unit)? = null,
    overlay: (DrawScope.() -> Unit)? = null
) {
    val resolvedThemeColors = config.resolveThemeColors(isDarkTheme)
    val fontFamilies = defaultLatexFontFamilies()
    val provider = remember(fontFamilies) {
        MathFontProviderFactory.create(fontFamilies)
    }
    val context = remember(config, isDarkTheme, fontFamilies, provider) {
        config.toContext(isDarkTheme, fontFamilies, provider)
    }

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // NodeLayout 缓存：仅在 config 启用时创建，跨渲染周期复用相同 AST 子树+上下文的测量结果
    val layoutCache = remember(config.enableLayoutCache) {
        if (config.enableLayoutCache) LayoutCache() else null
    }

    // 测量，同时填充 layoutMap
    val renderResult = remember(children, context, density) {
        layoutMap?.clear()
        LatexRenderer.measure(children, context, measurer, density, layoutMap = layoutMap, cache = layoutCache)
    }

    // 通知渲染信息变化
    LaunchedEffect(renderResult) {
        onRenderInfoChanged?.invoke(
            EditorRenderInfo(
                canvasWidth = renderResult.canvasWidth,
                canvasHeight = renderResult.canvasHeight,
                horizontalPadding = renderResult.horizontalPadding,
                verticalPadding = renderResult.verticalPadding
            )
        )
    }

    val widthDp = with(density) { renderResult.canvasWidth.toDp() }
    val heightDp = with(density) { renderResult.canvasHeight.toDp() }

    Canvas(modifier = modifier.size(widthDp, heightDp)) {
        // 1. 绘制 LaTeX 内容
        with(LatexRenderer) {
            draw(renderResult, resolvedThemeColors.backgroundColor)
        }

        // 2. 叠加编辑器 UI（光标、选区等）
        overlay?.invoke(this)
    }
}
