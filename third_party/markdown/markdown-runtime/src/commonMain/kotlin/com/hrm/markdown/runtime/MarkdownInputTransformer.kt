package com.hrm.markdown.runtime

/**
 * Markdown 输入转换器。
 *
 * 用于把外部自定义语法转换成官方 directive 语法，避免污染 parser。
 */
interface MarkdownInputTransformer {
    val id: String

    fun transform(input: String): MarkdownTransformResult
}
