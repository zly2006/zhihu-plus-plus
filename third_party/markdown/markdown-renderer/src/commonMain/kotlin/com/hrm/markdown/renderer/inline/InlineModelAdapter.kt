package com.hrm.markdown.renderer.inline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.hrm.codehigh.renderer.InlineCodeDefaults
import com.hrm.codehigh.renderer.measureInlineCodeSize
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.markdown.parser.core.CharacterUtils
import com.hrm.markdown.renderer.DefaultMarkdownImage
import com.hrm.markdown.renderer.LocalImageRenderer
import com.hrm.markdown.renderer.MarkdownImageData
import com.hrm.markdown.renderer.MarkdownTheme
import com.hrm.markdown.renderer.internal.adapter.createDirectiveInlineRenderScope
import com.hrm.markdown.renderer.internal.core.model.DirectiveInlineWidgetModel
import com.hrm.markdown.renderer.internal.core.model.ImageWidgetModel
import com.hrm.markdown.renderer.internal.core.model.InlineAtom
import com.hrm.markdown.renderer.internal.core.model.InlineCodeWidgetModel
import com.hrm.markdown.renderer.internal.core.model.InlineMathWidgetModel
import com.hrm.markdown.renderer.internal.core.model.InlineModel
import com.hrm.markdown.renderer.internal.core.model.RubyTextWidgetModel
import com.hrm.markdown.renderer.internal.core.model.SpanMark
import com.hrm.markdown.renderer.internal.core.model.SpoilerWidgetModel
import com.hrm.markdown.renderer.internal.core.model.TextAtom
import com.hrm.markdown.renderer.internal.core.model.WidgetAtom
import com.hrm.markdown.renderer.internal.layout.inline.InlineFlowInput
import com.hrm.markdown.renderer.internal.layout.inline.InlineFlowSegment
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry
import kotlin.math.ceil
import com.hrm.codehigh.renderer.InlineCode as CodeHighInlineCode

internal fun buildInlineRenderResultFromModel(
    model: InlineModel,
    theme: MarkdownTheme,
    hostTextStyle: TextStyle,
    directiveRegistry: MarkdownDirectiveRegistry,
    onLinkClick: ((String) -> Unit)? = null,
    onFootnoteClick: ((String) -> Unit)? = null,
    latexMeasurer: LatexMeasurerState,
    density: Density,
    textMeasurer: TextMeasurer,
    codeTheme: CodeTheme? = null,
): InlineRenderResult {
    val flowSegments = mutableListOf<InlineFlowSegment>()
    val context = InlineRenderBuildContext(
        paintPayloads = linkedMapOf(),
        flowSegments = flowSegments,
    )
    val annotated = buildAnnotatedString {
        renderInlineModel(
            model = model,
            theme = theme,
            hostTextStyle = hostTextStyle,
            context = context,
            directiveRegistry = directiveRegistry,
            onLinkClick = onLinkClick,
            onFootnoteClick = onFootnoteClick,
            latexMeasurer = latexMeasurer,
            density = density,
            textMeasurer = textMeasurer,
            inlineCodeTheme = codeTheme,
        )
    }
    return InlineRenderResult(
        annotated = annotated,
        paintPayloads = context.paintPayloads,
        flowInput = InlineFlowInput(flowSegments),
    )
}

internal fun AnnotatedString.Builder.renderInlineModel(
    model: InlineModel,
    theme: MarkdownTheme,
    hostTextStyle: TextStyle,
    context: InlineRenderBuildContext,
    directiveRegistry: MarkdownDirectiveRegistry,
    onLinkClick: ((String) -> Unit)?,
    onFootnoteClick: ((String) -> Unit)?,
    latexMeasurer: LatexMeasurerState,
    density: Density,
    textMeasurer: TextMeasurer,
    inlineCodeTheme: CodeTheme? = null,
) {
    model.atoms.forEachIndexed { index, atom ->
        when (atom) {
            is TextAtom -> {
                val normalized = normalizeTextAtomForCjkInlineMathSpacing(
                    atoms = model.atoms,
                    index = index,
                    atom = atom,
                )
                if (normalized.text.isNotEmpty()) {
                    renderTextAtom(
                        atom = normalized,
                        theme = theme,
                        context = context,
                        onLinkClick = onLinkClick,
                        onFootnoteClick = onFootnoteClick,
                    )
                }
            }

            is WidgetAtom -> renderWidgetAtom(
                atom = atom,
                theme = theme,
                hostTextStyle = hostTextStyle,
                context = context,
                directiveRegistry = directiveRegistry,
                onLinkClick = onLinkClick,
                onFootnoteClick = onFootnoteClick,
                latexMeasurer = latexMeasurer,
                density = density,
                textMeasurer = textMeasurer,
                inlineCodeTheme = inlineCodeTheme,
            )
        }
    }
}

private fun normalizeTextAtomForCjkInlineMathSpacing(
    atoms: List<InlineAtom>,
    index: Int,
    atom: TextAtom,
): TextAtom {
    var text = atom.text
    if (text.isEmpty()) return atom

    val previous = atoms.getOrNull(index - 1)
    val next = atoms.getOrNull(index + 1)

    if (previous.isInlineMathWidgetAtom() && text.firstOrNull()?.isWhitespace() == true) {
        val nextVisible = text.firstOrNull { !it.isWhitespace() }
        if (nextVisible != null && CharacterUtils.isCJKOrFullWidthPunctuation(nextVisible)) {
            text = text.trimStart()
        }
    }

    if (next.isInlineMathWidgetAtom() && text.lastOrNull()?.isWhitespace() == true) {
        val previousVisible = text.lastOrNull { !it.isWhitespace() }
        if (previousVisible != null && CharacterUtils.isCJKOrFullWidthPunctuation(previousVisible)) {
            text = text.trimEnd()
        }
    }

    return if (text == atom.text) atom else atom.copy(text = text)
}

private fun InlineAtom?.isInlineMathWidgetAtom(): Boolean {
    return (this as? WidgetAtom)?.widget is InlineMathWidgetModel
}

private fun AnnotatedString.Builder.renderTextAtom(
    atom: TextAtom,
    theme: MarkdownTheme,
    context: InlineRenderBuildContext,
    onLinkClick: ((String) -> Unit)?,
    onFootnoteClick: ((String) -> Unit)?,
) {
    val clickMark =
        atom.marks.lastOrNull { it.kind == "link" || it.kind == "footnote" || it.kind == "citation" }
    val abbreviation = atom.marks.lastOrNull { it.kind == "abbreviation" }?.payload?.get("fullText")
    val spanStyle = atom.marks.fold(SpanStyle()) { acc, mark ->
        acc.merge(spanStyleForMark(mark, theme))
    }

    val segment = buildAnnotatedString {
        val appendText: AnnotatedString.Builder.() -> Unit = {
            if (!abbreviation.isNullOrEmpty()) {
                pushStringAnnotation(tag = "abbreviation", annotation = abbreviation)
            }
            if (spanStyle != SpanStyle()) {
                withStyle(spanStyle) { append(atom.text) }
            } else {
                append(atom.text)
            }
            if (!abbreviation.isNullOrEmpty()) {
                pop()
            }
        }

        when (clickMark?.kind) {
            "link" -> {
                val target = clickMark.payload["target"].orEmpty()
                withLink(
                    LinkAnnotation.Clickable(
                        tag = clickMark.payload["tag"] ?: "link",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = theme.linkColor,
                                textDecoration = TextDecoration.Underline,
                            )
                        ),
                        linkInteractionListener = { onLinkClick?.invoke(target) },
                    )
                ) { appendText() }
            }

            "footnote" -> {
                val label = clickMark.payload["label"].orEmpty()
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "footnote",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = theme.linkColor,
                                fontSize = theme.footnoteStyle.fontSize,
                                baselineShift = BaselineShift.Superscript,
                            )
                        ),
                        linkInteractionListener = { onFootnoteClick?.invoke(label) },
                    )
                ) { appendText() }
            }

            "citation" -> {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "citation",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = theme.linkColor,
                                fontSize = theme.footnoteStyle.fontSize,
                                baselineShift = BaselineShift.Superscript,
                            )
                        ),
                        linkInteractionListener = { },
                    )
                ) { appendText() }
            }

            else -> appendText()
        }
    }
    context.emitTextAtom(this, segment)
}

private fun spanStyleForMark(mark: SpanMark, theme: MarkdownTheme): SpanStyle = when (mark.kind) {
    "emphasis" -> SpanStyle(fontStyle = FontStyle.Italic)
    "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
    "strikethrough" -> theme.strikethroughStyle
    "highlight" -> SpanStyle(background = theme.highlightColor)
    "superscript" -> theme.superscriptStyle.merge(SpanStyle(baselineShift = BaselineShift.Superscript))
    "subscript" -> theme.subscriptStyle.merge(SpanStyle(baselineShift = BaselineShift.Subscript))
    "inserted" -> theme.insertedTextStyle
    "styled" -> {
        val style = mark.payload["style"]?.let(::parseCssStyleToSpanStyle)
        val classes = mark.payload["class"]?.split(" ")?.filter { it.isNotBlank() }.orEmpty()
        style ?: inferStyleFromClasses(classes, theme) ?: SpanStyle()
    }

    "abbreviation" -> theme.abbreviationStyle
    "kbd" -> theme.kbdStyle
    "inline_html" -> SpanStyle(
        color = Color.Gray,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
    )

    else -> SpanStyle()
}

private fun AnnotatedString.Builder.renderWidgetAtom(
    atom: WidgetAtom,
    theme: MarkdownTheme,
    hostTextStyle: TextStyle,
    context: InlineRenderBuildContext,
    directiveRegistry: MarkdownDirectiveRegistry,
    onLinkClick: ((String) -> Unit)?,
    onFootnoteClick: ((String) -> Unit)?,
    latexMeasurer: LatexMeasurerState,
    density: Density,
    textMeasurer: TextMeasurer,
    inlineCodeTheme: CodeTheme? = null,
) {
    when (val widget = atom.widget) {
        is InlineCodeWidgetModel -> renderInlineCodeWidget(
            widget,
            theme,
            context,
            density,
            textMeasurer,
            inlineCodeTheme
        )

        is ImageWidgetModel -> renderImageWidget(widget, context)
        is InlineMathWidgetModel -> renderInlineMathWidget(
            widget,
            theme,
            hostTextStyle,
            context,
            latexMeasurer,
            density,
            textMeasurer
        )

        is SpoilerWidgetModel -> renderSpoilerWidget(
            widget = widget,
            theme = theme,
            hostTextStyle = hostTextStyle,
            context = context,
            directiveRegistry = directiveRegistry,
            onLinkClick = onLinkClick,
            onFootnoteClick = onFootnoteClick,
            latexMeasurer = latexMeasurer,
            density = density,
            textMeasurer = textMeasurer,
            inlineCodeTheme = inlineCodeTheme,
        )

        is DirectiveInlineWidgetModel -> renderDirectiveInlineWidget(
            widget,
            theme,
            context,
            directiveRegistry,
            density
        )

        is RubyTextWidgetModel -> renderRubyTextWidget(widget, theme, context, density)
    }
}

private fun AnnotatedString.Builder.renderInlineCodeWidget(
    widget: InlineCodeWidgetModel,
    theme: MarkdownTheme,
    context: InlineRenderBuildContext,
    density: Density?,
    textMeasurer: TextMeasurer?,
    inlineCodeTheme: CodeTheme?,
) {
    if (density != null && textMeasurer != null && inlineCodeTheme != null) {
        val inlineCodeStyle = InlineCodeDefaults.style(inlineCodeTheme)
        val size = measureInlineCodeSize(
            text = widget.code,
            style = inlineCodeStyle,
            density = density,
            textMeasurer = textMeasurer,
        )
        context.emitInlineCodeWidget(
            builder = this,
            widget = widget,
            widthPx = ceil(size.width),
            heightPx = size.height,
        ) {
            CodeHighInlineCode(text = widget.code, style = inlineCodeStyle)
        }
    } else {
        context.emitStyledTextAtom(this, widget.code, theme.inlineCodeStyle)
    }
}

private fun AnnotatedString.Builder.renderImageWidget(
    widget: ImageWidgetModel,
    context: InlineRenderBuildContext,
) {
    context.emitImageWidget(
        builder = this,
        widget = widget,
        widthPx = widget.width?.toFloat() ?: 200f,
        heightPx = widget.height?.toFloat() ?: 150f,
    ) {
        val imageData = MarkdownImageData(
            url = widget.url,
            altText = widget.altText,
            title = widget.title,
            width = widget.width,
            height = widget.height,
            attributes = widget.attributes,
        )
        val customRenderer = LocalImageRenderer.current
        if (customRenderer != null) {
            customRenderer(imageData, Modifier)
        } else {
            DefaultMarkdownImage(data = imageData)
        }
    }
}

private fun AnnotatedString.Builder.renderInlineMathWidget(
    widget: InlineMathWidgetModel,
    theme: MarkdownTheme,
    hostTextStyle: TextStyle,
    context: InlineRenderBuildContext,
    latexMeasurer: LatexMeasurerState,
    density: Density,
    textMeasurer: TextMeasurer,
) {
    val trimmedLatex = widget.latex.trim()
    val fontSize = theme.mathFontSize
    val latexConfig = LatexConfig(
        fontSize = fontSize.sp,
        theme = theme.latexTheme,
        mathFont = theme.mathFont,
    )
    val dims = latexMeasurer.measure(trimmedLatex, latexConfig)
    val placeholderWidth = dims?.contentWidthPx ?: with(density) {
        (fontSize * estimateLatexWidth(
            trimmedLatex
        )).sp.toPx()
    }
    val placeholderHeight = run {
        val measuredHeightPx = dims?.heightPx ?: with(density) { (fontSize * 1.6f).sp.toPx() }
        val hostHeightPx = textMeasurer.measure("Ag", style = hostTextStyle).size.height.toFloat()
        val extraPx = with(density) { 2f.toDp().toPx() }
        maxOf(hostHeightPx, measuredHeightPx + extraPx)
    }

    context.emitInlineMathWidget(
        builder = this,
        widget = widget,
        widthPx = placeholderWidth,
        heightPx = placeholderHeight,
    ) {
        val canvasWidthDp = if (dims != null) {
            with(density) { dims.widthPx.toDp() }
        } else {
            null
        }
        val canvasHeightDp = if (dims != null) {
            with(density) { dims.heightPx.toDp() }
        } else {
            null
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clipToBounds()
        ) {
            val latexModifier = Modifier
                .then(
                    if (canvasWidthDp != null && canvasHeightDp != null) {
                        Modifier.requiredSize(canvasWidthDp, canvasHeightDp)
                    } else {
                        Modifier
                    }
                )
            Latex(
                latex = trimmedLatex,
                modifier = latexModifier,
                config = latexConfig,
            )
        }
    }
}

private fun AnnotatedString.Builder.renderSpoilerWidget(
    widget: SpoilerWidgetModel,
    theme: MarkdownTheme,
    hostTextStyle: TextStyle,
    context: InlineRenderBuildContext,
    directiveRegistry: MarkdownDirectiveRegistry,
    onLinkClick: ((String) -> Unit)?,
    onFootnoteClick: ((String) -> Unit)?,
    latexMeasurer: LatexMeasurerState,
    density: Density,
    textMeasurer: TextMeasurer,
    inlineCodeTheme: CodeTheme?,
) {
    val fontSize = theme.bodyStyle.fontSize.value
    val avgCharWidth = widget.alternateText.sumOf { ch -> if (ch.code > 0x7F) 12 else 7 }
        .toFloat() / 10f * (fontSize / 16f)
    context.emitSpoilerWidget(
        builder = this,
        widget = widget,
        widthPx = density.scaledSpValueToPx(avgCharWidth + 8f),
        heightPx = density.scaledSpValueToPx(fontSize * 1.5f),
    ) {
        SpoilerContent(
            model = widget.content,
            theme = theme,
            hostTextStyle = hostTextStyle,
            directiveRegistry = directiveRegistry,
            onLinkClick = onLinkClick,
            onFootnoteClick = onFootnoteClick,
            latexMeasurer = latexMeasurer,
            density = density,
            textMeasurer = textMeasurer,
            inlineCodeTheme = inlineCodeTheme,
        )
    }
}

private fun AnnotatedString.Builder.renderDirectiveInlineWidget(
    widget: DirectiveInlineWidgetModel,
    theme: MarkdownTheme,
    context: InlineRenderBuildContext,
    directiveRegistry: MarkdownDirectiveRegistry,
    density: Density?,
) {
    val renderer = directiveRegistry.findInlineDirectiveRenderer(widget.tagName)
    if (renderer != null) {
        val fontSize = theme.bodyStyle.fontSize.value
        val estimatedWidth = widget.alternateText.sumOf { ch -> if (ch.code > 0x7F) 12 else 7 }
            .toFloat() / 10f * (fontSize / 16f)
        context.emitDirectiveInlineWidget(
            builder = this,
            widget = widget,
            widthPx = density.scaledSpValueToPx(estimatedWidth + 8f),
            heightPx = density.scaledSpValueToPx(fontSize * 1.5f),
        ) {
            renderer(
                createDirectiveInlineRenderScope(
                    tagName = widget.tagName,
                    args = widget.args,
                    alternateText = widget.alternateText,
                )
            )
        }
    } else {
        context.emitStyledTextAtom(
            builder = this,
            text = widget.alternateText,
            style = SpanStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = theme.bodyStyle.fontSize * 0.875f,
                color = theme.linkColor,
            ),
        )
    }
}

private fun AnnotatedString.Builder.renderRubyTextWidget(
    widget: RubyTextWidgetModel,
    theme: MarkdownTheme,
    context: InlineRenderBuildContext,
    density: Density?,
) {
    val fontSize = theme.bodyStyle.fontSize.value
    val baseWidth =
        widget.base.sumOf { ch -> if (ch.code > 0x7F) 12 else 7 }.toFloat() / 10f * (fontSize / 16f)
    context.emitRubyTextWidget(
        builder = this,
        widget = widget,
        widthPx = density.scaledSpValueToPx(baseWidth + 2f),
        heightPx = density.scaledSpValueToPx(fontSize * 2.0f),
    ) {
        RubyTextContent(
            base = widget.base,
            annotation = widget.annotation,
            theme = theme,
        )
    }
}

private fun Density?.scaledSpValueToPx(value: Float): Float {
    return this?.run { value.sp.toPx() } ?: value
}
