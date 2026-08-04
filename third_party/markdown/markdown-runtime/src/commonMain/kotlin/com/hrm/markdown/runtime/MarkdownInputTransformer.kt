package com.hrm.markdown.runtime

/**
 * Markdown 输入转换器。
 *
 * 用于把外部自定义语法转换成官方 directive 语法，避免污染 parser。
 */
interface MarkdownInputTransformer {
    val id: String

    /**
     * 预留给未来的增量转换能力。
     * 当前实现中，只要存在任何 transformer，渲染器就会禁用 streaming fast path。
     */
    val supportsStreaming: Boolean get() = false

    fun transform(input: String): MarkdownTransformResult
}
