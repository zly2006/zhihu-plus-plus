package com.hrm.markdown.parser.block.postprocessors

import com.hrm.markdown.parser.ast.*

/**
 * GFM：过滤禁止的原始 HTML 标签。
 */
class HtmlFilterProcessor : PostProcessor {
    override val priority: Int = 200

    override fun process(document: Document) {
        processRecursive(document)
    }

    private fun processRecursive(node: Node) {
        when (node) {
            is HtmlBlock -> {
                val filtered = filterGfmDisallowedTags(node.literal)
                if (filtered != node.literal) {
                    node.literal = filtered
                }
            }
            is InlineHtml -> {
                val filtered = filterGfmDisallowedTags(node.literal)
                if (filtered != node.literal) {
                    node.literal = filtered
                }
            }
            is ContainerNode -> {
                for (child in node.children.toList()) {
                    processRecursive(child)
                }
            }
            else -> {}
        }
    }

    private fun filterGfmDisallowedTags(html: String): String {
        return GFM_DISALLOWED_TAG_REGEX.replace(html) { match ->
            "<!-- ${match.value} (filtered) -->"
        }
    }

    companion object {
        private val GFM_DISALLOWED_TAG_REGEX = Regex(
            "<(title|textarea|style|xmp|iframe|noembed|noframes|script|plaintext)(\\s[^>]*)?>",
            RegexOption.IGNORE_CASE
        )
    }
}
