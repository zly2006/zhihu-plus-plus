package com.hrm.markdown.renderer.internal.selection

/**
 * 选区锚点：以"块的稳定 id + 块内字符偏移"表达，不依赖屏幕坐标。
 *
 * - [blockStableId] 取 LayoutInlineBlockModel.identity.stableId（基于源范围，reflow 不变）。
 * - [charInBlock] 是该 block 内所有 LayoutTextRun 文本顺序拼接后的字符总偏移。
 */
internal data class SelectionAnchor(
    val blockStableId: Long,
    val charInBlock: Int,
)

/** 归一化后的选区范围（start 不晚于 end）。 */
internal data class SelectionRange(
    val start: SelectionAnchor,
    val end: SelectionAnchor,
)
