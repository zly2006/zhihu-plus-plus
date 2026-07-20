package com.hrm.markdown.renderer.internal.layout.inline

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.markdown.renderer.MarkdownConfig
import com.hrm.markdown.renderer.MarkdownTheme
import com.hrm.markdown.renderer.inline.InlineRenderResult
import com.hrm.markdown.renderer.inline.buildInlineRenderResultFromModel
import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity
import com.hrm.markdown.renderer.internal.core.model.InlineModel
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry
import kotlin.math.ceil

internal class InlineLayoutRuntime {
    private val renderResultCache = InlineRenderResultCache()
    private val flowLayoutCache = InlineFlowLayoutCache()

    fun renderResult(
        model: InlineModel,
        style: TextStyle,
        epoch: InlineLayoutEpoch,
        theme: MarkdownTheme,
        directiveRegistry: MarkdownDirectiveRegistry,
        onLinkClick: ((String) -> Unit)?,
        onFootnoteClick: ((String) -> Unit)?,
        latexMeasurer: LatexMeasurerState,
        density: Density,
        textMeasurer: TextMeasurer,
        codeTheme: CodeTheme?,
    ): InlineRenderResult {
        return renderResultCache.getOrPut(
            epoch = epoch,
            stableId = model.identity.stableId,
            contentRevision = model.identity.contentRevision,
            style = style,
        ) {
            buildInlineRenderResultFromModel(
                model = model,
                theme = theme,
                hostTextStyle = style,
                directiveRegistry = directiveRegistry,
                onLinkClick = onLinkClick,
                onFootnoteClick = onFootnoteClick,
                latexMeasurer = latexMeasurer,
                density = density,
                textMeasurer = textMeasurer,
                codeTheme = codeTheme,
            )
        }
    }

    fun flowLayout(
        identity: RenderIdentity,
        inlineResult: InlineRenderResult,
        style: TextStyle,
        epoch: InlineLayoutEpoch,
        density: Density,
        textMeasurer: TextMeasurer,
        widthPx: Float,
        maxLines: Int,
    ): InlineFlowLayout {
        return flowLayoutCache.getOrPut(
            epoch = epoch,
            layoutRevision = identity.layoutRevision,
            widthPx = widthPx,
            maxLines = maxLines,
            style = style,
            density = density,
            textMeasurer = textMeasurer,
        ) {
            computeInlineFlowLayout(
                input = inlineResult.flowInput,
                style = style,
                density = density,
                textMeasurer = textMeasurer,
                maxWidthPx = widthPx,
                maxLines = maxLines,
            )
        }
    }

    fun intrinsicHeightPx(
        identity: RenderIdentity,
        inlineResult: InlineRenderResult,
        style: TextStyle,
        epoch: InlineLayoutEpoch,
        density: Density,
        textMeasurer: TextMeasurer,
        maxLines: Int,
        widthPx: Int,
    ): Int {
        val targetWidth = if (widthPx == Constraints.Infinity || widthPx <= 0) {
            computeMaxIntrinsicWidthPx(
                input = inlineResult.flowInput,
                style = style,
                textMeasurer = textMeasurer,
            ).coerceAtLeast(1)
        } else {
            widthPx
        }
        return ceil(
            flowLayout(
                identity = identity,
                inlineResult = inlineResult,
                style = style,
                epoch = epoch,
                density = density,
                textMeasurer = textMeasurer,
                widthPx = targetWidth.toFloat(),
                maxLines = maxLines,
            ).heightPx
        ).toInt()
    }
}

internal fun inlineLayoutEpoch(
    theme: MarkdownTheme,
    codeTheme: CodeTheme?,
    directiveRegistry: MarkdownDirectiveRegistry,
    config: MarkdownConfig?,
    onLinkClick: ((String) -> Unit)?,
    onFootnoteClick: ((String) -> Unit)?,
    density: Density,
    textMeasurer: TextMeasurer,
    latexMeasurer: LatexMeasurerState,
): InlineLayoutEpoch = InlineLayoutEpoch(
    themeHash = theme.hashCode(),
    codeThemeHash = codeTheme.hashCode(),
    directiveRegistryHash = directiveRegistry.hashCode(),
    configHash = config.hashCode(),
    densityBits = density.density.toBits(),
    fontScaleBits = density.fontScale.toBits(),
    textMeasurerHash = textMeasurer.hashCode(),
    latexMeasurerHash = latexMeasurer.hashCode(),
    onLinkClickHash = onLinkClick.hashCode(),
    onFootnoteClickHash = onFootnoteClick.hashCode(),
)
