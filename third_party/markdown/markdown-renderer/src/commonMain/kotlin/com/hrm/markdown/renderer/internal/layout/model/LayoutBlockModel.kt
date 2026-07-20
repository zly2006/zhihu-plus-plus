package com.hrm.markdown.renderer.internal.layout.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import com.hrm.markdown.renderer.inline.InlinePlaceholderId
import com.hrm.markdown.renderer.inline.InlineWidgetPaintPayload
import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity
import com.hrm.markdown.renderer.internal.core.model.BibliographyDefinitionBlockModel
import com.hrm.markdown.renderer.internal.core.model.BibliographyEntryBlockModel
import com.hrm.markdown.renderer.internal.core.model.BlockWidgetModel
import com.hrm.markdown.renderer.internal.core.model.ColumnsLayoutBlockModel
import com.hrm.markdown.renderer.internal.core.model.DefinitionDescriptionBlockModel
import com.hrm.markdown.renderer.internal.core.model.DefinitionListBlockModel
import com.hrm.markdown.renderer.internal.core.model.DefinitionTermBlockModel
import com.hrm.markdown.renderer.internal.core.model.FigureBlockModel
import com.hrm.markdown.renderer.internal.core.model.FootnoteDefinitionBlockModel
import com.hrm.markdown.renderer.internal.core.model.InlineWidgetModel
import com.hrm.markdown.renderer.internal.core.model.InternalRenderBlockModel
import com.hrm.markdown.renderer.internal.core.model.ListBlockModel
import com.hrm.markdown.renderer.internal.core.model.TabBlockModel
import com.hrm.markdown.renderer.internal.core.model.TableBlockModel
import com.hrm.markdown.renderer.internal.core.model.TableCellBlockModel
import com.hrm.markdown.renderer.internal.core.model.TocBlockModel
import com.hrm.markdown.renderer.internal.core.model.TocEntryBlockModel

sealed interface InternalLayoutBlockModel {
    val identity: RenderIdentity
    val frame: LayoutRect
    val contentFrame: LayoutRect
}

data class LayoutRenderBlockModel(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val block: InternalRenderBlockModel,
    val children: List<InternalLayoutBlockModel> = emptyList(),
) : InternalLayoutBlockModel

data class LayoutWidgetBlockModel(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val block: InternalRenderBlockModel,
    val widget: BlockWidgetModel,
    val measurement: BlockWidgetMeasurement,
) : InternalLayoutBlockModel

data class LayoutListBlockModel(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val block: ListBlockModel,
    val items: List<LayoutListItemGroup>,
) : InternalLayoutBlockModel

data class LayoutListItemGroup(
    val identity: RenderIdentity,
    val frame: LayoutRect,
    val contentFrame: LayoutRect,
    val markerText: String,
    val taskListItem: Boolean,
    val checked: Boolean,
    val children: List<InternalLayoutBlockModel>,
)

data class LayoutColumnsBlockModel(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val block: ColumnsLayoutBlockModel,
    val columns: List<LayoutColumnGroup>,
) : InternalLayoutBlockModel

data class LayoutColumnGroup(
    val identity: RenderIdentity,
    val frame: LayoutRect,
    val contentFrame: LayoutRect,
    val width: String,
    val children: List<InternalLayoutBlockModel>,
)

data class LayoutTabBlockModel(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val block: TabBlockModel,
    val tabs: List<LayoutTabGroup>,
) : InternalLayoutBlockModel

data class LayoutTabGroup(
    val identity: RenderIdentity,
    val frame: LayoutRect,
    val contentFrame: LayoutRect,
    val title: String,
    val children: List<InternalLayoutBlockModel>,
)

data class LayoutFootnoteBlockModel(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val block: FootnoteDefinitionBlockModel,
    val leadChild: InternalLayoutBlockModel?,
    val trailingChildren: List<InternalLayoutBlockModel>,
) : InternalLayoutBlockModel

data class LayoutTableBlockModel(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val block: TableBlockModel,
    val columnWidths: List<Float>,
    val rows: List<LayoutTableRowGroup>,
) : InternalLayoutBlockModel

data class LayoutTableRowGroup(
    val identity: RenderIdentity,
    val frame: LayoutRect,
    val contentFrame: LayoutRect,
    val isHeader: Boolean,
    val cells: List<LayoutTableCellGroup>,
)

data class LayoutTableCellGroup(
    val identity: RenderIdentity,
    val frame: LayoutRect,
    val contentFrame: LayoutRect,
    val cell: TableCellBlockModel?,
    val alignmentOrdinal: Int,
    val isHeader: Boolean,
)

data class LayoutDefinitionListBlockModel(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val block: DefinitionListBlockModel,
    val items: List<LayoutDefinitionListItemGroup>,
) : InternalLayoutBlockModel

sealed interface LayoutDefinitionListItemGroup {
    val identity: RenderIdentity
    val frame: LayoutRect
    val contentFrame: LayoutRect
}

data class LayoutDefinitionTermGroup(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val item: DefinitionTermBlockModel,
) : LayoutDefinitionListItemGroup

data class LayoutDefinitionDescriptionGroup(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val item: DefinitionDescriptionBlockModel,
    val children: List<InternalLayoutBlockModel>,
) : LayoutDefinitionListItemGroup

data class LayoutFigureBlockModel(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val block: FigureBlockModel,
    val imageFrame: LayoutRect,
    val captionFrame: LayoutRect?,
) : InternalLayoutBlockModel

data class LayoutTocBlockModel(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val block: TocBlockModel,
    val entries: List<LayoutTocEntryGroup>,
) : InternalLayoutBlockModel

data class LayoutTocEntryGroup(
    val identity: RenderIdentity,
    val frame: LayoutRect,
    val contentFrame: LayoutRect,
    val entry: TocEntryBlockModel,
)

data class LayoutBibliographyBlockModel(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val block: BibliographyDefinitionBlockModel,
    val entries: List<LayoutBibliographyEntryGroup>,
) : InternalLayoutBlockModel

data class LayoutBibliographyEntryGroup(
    val identity: RenderIdentity,
    val frame: LayoutRect,
    val contentFrame: LayoutRect,
    val entry: BibliographyEntryBlockModel,
)

data class BlockWidgetMeasurement(
    val widthPx: Float,
    val heightPx: Float,
    val baselinePx: Float? = null,
    val scrollableHorizontally: Boolean = false,
)

internal data class LayoutInlineBlockModel(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    override val contentFrame: LayoutRect,
    val style: TextStyle,
    val inlinePayloads: Map<InlinePlaceholderId, InlineWidgetPaintPayload>,
    val showDivider: Boolean = false,
    val lines: List<LayoutInlineLine>,
) : InternalLayoutBlockModel

internal data class LayoutInlineLine(
    val frame: LayoutRect,
    val baseline: Float,
    val runs: List<LayoutInlineRun>,
)

internal sealed interface LayoutInlineRun {
    val identity: RenderIdentity
    val frame: LayoutRect
}

internal data class LayoutTextRun(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    val text: AnnotatedString,
) : LayoutInlineRun

internal data class LayoutWidgetRun(
    override val identity: RenderIdentity,
    override val frame: LayoutRect,
    val id: InlinePlaceholderId,
    val widget: InlineWidgetModel,
    val alternateText: String = "",
) : LayoutInlineRun
