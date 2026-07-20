package com.hrm.markdown.renderer.internal.layout.engine

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.markdown.renderer.DiagramHostRegistry
import com.hrm.markdown.renderer.MarkdownTheme
import com.hrm.markdown.renderer.internal.core.compile.RenderCompileEnvironment
import com.hrm.markdown.renderer.internal.layout.inline.InlineLayoutEpoch
import com.hrm.markdown.renderer.internal.layout.inline.InlineLayoutRuntime

internal data class LayoutEnvironment(
    val viewportWidth: Float,
    val blockSpacing: Float = 0f,
    val markdownTheme: MarkdownTheme,
    val codeTheme: CodeTheme? = null,
    val onLinkClick: ((String) -> Unit)? = null,
    val onFootnoteClick: ((String) -> Unit)? = null,
    val density: Density,
    val textMeasurer: TextMeasurer,
    val latexMeasurer: LatexMeasurerState,
    val compileEnvironment: RenderCompileEnvironment,
    val diagramHostRegistry: DiagramHostRegistry,
    val inlineLayoutRuntime: InlineLayoutRuntime,
    val inlineLayoutEpoch: InlineLayoutEpoch,
)
