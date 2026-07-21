package com.hrm.markdown.parser.lint

import com.hrm.markdown.parser.ast.*
import com.hrm.markdown.parser.block.postprocessors.PostProcessor

/**
 * 语法验证后处理器（Linting）。
 *
 * 在 AST 构建完成后遍历整个文档，检测无效语法并生成诊断信息。
 *
 * ## 检查项目
 * - **未闭合围栏代码块**：检测代码块缺少闭合围栏
 * - **重复标题 ID**：检测多个标题生成了相同的 ID
 * - **无效脚注引用**：引用了不存在的脚注定义
 * - **未使用的脚注定义**：定义了但从未被引用
 * - **标题层级不连续**：如 h1 直接跳到 h3
 * - **未闭合数学块**：数学块缺少闭合 $$
 * - **图片缺少 alt 文本**：可访问性检查
 * - **空链接目标**：链接 destination 为空
 *
 * ## 使用方式
 * ```kotlin
 * val parser = MarkdownParser(enableLinting = true)
 * val doc = parser.parse(input)
 * val diagnostics = parser.diagnostics
 * diagnostics.diagnostics.forEach { println(it) }
 * ```
 */
class LintingPostProcessor : PostProcessor {
    override val priority: Int = 900 // 在所有其他后处理器之后执行

    /** 最近一次处理产生的诊断结果。 */
    var result: DiagnosticResult = DiagnosticResult()
        private set

    override fun process(document: Document) {
        result = DiagnosticResult()
        checkHeadingLevelSkips(document)
        checkDuplicateHeadingIds(document)
        checkFootnoteReferences(document)
        checkInlineIssues(document)
        checkWcagAccessibility(document)
        result.sort()
    }

    /**
     * 检查标题层级是否连续。
     * 例如 h1 → h3（跳过了 h2）产生警告。
     */
    private fun checkHeadingLevelSkips(document: Document) {
        val headings = mutableListOf<Pair<Int, Int>>() // (level, line)
        collectHeadings(document, headings)

        if (headings.isEmpty()) return

        var lastLevel = 0
        for ((level, line) in headings) {
            if (lastLevel > 0 && level > lastLevel + 1) {
                result.add(
                    Diagnostic(
                        severity = DiagnosticSeverity.WARNING,
                        code = DiagnosticCode.HEADING_LEVEL_SKIP,
                        message = "标题层级从 h${lastLevel} 跳到 h${level}，跳过了 h${lastLevel + 1}",
                        line = line,
                    )
                )
            }
            lastLevel = level
        }
    }

    private fun collectHeadings(node: Node, out: MutableList<Pair<Int, Int>>) {
        when (node) {
            is Heading -> out.add(node.level to node.lineRange.startLine)
            is SetextHeading -> out.add(node.level to node.lineRange.startLine)
            is ContainerNode -> {
                for (child in node.children) {
                    collectHeadings(child, out)
                }
            }
            else -> {}
        }
    }

    /**
     * 检查重复的标题 ID。
     *
     * 使用标题文本的 slug 进行检测（而非最终 ID），
     * 因为 HeadingIdProcessor 会自动对重复 slug 追加 `-1`、`-2` 后缀。
     * 即使最终 ID 不同，同一 slug 出现多次仍应报告为潜在问题。
     */
    private fun checkDuplicateHeadingIds(document: Document) {
        val slugToLines = mutableMapOf<String, MutableList<Int>>()
        collectHeadingSlugs(document, slugToLines)

        for ((slug, lines) in slugToLines) {
            if (lines.size > 1) {
                for (i in 1 until lines.size) {
                    result.add(
                        Diagnostic(
                            severity = DiagnosticSeverity.WARNING,
                            code = DiagnosticCode.DUPLICATE_HEADING_ID,
                            message = "标题 ID \"${slug}\" 重复（首次出现在第 ${lines[0] + 1} 行）",
                            line = lines[i],
                        )
                    )
                }
            }
        }
    }

    private fun collectHeadingSlugs(node: Node, out: MutableMap<String, MutableList<Int>>) {
        when (node) {
            is Heading -> {
                val text = extractPlainText(node)
                val slug = generateSlug(text)
                out.getOrPut(slug) { mutableListOf() }.add(node.lineRange.startLine)
            }
            is SetextHeading -> {
                val text = extractPlainText(node)
                val slug = generateSlug(text)
                out.getOrPut(slug) { mutableListOf() }.add(node.lineRange.startLine)
            }
            is ContainerNode -> {
                for (child in node.children) {
                    collectHeadingSlugs(child, out)
                }
            }
            else -> {}
        }
    }

    /**
     * 从 AST 节点提取纯文本（与 HeadingIdProcessor 一致）。
     */
    private fun extractPlainText(node: Node): String = when (node) {
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

    /**
     * 生成 URL-safe slug（与 HeadingIdProcessor 一致）。
     */
    private fun generateSlug(text: String): String {
        return text.lowercase()
            .replace(Regex("[^\\w\\u4e00-\\u9fff-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .ifEmpty { "heading" }
    }

    /**
     * 检查脚注引用和定义的一致性。
     * - 引用了不存在的脚注定义 → ERROR
     * - 定义了但从未被引用 → WARNING
     */
    private fun checkFootnoteReferences(document: Document) {
        val referencedLabels = mutableSetOf<String>()
        val referenceLocations = mutableListOf<Pair<String, Int>>() // (label, line)
        collectFootnoteReferences(document, referencedLabels, referenceLocations)

        val definedLabels = document.footnoteDefinitions.keys

        // 检查无效引用
        for ((label, line) in referenceLocations) {
            if (label !in definedLabels) {
                result.add(
                    Diagnostic(
                        severity = DiagnosticSeverity.ERROR,
                        code = DiagnosticCode.INVALID_FOOTNOTE_REFERENCE,
                        message = "脚注引用 [^${label}] 没有对应的定义",
                        line = line,
                    )
                )
            }
        }

        // 检查未使用的定义
        for ((label, def) in document.footnoteDefinitions) {
            if (label !in referencedLabels) {
                result.add(
                    Diagnostic(
                        severity = DiagnosticSeverity.WARNING,
                        code = DiagnosticCode.UNUSED_FOOTNOTE_DEFINITION,
                        message = "脚注定义 [^${label}] 未被引用",
                        line = def.lineRange.startLine,
                    )
                )
            }
        }
    }

    private fun collectFootnoteReferences(
        node: Node,
        labels: MutableSet<String>,
        locations: MutableList<Pair<String, Int>>
    ) {
        when (node) {
            is FootnoteReference -> {
                labels.add(node.label)
                locations.add(node.label to node.lineRange.startLine)
            }
            is ContainerNode -> {
                for (child in node.children) {
                    collectFootnoteReferences(child, labels, locations)
                }
            }
            else -> {}
        }
    }

    /**
     * 检查行内元素问题。
     * - 图片缺少 alt 文本
     * - 空链接目标
     */
    private fun checkInlineIssues(document: Document) {
        checkInlineIssuesRecursive(document)
    }

    private fun checkInlineIssuesRecursive(node: Node) {
        when (node) {
            is Image -> {
                if (node.children.isEmpty()) {
                    result.add(
                        Diagnostic(
                            severity = DiagnosticSeverity.WARNING,
                            code = DiagnosticCode.MISSING_IMAGE_ALT,
                            message = "图片缺少 alt 文本",
                            line = node.lineRange.startLine,
                        )
                    )
                }
                if (node.destination.isBlank()) {
                    result.add(
                        Diagnostic(
                            severity = DiagnosticSeverity.WARNING,
                            code = DiagnosticCode.EMPTY_LINK_DESTINATION,
                            message = "图片链接地址为空",
                            line = node.lineRange.startLine,
                        )
                    )
                }
            }
            is Link -> {
                if (node.destination.isBlank()) {
                    result.add(
                        Diagnostic(
                            severity = DiagnosticSeverity.WARNING,
                            code = DiagnosticCode.EMPTY_LINK_DESTINATION,
                            message = "链接地址为空",
                            line = node.lineRange.startLine,
                        )
                    )
                }
            }
            else -> {}
        }
        if (node is ContainerNode) {
            for (child in node.children) {
                checkInlineIssuesRecursive(child)
            }
        }
    }

    // ────── wcag accessibility checks ──────

    /**
     * runs all wcag accessibility checks on the document.
     */
    private fun checkWcagAccessibility(document: Document) {
        checkWcagRecursive(document)
    }

    private fun checkWcagRecursive(node: Node) {
        when (node) {
            is Link -> {
                // check for empty link text
                val linkText = extractPlainText(node).trim()
                if (linkText.isEmpty()) {
                    result.add(
                        Diagnostic(
                            severity = DiagnosticSeverity.WARNING,
                            code = DiagnosticCode.EMPTY_LINK_TEXT,
                            message = "Link has no text content",
                            line = node.lineRange.startLine,
                        )
                    )
                } else if (isNonDescriptiveLinkText(linkText)) {
                    result.add(
                        Diagnostic(
                            severity = DiagnosticSeverity.WARNING,
                            code = DiagnosticCode.LINK_TEXT_NOT_DESCRIPTIVE,
                            message = "Link text \"$linkText\" is not descriptive",
                            line = node.lineRange.startLine,
                        )
                    )
                }
            }
            is Image -> {
                // check for long alt text (> 125 chars)
                val altText = extractPlainText(node)
                if (altText.length > MAX_ALT_TEXT_LENGTH) {
                    result.add(
                        Diagnostic(
                            severity = DiagnosticSeverity.WARNING,
                            code = DiagnosticCode.LONG_ALT_TEXT,
                            message = "Image alt text is ${altText.length} characters (recommended max $MAX_ALT_TEXT_LENGTH)",
                            line = node.lineRange.startLine,
                        )
                    )
                }
            }
            is FencedCodeBlock -> {
                // check for missing language
                if (node.language.isEmpty()) {
                    result.add(
                        Diagnostic(
                            severity = DiagnosticSeverity.INFO,
                            code = DiagnosticCode.MISSING_LANG_IN_CODE_BLOCK,
                            message = "Fenced code block has no language specified",
                            line = node.lineRange.startLine,
                        )
                    )
                }
            }
            is Table -> {
                // check for empty header cells
                checkTableHeaders(node)
            }
            else -> {}
        }
        if (node is ContainerNode) {
            for (child in node.children) {
                checkWcagRecursive(child)
            }
        }
    }

    /**
     * checks if table header cells are empty.
     */
    private fun checkTableHeaders(table: Table) {
        val head = table.children.filterIsInstance<TableHead>().firstOrNull() ?: return
        val headerRow = head.children.filterIsInstance<TableRow>().firstOrNull() ?: return
        val headerCells = headerRow.children.filterIsInstance<TableCell>()
        val allEmpty = headerCells.all { extractPlainText(it).isBlank() }
        if (allEmpty && headerCells.isNotEmpty()) {
            result.add(
                Diagnostic(
                    severity = DiagnosticSeverity.WARNING,
                    code = DiagnosticCode.TABLE_MISSING_HEADER,
                    message = "Table has empty header cells",
                    line = table.lineRange.startLine,
                )
            )
        }
    }

    /**
     * checks if link text is generic/non-descriptive.
     */
    private fun isNonDescriptiveLinkText(text: String): Boolean {
        val normalized = text.lowercase().trim()
        return normalized in NON_DESCRIPTIVE_LINK_TEXTS
    }

    companion object {
        const val MAX_ALT_TEXT_LENGTH = 125

        private val NON_DESCRIPTIVE_LINK_TEXTS = setOf(
            "click here",
            "here",
            "read more",
            "more",
            "link",
            "this",
            "this link",
            "go",
            "go here",
        )
    }
}
