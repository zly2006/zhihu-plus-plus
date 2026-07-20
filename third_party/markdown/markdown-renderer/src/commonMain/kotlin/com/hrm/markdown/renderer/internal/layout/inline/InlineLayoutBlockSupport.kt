package com.hrm.markdown.renderer.internal.layout.inline

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.markdown.renderer.MarkdownTheme
import com.hrm.markdown.renderer.inline.InlinePlaceholderId
import com.hrm.markdown.renderer.inline.InlineRenderResult
import com.hrm.markdown.renderer.inline.InlineWidgetPaintPayload
import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity
import com.hrm.markdown.renderer.internal.core.identity.renderIdentityFromText
import com.hrm.markdown.renderer.internal.core.model.InlineModel
import com.hrm.markdown.renderer.internal.core.model.InlineWidgetModel
import com.hrm.markdown.renderer.internal.core.model.WidgetAtom
import com.hrm.markdown.renderer.internal.layout.model.LayoutInlineBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutInlineLine
import com.hrm.markdown.renderer.internal.layout.model.LayoutInsets
import com.hrm.markdown.renderer.internal.layout.model.LayoutRect
import com.hrm.markdown.renderer.internal.layout.model.LayoutTextRun
import com.hrm.markdown.renderer.internal.layout.model.LayoutWidgetRun
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry

internal fun inlineWidgetByPlaceholderId(
    model: InlineModel,
): Map<InlinePlaceholderId, InlineWidgetModel> {
    return model.atoms.asSequence()
        .mapNotNull { atom -> (atom as? WidgetAtom)?.widget }
        .associateBy { widget -> InlinePlaceholderId.from(widget) }
}

internal fun buildInlineLayoutLines(
    identity: RenderIdentity,
    contentLeft: Float,
    contentTop: Float,
    layout: InlineFlowLayout,
    widgetById: Map<InlinePlaceholderId, InlineWidgetModel>,
): List<LayoutInlineLine> {
    var lineTop = contentTop
    return layout.lines.map { line ->
        var cursorX = contentLeft
        val runs = line.items.map { item ->
            when (item) {
                is LineItem.TextItem -> {
                    val run = LayoutTextRun(
                        identity = RenderIdentity(
                            stableId = renderIdentityFromText(
                                item.text.text,
                                identity.stableId + cursorX.toLong()
                            ),
                            contentRevision = identity.contentRevision,
                            layoutRevision = identity.layoutRevision,
                            paintRevision = identity.paintRevision,
                        ),
                        frame = LayoutRect(cursorX, lineTop, item.widthPx, item.heightPx),
                        text = item.text,
                    )
                    cursorX += item.widthPx
                    run
                }

                is LineItem.InlineItem -> {
                    val widget = widgetById[item.id]
                    val run = LayoutWidgetRun(
                        identity = widget?.identity ?: identity,
                        frame = LayoutRect(cursorX, lineTop, item.widthPx, item.heightPx),
                        id = item.id,
                        widget = widget
                            ?: throw IllegalStateException("Missing inline widget for placeholder ${item.id}"),
                        alternateText = item.alternateText,
                    )
                    cursorX += item.widthPx
                    run
                }
            }
        }
        LayoutInlineLine(
            frame = LayoutRect(contentLeft, lineTop, line.lineWidthPx, line.lineHeightPx),
            baseline = line.baselinePx,
            runs = runs,
        ).also {
            lineTop += line.lineHeightPx
        }
    }
}

internal fun buildInlineLayoutBlockModel(
    identity: RenderIdentity,
    frame: LayoutRect,
    contentFrame: LayoutRect,
    style: TextStyle,
    layout: InlineFlowLayout,
    inlinePayloads: Map<InlinePlaceholderId, InlineWidgetPaintPayload>,
    widgetById: Map<InlinePlaceholderId, InlineWidgetModel>,
    showDivider: Boolean = false,
): LayoutInlineBlockModel {
    return LayoutInlineBlockModel(
        identity = identity,
        frame = frame,
        contentFrame = contentFrame,
        style = style,
        inlinePayloads = inlinePayloads,
        showDivider = showDivider,
        lines = buildInlineLayoutLines(
            identity = identity,
            contentLeft = contentFrame.left,
            contentTop = contentFrame.top,
            layout = layout,
            widgetById = widgetById,
        ),
    )
}

internal fun buildInlineLayoutBlockFromModel(
    identity: RenderIdentity,
    model: InlineModel,
    style: TextStyle,
    left: Float,
    top: Float,
    width: Float,
    insets: LayoutInsets = LayoutInsets(),
    theme: MarkdownTheme,
    directiveRegistry: MarkdownDirectiveRegistry,
    latexMeasurer: LatexMeasurerState,
    density: Density,
    textMeasurer: TextMeasurer,
    inlineLayoutRuntime: InlineLayoutRuntime,
    inlineLayoutEpoch: InlineLayoutEpoch,
    codeTheme: CodeTheme? = null,
    onLinkClick: ((String) -> Unit)? = null,
    onFootnoteClick: ((String) -> Unit)? = null,
    maxLines: Int = Int.MAX_VALUE,
    showDivider: Boolean = false,
): LayoutInlineBlockModel {
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
        codeTheme = codeTheme,
    )
    return buildInlineLayoutBlockFromResult(
        identity = identity,
        model = model,
        style = style,
        left = left,
        top = top,
        width = width,
        insets = insets,
        theme = theme,
        inlineResult = inlineResult,
        density = density,
        textMeasurer = textMeasurer,
        inlineLayoutRuntime = inlineLayoutRuntime,
        inlineLayoutEpoch = inlineLayoutEpoch,
        maxLines = maxLines,
        showDivider = showDivider,
    )
}

internal fun buildInlineLayoutBlockFromResult(
    identity: RenderIdentity,
    model: InlineModel,
    style: TextStyle,
    left: Float,
    top: Float,
    width: Float,
    insets: LayoutInsets = LayoutInsets(),
    theme: MarkdownTheme,
    inlineResult: InlineRenderResult,
    density: Density,
    textMeasurer: TextMeasurer,
    inlineLayoutRuntime: InlineLayoutRuntime,
    inlineLayoutEpoch: InlineLayoutEpoch,
    maxLines: Int = Int.MAX_VALUE,
    showDivider: Boolean = false,
): LayoutInlineBlockModel {
    val contentLeft = left + insets.left
    val contentTop = top + insets.top
    val contentWidth = (width - insets.left - insets.right).coerceAtLeast(0f)
    val layout = inlineLayoutRuntime.flowLayout(
        identity = identity,
        inlineResult = inlineResult,
        style = style,
        epoch = inlineLayoutEpoch,
        density = density,
        textMeasurer = textMeasurer,
        widthPx = contentWidth,
        maxLines = maxLines,
    )
    val dividerHeight = if (showDivider) {
        4f + with(density) { theme.dividerThickness.toPx() }
    } else {
        0f
    }
    val contentHeight = layout.heightPx + dividerHeight
    return buildInlineLayoutBlockModel(
        identity = identity,
        frame = LayoutRect(left, top, width, insets.top + contentHeight + insets.bottom),
        contentFrame = LayoutRect(contentLeft, contentTop, contentWidth, contentHeight),
        style = style,
        layout = layout,
        inlinePayloads = inlineResult.paintPayloads,
        widgetById = inlineWidgetByPlaceholderId(model),
        showDivider = showDivider,
    )
}
