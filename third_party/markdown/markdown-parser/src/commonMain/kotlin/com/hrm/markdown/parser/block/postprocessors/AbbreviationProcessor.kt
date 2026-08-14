package com.hrm.markdown.parser.block.postprocessors

import com.hrm.markdown.parser.ast.*

/**
 * 将缩写定义应用到文档中的 Text 节点。
 */
class AbbreviationProcessor : PostProcessor {
    override val priority: Int = 300

    override fun process(document: Document) {
        if (document.abbreviationDefinitions.isEmpty()) return
        val abbrs = document.abbreviationDefinitions.values.sortedByDescending { it.abbreviation.length }
        processRecursive(document, abbrs)
    }

    private fun processRecursive(node: Node, abbrs: List<AbbreviationDefinition>) {
        if (node is ContainerNode) {
            val children = node.children.toList()
            for (child in children) {
                if (child is Text) {
                    replaceAbbreviationsInText(node, child, abbrs)
                } else {
                    processRecursive(child, abbrs)
                }
            }
        }
    }

    private fun replaceAbbreviationsInText(
        parent: ContainerNode,
        textNode: Text,
        abbrs: List<AbbreviationDefinition>
    ) {
        val text = textNode.literal
        val replacements = mutableListOf<Triple<Int, Int, AbbreviationDefinition>>()

        for (def in abbrs) {
            val abbr = def.abbreviation
            var searchFrom = 0
            while (true) {
                val idx = text.indexOf(abbr, searchFrom)
                if (idx < 0) break
                val before = if (idx > 0) text[idx - 1] else ' '
                val after = if (idx + abbr.length < text.length) text[idx + abbr.length] else ' '
                if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) {
                    replacements.add(Triple(idx, idx + abbr.length, def))
                }
                searchFrom = idx + abbr.length
            }
        }

        if (replacements.isEmpty()) return

        val sorted = replacements.sortedBy { it.first }
        val filtered = mutableListOf<Triple<Int, Int, AbbreviationDefinition>>()
        var lastEnd = 0
        for (r in sorted) {
            if (r.first >= lastEnd) {
                filtered.add(r)
                lastEnd = r.second
            }
        }

        val newNodes = mutableListOf<Node>()
        var pos = 0
        for ((start, end, def) in filtered) {
            if (start > pos) {
                newNodes.add(Text(text.substring(pos, start)))
            }
            newNodes.add(Abbreviation(abbreviation = def.abbreviation, fullText = def.fullText))
            pos = end
        }
        if (pos < text.length) {
            newNodes.add(Text(text.substring(pos)))
        }

        val idx = parent.children.indexOf(textNode)
        if (idx >= 0) {
            parent.removeChild(textNode)
            for ((i, n) in newNodes.withIndex()) {
                parent.insertChild(idx + i, n)
            }
        }
    }
}
