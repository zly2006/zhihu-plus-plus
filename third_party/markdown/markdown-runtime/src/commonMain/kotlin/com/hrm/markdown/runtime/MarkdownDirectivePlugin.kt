package com.hrm.markdown.runtime

/**
 * Markdown 扩展插件。
 *
 * 一个插件可以同时提供：
 * - 输入转换器：把外部特殊语法转换成官方 directive
 * - 块级 directive 渲染器：把 directive 渲染为原生 Compose 内容
 * - 行内 directive 渲染器：把行内 directive 渲染为原生 Compose 内容
 * - HTML fallback：为 HTML 导出提供自定义降级输出
 */
interface MarkdownDirectivePlugin {
    val id: String
    val priority: Int get() = 0
    val inputTransformers: List<MarkdownInputTransformer> get() = emptyList()
    val blockDirectiveRenderers: Map<String, MarkdownBlockDirectiveRenderer> get() = emptyMap()
    val inlineDirectiveRenderers: Map<String, MarkdownInlineDirectiveRenderer> get() = emptyMap()
    val htmlDirectiveFallbacks: Map<String, HtmlDirectiveFallback> get() = emptyMap()
    val htmlInlineDirectiveFallbacks: Map<String, HtmlInlineDirectiveFallback> get() = emptyMap()
}
