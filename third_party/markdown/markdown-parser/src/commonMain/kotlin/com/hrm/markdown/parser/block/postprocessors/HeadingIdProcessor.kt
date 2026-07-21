package com.hrm.markdown.parser.block.postprocessors

import com.hrm.markdown.parser.ast.*

/**
 * 自动为所有标题生成 ID（slug），基于标题文本内容。
 */
class HeadingIdProcessor : PostProcessor {
    override val priority: Int = 100

    override fun process(document: Document) {
        val usedIds = mutableMapOf<String, Int>()
        for (child in document.children) {
            processRecursive(child, usedIds)
        }
    }

    private fun processRecursive(node: Node, usedIds: MutableMap<String, Int>) {
        when (node) {
            is Heading -> {
                if (node.customId == null) {
                    val text = extractPlainText(node)
                    val slug = generateSlug(text)
                    node.autoId = deduplicateId(slug, usedIds)
                } else {
                    usedIds[node.customId!!] = (usedIds[node.customId!!] ?: 0) + 1
                }
            }
            is SetextHeading -> {
                val text = extractPlainText(node)
                val slug = generateSlug(text)
                node.autoId = deduplicateId(slug, usedIds)
            }
            is ContainerNode -> {
                for (child in node.children) {
                    processRecursive(child, usedIds)
                }
            }
            else -> {}
        }
    }

    companion object {
        fun extractPlainText(node: Node): String {
            return when (node) {
                is Text -> node.literal
                is InlineCode -> node.literal
                is Emoji -> node.literal
                is EscapedChar -> node.literal
                is HtmlEntity -> node.resolved.ifEmpty { node.literal }
                is SoftLineBreak -> " "
                is HardLineBreak -> " "
                is ContainerNode -> node.children.joinToString("") { extractPlainText(it) }
                else -> ""
            }
        }

        fun generateSlug(text: String): String {
            return text.lowercase()
                .replace(Regex("[^\\w\\u4e00-\\u9fff-]"), "-")
                .replace(Regex("-+"), "-")
                .trim('-')
                .ifEmpty { "heading" }
        }

        fun deduplicateId(slug: String, usedIds: MutableMap<String, Int>): String {
            val count = usedIds[slug]
            return if (count == null) {
                usedIds[slug] = 1
                slug
            } else {
                usedIds[slug] = count + 1
                val newId = "$slug-$count"
                usedIds[newId] = 1
                newId
            }
        }
    }
}
