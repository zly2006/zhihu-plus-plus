package com.hrm.markdown.renderer.internal.core.model

import androidx.compose.runtime.Composable
import com.hrm.markdown.parser.ast.Table
import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity

sealed interface InternalRenderBlockModel {
    val identity: RenderIdentity
}

data class ParagraphBlockModel(
    override val identity: RenderIdentity,
    val inline: InlineModel,
) : InternalRenderBlockModel

data class HeadingBlockModel(
    override val identity: RenderIdentity,
    val level: Int,
    val numbering: String?,
    val inline: InlineModel,
) : InternalRenderBlockModel

data class CodeBlockModel(
    override val identity: RenderIdentity,
    val language: String,
    val title: String?,
    val code: String,
    val showLineNumbers: Boolean,
    val startLine: Int,
    val highlightedLines: Set<Int>,
    val widget: BlockWidgetModel,
) : InternalRenderBlockModel

data class MathBlockModel(
    override val identity: RenderIdentity,
    val latex: String,
    val widget: BlockWidgetModel,
) : InternalRenderBlockModel

data class BlockQuoteBlockModel(
    override val identity: RenderIdentity,
    val children: List<InternalRenderBlockModel>,
) : InternalRenderBlockModel

data class ListBlockModel(
    override val identity: RenderIdentity,
    val ordered: Boolean,
    val startNumber: Int,
    val bulletChar: Char,
    val delimiter: Char,
    val tight: Boolean,
    val items: List<ListItemBlockModel>,
) : InternalRenderBlockModel

data class ListItemBlockModel(
    val identity: RenderIdentity,
    val taskListItem: Boolean,
    val checked: Boolean,
    val children: List<InternalRenderBlockModel>,
)

data class TableBlockModel(
    override val identity: RenderIdentity,
    val columnAlignments: List<Table.Alignment>,
    val rows: List<TableRowBlockModel>,
) : InternalRenderBlockModel

data class TableRowBlockModel(
    val identity: RenderIdentity,
    val isHeader: Boolean,
    val cells: List<TableCellBlockModel>,
)

data class TableCellBlockModel(
    val identity: RenderIdentity,
    val alignment: Table.Alignment,
    val isHeader: Boolean,
    val inline: InlineModel,
)

data class AdmonitionBlockModel(
    override val identity: RenderIdentity,
    val type: String,
    val title: String,
    val children: List<InternalRenderBlockModel>,
) : InternalRenderBlockModel

data class HtmlBlockModel(
    override val identity: RenderIdentity,
    val html: String,
) : InternalRenderBlockModel

data class NativeBlockModel(
    override val identity: RenderIdentity,
    val content: @Composable () -> Unit,
) : InternalRenderBlockModel

data class CustomContainerBlockModel(
    override val identity: RenderIdentity,
    val type: String,
    val title: String,
    val cssClasses: List<String>,
    val cssId: String?,
    val children: List<InternalRenderBlockModel>,
) : InternalRenderBlockModel

data class ColumnsLayoutBlockModel(
    override val identity: RenderIdentity,
    val columns: List<ColumnBlockModel>,
) : InternalRenderBlockModel

data class ColumnBlockModel(
    val identity: RenderIdentity,
    val width: String,
    val children: List<InternalRenderBlockModel>,
)

data class DefinitionListBlockModel(
    override val identity: RenderIdentity,
    val items: List<DefinitionListItemModel>,
) : InternalRenderBlockModel

sealed interface DefinitionListItemModel {
    val identity: RenderIdentity
}

data class DefinitionTermBlockModel(
    override val identity: RenderIdentity,
    val inline: InlineModel,
) : DefinitionListItemModel

data class DefinitionDescriptionBlockModel(
    override val identity: RenderIdentity,
    val children: List<InternalRenderBlockModel>,
) : DefinitionListItemModel

data class FootnoteDefinitionBlockModel(
    override val identity: RenderIdentity,
    val label: String,
    val index: Int,
    val children: List<InternalRenderBlockModel>,
) : InternalRenderBlockModel

data class TocBlockModel(
    override val identity: RenderIdentity,
    val entries: List<TocEntryBlockModel>,
) : InternalRenderBlockModel

data class TocEntryBlockModel(
    val text: String,
    val level: Int,
    val id: String?,
)

data class PageBreakBlockModel(
    override val identity: RenderIdentity,
) : InternalRenderBlockModel

data class DirectiveBlockModel(
    override val identity: RenderIdentity,
    val tagName: String,
    val args: Map<String, String>,
    val children: List<InternalRenderBlockModel>,
) : InternalRenderBlockModel

data class TabBlockModel(
    override val identity: RenderIdentity,
    val items: List<TabItemBlockModel>,
) : InternalRenderBlockModel

data class TabItemBlockModel(
    val identity: RenderIdentity,
    val title: String,
    val children: List<InternalRenderBlockModel>,
)

data class BibliographyDefinitionBlockModel(
    override val identity: RenderIdentity,
    val entries: List<BibliographyEntryBlockModel>,
) : InternalRenderBlockModel

data class BibliographyEntryBlockModel(
    val key: String,
    val content: String,
)

data class FigureBlockModel(
    override val identity: RenderIdentity,
    val imageUrl: String,
    val caption: String,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val attributes: Map<String, String>,
) : InternalRenderBlockModel

data class DiagramBlockModel(
    override val identity: RenderIdentity,
    val diagramType: String,
    val code: String,
    val widget: BlockWidgetModel,
) : InternalRenderBlockModel

data class ThematicBreakBlockModel(
    override val identity: RenderIdentity,
) : InternalRenderBlockModel

data class FallbackContainerBlockModel(
    override val identity: RenderIdentity,
    val children: List<InternalRenderBlockModel>,
) : InternalRenderBlockModel

data class FallbackLeafBlockModel(
    override val identity: RenderIdentity,
) : InternalRenderBlockModel
