package com.hrm.markdown.renderer.internal.selection

import com.hrm.markdown.parser.ast.Autolink
import com.hrm.markdown.parser.ast.Admonition
import com.hrm.markdown.parser.ast.BibliographyDefinition
import com.hrm.markdown.parser.ast.BlockQuote
import com.hrm.markdown.parser.ast.CitationReference
import com.hrm.markdown.parser.ast.ColumnItem
import com.hrm.markdown.parser.ast.ColumnsLayout
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.CustomContainer
import com.hrm.markdown.parser.ast.DefinitionDescription
import com.hrm.markdown.parser.ast.DefinitionList
import com.hrm.markdown.parser.ast.DefinitionTerm
import com.hrm.markdown.parser.ast.DiagramBlock
import com.hrm.markdown.parser.ast.DirectiveBlock
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.ast.Emoji
import com.hrm.markdown.parser.ast.EscapedChar
import com.hrm.markdown.parser.ast.FencedCodeBlock
import com.hrm.markdown.parser.ast.Figure
import com.hrm.markdown.parser.ast.FootnoteReference
import com.hrm.markdown.parser.ast.FootnoteDefinition
import com.hrm.markdown.parser.ast.HardLineBreak
import com.hrm.markdown.parser.ast.Heading
import com.hrm.markdown.parser.ast.HtmlBlock
import com.hrm.markdown.parser.ast.HtmlEntity
import com.hrm.markdown.parser.ast.Image
import com.hrm.markdown.parser.ast.IndentedCodeBlock
import com.hrm.markdown.parser.ast.InlineCode
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.KeyboardInput
import com.hrm.markdown.parser.ast.ListBlock
import com.hrm.markdown.parser.ast.ListItem
import com.hrm.markdown.parser.ast.MathBlock
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.Paragraph
import com.hrm.markdown.parser.ast.RubyText
import com.hrm.markdown.parser.ast.SetextHeading
import com.hrm.markdown.parser.ast.SoftLineBreak
import com.hrm.markdown.parser.ast.Table
import com.hrm.markdown.parser.ast.TableBody
import com.hrm.markdown.parser.ast.TableCell
import com.hrm.markdown.parser.ast.TableHead
import com.hrm.markdown.parser.ast.TableRow
import com.hrm.markdown.parser.ast.TabBlock
import com.hrm.markdown.parser.ast.TabItem
import com.hrm.markdown.parser.ast.Text
import com.hrm.markdown.parser.ast.TocPlaceholder
import com.hrm.markdown.renderer.internal.core.compile.blockStableId

internal interface SelectionTextEntry {
    val stableId: Long
    val order: Int
    val totalChars: Int
    val text: String
}

internal class SelectionDocumentEntry(
    override val stableId: Long,
    override val order: Int,
    textProvider: () -> String,
) : SelectionTextEntry {
    constructor(stableId: Long, order: Int, text: String) : this(stableId, order, { text })

    override val text: String by lazy(LazyThreadSafetyMode.NONE, textProvider)

    override val totalChars: Int
        get() = text.length
}

internal class SelectionDocumentIndex(
    val entries: List<SelectionDocumentEntry>,
) {
    private val orderByStableId = entries.associate { it.stableId to it.order }
    private val entryByStableId = entries.associateBy { it.stableId }

    val isEmpty: Boolean
        get() = entries.isEmpty()

    fun entryOf(stableId: Long): SelectionDocumentEntry? = entryByStableId[stableId]

    fun orderOf(stableId: Long): Int? = orderByStableId[stableId]

    fun clampAnchor(anchor: SelectionAnchor): SelectionAnchor? {
        val entry = entryByStableId[anchor.blockStableId] ?: return null
        return anchor.copy(charInBlock = anchor.charInBlock.coerceIn(0, entry.totalChars))
    }

    fun compare(a: SelectionAnchor, b: SelectionAnchor): Int {
        val firstOrder = orderByStableId[a.blockStableId] ?: return -1
        val secondOrder = orderByStableId[b.blockStableId] ?: return 1
        return if (firstOrder == secondOrder) {
            a.charInBlock.compareTo(b.charInBlock)
        } else {
            firstOrder.compareTo(secondOrder)
        }
    }

    fun normalize(a: SelectionAnchor, b: SelectionAnchor): SelectionRange =
        if (compare(a, b) <= 0) SelectionRange(a, b) else SelectionRange(b, a)

    val firstAnchor: SelectionAnchor?
        get() = entries.firstOrNull()?.let { SelectionAnchor(it.stableId, 0) }

    val lastAnchor: SelectionAnchor?
        get() = entries.lastOrNull()?.let { SelectionAnchor(it.stableId, it.totalChars) }
}

internal fun buildSelectionDocumentIndex(document: Document): SelectionDocumentIndex {
    val entries = ArrayList<SelectionDocumentEntry>()

    fun add(node: Node, text: () -> String) {
        entries += SelectionDocumentEntry(
            stableId = blockStableId(node),
            order = entries.size,
            textProvider = text,
        )
    }

    fun visit(node: Node) {
        when (node) {
            is Paragraph,
            is Heading,
            is SetextHeading,
            is DefinitionTerm -> add(node) { selectionPlainText(node) }

            is MathBlock -> add(node) { node.literal }
            is FencedCodeBlock -> add(node) { node.literal }
            is IndentedCodeBlock -> add(node) { node.literal }
            is DiagramBlock -> add(node) { node.literal }
            is HtmlBlock -> add(node) { node.literal }
            is Table -> add(node) { tablePlainText(node) }

            is Figure,
            is TocPlaceholder,
            is BibliographyDefinition -> Unit

            is BlockQuote,
            is ListBlock,
            is ListItem,
            is Admonition,
            is CustomContainer,
            is ColumnsLayout,
            is ColumnItem,
            is DefinitionList,
            is DefinitionDescription,
            is FootnoteDefinition,
            is DirectiveBlock,
            is TabBlock,
            is TabItem -> node.children.forEach(::visit)
            else -> Unit
        }
    }

    document.children.forEach(::visit)
    return SelectionDocumentIndex(entries)
}

private fun tablePlainText(table: Table): String {
    val rows = buildList {
        table.children.filterIsInstance<TableHead>().forEach { head ->
            addAll(head.children.filterIsInstance<TableRow>())
        }
        table.children.filterIsInstance<TableBody>().forEach { body ->
            addAll(body.children.filterIsInstance<TableRow>())
        }
    }
    return rows.joinToString("\n") { row ->
        row.children.filterIsInstance<TableCell>().joinToString("\t") { selectionPlainText(it) }
    }
}

private fun selectionPlainText(node: Node): String = when (node) {
    is Text -> node.literal
    is SoftLineBreak -> " "
    is HardLineBreak -> "\n"
    is InlineCode -> node.literal
    is InlineMath -> node.literal
    is Autolink -> node.destination
    is FootnoteReference -> "[${node.index}]"
    is CitationReference -> "[${node.key}]"
    is HtmlEntity -> node.resolved.ifEmpty { node.literal }
    is EscapedChar -> node.literal
    is Emoji -> node.unicode ?: node.literal.ifEmpty { ":${node.shortcode}:" }
    is KeyboardInput -> node.literal
    is RubyText -> node.base
    is Image -> node.children.joinToString("") { selectionPlainText(it) }
    is ContainerNode -> node.children.joinToString("") { selectionPlainText(it) }
    else -> ""
}
