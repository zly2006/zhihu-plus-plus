package com.hrm.markdown.runtime

/**
 * 单次输入转换的结果。
 */
data class MarkdownTransformResult(
    val markdown: String,
    val sourceMap: MarkdownSourceMap = MarkdownSourceMap.Identity,
)
