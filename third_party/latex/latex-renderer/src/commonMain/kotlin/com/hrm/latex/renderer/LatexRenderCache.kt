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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.layout.LatexRenderResult
import com.hrm.latex.renderer.layout.LayoutMap
import com.hrm.latex.renderer.measure.LatexDimensions
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme

/**
 * Page-level LRU of completed parse and layout results. Active formulas are pinned so eviction can
 * never resize or clip a currently composed inline formula. Access is confined to Compose state.
 */
@Stable
class LatexRenderCache(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    init {
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    private val entries = mutableMapOf<LatexRenderKey, PreparedLatex>()
    private val preparedStates = mutableMapOf<LatexRenderKey, androidx.compose.runtime.MutableState<PreparedLatex?>>()
    private val dimensionStates = mutableMapOf<LatexRenderKey, androidx.compose.runtime.MutableState<LatexDimensions?>>()
    private val accessOrder = mutableListOf<LatexRenderKey>()
    private val pinCounts = mutableMapOf<LatexRenderKey, Int>()

    var revision by mutableIntStateOf(0)
        private set

    val size: Int get() = entries.size

    fun dimensions(
        latex: String,
        config: LatexConfig,
        density: Density,
        isDarkTheme: Boolean = false,
    ): LatexDimensions? = get(latexRenderKey(latex, config, density, isDarkTheme))?.dimensions

    fun observeDimensions(
        latex: String,
        config: LatexConfig,
        density: Density,
        isDarkTheme: Boolean = false,
    ): State<LatexDimensions?> {
        val key = latexRenderKey(latex, config, density, isDarkTheme)
        val cached = get(key)
        return dimensionStates.getOrPut(key) { mutableStateOf(cached?.dimensions) }
    }

    internal fun observePrepared(key: LatexRenderKey): State<PreparedLatex?> {
        val cached = get(key)
        return preparedStates.getOrPut(key) { mutableStateOf(cached) }
    }

    internal fun get(key: LatexRenderKey): PreparedLatex? {
        val value = entries[key] ?: return null
        accessOrder.remove(key)
        accessOrder += key
        return value
    }

    internal fun pin(key: LatexRenderKey) {
        pinCounts[key] = (pinCounts[key] ?: 0) + 1
    }

    internal fun unpin(key: LatexRenderKey) {
        val remaining = (pinCounts[key] ?: 1) - 1
        if (remaining > 0) {
            pinCounts[key] = remaining
        } else {
            pinCounts.remove(key)
            trimToLimit()
        }
    }

    internal fun put(key: LatexRenderKey, value: PreparedLatex) {
        entries[key] = value
        preparedStates.getOrPut(key) { mutableStateOf(null) }.value = value
        dimensionStates.getOrPut(key) { mutableStateOf(null) }.value = value.dimensions
        accessOrder.remove(key)
        accessOrder += key
        trimToLimit()
        revision++
    }

    private fun trimToLimit() {
        while (entries.size > maxEntries) {
            val evictionIndex = accessOrder.indexOfFirst { (pinCounts[it] ?: 0) == 0 }
            if (evictionIndex < 0) return
            val evicted = accessOrder.removeAt(evictionIndex)
            entries.remove(evicted)
            preparedStates.remove(evicted)?.value = null
            dimensionStates.remove(evicted)?.value = null
        }
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES: Int = 128
    }
}

val LocalLatexRenderCache = staticCompositionLocalOf<LatexRenderCache?> { null }

@Composable
fun rememberLatexRenderCache(maxEntries: Int = LatexRenderCache.DEFAULT_MAX_ENTRIES): LatexRenderCache =
    remember(maxEntries) { LatexRenderCache(maxEntries) }

internal data class LatexRenderKey(
    val latex: String,
    val layoutConfig: LatexConfig,
    val isDarkTheme: Boolean,
    val density: Float,
    val fontScale: Float,
)

internal class PreparedLatex(
    val document: LatexNode.Document,
    val renderResult: LatexRenderResult,
    val layoutMap: LayoutMap?,
) {
    val dimensions = LatexDimensions(
        widthPx = renderResult.canvasWidth,
        heightPx = renderResult.canvasHeight,
        baselinePx = renderResult.layout.baseline + renderResult.verticalPadding,
        contentWidthPx = renderResult.layout.width,
        contentHeightPx = renderResult.layout.height,
        contentBaselinePx = renderResult.layout.baseline,
    )
}

internal fun latexRenderKey(
    latex: String,
    config: LatexConfig,
    density: Density,
    isDarkTheme: Boolean,
): LatexRenderKey = LatexRenderKey(
    latex = latex,
    layoutConfig = config.copy(
        accessibilityEnabled = false,
        onNodeClick = null,
        onHyperlinkClick = null,
        enableLayoutCache = false,
    ),
    isDarkTheme = if (config.theme is LatexTheme.Adaptive) isDarkTheme else false,
    density = density.density,
    fontScale = density.fontScale,
)
