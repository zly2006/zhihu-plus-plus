package com.hrm.markdown.runtime

/**
 * 输入转换后的源码映射信息。
 *
 * 当前版本先保留结构，为后续诊断和源码回跳打基础。
 */
sealed interface MarkdownSourceMap {
    data object Identity : MarkdownSourceMap

    data class Segmented(
        val segments: List<Segment>
    ) : MarkdownSourceMap {
        data class Segment(
            val outputStart: Int,
            val outputEnd: Int,
            val inputStart: Int,
            val inputEnd: Int,
        )
    }
}

internal fun composeSourceMap(
    previous: MarkdownSourceMap,
    current: MarkdownSourceMap,
): MarkdownSourceMap {
    if (previous is MarkdownSourceMap.Identity) return current
    if (current is MarkdownSourceMap.Identity) return previous
    // 先保留最后一次非 Identity 映射，避免过度承诺尚未接入的精确偏移合成。
    return current
}
