package com.hrm.markdown.renderer.block

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.rememberTextMeasurer
import com.hrm.codehigh.theme.LocalCodeTheme
import com.hrm.markdown.parser.ast.Image
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.Paragraph
import com.hrm.markdown.parser.ast.Text
import com.hrm.markdown.renderer.DefaultMarkdownImage
import com.hrm.markdown.renderer.LocalCodeHighlightTheme
import com.hrm.markdown.renderer.LocalImageRenderer
import com.hrm.markdown.renderer.LocalMarkdownDirectiveRegistry
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.LocalOnFootnoteClick
import com.hrm.markdown.renderer.LocalOnLinkClick
import com.hrm.markdown.renderer.MarkdownImageData
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.markdown.renderer.inline.buildInlineAnnotatedString
import com.hrm.markdown.renderer.inline.InlineFlowText
import com.hrm.markdown.renderer.inline.InlineContentEntry
import com.hrm.markdown.renderer.inline.rememberInlineContent

/**
 * 段落渲染器。
 *
 * 当段落中不包含图片时，直接使用 BasicText + InlineTextContent 渲染富文本。
 * 当段落中包含图片时，将段落拆分为「文本段」和「图片段」，
 * 图片作为独立的块级 Composable 渲染，避免 InlineTextContent 的 Placeholder 尺寸限制
 * 导致图片无法正常显示。
 */
@Composable
internal fun ParagraphRenderer(
    node: Paragraph,
    modifier: Modifier = Modifier,
) {
    val hasImage = remember(node, node.contentHash, node.lineRange.endLine, node.childCount()) {
        node.children.any { it is Image }
    }

    if (!hasImage) {
        // 无图片的段落：保持原有的简单渲染路径
        SimpleParagraphRenderer(node, modifier)
    } else {
        // 包含图片的段落：拆分为文本段和图片段
        MixedParagraphRenderer(node, modifier)
    }
}

/**
 * 简单段落渲染（不含图片）。
 */
@Composable
private fun SimpleParagraphRenderer(
    node: Paragraph,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val onLinkClick = LocalOnLinkClick.current
    val inlineResult = rememberInlineContent(
        parent = node,
        onLinkClick = onLinkClick,
        hostTextStyle = theme.bodyStyle,
    )
    InlineFlowText(
        annotated = inlineResult.annotated,
        inlineContents = inlineResult.inlineContents,
        modifier = modifier.fillMaxWidth(),
        style = theme.bodyStyle,
    )
}

/**
 * 段落内容片段：文本或图片。
 */
private sealed class ParagraphSegment {
    /** 一段连续的非图片行内节点 */
    data class TextRun(val nodes: List<Node>) : ParagraphSegment()

    /** 图片节点 */
    data class ImageItem(val image: Image) : ParagraphSegment()
}

/**
 * 混合段落渲染（包含图片）。
 * 将段落的子节点拆分为文本段和图片段，分别渲染。
 */
@Composable
private fun MixedParagraphRenderer(
    node: Paragraph,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val onLinkClick = LocalOnLinkClick.current
    val onFootnoteClick = LocalOnFootnoteClick.current
    val customRenderer = LocalImageRenderer.current
    val directiveRegistry = LocalMarkdownDirectiveRegistry.current
    val latexMeasurer = rememberLatexMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val codeTheme = LocalCodeHighlightTheme.current ?: LocalCodeTheme.current

    // 将段落子节点拆分为文本段和图片段
    val segments = remember(node, node.contentHash, node.lineRange.endLine, node.childCount()) {
        splitParagraphSegments(node.children)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        for (segment in segments) {
            when (segment) {
                is ParagraphSegment.TextRun -> {
                    val built = remember(
                        segment, theme, onLinkClick, onFootnoteClick, directiveRegistry,
                        latexMeasurer, density, textMeasurer, codeTheme,
                    ) {
                        val inlineContents = mutableMapOf<String, InlineContentEntry>()
                        val annotated = buildInlineAnnotatedString(
                            nodes = segment.nodes,
                            theme = theme,
                            hostTextStyle = theme.bodyStyle,
                            inlineContents = inlineContents,
                            directiveRegistry = directiveRegistry,
                            onLinkClick = onLinkClick,
                            onFootnoteClick = onFootnoteClick,
                            latexMeasurer = latexMeasurer,
                            density = density,
                            textMeasurer = textMeasurer,
                            codeTheme = codeTheme,
                        )
                        annotated to inlineContents
                    }
                    val (annotated, inlineContents) = built
                    if (annotated.isNotEmpty()) {
                        InlineFlowText(
                            annotated = annotated,
                            inlineContents = inlineContents,
                            modifier = Modifier.fillMaxWidth(),
                            style = theme.bodyStyle,
                        )
                    }
                }

                is ParagraphSegment.ImageItem -> {
                    val img = segment.image
                    val altText = img.children.filterIsInstance<Text>()
                        .joinToString("") { it.literal }
                    val imageData = MarkdownImageData(
                        url = img.destination,
                        altText = altText,
                        title = img.title,
                        width = img.imageWidth,
                        height = img.imageHeight,
                        attributes = img.attributes,
                    )
                    if (customRenderer != null) {
                        customRenderer(imageData, Modifier)
                    } else {
                        DefaultMarkdownImage(data = imageData)
                    }
                }
            }
        }
    }
}

/**
 * 将段落的子节点列表按图片边界拆分为多个 Segment。
 * 连续的非图片节点合并为一个 TextRun，图片节点独立为 ImageItem。
 */
private fun splitParagraphSegments(children: List<Node>): List<ParagraphSegment> {
    val segments = mutableListOf<ParagraphSegment>()
    val currentTextNodes = mutableListOf<Node>()

    for (child in children) {
        if (child is Image) {
            // 遇到图片前，先把积累的文本节点收集为一个 TextRun
            if (currentTextNodes.isNotEmpty()) {
                segments.add(ParagraphSegment.TextRun(currentTextNodes.toList()))
                currentTextNodes.clear()
            }
            segments.add(ParagraphSegment.ImageItem(child))
        } else {
            currentTextNodes.add(child)
        }
    }

    // 收集尾部的文本节点
    if (currentTextNodes.isNotEmpty()) {
        segments.add(ParagraphSegment.TextRun(currentTextNodes.toList()))
    }

    return segments
}
