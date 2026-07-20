package com.hrm.markdown.renderer.internal.selection

/**
 * 按选区范围抽取纯文本。
 * - 首块从 start.charInBlock 到块末；
 * - 末块从 0 到 end.charInBlock；
 * - 中间块全选；
 * - 块之间用 "\n" 连接（gap 块——表格/代码块等——因不在索引中而被自然跳过）。
 */
internal fun extractSelectedText(index: SelectionModelIndex, range: SelectionRange): String {
    return extractSelectedText(index.entries, index::entryOf, range)
}

internal fun extractSelectedText(index: SelectionDocumentIndex, range: SelectionRange): String {
    return extractSelectedText(index.entries, index::entryOf, range)
}

private fun extractSelectedText(
    entries: List<SelectionTextEntry>,
    entryOf: (Long) -> SelectionTextEntry?,
    range: SelectionRange,
): String {
    if (entries.isEmpty()) return ""
    val startEntry = entryOf(range.start.blockStableId) ?: return ""
    val endEntry = entryOf(range.end.blockStableId) ?: return ""

    val startOrder = startEntry.order
    val endOrder = endEntry.order
    if (startOrder > endOrder) return ""

    val parts = ArrayList<String>()
    for (entry in entries) {
        if (entry.order !in startOrder..endOrder) continue
        val from = if (entry.order == startOrder) range.start.charInBlock.coerceIn(
            0,
            entry.totalChars
        ) else 0
        val to = if (entry.order == endOrder) range.end.charInBlock.coerceIn(
            0,
            entry.totalChars
        ) else entry.totalChars
        if (to <= from) {
            parts += ""
        } else {
            parts += entry.text.substring(from, to)
        }
    }
    return parts.joinToString("\n")
}
