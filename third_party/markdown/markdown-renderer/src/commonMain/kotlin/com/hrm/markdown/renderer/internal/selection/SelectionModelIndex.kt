package com.hrm.markdown.renderer.internal.selection

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import com.hrm.markdown.renderer.internal.core.model.CodeBlockModel
import com.hrm.markdown.renderer.internal.core.model.DiagramBlockModel
import com.hrm.markdown.renderer.internal.core.model.DirectiveInlineWidgetModel
import com.hrm.markdown.renderer.internal.core.model.HtmlBlockModel
import com.hrm.markdown.renderer.internal.core.model.ImageWidgetModel
import com.hrm.markdown.renderer.internal.core.model.InlineCodeWidgetModel
import com.hrm.markdown.renderer.internal.core.model.InlineMathWidgetModel
import com.hrm.markdown.renderer.internal.core.model.InlineModel
import com.hrm.markdown.renderer.internal.core.model.MathBlockModel
import com.hrm.markdown.renderer.internal.core.model.RubyTextWidgetModel
import com.hrm.markdown.renderer.internal.core.model.SpoilerWidgetModel
import com.hrm.markdown.renderer.internal.core.model.TextAtom
import com.hrm.markdown.renderer.internal.core.model.WidgetAtom
import com.hrm.markdown.renderer.internal.layout.inline.runPlacements
import com.hrm.markdown.renderer.internal.layout.inline.textMeasurementStyle
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutColumnsBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutDefinitionDescriptionGroup
import com.hrm.markdown.renderer.internal.layout.model.LayoutDefinitionListBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutFootnoteBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutInlineBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutInlineRun
import com.hrm.markdown.renderer.internal.layout.model.LayoutListBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutRenderBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutTabBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutTableBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutTextRun
import com.hrm.markdown.renderer.internal.layout.model.LayoutWidgetBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutWidgetRun

/**
 * 单个 LayoutTextRun 在所属 block 字符空间内占据的区间 `[charStart, charEnd)`。
 * [lineIndex]/[runIndex] 用于命中测试与高亮时回到几何模型定位。
 */
internal data class SelectionRunSpan(
    val lineIndex: Int,
    val runIndex: Int,
    val run: LayoutInlineRun,
    val text: String,
    val textLayout: TextLayoutResult? = null,
    val charStart: Int,
    val charEnd: Int,
)

/**
 * 一个可选中的 inline 块在文档中的索引条目。
 * [order] 为深度优先访问顺序；[totalChars] 为块内全部文本长度。
 */
internal data class SelectionBlockEntry(
    override val stableId: Long,
    override val order: Int,
    val block: InternalLayoutBlockModel,
    val runs: List<SelectionRunSpan>,
    override val totalChars: Int,
    override val text: String,
) : SelectionTextEntry

/**
 * 选区文档模型索引：把整棵 layout 树里所有可选 inline 块按文档序拍平，
 * 提供锚点比较、归一化、夹紧、字符→run 定位等纯函数能力。
 */
internal class SelectionModelIndex(
    val entries: List<SelectionBlockEntry>,
) {
    private val orderByStableId: Map<Long, Int> =
        entries.associate { it.stableId to it.order }
    private val entryByStableId: Map<Long, SelectionBlockEntry> =
        entries.associateBy { it.stableId }

    val isEmpty: Boolean get() = entries.isEmpty()

    fun entryOf(stableId: Long): SelectionBlockEntry? = entryByStableId[stableId]

    fun orderOf(stableId: Long): Int? = orderByStableId[stableId]

    /** 把锚点的 charInBlock 夹紧到所属块的合法范围；块不存在则返回 null。 */
    fun clampAnchor(anchor: SelectionAnchor): SelectionAnchor? {
        val entry = entryByStableId[anchor.blockStableId] ?: return null
        val clamped = anchor.charInBlock.coerceIn(0, entry.totalChars)
        return if (clamped == anchor.charInBlock) anchor else anchor.copy(charInBlock = clamped)
    }

    /** 文档序比较：先比 order，再比块内偏移。块不存在时按 0 处理。 */
    fun compare(a: SelectionAnchor, b: SelectionAnchor): Int {
        val oa = orderByStableId[a.blockStableId] ?: 0
        val ob = orderByStableId[b.blockStableId] ?: 0
        if (oa != ob) return oa.compareTo(ob)
        return a.charInBlock.compareTo(b.charInBlock)
    }

    /** 顺序无关地构造规范范围（start 不晚于 end）。 */
    fun normalize(a: SelectionAnchor, b: SelectionAnchor): SelectionRange =
        if (compare(a, b) <= 0) SelectionRange(a, b) else SelectionRange(b, a)

    /** 把块内字符偏移定位到具体 run 及 run 内偏移。 */
    fun charToRun(entry: SelectionBlockEntry, charInBlock: Int): Pair<SelectionRunSpan, Int>? {
        if (entry.runs.isEmpty()) return null
        val clamped = charInBlock.coerceIn(0, entry.totalChars)
        for (span in entry.runs) {
            if (clamped < span.charEnd) {
                return span to (clamped - span.charStart).coerceAtLeast(0)
            }
        }
        val last = entry.runs.last()
        return last to (last.charEnd - last.charStart)
    }

    val firstAnchor: SelectionAnchor?
        get() = entries.firstOrNull()?.let { SelectionAnchor(it.stableId, 0) }

    val lastAnchor: SelectionAnchor?
        get() = entries.lastOrNull()?.let { SelectionAnchor(it.stableId, it.totalChars) }
}

/**
 * 深度优先遍历 layout 树，收集可选文本块。
 * Inline 块支持逐字命中与局部高亮；表格、代码、数学公式和图表等非 inline 块按原子块参与复制。
 */
internal fun buildSelectionIndex(blocks: List<InternalLayoutBlockModel>): SelectionModelIndex {
    val entries = ArrayList<SelectionBlockEntry>()
    var nextOrder = 0

    fun visit(block: InternalLayoutBlockModel) {
        when (block) {
            is LayoutInlineBlockModel -> {
                entries += buildBlockEntry(block, nextOrder++)
            }

            is LayoutTableBlockModel -> {
                val text = tablePlainText(block)
                if (text.isNotEmpty()) entries += buildTextOnlyEntry(block, nextOrder++, text)
            }

            is LayoutWidgetBlockModel -> {
                val text = widgetBlockPlainText(block)
                if (text.isNotEmpty()) entries += buildTextOnlyEntry(block, nextOrder++, text)
            }

            is LayoutRenderBlockModel -> {
                val text = renderBlockPlainText(block)
                if (text.isNotEmpty()) {
                    entries += buildTextOnlyEntry(block, nextOrder++, text)
                } else {
                    block.children.forEach(::visit)
                }
            }
            is LayoutListBlockModel -> block.items.forEach { item -> item.children.forEach(::visit) }
            is LayoutColumnsBlockModel -> block.columns.forEach { col -> col.children.forEach(::visit) }
            is LayoutTabBlockModel -> block.tabs.forEach { tab -> tab.children.forEach(::visit) }
            is LayoutFootnoteBlockModel -> {
                block.leadChild?.let(::visit)
                block.trailingChildren.forEach(::visit)
            }
            is LayoutDefinitionListBlockModel -> block.items.forEach { item ->
                if (item is LayoutDefinitionDescriptionGroup) item.children.forEach(::visit)
            }
            // Excluded from selection: figure/toc/bibliography and blocks without a stable plain-text form.
            else -> Unit
        }
    }

    blocks.forEach(::visit)
    return SelectionModelIndex(entries)
}

internal fun SelectionModelIndex.withMeasuredTextRuns(
    textMeasurer: TextMeasurer,
): SelectionModelIndex {
    val measuredEntries = entries.map { entry ->
        val inlineBlock = entry.block as? LayoutInlineBlockModel ?: return@map entry
        val style = textMeasurementStyle(inlineBlock.style)
        entry.copy(
            runs = entry.runs.map { span ->
                val textRun = span.run as? LayoutTextRun ?: return@map span
                span.copy(
                    textLayout = textMeasurer.measure(
                        text = textRun.text,
                        style = style,
                        constraints = Constraints(maxWidth = Int.MAX_VALUE),
                        maxLines = 1,
                        softWrap = false,
                    )
                )
            }
        )
    }
    return SelectionModelIndex(measuredEntries)
}

private fun buildBlockEntry(block: LayoutInlineBlockModel, order: Int): SelectionBlockEntry {
    val runs = ArrayList<SelectionRunSpan>()
    var cursor = 0
    for (placement in block.runPlacements()) {
        val run = placement.run
        val text = when (run) {
            is LayoutTextRun -> run.text.text
            is LayoutWidgetRun -> run.alternateText
        }
        if (text.isNotEmpty()) {
            val len = text.length
            runs += SelectionRunSpan(
                lineIndex = placement.lineIndex,
                runIndex = placement.runIndex,
                run = run,
                text = text,
                charStart = cursor,
                charEnd = cursor + len,
            )
            cursor += len
        }
    }
    return SelectionBlockEntry(
        stableId = block.identity.stableId,
        order = order,
        block = block,
        runs = runs,
        totalChars = cursor,
        text = buildString { for (span in runs) append(span.text) },
    )
}

private fun buildTextOnlyEntry(
    block: InternalLayoutBlockModel,
    order: Int,
    text: String,
): SelectionBlockEntry =
    SelectionBlockEntry(
        stableId = block.identity.stableId,
        order = order,
        block = block,
        runs = emptyList(),
        totalChars = text.length,
        text = text,
    )

private fun tablePlainText(block: LayoutTableBlockModel): String =
    block.rows.joinToString("\n") { row ->
        row.cells.joinToString("\t") { cell ->
            cell.cell?.inline?.plainText().orEmpty()
        }
    }

private fun widgetBlockPlainText(block: LayoutWidgetBlockModel): String =
    when (val renderBlock = block.block) {
        is CodeBlockModel -> renderBlock.code
        is MathBlockModel -> renderBlock.latex
        is DiagramBlockModel -> renderBlock.code
        else -> ""
    }

private fun renderBlockPlainText(block: LayoutRenderBlockModel): String =
    when (val renderBlock = block.block) {
        is HtmlBlockModel -> renderBlock.html
        else -> ""
    }

private fun InlineModel.plainText(): String =
    buildString {
        for (atom in atoms) {
            when (atom) {
                is TextAtom -> append(atom.text)
                is WidgetAtom -> append(atom.widget.plainText())
            }
        }
    }

private fun com.hrm.markdown.renderer.internal.core.model.InlineWidgetModel.plainText(): String =
    when (this) {
        is InlineCodeWidgetModel -> code
        is InlineMathWidgetModel -> latex
        is ImageWidgetModel -> altText.ifEmpty { title ?: url }
        is SpoilerWidgetModel -> alternateText
        is DirectiveInlineWidgetModel -> alternateText
        is RubyTextWidgetModel -> base
    }
