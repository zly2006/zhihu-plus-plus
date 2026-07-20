package com.hrm.markdown.renderer.block

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.hrm.markdown.parser.ast.Paragraph
import com.hrm.markdown.renderer.DefaultMarkdownImage
import com.hrm.markdown.renderer.LocalImageRenderer
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.MarkdownImageData
import com.hrm.markdown.renderer.inline.InlineLayoutBlockText
import com.hrm.markdown.renderer.inline.inlineNodesRevision
import com.hrm.markdown.renderer.inline.rememberInlineModel
import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity
import com.hrm.markdown.renderer.internal.core.identity.renderIdentityMix
import com.hrm.markdown.renderer.internal.core.identity.renderIdentitySeed
import com.hrm.markdown.renderer.internal.core.model.ImageWidgetModel
import com.hrm.markdown.renderer.internal.core.model.InlineAtom
import com.hrm.markdown.renderer.internal.core.model.InlineModel
import com.hrm.markdown.renderer.internal.core.model.WidgetAtom

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
    val inlineRevision = remember(node.contentHash, node.lineRange.startLine, node.lineRange.endLine, node.childCount()) {
        inlineNodesRevision(node.children)
    }
    val inlineModel = rememberInlineModel(
        nodes = node.children,
        inlineRevision = inlineRevision,
    )
    val hasImage = remember(inlineModel.identity.contentRevision) {
        inlineModel.atoms.any { atom ->
            atom is WidgetAtom && atom.widget is ImageWidgetModel
        }
    }

    RenderParagraphInlineModel(
        inlineModel = inlineModel,
        hasImage = hasImage,
        modifier = modifier,
    )
}

/**
 * 简单段落渲染（不含图片）。
 */
@Composable
internal fun RenderParagraphInlineModel(
    inlineModel: InlineModel,
    hasImage: Boolean = inlineModel.atoms.any { atom ->
        atom is WidgetAtom && atom.widget is ImageWidgetModel
    },
    modifier: Modifier = Modifier,
) {
    if (hasImage) {
        RenderMixedParagraphBlockModel(inlineModel, modifier)
    } else {
        RenderParagraphBlockModel(inlineModel, modifier)
    }
}

@Composable
internal fun RenderParagraphBlockModel(
    inlineModel: InlineModel,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    InlineLayoutBlockText(
        model = inlineModel,
        modifier = modifier.fillMaxWidth(),
        style = theme.bodyStyle,
    )
}

/**
 * 段落内容片段：文本或图片。
 */
private sealed class ParagraphSegment {
    /** 一段连续的非图片行内模型 */
    data class TextRun(
        val model: InlineModel,
    ) : ParagraphSegment()

    /** 图片 widget */
    data class ImageItem(val image: ImageWidgetModel) : ParagraphSegment()
}

/**
 * 混合段落渲染（包含图片）。
 * 将段落的子节点拆分为文本段和图片段，分别渲染。
 */
@Composable
internal fun RenderMixedParagraphBlockModel(
    inlineModel: InlineModel,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val customRenderer = LocalImageRenderer.current

    val segments = remember(inlineModel.identity.contentRevision) {
        splitParagraphSegments(inlineModel)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        for (segment in segments) {
            when (segment) {
                is ParagraphSegment.TextRun -> {
                    if (segment.model.atoms.isNotEmpty()) {
                        InlineLayoutBlockText(
                            model = segment.model,
                            modifier = Modifier.fillMaxWidth(),
                            style = theme.bodyStyle,
                        )
                    }
                }

                is ParagraphSegment.ImageItem -> {
                    val img = segment.image
                    val imageData = MarkdownImageData(
                        url = img.url,
                        altText = img.altText,
                        title = img.title,
                        width = img.width,
                        height = img.height,
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
 * 将段落的 inline model 按图片 widget 边界拆分为多个 Segment。
 * 连续的非图片 atom 合并为一个 TextRun，图片 widget 独立为 ImageItem。
 */
private fun splitParagraphSegments(model: InlineModel): List<ParagraphSegment> {
    val segments = mutableListOf<ParagraphSegment>()
    val currentAtoms = mutableListOf<InlineAtom>()

    for (atom in model.atoms) {
        val imageWidget = (atom as? WidgetAtom)?.widget as? ImageWidgetModel
        if (imageWidget != null) {
            if (currentAtoms.isNotEmpty()) {
                segments.add(ParagraphSegment.TextRun(currentAtoms.toInlineModelSegment()))
                currentAtoms.clear()
            }
            segments.add(ParagraphSegment.ImageItem(imageWidget))
        } else {
            currentAtoms.add(atom)
        }
    }

    if (currentAtoms.isNotEmpty()) {
        segments.add(ParagraphSegment.TextRun(currentAtoms.toInlineModelSegment()))
    }

    return segments
}

private fun List<InlineAtom>.toInlineModelSegment(): InlineModel {
    val atoms = toList()
    return InlineModel(
        identity = RenderIdentity(
            stableId = fold(renderIdentitySeed()) { acc, atom ->
                renderIdentityMix(acc, atom.identity.stableId)
            },
            contentRevision = fold(renderIdentitySeed()) { acc, atom ->
                renderIdentityMix(acc, atom.identity.contentRevision)
            },
            layoutRevision = fold(renderIdentitySeed()) { acc, atom ->
                renderIdentityMix(acc, atom.identity.layoutRevision)
            },
            paintRevision = fold(renderIdentitySeed()) { acc, atom ->
                renderIdentityMix(acc, atom.identity.paintRevision)
            },
        ),
        atoms = atoms,
    )
}
