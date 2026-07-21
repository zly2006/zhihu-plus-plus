package com.hrm.markdown.renderer

import com.hrm.markdown.parser.MarkdownParser
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.html.HtmlRenderer
import com.hrm.markdown.runtime.MarkdownDirectivePlugin
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry
import com.hrm.markdown.runtime.MarkdownDirectivePipeline

/**
 * 高层 HTML 导出入口：统一走 runtime transform pipeline，避免绕过插件导致输出不一致。
 */
object MarkdownHtml {
    fun render(
        markdown: String,
        config: MarkdownConfig = MarkdownConfig.Default,
        directivePlugins: List<MarkdownDirectivePlugin> = emptyList(),
        softBreak: String = "\n",
        escapeHtml: Boolean = false,
        xhtml: Boolean = true,
    ): String {
        val registry = MarkdownDirectiveRegistry(directivePlugins)
        val normalized = MarkdownDirectivePipeline(registry).transform(markdown).markdown

        val parser = MarkdownParser(
            flavour = config.flavour,
            customEmojiMap = config.customEmojiMap,
            enableAsciiEmoticons = config.enableAsciiEmoticons,
            enableLinting = config.enableLinting,
        )
        val document = parser.parse(normalized)

        return render(
            document = document,
            directivePlugins = directivePlugins,
            softBreak = softBreak,
            escapeHtml = escapeHtml,
            xhtml = xhtml,
        )
    }

    fun render(
        document: Document,
        directivePlugins: List<MarkdownDirectivePlugin> = emptyList(),
        softBreak: String = "\n",
        escapeHtml: Boolean = false,
        xhtml: Boolean = true,
    ): String {
        val registry = MarkdownDirectiveRegistry(directivePlugins)
        val renderer = HtmlRenderer(
            softBreak = softBreak,
            escapeHtml = escapeHtml,
            xhtml = xhtml,
            directiveBlockFallback = { node ->
                registry.findHtmlDirectiveFallback(node.tagName)?.render(node)
            },
            directiveInlineFallback = { node ->
                registry.findHtmlInlineDirectiveFallback(node.tagName)?.render(node)
            }
        )
        return renderer.render(document)
    }
}
