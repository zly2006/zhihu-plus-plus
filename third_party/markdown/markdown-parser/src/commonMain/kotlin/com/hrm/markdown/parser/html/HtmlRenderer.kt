package com.hrm.markdown.parser.html

import com.hrm.markdown.parser.ast.*
import com.hrm.markdown.parser.flavour.ExtendedFlavour
import com.hrm.markdown.parser.flavour.MarkdownFlavour

/**
 * 将 Markdown AST 节点树输出为标准 HTML 的渲染器。
 *
 * 实现 [NodeVisitor] 接口，递归遍历 AST 生成 HTML 字符串。
 * 支持服务端 SSR 和 HTML 导出场景。
 *
 * ## 用法
 *
 * ```kotlin
 * val parser = MarkdownParser()
 * val document = parser.parse("# Hello\n\nWorld")
 * val html = HtmlRenderer.render(document)
 * // <h1 id="hello">Hello</h1>\n<p>World</p>\n
 * ```
 *
 * ## 配置
 *
 * ```kotlin
 * val renderer = HtmlRenderer(
 *     softBreak = "<br />\n",  // 软换行输出 <br>
 *     hardBreak = "<br />\n",  // 硬换行输出 <br>
 *     escapeHtml = true,       // 转义原始 HTML
 *     xhtml = true,            // XHTML 自闭合标签
 * )
 * val html = renderer.render(document)
 * ```
 *
 * 参考 JetBrains Markdown 的 `HtmlGenerator(src, parsedTree, flavour).generateHtml()` API 设计。
 */
class HtmlRenderer(
    /** 软换行输出内容，默认 "\n" */
    val softBreak: String = "\n",
    /** 硬换行标签 */
    val hardBreak: String = "<br />\n",
    /** 是否转义原始 HTML 内容 */
    val escapeHtml: Boolean = false,
    /** 是否使用 XHTML 风格自闭合标签 */
    val xhtml: Boolean = true,
    /** 自定义块级 directive HTML fallback，返回 null 则走默认输出 */
    val directiveBlockFallback: ((DirectiveBlock) -> String?)? = null,
    /** 自定义行内 directive HTML fallback，返回 null 则走默认输出 */
    val directiveInlineFallback: ((DirectiveInline) -> String?)? = null,
) : NodeVisitor<Unit> {

    private val sb = StringBuilder()

    /**
     * 渲染 Document AST 为 HTML 字符串。
     */
    fun render(document: Document): String {
        sb.clear()
        document.accept(this)
        return sb.toString()
    }

    // ─────────────── 辅助方法 ───────────────

    private fun tag(name: String, attrs: Map<String, String?> = emptyMap(), selfClosing: Boolean = false) {
        sb.append('<').append(name)
        for ((key, value) in attrs) {
            if (value != null) {
                sb.append(' ').append(key).append("=\"").append(escapeAttr(value)).append('"')
            }
        }
        if (selfClosing && xhtml) {
            sb.append(" /")
        }
        sb.append('>')
    }

    private fun closeTag(name: String) {
        sb.append("</").append(name).append('>')
    }

    private fun escape(text: String): String = buildString(text.length) {
        for (ch in text) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                else -> append(ch)
            }
        }
    }

    private fun escapeAttr(text: String): String = buildString(text.length) {
        for (ch in text) {
            when (ch) {
                '&' -> append("&amp;")
                '"' -> append("&quot;")
                else -> append(ch)
            }
        }
    }

    private fun visitChildren(node: ContainerNode) {
        for (child in node.children) {
            child.accept(this)
        }
    }

    /**
     * 构建 HTML 属性映射（id, class, 及自定义 key-value）。
     */
    private fun buildAttrs(
        baseAttrs: Map<String, String?> = emptyMap(),
        attributes: Map<String, String> = emptyMap(),
    ): Map<String, String?> {
        if (attributes.isEmpty()) return baseAttrs
        val merged = LinkedHashMap<String, String?>(baseAttrs)
        for ((k, v) in attributes) {
            when (k) {
                "class" -> {
                    val existing = merged["class"]
                    merged["class"] = if (existing != null) "$existing $v" else v
                }
                else -> merged[k] = v
            }
        }
        return merged
    }

    // ─────────────── 块级节点 ───────────────

    override fun visitDocument(node: Document) {
        visitChildren(node)
    }

    override fun visitHeading(node: Heading) {
        val tagName = "h${node.level}"
        val baseAttrs = buildMap<String, String?> {
            node.id?.let { put("id", it) }
        }
        val attrs = buildAttrs(baseAttrs, node.blockAttributes)
        tag(tagName, attrs)
        visitChildren(node)
        closeTag(tagName)
        sb.append('\n')
    }

    override fun visitSetextHeading(node: SetextHeading) {
        val tagName = "h${node.level}"
        val baseAttrs = buildMap<String, String?> {
            node.id?.let { put("id", it) }
        }
        val attrs = buildAttrs(baseAttrs, node.blockAttributes)
        tag(tagName, attrs)
        visitChildren(node)
        closeTag(tagName)
        sb.append('\n')
    }

    override fun visitParagraph(node: Paragraph) {
        // tight list items: render content without <p> tags and without trailing newline
        val parent = node.parent
        if (parent is ListItem) {
            val grandParent = parent.parent
            if (grandParent is ListBlock && grandParent.tight) {
                visitChildren(node)
                val siblings = parent.children
                if (siblings.last() !== node) {
                    sb.append('\n')
                }
                return
            }
        }
        val attrs = buildAttrs(attributes = node.blockAttributes)
        tag("p", attrs)
        visitChildren(node)
        closeTag("p")
        sb.append('\n')
    }

    override fun visitThematicBreak(node: ThematicBreak) {
        tag("hr", selfClosing = true)
        sb.append('\n')
    }

    override fun visitFencedCodeBlock(node: FencedCodeBlock) {
        val codeAttrs = mutableMapOf<String, String?>()
        if (node.language.isNotEmpty()) {
            codeAttrs["class"] = "language-${node.language}"
        }
        // 合并自定义属性
        if (!node.attributes.isEmpty) {
            val attrMap = node.attributes.toMap()
            for ((k, v) in attrMap) {
                when (k) {
                    "class" -> {
                        val existing = codeAttrs["class"]
                        codeAttrs["class"] = if (existing != null) "$existing $v" else v
                    }
                    else -> codeAttrs[k] = v
                }
            }
        }
        // build pre-level attributes for code block enhancements
        val preAttrs = mutableMapOf<String, String?>()
        if (node.highlightLines.isNotEmpty()) {
            preAttrs["data-hl-lines"] = node.highlightLines.joinToString(" ") { range ->
                if (range.first == range.last) range.first.toString()
                else "${range.first}-${range.last}"
            }
        }
        if (node.showLineNumbers) {
            preAttrs["data-linenums"] = "true"
        }
        if (node.startLineNumber != 1) {
            preAttrs["data-startline"] = node.startLineNumber.toString()
        }
        tag("pre", preAttrs)
        tag("code", codeAttrs)
        sb.append(escape(node.literal))
        closeTag("code")
        closeTag("pre")
        sb.append('\n')
    }

    override fun visitIndentedCodeBlock(node: IndentedCodeBlock) {
        tag("pre")
        tag("code")
        sb.append(escape(node.literal))
        closeTag("code")
        closeTag("pre")
        sb.append('\n')
    }

    override fun visitBlockQuote(node: BlockQuote) {
        val attrs = buildAttrs(attributes = node.blockAttributes)
        tag("blockquote", attrs)
        sb.append('\n')
        visitChildren(node)
        closeTag("blockquote")
        sb.append('\n')
    }

    override fun visitListBlock(node: ListBlock) {
        val baseAttrs = buildMap<String, String?> {
            if (node.ordered && node.startNumber != 1) {
                put("start", node.startNumber.toString())
            }
        }
        val attrs = buildAttrs(baseAttrs, node.blockAttributes)
        if (node.ordered) {
            tag("ol", attrs)
        } else {
            tag("ul", attrs)
        }
        sb.append('\n')
        visitChildren(node)
        if (node.ordered) closeTag("ol") else closeTag("ul")
        sb.append('\n')
    }

    override fun visitListItem(node: ListItem) {
        val parentList = node.parent as? ListBlock
        val isLoose = parentList != null && !parentList.tight
        val fc = node.children.firstOrNull()
        // add newline after <li> if loose (with children) or tight with non-paragraph first child
        // empty items never get newline after <li>
        val hasChildren = fc != null
        val nlAfterLi = (isLoose && hasChildren) || (parentList != null && parentList.tight && fc != null && fc !is Paragraph)
        if (node.taskListItem) {
            tag("li")
            if (nlAfterLi) sb.append('\n')
            val checked = if (node.checked) " checked=\"\"" else ""
            val disabled = " disabled=\"\""
            sb.append("<input type=\"checkbox\"$checked$disabled")
            if (xhtml) sb.append(" /")
            sb.append("> ")
            visitChildren(node)
            closeTag("li")
        } else {
            tag("li")
            if (nlAfterLi) sb.append('\n')
            visitChildren(node)
            closeTag("li")
        }
        sb.append('\n')
    }

    override fun visitHtmlBlock(node: HtmlBlock) {
        if (escapeHtml) {
            sb.append(escape(node.literal))
        } else {
            sb.append(node.literal)
        }
        // only add newline if literal doesn't already end with one
        if (!node.literal.endsWith('\n')) {
            sb.append('\n')
        }
    }

    override fun visitLinkReferenceDefinition(node: LinkReferenceDefinition) {
        // 不输出到 HTML
    }

    override fun visitTable(node: Table) {
        val attrs = buildAttrs(attributes = node.blockAttributes)
        tag("table", attrs)
        sb.append('\n')
        visitChildren(node)
        closeTag("table")
        sb.append('\n')
    }

    override fun visitTableHead(node: TableHead) {
        tag("thead")
        sb.append('\n')
        visitChildren(node)
        closeTag("thead")
        sb.append('\n')
    }

    override fun visitTableBody(node: TableBody) {
        tag("tbody")
        sb.append('\n')
        visitChildren(node)
        closeTag("tbody")
        sb.append('\n')
    }

    override fun visitTableRow(node: TableRow) {
        tag("tr")
        sb.append('\n')
        visitChildren(node)
        closeTag("tr")
        sb.append('\n')
    }

    override fun visitTableCell(node: TableCell) {
        val tagName = if (node.isHeader) "th" else "td"
        val attrs = when (node.alignment) {
            Table.Alignment.LEFT -> mapOf<String, String?>("align" to "left")
            Table.Alignment.CENTER -> mapOf<String, String?>("align" to "center")
            Table.Alignment.RIGHT -> mapOf<String, String?>("align" to "right")
            Table.Alignment.NONE -> emptyMap()
        }
        tag(tagName, attrs)
        visitChildren(node)
        closeTag(tagName)
        sb.append('\n')
    }

    override fun visitFootnoteDefinition(node: FootnoteDefinition) {
        val attrs = mapOf<String, String?>("id" to "fn-${node.label}")
        tag("div", attrs)
        sb.append('\n')
        tag("p")
        sb.append("[${node.index}] ")
        visitChildren(node)
        closeTag("p")
        sb.append('\n')
        closeTag("div")
        sb.append('\n')
    }

    override fun visitMathBlock(node: MathBlock) {
        val attrs = mapOf<String, String?>("class" to "math-display")
        tag("div", attrs)
        sb.append("\\[")
        sb.append(escape(node.literal))
        sb.append("\\]")
        closeTag("div")
        sb.append('\n')
    }

    override fun visitDefinitionList(node: DefinitionList) {
        tag("dl")
        sb.append('\n')
        visitChildren(node)
        closeTag("dl")
        sb.append('\n')
    }

    override fun visitDefinitionTerm(node: DefinitionTerm) {
        tag("dt")
        visitChildren(node)
        closeTag("dt")
        sb.append('\n')
    }

    override fun visitDefinitionDescription(node: DefinitionDescription) {
        tag("dd")
        visitChildren(node)
        closeTag("dd")
        sb.append('\n')
    }

    override fun visitAdmonition(node: Admonition) {
        val attrs = mapOf<String, String?>("class" to "admonition ${node.type.lowercase()}")
        tag("div", attrs)
        sb.append('\n')
        if (node.title.isNotEmpty()) {
            tag("p", mapOf("class" to "admonition-title"))
            sb.append(escape(node.title))
            closeTag("p")
            sb.append('\n')
        }
        visitChildren(node)
        closeTag("div")
        sb.append('\n')
    }

    override fun visitFrontMatter(node: FrontMatter) {
        // 不输出到 HTML（元数据）
    }

    override fun visitNativeBlock(node: NativeBlock) {
        // NativeBlock 仅供 Compose 渲染器消费，HTML 导出时跳过。
    }

    override fun visitBlankLine(node: BlankLine) {
        // 不输出
    }

    override fun visitTocPlaceholder(node: TocPlaceholder) {
        val attrs = mapOf<String, String?>("class" to "table-of-contents")
        tag("nav", attrs)
        sb.append('\n')
        tag("p")
        sb.append("[TOC]")
        closeTag("p")
        sb.append('\n')
        closeTag("nav")
        sb.append('\n')
    }

    override fun visitAbbreviationDefinition(node: AbbreviationDefinition) {
        // 不直接输出（缩写在行内 Abbreviation 节点中渲染）
    }

    override fun visitCustomContainer(node: CustomContainer) {
        val classes = buildList {
            add("custom-container")
            if (node.type.isNotEmpty()) add(node.type)
            addAll(node.cssClasses)
        }
        val attrs = mutableMapOf<String, String?>(
            "class" to classes.joinToString(" ")
        )
        node.cssId?.let { attrs["id"] = it }
        tag("div", attrs)
        sb.append('\n')
        if (node.title.isNotEmpty()) {
            tag("p", mapOf("class" to "custom-container-title"))
            sb.append(escape(node.title))
            closeTag("p")
            sb.append('\n')
        }
        visitChildren(node)
        closeTag("div")
        sb.append('\n')
    }

    override fun visitDiagramBlock(node: DiagramBlock) {
        val attrs = mapOf<String, String?>("class" to "diagram ${node.diagramType}")
        tag("div", attrs)
        sb.append('\n')
        tag("pre")
        tag("code", mapOf("class" to "language-${node.diagramType}"))
        sb.append(escape(node.literal))
        closeTag("code")
        closeTag("pre")
        sb.append('\n')
        closeTag("div")
        sb.append('\n')
    }

    override fun visitColumnsLayout(node: ColumnsLayout) {
        tag("div", mapOf("class" to "columns-layout"))
        sb.append('\n')
        visitChildren(node)
        closeTag("div")
        sb.append('\n')
    }

    override fun visitColumnItem(node: ColumnItem) {
        val attrs = mutableMapOf<String, String?>("class" to "column-item")
        if (node.width.isNotEmpty()) {
            attrs["style"] = "width:${node.width}"
        }
        tag("div", attrs)
        sb.append('\n')
        visitChildren(node)
        closeTag("div")
        sb.append('\n')
    }

    override fun visitPageBreak(node: PageBreak) {
        tag("div", mapOf("class" to "page-break", "style" to "page-break-after:always"))
        closeTag("div")
        sb.append('\n')
    }

    override fun visitDirectiveBlock(node: DirectiveBlock) {
        val custom = directiveBlockFallback?.invoke(node)
        if (custom != null) {
            sb.append(custom)
            if (!custom.endsWith('\n')) {
                sb.append('\n')
            }
            return
        }
        val argsStr = node.args.entries.joinToString(" ") { "${it.key}=${it.value}" }
        val attrs = mutableMapOf<String, String?>(
            "data-directive" to node.tagName,
        )
        if (argsStr.isNotEmpty()) {
            attrs["data-args"] = argsStr
        }
        tag("div", attrs)
        sb.append('\n')
        visitChildren(node)
        closeTag("div")
        sb.append('\n')
    }

    // ─────────────── 行内节点 ───────────────

    override fun visitText(node: Text) {
        sb.append(escape(node.literal))
    }

    override fun visitSoftLineBreak(node: SoftLineBreak) {
        sb.append(softBreak)
    }

    override fun visitHardLineBreak(node: HardLineBreak) {
        sb.append(hardBreak)
    }

    override fun visitEmphasis(node: Emphasis) {
        tag("em")
        visitChildren(node)
        closeTag("em")
    }

    override fun visitStrongEmphasis(node: StrongEmphasis) {
        tag("strong")
        visitChildren(node)
        closeTag("strong")
    }

    override fun visitStrikethrough(node: Strikethrough) {
        tag("del")
        visitChildren(node)
        closeTag("del")
    }

    override fun visitInlineCode(node: InlineCode) {
        tag("code")
        sb.append(escape(node.literal))
        closeTag("code")
    }

    override fun visitLink(node: Link) {
        val attrs = buildAttrs(
            baseAttrs = buildMap {
                put("href", node.destination)
                node.title?.let { put("title", it) }
            },
            attributes = node.attributes,
        )
        tag("a", attrs)
        visitChildren(node)
        closeTag("a")
    }

    override fun visitImage(node: Image) {
        val baseAttrs = buildMap<String, String?> {
            put("src", node.destination)
            // alt 文本从子节点提取
            val alt = extractAltText(node)
            put("alt", alt)
            node.title?.let { put("title", it) }
            node.imageWidth?.let { put("width", it.toString()) }
            node.imageHeight?.let { put("height", it.toString()) }
        }
        val attrs = buildAttrs(baseAttrs, node.attributes)
        tag("img", attrs, selfClosing = true)
    }

    private fun extractAltText(node: ContainerNode): String = buildString {
        for (child in node.children) {
            when (child) {
                is Text -> append(child.literal)
                is InlineCode -> append(child.literal)
                is SoftLineBreak -> append(' ')
                is HardLineBreak -> append(' ')
                is ContainerNode -> append(extractAltText(child))
                else -> {}
            }
        }
    }

    override fun visitAutolink(node: Autolink) {
        if (node.isEmail) {
            tag("a", mapOf("href" to "mailto:${node.destination}"))
        } else {
            tag("a", mapOf("href" to node.destination))
        }
        val display = node.rawText.ifEmpty { node.destination }
        sb.append(escape(display))
        closeTag("a")
    }

    override fun visitInlineHtml(node: InlineHtml) {
        if (escapeHtml) {
            sb.append(escape(node.literal))
        } else {
            sb.append(node.literal)
        }
    }

    override fun visitHtmlEntity(node: HtmlEntity) {
        // output the resolved unicode, falling back to the original entity
        val text = node.resolved.ifEmpty { node.literal }
        sb.append(escape(text))
    }

    override fun visitEscapedChar(node: EscapedChar) {
        sb.append(escape(node.literal))
    }

    override fun visitFootnoteReference(node: FootnoteReference) {
        tag("sup")
        tag("a", mapOf("href" to "#fn-${node.label}", "class" to "footnote-ref"))
        sb.append("[${node.index}]")
        closeTag("a")
        closeTag("sup")
    }

    override fun visitInlineMath(node: InlineMath) {
        tag("span", mapOf("class" to "math-inline"))
        sb.append("\\(")
        sb.append(escape(node.literal))
        sb.append("\\)")
        closeTag("span")
    }

    override fun visitHighlight(node: Highlight) {
        tag("mark")
        visitChildren(node)
        closeTag("mark")
    }

    override fun visitSuperscript(node: Superscript) {
        tag("sup")
        visitChildren(node)
        closeTag("sup")
    }

    override fun visitSubscript(node: Subscript) {
        tag("sub")
        visitChildren(node)
        closeTag("sub")
    }

    override fun visitInsertedText(node: InsertedText) {
        tag("ins")
        visitChildren(node)
        closeTag("ins")
    }

    override fun visitEmoji(node: Emoji) {
        val unicode = node.unicode
        if (unicode != null) {
            sb.append(unicode)
        } else {
            sb.append(":${node.shortcode}:")
        }
    }

    override fun visitStyledText(node: StyledText) {
        val attrs = buildAttrs(attributes = node.attributes)
        tag("span", attrs)
        visitChildren(node)
        closeTag("span")
    }

    override fun visitAbbreviation(node: Abbreviation) {
        tag("abbr", mapOf("title" to node.fullText))
        sb.append(escape(node.abbreviation))
        closeTag("abbr")
    }

    override fun visitKeyboardInput(node: KeyboardInput) {
        tag("kbd")
        sb.append(escape(node.literal))
        closeTag("kbd")
    }

    override fun visitDirectiveInline(node: DirectiveInline) {
        val custom = directiveInlineFallback?.invoke(node)
        if (custom != null) {
            sb.append(custom)
            return
        }
        val argsStr = node.args.entries.joinToString(" ") { "${it.key}=${it.value}" }
        val attrs = mutableMapOf<String, String?>(
            "data-directive" to node.tagName,
        )
        if (argsStr.isNotEmpty()) {
            attrs["data-args"] = argsStr
        }
        tag("span", attrs)
        closeTag("span")
    }

    override fun visitTabBlock(node: TabBlock) {
        tag("div", mapOf("class" to "tab-block"))
        sb.append('\n')
        visitChildren(node)
        closeTag("div")
        sb.append('\n')
    }

    override fun visitTabItem(node: TabItem) {
        tag("div", mapOf("class" to "tab-item", "data-tab-title" to node.title))
        sb.append('\n')
        visitChildren(node)
        closeTag("div")
        sb.append('\n')
    }

    override fun visitBibliographyDefinition(node: BibliographyDefinition) {
        tag("div", mapOf("class" to "bibliography"))
        sb.append('\n')
        tag("h4")
        sb.append("References")
        closeTag("h4")
        sb.append('\n')
        tag("ol", mapOf("class" to "bibliography-list"))
        sb.append('\n')
        for ((key, entry) in node.entries) {
            tag("li", mapOf("id" to "bib-$key"))
            sb.append(escape(entry.content))
            closeTag("li")
            sb.append('\n')
        }
        closeTag("ol")
        sb.append('\n')
        closeTag("div")
        sb.append('\n')
    }

    override fun visitCitationReference(node: CitationReference) {
        tag("sup")
        tag("a", mapOf("href" to "#bib-${node.key}", "class" to "citation-ref"))
        sb.append("[${escape(node.key)}]")
        closeTag("a")
        closeTag("sup")
    }

    override fun visitSpoiler(node: Spoiler) {
        tag("span", mapOf("class" to "spoiler"))
        visitChildren(node)
        closeTag("span")
    }

    override fun visitWikiLink(node: WikiLink) {
        val display = node.label ?: node.target
        tag("a", mapOf("href" to node.target, "class" to "wikilink"))
        sb.append(escape(display))
        closeTag("a")
    }

    override fun visitRubyText(node: RubyText) {
        tag("ruby")
        sb.append(escape(node.base))
        tag("rp")
        sb.append("(")
        closeTag("rp")
        tag("rt")
        sb.append(escape(node.annotation))
        closeTag("rt")
        tag("rp")
        sb.append(")")
        closeTag("rp")
        closeTag("ruby")
    }

    override fun visitFigure(node: Figure) {
        tag("figure")
        sb.append('\n')
        tag("img", mapOf(
            "src" to node.imageUrl,
            "alt" to node.caption,
        ), selfClosing = true)
        sb.append('\n')
        if (node.caption.isNotEmpty()) {
            tag("figcaption")
            sb.append(escape(node.caption))
            closeTag("figcaption")
            sb.append('\n')
        }
        closeTag("figure")
        sb.append('\n')
    }

    companion object {
        /**
         * 便捷方法：将 Document 渲染为 HTML 字符串。
         *
         * ```kotlin
         * val html = HtmlRenderer.render(document)
         * ```
         */
        fun render(document: Document): String {
            return HtmlRenderer().render(document)
        }

        /**
         * 便捷方法：将 Markdown 文本解析并渲染为 HTML。
         *
         * ```kotlin
         * val html = HtmlRenderer.renderMarkdown("# Hello")
         * // <h1 id="hello">Hello</h1>\n
         * ```
         */
        fun renderMarkdown(
            markdown: String,
            softBreak: String = "\n",
            escapeHtml: Boolean = false,
            flavour: MarkdownFlavour = ExtendedFlavour,
        ): String {
            val parser = com.hrm.markdown.parser.MarkdownParser(flavour)
            val document = parser.parse(markdown)
            return HtmlRenderer(softBreak = softBreak, escapeHtml = escapeHtml).render(document)
        }
    }
}
