package com.hrm.markdown.renderer.inline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.renderer.internal.core.compile.compileInlineModel
import com.hrm.markdown.renderer.internal.core.model.InlineModel

private const val INLINE_REVISION_OFFSET_BASIS = -3750763034362895579L
private const val INLINE_REVISION_FNV_PRIME = 1099511628211L

@Composable
internal fun rememberInlineModel(parent: ContainerNode): InlineModel {
    val inlineRevision = remember(
        parent.contentHash,
        parent.lineRange.startLine,
        parent.lineRange.endLine,
        parent.childCount(),
    ) {
        inlineNodesRevision(parent.children)
    }
    return rememberInlineModel(
        nodes = parent.children,
        inlineRevision = inlineRevision,
    )
}

@Composable
internal fun rememberInlineModel(
    nodes: List<Node>,
    inlineRevision: Long,
): InlineModel {
    return remember(inlineRevision) {
        compileInlineModel(
            nodes = nodes,
            inlineRevision = inlineRevision,
        )
    }
}

internal fun inlineNodesRevision(nodes: List<Node>): Long {
    var acc = INLINE_REVISION_OFFSET_BASIS
    for (node in nodes) {
        acc = inlineNodeRevision(acc, node)
    }
    return acc
}

private fun inlineNodeRevision(acc: Long, node: Node): Long {
    var next = acc
    next = inlineMixRevision(next, inlineNodeKindId(node))
    next = inlineMixRevision(next, node.contentHash)
    next = inlineMixRevision(next, node.sourceRange.start.offset.toLong())
    next = inlineMixRevision(next, node.sourceRange.end.offset.toLong())
    next = inlineMixRevision(next, node.lineRange.startLine.toLong())
    next = inlineMixRevision(next, node.lineRange.endLine.toLong())
    if (node is ContainerNode) {
        next = inlineMixRevision(next, node.childCount().toLong())
        next = inlineMixRevision(next, inlineNodesRevision(node.children))
    }
    return next
}

private fun inlineNodeKindId(node: Node): Long = when (node) {
    is com.hrm.markdown.parser.ast.Text -> 1L
    is com.hrm.markdown.parser.ast.SoftLineBreak -> 2L
    is com.hrm.markdown.parser.ast.HardLineBreak -> 3L
    is com.hrm.markdown.parser.ast.Emphasis -> 4L
    is com.hrm.markdown.parser.ast.StrongEmphasis -> 5L
    is com.hrm.markdown.parser.ast.Strikethrough -> 6L
    is com.hrm.markdown.parser.ast.InlineCode -> 7L
    is com.hrm.markdown.parser.ast.Link -> 8L
    is com.hrm.markdown.parser.ast.Image -> 9L
    is com.hrm.markdown.parser.ast.Autolink -> 10L
    is com.hrm.markdown.parser.ast.InlineHtml -> 11L
    is com.hrm.markdown.parser.ast.HtmlEntity -> 12L
    is com.hrm.markdown.parser.ast.EscapedChar -> 13L
    is com.hrm.markdown.parser.ast.FootnoteReference -> 14L
    is com.hrm.markdown.parser.ast.InlineMath -> 15L
    is com.hrm.markdown.parser.ast.Highlight -> 16L
    is com.hrm.markdown.parser.ast.Superscript -> 17L
    is com.hrm.markdown.parser.ast.Subscript -> 18L
    is com.hrm.markdown.parser.ast.InsertedText -> 19L
    is com.hrm.markdown.parser.ast.Emoji -> 20L
    is com.hrm.markdown.parser.ast.StyledText -> 21L
    is com.hrm.markdown.parser.ast.Abbreviation -> 22L
    is com.hrm.markdown.parser.ast.KeyboardInput -> 23L
    is com.hrm.markdown.parser.ast.CitationReference -> 24L
    is com.hrm.markdown.parser.ast.Spoiler -> 25L
    is com.hrm.markdown.parser.ast.DirectiveInline -> 26L
    is com.hrm.markdown.parser.ast.WikiLink -> 27L
    is com.hrm.markdown.parser.ast.RubyText -> 28L
    else -> 0L
}

private fun inlineMixRevision(acc: Long, value: Long): Long = (acc xor value) * INLINE_REVISION_FNV_PRIME
