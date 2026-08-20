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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import com.hrm.latex.base.log.HLog
import com.hrm.latex.parser.IncrementalLatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.visitor.AccessibilityVisitor
import com.hrm.latex.renderer.font.MathFontProvider
import com.hrm.latex.renderer.font.MathFontProviderFactory
import com.hrm.latex.renderer.font.rememberResolvedMathFont
import com.hrm.latex.renderer.layout.LatexRenderer
import com.hrm.latex.renderer.layout.LayoutCache
import com.hrm.latex.renderer.layout.LayoutMap
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LineBreakingConfig
import com.hrm.latex.renderer.model.defaultLatexFontFamilies
import com.hrm.latex.renderer.model.resolveThemeColors
import com.hrm.latex.renderer.model.toContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext

private const val TAG = "Latex"
// One preparation lane leaves CPU time for input, Compose layout, and drawing. Document-level
// prefetch plus completed-layout caching makes parallel formula layout counterproductive on phones.
private val LatexPreparationDispatcher = Dispatchers.Default.limitedParallelism(1)

private class LatexPreparationResources(
    val layoutCache: LayoutCache?,
) {
    var provider: MathFontProvider? = null
}

/**
 * Latex 渲染组件
 *
 * 自动支持增量解析能力，可以安全处理不完整的 LaTeX 输入
 *
 * 性能优化：
 * - 复用解析器实例，避免重复创建
 * - 异步解析和布局，不阻塞主线程
 * - 页面级缓存复用已完成布局，避免公式滚出视口后重新计算
 * - 有限后台并发，避免大量公式同时抢占 CPU
 *
 * @param latex LaTeX 字符串（支持增量输入，会自动解析可解析部分）
 * @param modifier 修饰符
 * @param config 渲染配置（包含主题、字体大小等）
 * @param isDarkTheme 当前环境是否为深色模式。
 * 仅在 `config.theme = LatexTheme.auto(...)` 时用于选择 light/dark 色板；
 * 固定主题和 `LatexTheme.material3()` 会直接使用 theme 中的颜色。
 */
@Composable
fun Latex(
    latex: String,
    modifier: Modifier = Modifier,
    config: LatexConfig = LatexConfig(),
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val resolvedThemeColors = config.resolveThemeColors(isDarkTheme)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val renderCache = LocalLatexRenderCache.current
    val effectiveMathFont = rememberResolvedMathFont(config.mathFont)
    val fontFamilies = effectiveMathFont.fontFamiliesOrNull() ?: defaultLatexFontFamilies()
    val effectiveConfig = remember(config, effectiveMathFont) { config.copy(mathFont = effectiveMathFont) }
    val parser = remember { IncrementalLatexParser() }
    val preparationResources = remember(effectiveMathFont, fontFamilies, effectiveConfig.enableLayoutCache) {
        LatexPreparationResources(
            layoutCache = if (effectiveConfig.enableLayoutCache) LayoutCache() else null,
        )
    }
    val interactive = config.onNodeClick != null || config.onHyperlinkClick != null
    val renderKey = remember(latex, effectiveConfig, isDarkTheme, density.density, density.fontScale) {
        latexRenderKey(latex, effectiveConfig, density, isDarkTheme)
    }
    val cachedPrepared = if (interactive) null else renderCache?.observePrepared(renderKey)?.value
    var prepared by remember(renderKey, interactive) { mutableStateOf(cachedPrepared) }
    val currentPrepared = prepared ?: cachedPrepared

    DisposableEffect(renderCache, renderKey, interactive) {
        if (renderCache != null && !interactive) renderCache.pin(renderKey)
        onDispose {
            if (renderCache != null && !interactive) renderCache.unpin(renderKey)
        }
    }

    LaunchedEffect(renderKey, effectiveConfig, fontFamilies, textMeasurer, interactive, currentPrepared) {
        if (currentPrepared != null || latex.isBlank()) return@LaunchedEffect
        val result = withContext(LatexPreparationDispatcher) {
            try {
                val currentInput = parser.getCurrentInput()
                if (latex.startsWith(currentInput)) {
                    parser.append(latex.substring(currentInput.length))
                } else {
                    parser.clear()
                    parser.append(latex)
                }
                val document = parser.getCurrentDocument()
                currentCoroutineContext().ensureActive()
                yield()

                // Provider and layout caches stay on the one preparation lane because their lazy
                // glyph data is not thread-safe. Drawing consumes only the immutable result.
                val provider = preparationResources.provider
                    ?: MathFontProviderFactory.create(fontFamilies)
                        .also { preparationResources.provider = it }
                val context = effectiveConfig.toContext(isDarkTheme, fontFamilies, provider)
                val layoutMap = if (interactive) LayoutMap() else null
                val renderResult = LatexRenderer.measure(
                    children = document.children,
                    context = context,
                    textMeasurer = textMeasurer,
                    density = density,
                    highlightRanges = effectiveConfig.highlight.ranges,
                    layoutMap = layoutMap,
                    cache = preparationResources.layoutCache,
                )
                currentCoroutineContext().ensureActive()
                PreparedLatex(document, renderResult, layoutMap)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                HLog.e(TAG, "公式准备失败", e)
                null
            }
        } ?: return@LaunchedEffect

        currentCoroutineContext().ensureActive()
        if (!interactive) renderCache?.put(renderKey, result)
        prepared = result
    }

    val document = currentPrepared?.document ?: LatexNode.Document(emptyList())

    // 无障碍描述：当启用时，使用 AccessibilityVisitor 生成屏幕阅读器文本
    val accessibilityDescription = if (config.accessibilityEnabled) {
        remember(document) {
            AccessibilityVisitor.describe(document)
        }
    } else null

    LatexDocument(
        modifier = modifier,
        prepared = currentPrepared,
        placeholderHeightPx = with(density) { effectiveConfig.fontSize.toPx() * 1.4f },
        backgroundColor = resolvedThemeColors.backgroundColor,
        contentDescription = accessibilityDescription,
        onNodeClick = config.onNodeClick,
        onHyperlinkClick = config.onHyperlinkClick,
        latex = latex
    )
}

/**
 * 自动换行的 LaTeX 渲染组件。
 *
 * 该组件会根据父容器宽度自动包装长公式。
 * 换行优先发生在关系运算符（`=`、`<`、`>`）和二元运算符（`+`、`-`、`×`）之后。
 *
 * @param latex LaTeX 字符串（支持增量输入）
 * @param modifier 修饰符
 * @param config 渲染配置（包含主题、字体大小等）
 * @param isDarkTheme 当前环境是否为深色模式。
 * 仅在 `config.theme = LatexTheme.auto(...)` 时参与主题解析。
 */
@Composable
fun LatexAutoWrap(
    latex: String,
    modifier: Modifier = Modifier,
    config: LatexConfig = LatexConfig(),
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.toPx() }

        val wrappingConfig = config.copy(
            lineBreaking = LineBreakingConfig(
                enabled = true,
                maxWidth = maxWidthPx
            )
        )

        Latex(
            latex = latex,
            modifier = Modifier,
            config = wrappingConfig,
            isDarkTheme = isDarkTheme
        )
    }
}

/**
 * Latex 文档渲染组件
 *
 * @param modifier 修饰符
 * @param prepared 后台完成的解析与布局结果
 * @param placeholderHeightPx 准备期间保留的占位高度
 * @param backgroundColor 背景颜色
 * @param contentDescription 无障碍描述文本（非空时启用 semantics）
 */
@Composable
private fun LatexDocument(
    modifier: Modifier = Modifier,
    prepared: PreparedLatex?,
    placeholderHeightPx: Float,
    backgroundColor: Color = Color.Transparent,
    contentDescription: String? = null,
    onNodeClick: ((startOffset: Int, endOffset: Int, latex: String) -> Unit)? = null,
    onHyperlinkClick: ((url: String) -> Unit)? = null,
    latex: String = ""
) {
    val density = LocalDensity.current
    val renderResult = prepared?.renderResult
    val layoutMap = prepared?.layoutMap
    val widthDp = with(density) { (renderResult?.canvasWidth ?: 0f).toDp() }
    val heightDp = with(density) { (renderResult?.canvasHeight ?: placeholderHeightPx).toDp() }

    val canvasModifier = if (contentDescription != null) {
        modifier
            .semantics { this.contentDescription = contentDescription }
            .size(widthDp, heightDp)
    } else {
        modifier.size(widthDp, heightDp)
    }.let { mod ->
        if ((onNodeClick != null || onHyperlinkClick != null) && layoutMap != null && renderResult != null) {
            mod.pointerInput(layoutMap, latex, onHyperlinkClick) {
                detectTapGestures { offset ->
                    // 将点击坐标转换为内容区相对坐标
                    val contentX = offset.x - renderResult.horizontalPadding
                    val contentY = offset.y - renderResult.verticalPadding
                    val hit = layoutMap.hitTest(contentX, contentY)
                    if (hit != null) {
                        // 超链接专用回调：命中 Hyperlink 节点时直接返回 URL
                        if (onHyperlinkClick != null && hit.node is LatexNode.Hyperlink) {
                            onHyperlinkClick(hit.node.url)
                        }
                        // 通用节点点击回调
                        if (onNodeClick != null) {
                            val range = hit.node.sourceRange
                            if (range != null) {
                                onNodeClick(range.start, range.end, latex)
                            }
                        }
                    }
                }
            }
        } else mod
    }

    Canvas(modifier = canvasModifier) {
        val result = renderResult ?: return@Canvas
        // 使用 LatexRenderer 共享逻辑进行绘制（与导出路径共用同一份代码）
        with(LatexRenderer) {
            draw(result, backgroundColor)
        }
    }
}
