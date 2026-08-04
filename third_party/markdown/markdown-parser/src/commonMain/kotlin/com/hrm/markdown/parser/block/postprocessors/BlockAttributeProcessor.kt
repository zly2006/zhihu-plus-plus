package com.hrm.markdown.parser.block.postprocessors

import com.hrm.markdown.parser.ast.*

/**
 * 块级属性后处理器：解析 kramdown/Pandoc 风格的块属性语法。
 *
 * 支持两种用法：
 *
 * ### 1. 独立属性段落（仅含 `{...}`，通过空行分隔）
 * ```markdown
 * # 标题
 *
 * {.special-heading #main-title}
 * ```
 *
 * ### 2. 块紧邻属性行（无空行）
 * ```markdown
 * # 标题
 * {.special}
 *
 * 段落文本。
 * {.highlight}
 * ```
 *
 * ## 规则
 * 1. `{...}` 必须独占一行（可有前后空白）。
 * 2. 独立属性段落附加到紧邻的前一个块级节点上（跳过空行），然后移除。
 * 3. 段落尾部的属性行附加到该段落自身，属性文本从段落中移除。
 * 4. 非段落块之后紧邻的属性段落（如标题后紧跟 `{.class}`）附加到该块。
 * 5. 支持 `.class`、`#id`、`key=value`、`key="quoted value"` 语法。
 */
class BlockAttributeProcessor : PostProcessor {
    override val priority: Int = 150

    private val ATTR_LINE_REGEX = Regex("""^\s*\{([^}]+)\}\s*$""")

    override fun process(document: Document) {
        processContainer(document)
    }

    private fun processContainer(container: ContainerNode) {
        val children = container.children.toList()
        val toRemove = mutableSetOf<Node>()

        for (i in children.indices) {
            val child = children[i]

            // 先递归处理子容器
            if (child is ContainerNode && child !is Paragraph) {
                processContainer(child)
            }

            if (child !is Paragraph) continue

            val raw = child.rawContent ?: continue

            // Case 1: 整个段落仅包含 {…}
            val trimmed = raw.trim()
            val wholeMatch = ATTR_LINE_REGEX.matchEntire(trimmed)
            if (wholeMatch != null) {
                val prevBlock = findPreviousBlock(children, i, toRemove)
                if (prevBlock != null) {
                    val attrs = parseBlockAttributes(wholeMatch.groupValues[1])
                    if (attrs.isNotEmpty()) {
                        applyAttributes(prevBlock, attrs)
                        toRemove.add(child)
                    }
                }
                continue
            }

            // Case 2: 段落最后一行是 {…}
            val lines = raw.split('\n')
            if (lines.size >= 2) {
                val lastLine = lines.last().trim()
                val tailMatch = ATTR_LINE_REGEX.matchEntire(lastLine)
                if (tailMatch != null) {
                    val attrs = parseBlockAttributes(tailMatch.groupValues[1])
                    if (attrs.isNotEmpty()) {
                        applyAttributes(child, attrs)
                        // 更新 rawContent（移除尾部属性行）
                        child.rawContent = lines.dropLast(1).joinToString("\n")
                        // 触发 inline parsing 以重新解析内容
                        // 通过 children 访问触发 lazy parsing，
                        // 但 lazy content 已经被 BlockParser 设置过了。
                        // 由于 rawContent 只是备份，lazy content 仍然包含属性行。
                        // 我们需要在 inline parsing 完成后清理 children。
                        forceInlineParseAndStripTrailingAttr(child)
                    }
                    continue
                }
            }
        }

        for (node in toRemove) {
            container.removeChild(node)
        }
    }

    /**
     * 强制触发行内解析并移除尾部属性文本。
     *
     * 当段落的 rawContent 以 `{...}` 结尾时，lazy inline 解析后
     * 最后的 Text 节点可能包含 `\n{...}` 内容。本方法清理之。
     */
    private fun forceInlineParseAndStripTrailingAttr(paragraph: Paragraph) {
        // 触发 lazy inline parsing
        val inlineChildren = paragraph.children
        if (inlineChildren.isEmpty()) return

        // 找到最后的有效节点（跳过 SoftLineBreak）
        val lastIdx = inlineChildren.lastIndex
        var idx = lastIdx

        // 情况1: 最后是 Text 节点，内容以 {…} 结尾
        while (idx >= 0) {
            val node = inlineChildren[idx]
            if (node is SoftLineBreak) {
                idx--
                continue
            }
            if (node is Text) {
                val text = node.literal
                val match = ATTR_LINE_REGEX.find(text)
                if (match != null) {
                    val beforeAttr = text.substring(0, match.range.first).trimEnd('\n', ' ')
                    if (beforeAttr.isEmpty()) {
                        // 整个 Text 节点都是属性，移除它和前面的 SoftLineBreak
                        paragraph.removeChild(node)
                        // 也移除它前面的 SoftLineBreak（如果有）
                        val newChildren = paragraph.children
                        if (newChildren.isNotEmpty() && newChildren.last() is SoftLineBreak) {
                            paragraph.removeChild(newChildren.last())
                        }
                    } else {
                        node.literal = beforeAttr
                    }
                }
            }
            break
        }
    }

    private fun findPreviousBlock(
        children: List<Node>,
        currentIndex: Int,
        toRemove: Set<Node>,
    ): Node? {
        var j = currentIndex - 1
        while (j >= 0) {
            val candidate = children[j]
            if (candidate in toRemove) { j--; continue }
            if (candidate is BlankLine) { j--; continue }
            return candidate
        }
        return null
    }

    internal fun parseBlockAttributes(content: String): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        val classes = mutableListOf<String>()
        var i = 0

        while (i < content.length) {
            while (i < content.length && (content[i] == ' ' || content[i] == '\t')) i++
            if (i >= content.length) break

            when (content[i]) {
                '.' -> {
                    i++
                    val nameStart = i
                    while (i < content.length && content[i] != ' ' && content[i] != '\t' &&
                        content[i] != '.' && content[i] != '#'
                    ) i++
                    val className = content.substring(nameStart, i)
                    if (className.isNotEmpty()) classes.add(className)
                }
                '#' -> {
                    i++
                    val nameStart = i
                    while (i < content.length && content[i] != ' ' && content[i] != '\t' &&
                        content[i] != '.' && content[i] != '#'
                    ) i++
                    val idName = content.substring(nameStart, i)
                    if (idName.isNotEmpty()) attrs["id"] = idName
                }
                else -> {
                    val keyStart = i
                    while (i < content.length && content[i] != '=' && content[i] != ' ' &&
                        content[i] != '\t'
                    ) i++
                    val key = content.substring(keyStart, i)
                    if (i < content.length && content[i] == '=') {
                        i++
                        val value: String
                        if (i < content.length && (content[i] == '"' || content[i] == '\'')) {
                            val quote = content[i]
                            i++
                            val valStart = i
                            while (i < content.length && content[i] != quote) i++
                            value = content.substring(valStart, i)
                            if (i < content.length) i++
                        } else {
                            val valStart = i
                            while (i < content.length && content[i] != ' ' && content[i] != '\t') i++
                            value = content.substring(valStart, i)
                        }
                        if (key.isNotEmpty()) attrs[key] = value
                    } else {
                        if (key.isNotEmpty()) attrs[key] = ""
                    }
                }
            }
        }

        if (classes.isNotEmpty()) {
            attrs["class"] = classes.joinToString(" ")
        }

        return attrs
    }

    private fun applyAttributes(node: Node, attrs: Map<String, String>) {
        when (node) {
            is Heading -> {
                node.blockAttributes = mergeAttributes(node.blockAttributes, attrs)
                attrs["id"]?.let { if (node.customId == null) node.customId = it }
            }
            is SetextHeading -> {
                node.blockAttributes = mergeAttributes(node.blockAttributes, attrs)
            }
            is Paragraph -> {
                node.blockAttributes = mergeAttributes(node.blockAttributes, attrs)
            }
            is BlockQuote -> {
                node.blockAttributes = mergeAttributes(node.blockAttributes, attrs)
            }
            is ListBlock -> {
                node.blockAttributes = mergeAttributes(node.blockAttributes, attrs)
            }
            is Table -> {
                node.blockAttributes = mergeAttributes(node.blockAttributes, attrs)
            }
            is FencedCodeBlock -> {
                val existing = node.attributes.toMap()
                val merged = mergeAttributes(existing, attrs)
                node.attributes = com.hrm.markdown.parser.core.Attributes(
                    id = merged["id"],
                    classes = merged["class"]?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList(),
                    pairs = merged.filterKeys { it != "id" && it != "class" },
                )
            }
            is CustomContainer -> {
                attrs["class"]?.let { cls ->
                    node.cssClasses = node.cssClasses + cls.split(" ").filter { it.isNotEmpty() }
                }
                attrs["id"]?.let { node.cssId = it }
            }
            else -> { }
        }
    }

    companion object {
        fun mergeAttributes(
            existing: Map<String, String>,
            new: Map<String, String>,
        ): Map<String, String> {
            val merged = existing.toMutableMap()
            for ((k, v) in new) {
                when (k) {
                    "class" -> {
                        val existingClass = merged["class"]
                        merged["class"] = if (existingClass != null) "$existingClass $v" else v
                    }
                    else -> merged[k] = v
                }
            }
            return merged
        }
    }
}
