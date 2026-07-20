package com.hrm.markdown.renderer.inline

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.codehigh.theme.LocalCodeTheme
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.markdown.renderer.LocalCodeHighlightTheme
import com.hrm.markdown.renderer.LocalMarkdownDirectiveRegistry
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.LocalOnFootnoteClick
import com.hrm.markdown.renderer.LocalOnLinkClick
import com.hrm.markdown.renderer.MarkdownTheme
import com.hrm.markdown.renderer.internal.compose.PaintInlineLayoutContent
import com.hrm.markdown.renderer.internal.core.model.InlineModel
import com.hrm.markdown.renderer.internal.layout.inline.InlineLayoutEpoch
import com.hrm.markdown.renderer.internal.layout.inline.InlineLayoutRuntime
import com.hrm.markdown.renderer.internal.layout.inline.buildInlineLayoutBlockFromResult
import com.hrm.markdown.renderer.internal.layout.inline.computeMaxIntrinsicWidthPx
import com.hrm.markdown.renderer.internal.layout.inline.computeMinIntrinsicWidthPx
import com.hrm.markdown.renderer.internal.layout.inline.inlineLayoutEpoch
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry

@Composable
internal fun InlineLayoutBlockText(
    model: InlineModel,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    val theme = LocalMarkdownTheme.current
    val directiveRegistry = LocalMarkdownDirectiveRegistry.current
    val onLinkClick = LocalOnLinkClick.current
    val onFootnoteClick = LocalOnFootnoteClick.current
    val latexMeasurer = rememberLatexMeasurer()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val inlineCodeTheme = LocalCodeHighlightTheme.current ?: LocalCodeTheme.current
    val inlineLayoutRuntime = remember { InlineLayoutRuntime() }
    val inlineLayoutEpoch = inlineLayoutEpoch(
        theme = theme,
        codeTheme = inlineCodeTheme,
        directiveRegistry = directiveRegistry,
        config = null,
        onLinkClick = onLinkClick,
        onFootnoteClick = onFootnoteClick,
        latexMeasurer = latexMeasurer,
        density = density,
        textMeasurer = textMeasurer,
    )
    val inlineResult = inlineLayoutRuntime.renderResult(
        model = model,
        style = style,
        epoch = inlineLayoutEpoch,
        theme = theme,
        directiveRegistry = directiveRegistry,
        onLinkClick = onLinkClick,
        onFootnoteClick = onFootnoteClick,
        latexMeasurer = latexMeasurer,
        density = density,
        textMeasurer = textMeasurer,
        codeTheme = inlineCodeTheme,
    )
    val measurePolicy = remember(
        model.identity,
        inlineResult,
        style,
        density,
        textMeasurer,
        maxLines,
        inlineLayoutRuntime,
        inlineLayoutEpoch,
    ) {
        inlineLayoutBlockMeasurePolicy(
            model = model,
            inlineResult = inlineResult,
            style = style,
            density = density,
            textMeasurer = textMeasurer,
            maxLines = maxLines,
            inlineLayoutRuntime = inlineLayoutRuntime,
            inlineLayoutEpoch = inlineLayoutEpoch,
        )
    }

    Layout(
        modifier = modifier,
        content = {
            InlineLayoutBlockMeasuredContent(
                model = model,
                style = style,
                theme = theme,
                directiveRegistry = directiveRegistry,
                onLinkClick = onLinkClick,
                onFootnoteClick = onFootnoteClick,
                latexMeasurer = latexMeasurer,
                density = density,
                textMeasurer = textMeasurer,
                maxLines = maxLines,
                inlineCodeTheme = inlineCodeTheme,
                inlineResult = inlineResult,
                inlineLayoutRuntime = inlineLayoutRuntime,
                inlineLayoutEpoch = inlineLayoutEpoch,
            )
        },
        measurePolicy = measurePolicy,
    )
}

@Composable
private fun InlineLayoutBlockMeasuredContent(
    model: InlineModel,
    style: TextStyle,
    theme: MarkdownTheme,
    directiveRegistry: MarkdownDirectiveRegistry,
    onLinkClick: ((String) -> Unit)?,
    onFootnoteClick: ((String) -> Unit)?,
    latexMeasurer: LatexMeasurerState,
    density: Density,
    textMeasurer: TextMeasurer,
    maxLines: Int,
    inlineCodeTheme: CodeTheme?,
    inlineResult: InlineRenderResult,
    inlineLayoutRuntime: InlineLayoutRuntime,
    inlineLayoutEpoch: InlineLayoutEpoch,
) {
    BoxWithConstraints {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val layoutBlock = remember(
            model,
            style,
            theme,
            directiveRegistry,
            onLinkClick,
            onFootnoteClick,
            latexMeasurer,
            density,
            textMeasurer,
            maxWidthPx,
            maxLines,
            inlineCodeTheme,
            inlineResult,
            inlineLayoutRuntime,
            inlineLayoutEpoch,
        ) {
            buildInlineLayoutBlockFromResult(
                identity = model.identity,
                model = model,
                style = style,
                left = 0f,
                top = 0f,
                width = maxWidthPx,
                theme = theme,
                inlineResult = inlineResult,
                density = density,
                textMeasurer = textMeasurer,
                inlineLayoutRuntime = inlineLayoutRuntime,
                inlineLayoutEpoch = inlineLayoutEpoch,
                maxLines = maxLines,
            )
        }

        PaintInlineLayoutContent(
            block = layoutBlock,
            modifier = Modifier,
        )
    }
}

private fun inlineLayoutBlockMeasurePolicy(
    model: InlineModel,
    inlineResult: InlineRenderResult,
    style: TextStyle,
    density: Density,
    textMeasurer: TextMeasurer,
    maxLines: Int,
    inlineLayoutRuntime: InlineLayoutRuntime,
    inlineLayoutEpoch: InlineLayoutEpoch,
): MeasurePolicy = object : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurables.singleOrNull()?.measure(constraints)
        val width = placeable?.width ?: constraints.minWidth
        val height = placeable?.height ?: constraints.minHeight
        return layout(width, height) {
            placeable?.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = computeMinIntrinsicWidthPx(
        input = inlineResult.flowInput,
        style = style,
        textMeasurer = textMeasurer,
    )

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = computeMaxIntrinsicWidthPx(
        input = inlineResult.flowInput,
        style = style,
        textMeasurer = textMeasurer,
    )

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = inlineLayoutRuntime.intrinsicHeightPx(
        identity = model.identity,
        inlineResult = inlineResult,
        style = style,
        epoch = inlineLayoutEpoch,
        density = density,
        textMeasurer = textMeasurer,
        maxLines = maxLines,
        widthPx = width,
    )

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = inlineLayoutRuntime.intrinsicHeightPx(
        identity = model.identity,
        inlineResult = inlineResult,
        style = style,
        epoch = inlineLayoutEpoch,
        density = density,
        textMeasurer = textMeasurer,
        maxLines = maxLines,
        widthPx = width,
    )
}
