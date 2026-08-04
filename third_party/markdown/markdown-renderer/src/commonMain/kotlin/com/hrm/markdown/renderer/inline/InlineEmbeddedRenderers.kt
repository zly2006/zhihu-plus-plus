package com.hrm.markdown.renderer.inline

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.hrm.codehigh.renderer.InlineCodeDefaults
import com.hrm.codehigh.renderer.measureInlineCodeSize
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.hrm.markdown.parser.ast.DirectiveInline
import com.hrm.markdown.parser.ast.Image
import com.hrm.markdown.parser.ast.InlineCode
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.RubyText
import com.hrm.markdown.parser.ast.Spoiler
import com.hrm.markdown.parser.ast.Text
import com.hrm.markdown.renderer.DefaultMarkdownImage
import com.hrm.markdown.renderer.LocalImageRenderer
import com.hrm.markdown.renderer.MarkdownImageData
import com.hrm.markdown.renderer.MarkdownTheme
import com.hrm.markdown.runtime.DirectiveInlineRenderScope
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry
import kotlin.math.ceil
import com.hrm.codehigh.renderer.InlineCode as CodeHighInlineCode

internal fun AnnotatedString.Builder.renderInlineCodeNode(
    node: InlineCode,
    theme: MarkdownTheme,
    inlineContents: MutableMap<String, InlineContentEntry>,
    density: Density?,
    textMeasurer: TextMeasurer?,
    inlineCodeTheme: CodeTheme?,
) {
    if (density != null && textMeasurer != null && inlineCodeTheme != null) {
        val inlineCodeStyle = InlineCodeDefaults.style(inlineCodeTheme)
        val size = measureInlineCodeSize(
            text = node.literal,
            style = inlineCodeStyle,
            density = density,
            textMeasurer = textMeasurer,
        )
        val id = "inlinecode_${node.hashCode()}"
        appendInlinePlaceholder(id)
        val itc = InlineTextContent(
            placeholder = Placeholder(
                width = with(density) { ceil(size.width).toSp() },
                height = with(density) { size.height.toSp() },
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
        ) {
            CodeHighInlineCode(text = node.literal, style = inlineCodeStyle)
        }
        inlineContents[id] = InlineContentEntry(
            alternateText = node.literal,
            inlineTextContent = itc,
        )
    } else {
        withStyle(theme.inlineCodeStyle) {
            append(node.literal)
        }
    }
}

internal fun AnnotatedString.Builder.renderImageNode(
    node: Image,
    inlineContents: MutableMap<String, InlineContentEntry>,
) {
    val id = "img_${node.hashCode()}"
    val altText = node.children.filterIsInstance<Text>().joinToString("") { it.literal }
    val placeholderWidth = (node.imageWidth?.toFloat() ?: 200f)
    val placeholderHeight = (node.imageHeight?.toFloat() ?: 150f)

    appendInlinePlaceholder(id)
    val itc = InlineTextContent(
        placeholder = Placeholder(
            width = placeholderWidth.sp,
            height = placeholderHeight.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
        ),
    ) {
        val imageData = MarkdownImageData(
            url = node.destination,
            altText = altText,
            title = node.title,
            width = node.imageWidth,
            height = node.imageHeight,
            attributes = node.attributes,
        )
        val customRenderer = LocalImageRenderer.current
        if (customRenderer != null) {
            customRenderer(imageData, Modifier)
        } else {
            DefaultMarkdownImage(data = imageData)
        }
    }
    inlineContents[id] = InlineContentEntry(
        alternateText = node.title ?: altText.ifEmpty { node.destination },
        inlineTextContent = itc,
    )
}

internal fun AnnotatedString.Builder.renderInlineMathNode(
    node: InlineMath,
    theme: MarkdownTheme,
    hostTextStyle: TextStyle,
    inlineContents: MutableMap<String, InlineContentEntry>,
    latexMeasurer: LatexMeasurerState?,
    density: Density?,
    textMeasurer: TextMeasurer?,
) {
    val id = "math_${node.hashCode()}"
    val fontSize = theme.mathFontSize
    val latexConfig = LatexConfig(
        fontSize = fontSize.sp,
        theme = LatexTheme.light(color = theme.mathColor),
        mathFont = theme.mathFont,
    )

    val dims = latexMeasurer?.measure(node.literal, latexConfig)
    val placeholderWidth = if (dims != null && density != null) {
        with(density) { dims.widthPx.toSp() }
    } else {
        (fontSize * estimateLatexWidth(node.literal)).sp
    }
    val placeholderHeight = if (density != null) {
        val measuredHeightPx = dims?.heightPx ?: with(density) { (fontSize * 1.6f).sp.toPx() }
        val hostHeightPx =
            textMeasurer?.measure("Ag", style = hostTextStyle)?.size?.height?.toFloat()
                ?: with(density) {
                    ((hostTextStyle.lineHeight.takeUnless { it.value.isNaN() }
                        ?: (hostTextStyle.fontSize * 1.5f))).toPx()
                }
        val extraPx = with(density) { 2f.toDp().toPx() }
        with(density) { maxOf(hostHeightPx, measuredHeightPx + extraPx).toSp() }
    } else {
        (fontSize * 1.8f).sp
    }

    appendInlinePlaceholder(id)
    val itc = InlineTextContent(
        placeholder = Placeholder(
            width = placeholderWidth,
            height = placeholderHeight,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
        ),
    ) {
        com.hrm.latex.renderer.Latex(
            latex = node.literal,
            config = latexConfig,
        )
    }
    inlineContents[id] = InlineContentEntry(
        alternateText = node.literal,
        inlineTextContent = itc,
    )
}

internal fun AnnotatedString.Builder.renderSpoilerNode(
    node: Spoiler,
    theme: MarkdownTheme,
    hostTextStyle: TextStyle,
    inlineContents: MutableMap<String, InlineContentEntry>,
    directiveRegistry: MarkdownDirectiveRegistry,
    onLinkClick: ((String) -> Unit)?,
    onFootnoteClick: ((String) -> Unit)?,
    latexMeasurer: LatexMeasurerState?,
    density: Density?,
    textMeasurer: TextMeasurer?,
    inlineCodeTheme: CodeTheme?,
) {
    val id = "spoiler_${node.hashCode()}"
    val plainText = extractPlainText(node)
    val fontSize = theme.bodyStyle.fontSize.value
    val avgCharWidth = plainText.sumOf { ch ->
        if (ch.code > 0x7F) 12 else 7
    }.toFloat() / 10f * (fontSize / 16f)
    val placeholderWidth = (avgCharWidth + 8f).sp
    val placeholderHeight = (fontSize * 1.5f).sp

    appendInlinePlaceholder(id)
    val itc = InlineTextContent(
        placeholder = Placeholder(
            width = placeholderWidth,
            height = placeholderHeight,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
        ),
    ) {
        SpoilerContent(
            node = node,
            theme = theme,
            hostTextStyle = hostTextStyle,
            inlineContents = inlineContents,
            directiveRegistry = directiveRegistry,
            onLinkClick = onLinkClick,
            onFootnoteClick = onFootnoteClick,
            latexMeasurer = latexMeasurer,
            density = density,
            textMeasurer = textMeasurer,
            inlineCodeTheme = inlineCodeTheme,
        )
    }
    inlineContents[id] = InlineContentEntry(
        alternateText = plainText,
        inlineTextContent = itc,
    )
}

internal fun AnnotatedString.Builder.renderDirectiveInlineNode(
    node: DirectiveInline,
    theme: MarkdownTheme,
    inlineContents: MutableMap<String, InlineContentEntry>,
    directiveRegistry: MarkdownDirectiveRegistry,
) {
    val alternateText = buildInlineDirectiveFallbackText(node)
    val renderer = directiveRegistry.findInlineDirectiveRenderer(node.tagName)
    if (renderer != null) {
        val id = "directive_inline_${node.hashCode()}_${node.tagName}"
        val fontSize = theme.bodyStyle.fontSize.value
        val estimatedWidth = alternateText.sumOf { ch ->
            if (ch.code > 0x7F) 12 else 7
        }.toFloat() / 10f * (fontSize / 16f)
        appendInlinePlaceholder(id)
        val itc = InlineTextContent(
            placeholder = Placeholder(
                width = (estimatedWidth + 8f).sp,
                height = (fontSize * 1.5f).sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
        ) {
            renderer(
                DirectiveInlineRenderScope(
                    tagName = node.tagName,
                    args = node.args,
                    node = node,
                    alternateText = alternateText,
                )
            )
        }
        inlineContents[id] = InlineContentEntry(
            alternateText = alternateText,
            inlineTextContent = itc,
        )
    } else {
        withStyle(
            SpanStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = theme.bodyStyle.fontSize * 0.875f,
                color = theme.linkColor,
            )
        ) {
            append(alternateText)
        }
    }
}

internal fun AnnotatedString.Builder.renderRubyTextNode(
    node: RubyText,
    theme: MarkdownTheme,
    inlineContents: MutableMap<String, InlineContentEntry>,
) {
    val id = "ruby_${node.hashCode()}"
    val fontSize = theme.bodyStyle.fontSize.value
    val baseWidth = node.base.sumOf { ch ->
        if (ch.code > 0x7F) 12 else 7
    }.toFloat() / 10f * (fontSize / 16f)
    val placeholderWidth = (baseWidth + 2f).sp
    val placeholderHeight = (fontSize * 2.0f).sp

    appendInlinePlaceholder(id)
    val itc = InlineTextContent(
        placeholder = Placeholder(
            width = placeholderWidth,
            height = placeholderHeight,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
        ),
    ) {
        RubyTextContent(
            base = node.base,
            annotation = node.annotation,
            theme = theme,
        )
    }
    inlineContents[id] = InlineContentEntry(
        alternateText = node.base,
        inlineTextContent = itc,
    )
}
