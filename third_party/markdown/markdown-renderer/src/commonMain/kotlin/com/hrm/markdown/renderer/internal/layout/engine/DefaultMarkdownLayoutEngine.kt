package com.hrm.markdown.renderer.internal.layout.engine

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity
import com.hrm.markdown.renderer.internal.core.identity.renderIdentityFromText
import com.hrm.markdown.renderer.internal.core.identity.renderIdentityFromValues
import com.hrm.markdown.renderer.internal.core.model.AdmonitionBlockModel
import com.hrm.markdown.renderer.internal.core.model.BibliographyDefinitionBlockModel
import com.hrm.markdown.renderer.internal.core.model.BlockQuoteBlockModel
import com.hrm.markdown.renderer.internal.core.model.CodeBlockModel
import com.hrm.markdown.renderer.internal.core.model.ColumnsLayoutBlockModel
import com.hrm.markdown.renderer.internal.core.model.CustomContainerBlockModel
import com.hrm.markdown.renderer.internal.core.model.DefinitionListBlockModel
import com.hrm.markdown.renderer.internal.core.model.DiagramBlockModel
import com.hrm.markdown.renderer.internal.core.model.DirectiveBlockModel
import com.hrm.markdown.renderer.internal.core.model.FallbackContainerBlockModel
import com.hrm.markdown.renderer.internal.core.model.FigureBlockModel
import com.hrm.markdown.renderer.internal.core.model.FootnoteDefinitionBlockModel
import com.hrm.markdown.renderer.internal.core.model.HeadingBlockModel
import com.hrm.markdown.renderer.internal.core.model.HtmlBlockModel
import com.hrm.markdown.renderer.internal.core.model.InlineModel
import com.hrm.markdown.renderer.internal.core.model.InternalRenderBlockModel
import com.hrm.markdown.renderer.internal.core.model.InternalRenderDocumentModel
import com.hrm.markdown.renderer.internal.core.model.ListBlockModel
import com.hrm.markdown.renderer.internal.core.model.MathBlockModel
import com.hrm.markdown.renderer.internal.core.model.PageBreakBlockModel
import com.hrm.markdown.renderer.internal.core.model.ParagraphBlockModel
import com.hrm.markdown.renderer.internal.core.model.TabBlockModel
import com.hrm.markdown.renderer.internal.core.model.TableBlockModel
import com.hrm.markdown.renderer.internal.core.model.TextAtom
import com.hrm.markdown.renderer.internal.core.model.ThematicBreakBlockModel
import com.hrm.markdown.renderer.internal.core.model.TocBlockModel
import com.hrm.markdown.renderer.internal.layout.inline.buildInlineLayoutBlockFromModel
import com.hrm.markdown.renderer.internal.layout.list.listItemContentIndentPx
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutBlockModel
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutDocumentMetadata
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutDocumentModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutBibliographyBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutBibliographyEntryGroup
import com.hrm.markdown.renderer.internal.layout.model.LayoutColumnGroup
import com.hrm.markdown.renderer.internal.layout.model.LayoutColumnsBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutDefinitionDescriptionGroup
import com.hrm.markdown.renderer.internal.layout.model.LayoutDefinitionListBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutDefinitionTermGroup
import com.hrm.markdown.renderer.internal.layout.model.LayoutFigureBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutFootnoteBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutInlineBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutInsets
import com.hrm.markdown.renderer.internal.layout.model.LayoutListBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutListItemGroup
import com.hrm.markdown.renderer.internal.layout.model.LayoutRect
import com.hrm.markdown.renderer.internal.layout.model.LayoutRenderBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutSize
import com.hrm.markdown.renderer.internal.layout.model.LayoutTabBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutTabGroup
import com.hrm.markdown.renderer.internal.layout.model.LayoutTableBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutTableCellGroup
import com.hrm.markdown.renderer.internal.layout.model.LayoutTableRowGroup
import com.hrm.markdown.renderer.internal.layout.model.LayoutTocBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutTocEntryGroup
import com.hrm.markdown.renderer.internal.layout.model.LayoutWidgetBlockModel
import com.hrm.markdown.renderer.internal.layout.widget.measureBlockWidget
import kotlin.math.max

internal object DefaultMarkdownLayoutEngine : MarkdownLayoutEngine {
    override fun layout(
        document: InternalRenderDocumentModel,
        environment: LayoutEnvironment,
    ): InternalLayoutDocumentModel {
        val (blocks, endY) = layoutBlocks(
            blocks = document.blocks,
            left = 0f,
            top = 0f,
            width = environment.viewportWidth,
            environment = environment,
        )
        return InternalLayoutDocumentModel(
            identity = document.identity,
            blocks = blocks,
            totalSize = LayoutSize(
                width = environment.viewportWidth,
                height = endY.coerceAtLeast(0f),
            ),
            metadata = InternalLayoutDocumentMetadata(
                footnoteDefinitionItemIndexes = buildMap {
                    blocks.forEachIndexed { index, block ->
                        val label =
                            (extractRenderBlock(block) as? FootnoteDefinitionBlockModel)?.label
                                ?: return@forEachIndexed
                        put(label, index)
                    }
                },
            ),
        )
    }

    internal fun layoutBlock(
        block: InternalRenderBlockModel,
        environment: LayoutEnvironment,
    ): InternalLayoutBlockModel = layoutBlock(
        block = block,
        left = 0f,
        top = 0f,
        width = environment.viewportWidth,
        environment = environment,
    )
}

private fun layoutBlocks(
    blocks: List<InternalRenderBlockModel>,
    left: Float,
    top: Float,
    width: Float,
    environment: LayoutEnvironment,
): Pair<List<InternalLayoutBlockModel>, Float> {
    val result = ArrayList<InternalLayoutBlockModel>(blocks.size)
    var cursorY = top
    blocks.forEachIndexed { index, block ->
        val layoutBlock = layoutBlock(
            block = block,
            left = left,
            top = cursorY,
            width = width,
            environment = environment,
        )
        result += layoutBlock
        cursorY = layoutBlock.frame.top + layoutBlock.frame.height
        if (index != blocks.lastIndex) {
            cursorY += environment.blockSpacing
        }
    }
    return result to cursorY
}

private fun layoutBlock(
    block: InternalRenderBlockModel,
    left: Float,
    top: Float,
    width: Float,
    environment: LayoutEnvironment,
): InternalLayoutBlockModel {
    val insets = blockInsets(block)
    val contentLeft = left + insets.left
    val contentTop = top + insets.top
    val contentWidth = (width - insets.left - insets.right).coerceAtLeast(0f)
    return when (block) {
        is CodeBlockModel -> layoutWidgetBlock(
            block,
            block.widget,
            left,
            top,
            width,
            insets,
            environment
        )

        is MathBlockModel -> layoutWidgetBlock(
            block,
            block.widget,
            left,
            top,
            width,
            insets,
            environment
        )

        is DiagramBlockModel -> layoutWidgetBlock(
            block,
            block.widget,
            left,
            top,
            width,
            insets,
            environment
        )

        is BlockQuoteBlockModel -> layoutContainerBlock(
            block,
            block.children,
            left,
            top,
            width,
            insets,
            environment
        )

        is AdmonitionBlockModel -> layoutContainerBlock(
            block,
            block.children,
            left,
            top,
            width,
            insets,
            environment,
            headerHeight = 28f
        )

        is CustomContainerBlockModel -> layoutContainerBlock(
            block,
            block.children,
            left,
            top,
            width,
            insets,
            environment,
            headerHeight = if (block.title.isNotBlank() || block.type.isNotBlank()) 28f else 0f,
        )

        is FootnoteDefinitionBlockModel -> layoutFootnoteBlock(
            block,
            left,
            top,
            width,
            insets,
            environment
        )

        is DirectiveBlockModel -> layoutContainerBlock(
            block,
            block.children,
            left,
            top,
            width,
            insets,
            environment,
            headerHeight = 24f
        )

        is FallbackContainerBlockModel -> layoutContainerBlock(
            block,
            block.children,
            left,
            top,
            width,
            insets,
            environment
        )

        is ColumnsLayoutBlockModel -> {
            val columnCount = block.columns.size.coerceAtLeast(1)
            val spacing = 8f
            val columnWidth =
                ((contentWidth - spacing * (columnCount - 1)) / columnCount).coerceAtLeast(0f)
            var maxColumnBottom = contentTop
            val columnGroups = block.columns.mapIndexed { index, column ->
                val columnLeft = contentLeft + index * (columnWidth + spacing)
                val (columnChildren, columnBottom) = layoutBlocks(
                    column.children,
                    columnLeft,
                    contentTop,
                    columnWidth,
                    environment
                )
                maxColumnBottom = max(maxColumnBottom, columnBottom)
                LayoutColumnGroup(
                    identity = column.identity,
                    frame = LayoutRect(
                        left = columnLeft,
                        top = contentTop,
                        width = columnWidth,
                        height = (columnBottom - contentTop).coerceAtLeast(0f),
                    ),
                    contentFrame = LayoutRect(
                        left = columnLeft,
                        top = contentTop,
                        width = columnWidth,
                        height = (columnBottom - contentTop).coerceAtLeast(0f),
                    ),
                    width = column.width,
                    children = columnChildren,
                )
            }
            val contentHeight = (maxColumnBottom - contentTop).coerceAtLeast(0f)
            LayoutColumnsBlockModel(
                identity = block.identity,
                frame = LayoutRect(left, top, width, insets.top + contentHeight + insets.bottom),
                contentFrame = LayoutRect(contentLeft, contentTop, contentWidth, contentHeight),
                block = block,
                columns = columnGroups,
            )
        }

        is ListBlockModel -> {
            var itemCursorY = contentTop
            val itemGroups = block.items.mapIndexed { index, item ->
                val itemIndent = environment.density.listItemContentIndentPx(
                    theme = environment.markdownTheme,
                    taskListItem = item.taskListItem,
                    ordered = block.ordered,
                )
                val itemTop = itemCursorY
                val (itemChildren, itemBottom) = layoutBlocks(
                    item.children,
                    contentLeft + itemIndent,
                    itemCursorY,
                    (contentWidth - itemIndent).coerceAtLeast(0f),
                    environment,
                )
                itemCursorY = itemBottom
                if (index != block.items.lastIndex) {
                    itemCursorY += if (block.tight) 4f else environment.blockSpacing
                }
                LayoutListItemGroup(
                    identity = item.identity,
                    frame = LayoutRect(
                        left = contentLeft,
                        top = itemTop,
                        width = contentWidth,
                        height = (itemBottom - itemTop).coerceAtLeast(0f),
                    ),
                    contentFrame = LayoutRect(
                        left = contentLeft + itemIndent,
                        top = itemTop,
                        width = (contentWidth - itemIndent).coerceAtLeast(0f),
                        height = (itemBottom - itemTop).coerceAtLeast(0f),
                    ),
                    markerText = when {
                        item.taskListItem -> ""
                        block.ordered -> "${block.startNumber + index}."
                        else -> "•"
                    },
                    taskListItem = item.taskListItem,
                    checked = item.checked,
                    children = itemChildren,
                )
            }
            LayoutListBlockModel(
                identity = block.identity,
                frame = LayoutRect(
                    left,
                    top,
                    width,
                    insets.top + (itemCursorY - contentTop).coerceAtLeast(0f) + insets.bottom
                ),
                contentFrame = LayoutRect(
                    contentLeft,
                    contentTop,
                    contentWidth,
                    (itemCursorY - contentTop).coerceAtLeast(0f)
                ),
                block = block,
                items = itemGroups,
            )
        }

        is TabBlockModel -> {
            val tabHeaderHeight = 36f
            val tabGroups = block.items.map { item ->
                val (tabChildren, bottom) = layoutBlocks(
                    item.children,
                    contentLeft,
                    contentTop + tabHeaderHeight,
                    contentWidth,
                    environment,
                )
                LayoutTabGroup(
                    identity = item.identity,
                    frame = LayoutRect(
                        left = contentLeft,
                        top = contentTop + tabHeaderHeight,
                        width = contentWidth,
                        height = (bottom - (contentTop + tabHeaderHeight)).coerceAtLeast(0f),
                    ),
                    contentFrame = LayoutRect(
                        left = contentLeft,
                        top = contentTop + tabHeaderHeight,
                        width = contentWidth,
                        height = (bottom - (contentTop + tabHeaderHeight)).coerceAtLeast(0f),
                    ),
                    title = item.title,
                    children = tabChildren,
                )
            }
            val contentHeight = (tabGroups.maxOfOrNull { it.frame.height } ?: 0f) + tabHeaderHeight
            LayoutTabBlockModel(
                identity = block.identity,
                frame = LayoutRect(left, top, width, insets.top + contentHeight + insets.bottom),
                contentFrame = LayoutRect(contentLeft, contentTop, contentWidth, contentHeight),
                block = block,
                tabs = tabGroups,
            )
        }

        is TableBlockModel -> layoutTableBlock(block, left, top, width, insets, environment)
        is DefinitionListBlockModel -> layoutDefinitionListBlock(
            block,
            left,
            top,
            width,
            insets,
            environment
        )

        is FigureBlockModel -> layoutFigureBlock(block, left, top, width, insets, environment)
        is TocBlockModel -> layoutTocBlock(block, left, top, width, insets, environment)
        is BibliographyDefinitionBlockModel -> layoutBibliographyBlock(
            block,
            left,
            top,
            width,
            insets,
            environment
        )

        is ParagraphBlockModel -> layoutInlineBlock(
            identity = block.identity,
            model = block.inline,
            style = environment.markdownTheme.bodyStyle,
            left = left,
            top = top,
            width = width,
            insets = insets,
            environment = environment,
        )

        is HeadingBlockModel -> {
            val style = environment.markdownTheme.headingStyles[(block.level - 1).coerceIn(
                0,
                environment.markdownTheme.headingStyles.lastIndex
            )]
            layoutInlineBlock(
                identity = block.identity,
                model = block.inline.prependHeadingNumbering(block.numbering),
                style = style,
                left = left,
                top = top,
                width = width,
                insets = insets,
                environment = environment,
                showDivider = block.level <= 2,
            )
        }

        else -> {
            val contentHeight = environment.measureLeafBlockContentHeight(block, contentWidth)
            layoutRenderBlock(block, left, top, width, insets, contentHeight, emptyList())
        }
    }
}

private fun layoutContainerBlock(
    block: InternalRenderBlockModel,
    children: List<InternalRenderBlockModel>,
    left: Float,
    top: Float,
    width: Float,
    insets: LayoutInsets,
    environment: LayoutEnvironment,
    headerHeight: Float = 0f,
): LayoutRenderBlockModel {
    val contentLeft = left + insets.left
    val contentTop = top + insets.top + headerHeight
    val contentWidth = (width - insets.left - insets.right).coerceAtLeast(0f)
    val (childLayouts, bottom) = layoutBlocks(
        children,
        contentLeft,
        contentTop,
        contentWidth,
        environment
    )
    val contentHeight = (bottom - contentTop).coerceAtLeast(0f) + headerHeight
    return layoutRenderBlock(block, left, top, width, insets, contentHeight, childLayouts)
}

private fun layoutFootnoteBlock(
    block: FootnoteDefinitionBlockModel,
    left: Float,
    top: Float,
    width: Float,
    insets: LayoutInsets,
    environment: LayoutEnvironment,
): LayoutFootnoteBlockModel {
    val contentLeft = left + insets.left
    val contentTop = top + insets.top
    val contentWidth = (width - insets.left - insets.right).coerceAtLeast(0f)
    val horizontalSpacing = with(environment.density) { 8.dp.toPx() }
    val labelStyle = environment.markdownTheme.bodyStyle.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = environment.markdownTheme.footnoteStyle.fontSize,
    )
    val arrowStyle = environment.markdownTheme.bodyStyle.copy(
        fontSize = environment.markdownTheme.footnoteStyle.fontSize,
    )
    val labelWidth = environment.textMeasurer.measure(
        text = "[${block.index}]",
        style = labelStyle,
        constraints = Constraints(maxWidth = Int.MAX_VALUE),
        maxLines = 1,
        softWrap = false,
    ).size.width.toFloat()
    val arrowWidth = environment.textMeasurer.measure(
        text = "↩",
        style = arrowStyle,
        constraints = Constraints(maxWidth = Int.MAX_VALUE),
        maxLines = 1,
        softWrap = false,
    ).size.width.toFloat()
    val leadLeft = contentLeft + labelWidth + arrowWidth + horizontalSpacing * 2f
    val leadWidth = (contentWidth - (leadLeft - contentLeft)).coerceAtLeast(0f)
    val leadChild = block.children.firstOrNull()?.let { child ->
        layoutBlock(
            block = child,
            left = leadLeft,
            top = contentTop,
            width = leadWidth,
            environment = environment,
        )
    }
    val leadBottom = leadChild?.let { it.frame.top + it.frame.height } ?: contentTop
    val (trailingChildren, bottom) = layoutBlocks(
        blocks = block.children.drop(1),
        left = contentLeft,
        top = leadBottom,
        width = contentWidth,
        environment = environment,
    )
    val contentHeight = (bottom - contentTop).coerceAtLeast(0f)
    return LayoutFootnoteBlockModel(
        identity = block.identity,
        frame = LayoutRect(
            left = left,
            top = top,
            width = width,
            height = insets.top + contentHeight + insets.bottom,
        ),
        contentFrame = LayoutRect(
            left = contentLeft,
            top = contentTop,
            width = contentWidth,
            height = contentHeight,
        ),
        block = block,
        leadChild = leadChild,
        trailingChildren = trailingChildren,
    )
}

private fun layoutInlineBlock(
    identity: RenderIdentity,
    model: InlineModel,
    style: TextStyle,
    left: Float,
    top: Float,
    width: Float,
    insets: LayoutInsets,
    environment: LayoutEnvironment,
    showDivider: Boolean = false,
): LayoutInlineBlockModel {
    return buildInlineLayoutBlockFromModel(
        identity = identity,
        model = model,
        style = style,
        left = left,
        top = top,
        width = width,
        insets = insets,
        theme = environment.markdownTheme,
        directiveRegistry = environment.compileEnvironment.directiveRegistry,
        latexMeasurer = environment.latexMeasurer,
        density = environment.density,
        textMeasurer = environment.textMeasurer,
        inlineLayoutRuntime = environment.inlineLayoutRuntime,
        inlineLayoutEpoch = environment.inlineLayoutEpoch,
        codeTheme = environment.codeTheme,
        onLinkClick = environment.onLinkClick,
        onFootnoteClick = environment.onFootnoteClick,
        showDivider = showDivider,
    )
}

private fun InlineModel.prependHeadingNumbering(numbering: String?): InlineModel {
    if (numbering.isNullOrBlank()) return this
    val prefix = "$numbering "
    val prefixStableId = renderIdentityFromText(prefix, identity.stableId)
    val prefixIdentity = RenderIdentity(
        stableId = prefixStableId,
        contentRevision = renderIdentityFromValues(identity.contentRevision, prefixStableId),
        layoutRevision = renderIdentityFromValues(identity.layoutRevision, prefixStableId),
        paintRevision = identity.paintRevision,
    )
    return copy(
        identity = RenderIdentity(
            stableId = identity.stableId,
            contentRevision = renderIdentityFromValues(
                identity.contentRevision,
                prefixIdentity.contentRevision
            ),
            layoutRevision = renderIdentityFromValues(
                identity.layoutRevision,
                prefixIdentity.layoutRevision
            ),
            paintRevision = identity.paintRevision,
        ),
        atoms = listOf(TextAtom(identity = prefixIdentity, text = prefix)) + atoms,
    )
}

private fun layoutTableBlock(
    block: TableBlockModel,
    left: Float,
    top: Float,
    width: Float,
    insets: LayoutInsets,
    environment: LayoutEnvironment,
): LayoutTableBlockModel {
    val contentLeft = left + insets.left
    val contentTop = top + insets.top
    val contentWidth = (width - insets.left - insets.right).coerceAtLeast(0f)
    val columnCount = tableColumnCount(block)
    val cellPadding =
        with(environment.density) { environment.markdownTheme.tableCellPadding.toPx() }
    val columnWidths = environment.computeTableColumnWidths(block, contentWidth)
    var cursorY = contentTop
    val rows = block.rows.map { row ->
        var cursorX = contentLeft
        val cells = (0 until columnCount).map { colIndex ->
            val cell = row.cells.getOrNull(colIndex)
            val columnWidth = columnWidths.getOrElse(colIndex) { 0f }
            val cellLeft = cursorX
            cursorX += columnWidth
            val alignment =
                block.columnAlignments.getOrElse(colIndex) { com.hrm.markdown.parser.ast.Table.Alignment.NONE }
            val style = environment.tableCellTextStyle(alignment, row.isHeader)
            val innerHeight = cell?.let {
                environment.measureInlineBlock(
                    model = it.inline,
                    style = style,
                    widthPx = (columnWidth - cellPadding * 2f).coerceAtLeast(16f),
                )
            } ?: environment.lineHeightPx(environment.markdownTheme.bodyStyle)
            LayoutTableCellGroup(
                identity = cell?.identity ?: row.identity,
                frame = LayoutRect(
                    left = cellLeft,
                    top = cursorY,
                    width = columnWidth,
                    height = innerHeight + cellPadding * 2f,
                ),
                contentFrame = LayoutRect(
                    left = cellLeft + cellPadding,
                    top = cursorY + cellPadding,
                    width = (columnWidth - cellPadding * 2f).coerceAtLeast(0f),
                    height = innerHeight,
                ),
                cell = cell,
                alignmentOrdinal = block.columnAlignments.getOrElse(colIndex) { com.hrm.markdown.parser.ast.Table.Alignment.NONE }.ordinal,
                isHeader = row.isHeader,
            )
        }
        val rowHeight = cells.maxOfOrNull { it.frame.height } ?: 0f
        val normalizedCells = cells.map { cell ->
            cell.copy(frame = cell.frame.copy(height = rowHeight))
        }
        val tableWidth = columnWidths.sum()
        val rowGroup = LayoutTableRowGroup(
            identity = row.identity,
            frame = LayoutRect(contentLeft, cursorY, tableWidth, rowHeight),
            contentFrame = LayoutRect(contentLeft, cursorY, tableWidth, rowHeight),
            isHeader = row.isHeader,
            cells = normalizedCells,
        )
        cursorY += rowHeight
        rowGroup
    }
    val contentHeight = (cursorY - contentTop).coerceAtLeast(0f)
    return LayoutTableBlockModel(
        identity = block.identity,
        frame = LayoutRect(left, top, width, insets.top + contentHeight + insets.bottom),
        contentFrame = LayoutRect(contentLeft, contentTop, columnWidths.sum(), contentHeight),
        block = block,
        columnWidths = columnWidths,
        rows = rows,
    )
}

private fun layoutDefinitionListBlock(
    block: DefinitionListBlockModel,
    left: Float,
    top: Float,
    width: Float,
    insets: LayoutInsets,
    environment: LayoutEnvironment,
): LayoutDefinitionListBlockModel {
    val contentLeft = left + insets.left
    val contentTop = top + insets.top
    val contentWidth = (width - insets.left - insets.right).coerceAtLeast(0f)
    val indent = 24f
    val spacing = 4f
    var cursorY = contentTop
    val items = block.items.map { item ->
        when (item) {
            is com.hrm.markdown.renderer.internal.core.model.DefinitionTermBlockModel -> {
                val height = environment.measureInlineBlock(
                    model = item.inline,
                    style = environment.markdownTheme.bodyStyle.copy(fontWeight = FontWeight.Bold),
                    widthPx = contentWidth,
                )
                val group = LayoutDefinitionTermGroup(
                    identity = item.identity,
                    frame = LayoutRect(contentLeft, cursorY, contentWidth, height),
                    contentFrame = LayoutRect(contentLeft, cursorY, contentWidth, height),
                    item = item,
                )
                cursorY += height + spacing
                group
            }

            is com.hrm.markdown.renderer.internal.core.model.DefinitionDescriptionBlockModel -> {
                val (children, bottom) = layoutBlocks(
                    item.children,
                    contentLeft + indent,
                    cursorY,
                    (contentWidth - indent).coerceAtLeast(0f),
                    environment
                )
                val height = (bottom - cursorY).coerceAtLeast(0f)
                val group = LayoutDefinitionDescriptionGroup(
                    identity = item.identity,
                    frame = LayoutRect(contentLeft, cursorY, contentWidth, height),
                    contentFrame = LayoutRect(
                        contentLeft + indent,
                        cursorY,
                        (contentWidth - indent).coerceAtLeast(0f),
                        height
                    ),
                    item = item,
                    children = children,
                )
                cursorY = bottom + spacing
                group
            }
        }
    }
    val contentHeight = (cursorY - contentTop - spacing).coerceAtLeast(0f)
    return LayoutDefinitionListBlockModel(
        identity = block.identity,
        frame = LayoutRect(left, top, width, insets.top + contentHeight + insets.bottom),
        contentFrame = LayoutRect(contentLeft, contentTop, contentWidth, contentHeight),
        block = block,
        items = items,
    )
}

private fun layoutFigureBlock(
    block: FigureBlockModel,
    left: Float,
    top: Float,
    width: Float,
    insets: LayoutInsets,
    environment: LayoutEnvironment,
): LayoutFigureBlockModel {
    val contentLeft = left + insets.left
    val contentTop = top + insets.top
    val contentWidth = (width - insets.left - insets.right).coerceAtLeast(0f)
    val imageHeight = block.imageHeight?.toFloat() ?: 220f
    val imageWidth =
        block.imageWidth?.toFloat()?.coerceAtMost(contentWidth) ?: contentWidth.coerceAtMost(360f)
    val imageFrame = LayoutRect(
        left = contentLeft + ((contentWidth - imageWidth) / 2f).coerceAtLeast(0f),
        top = contentTop,
        width = imageWidth,
        height = imageHeight,
    )
    val captionHeight =
        if (block.caption.isNotBlank()) environment.lineHeightPx(environment.markdownTheme.bodyStyle) else 0f
    val captionFrame = if (captionHeight > 0f) {
        LayoutRect(
            left = contentLeft,
            top = imageFrame.top + imageFrame.height + 4f,
            width = contentWidth,
            height = captionHeight,
        )
    } else null
    val contentHeight =
        imageFrame.height + if (captionFrame != null) 4f + captionFrame.height else 0f
    return LayoutFigureBlockModel(
        identity = block.identity,
        frame = LayoutRect(left, top, width, insets.top + contentHeight + insets.bottom),
        contentFrame = LayoutRect(contentLeft, contentTop, contentWidth, contentHeight),
        block = block,
        imageFrame = imageFrame,
        captionFrame = captionFrame,
    )
}

private fun layoutTocBlock(
    block: TocBlockModel,
    left: Float,
    top: Float,
    width: Float,
    insets: LayoutInsets,
    environment: LayoutEnvironment,
): LayoutTocBlockModel {
    val contentLeft = left + insets.left
    val contentTop = top + insets.top
    val contentWidth = (width - insets.left - insets.right).coerceAtLeast(0f)
    var cursorY = contentTop
    val entries = block.entries.map { entry ->
        val indent = ((entry.level - 1).coerceAtLeast(0) * 16f)
        val height =
            environment.measureTocEntryHeight(entry, (contentWidth - indent).coerceAtLeast(24f))
        val group = LayoutTocEntryGroup(
            identity = RenderIdentity(
                stableId = renderIdentityFromText("${entry.level}:${entry.text}:${entry.id.orEmpty()}"),
                contentRevision = 0L,
                layoutRevision = 0L,
                paintRevision = 0L,
            ),
            frame = LayoutRect(contentLeft, cursorY, contentWidth, height),
            contentFrame = LayoutRect(
                contentLeft + indent,
                cursorY,
                (contentWidth - indent).coerceAtLeast(0f),
                height
            ),
            entry = entry,
        )
        cursorY += height + environment.blockSpacing / 2f
        group
    }
    val contentHeight = (cursorY - contentTop - environment.blockSpacing / 2f).coerceAtLeast(0f)
    return LayoutTocBlockModel(
        identity = block.identity,
        frame = LayoutRect(left, top, width, insets.top + contentHeight + insets.bottom),
        contentFrame = LayoutRect(contentLeft, contentTop, contentWidth, contentHeight),
        block = block,
        entries = entries,
    )
}

private fun layoutBibliographyBlock(
    block: BibliographyDefinitionBlockModel,
    left: Float,
    top: Float,
    width: Float,
    insets: LayoutInsets,
    environment: LayoutEnvironment,
): LayoutBibliographyBlockModel {
    val contentLeft = left + insets.left
    val contentTop = top + insets.top
    val contentWidth = (width - insets.left - insets.right).coerceAtLeast(0f)
    val titleHeight =
        environment.lineHeightPx(environment.markdownTheme.headingStyles.getOrElse(3) { environment.markdownTheme.bodyStyle })
    var cursorY = contentTop + titleHeight + 8f
    val entryHeight = environment.lineHeightPx(environment.markdownTheme.bodyStyle)
    val entries = block.entries.map { entry ->
        val group = LayoutBibliographyEntryGroup(
            identity = RenderIdentity(
                stableId = renderIdentityFromText("${entry.key}:${entry.content}"),
                contentRevision = 0L,
                layoutRevision = 0L,
                paintRevision = 0L,
            ),
            frame = LayoutRect(contentLeft, cursorY, contentWidth, entryHeight),
            contentFrame = LayoutRect(contentLeft, cursorY, contentWidth, entryHeight),
            entry = entry,
        )
        cursorY += entryHeight + 4f
        group
    }
    val contentHeight = (cursorY - contentTop - 4f).coerceAtLeast(titleHeight)
    return LayoutBibliographyBlockModel(
        identity = block.identity,
        frame = LayoutRect(left, top, width, insets.top + contentHeight + insets.bottom),
        contentFrame = LayoutRect(contentLeft, contentTop, contentWidth, contentHeight),
        block = block,
        entries = entries,
    )
}

private fun layoutWidgetBlock(
    block: InternalRenderBlockModel,
    widget: com.hrm.markdown.renderer.internal.core.model.BlockWidgetModel,
    left: Float,
    top: Float,
    width: Float,
    insets: LayoutInsets,
    environment: LayoutEnvironment,
): LayoutWidgetBlockModel {
    val measurement = measureBlockWidget(
        widget = widget,
        viewportWidthPx = (width - insets.left - insets.right).coerceAtLeast(0f),
        environment = environment,
    )
    val contentFrame = LayoutRect(
        left = left + insets.left,
        top = top + insets.top,
        width = measurement.widthPx.coerceAtMost(
            (width - insets.left - insets.right).coerceAtLeast(
                0f
            )
        ),
        height = measurement.heightPx,
    )
    return LayoutWidgetBlockModel(
        identity = block.identity,
        frame = LayoutRect(
            left = left,
            top = top,
            width = width,
            height = insets.top + measurement.heightPx + insets.bottom,
        ),
        contentFrame = contentFrame,
        block = block,
        widget = widget,
        measurement = measurement,
    )
}

private fun layoutRenderBlock(
    block: InternalRenderBlockModel,
    left: Float,
    top: Float,
    width: Float,
    insets: LayoutInsets,
    contentHeight: Float,
    children: List<InternalLayoutBlockModel>,
): LayoutRenderBlockModel {
    return LayoutRenderBlockModel(
        identity = block.identity,
        frame = LayoutRect(
            left = left,
            top = top,
            width = width,
            height = insets.top + contentHeight + insets.bottom,
        ),
        contentFrame = LayoutRect(
            left = left + insets.left,
            top = top + insets.top,
            width = (width - insets.left - insets.right).coerceAtLeast(0f),
            height = contentHeight,
        ),
        block = block,
        children = children,
    )
}

private fun blockInsets(block: InternalRenderBlockModel): LayoutInsets = when (block) {
    is CodeBlockModel,
    is MathBlockModel,
    is DiagramBlockModel,
    is HtmlBlockModel,
    is BibliographyDefinitionBlockModel -> LayoutInsets(0f, 12f, 0f, 12f)

    is BlockQuoteBlockModel -> LayoutInsets(20f, 0f, 0f, 0f)
    is AdmonitionBlockModel,
    is CustomContainerBlockModel,
    is DirectiveBlockModel,
    is FigureBlockModel -> LayoutInsets(16f, 12f, 12f, 12f)

    is FootnoteDefinitionBlockModel -> LayoutInsets(0f, 4f, 0f, 0f)
    is PageBreakBlockModel,
    is ThematicBreakBlockModel -> LayoutInsets(0f, 8f, 0f, 8f)

    else -> LayoutInsets()
}

private fun extractRenderBlock(block: InternalLayoutBlockModel): InternalRenderBlockModel? =
    when (block) {
        is LayoutRenderBlockModel -> block.block
        is LayoutListBlockModel -> block.block
        is LayoutColumnsBlockModel -> block.block
        is LayoutTableBlockModel -> block.block
        is LayoutDefinitionListBlockModel -> block.block
        is LayoutFigureBlockModel -> block.block
        is LayoutTocBlockModel -> block.block
        is LayoutBibliographyBlockModel -> block.block
        is LayoutTabBlockModel -> block.block
        is LayoutFootnoteBlockModel -> block.block
        is LayoutWidgetBlockModel -> block.block
        else -> null
    }
