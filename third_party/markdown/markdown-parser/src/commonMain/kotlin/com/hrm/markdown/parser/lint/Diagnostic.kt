package com.hrm.markdown.parser.lint

import com.hrm.markdown.parser.SourcePosition

/**
 * 诊断信息的严重级别。
 */
enum class DiagnosticSeverity {
    /** 错误：严重的语法问题（如未闭合代码块）。 */
    ERROR,
    /** 警告：值得关注但不影响解析的问题（如标题层级跳跃）。 */
    WARNING,
    /** 信息：可选的改进建议。 */
    INFO,
}

/**
 * 诊断信息的规则代码。
 *
 * 每条规则代码唯一标识一种检查类型，便于按类别过滤或禁用。
 */
enum class DiagnosticCode(val description: String) {
    /** 未闭合的围栏代码块（缺少闭合 ``` 或 ~~~）。 */
    UNCLOSED_FENCED_CODE_BLOCK("Unclosed fenced code block"),

    /** 重复的标题 ID。 */
    DUPLICATE_HEADING_ID("Duplicate heading ID"),

    /** 无效的脚注引用（引用了不存在的脚注定义）。 */
    INVALID_FOOTNOTE_REFERENCE("Invalid footnote reference"),

    /** 未使用的脚注定义（定义了但从未被引用）。 */
    UNUSED_FOOTNOTE_DEFINITION("Unused footnote definition"),

    /** 标题层级不连续（如 h1 直接跳到 h3）。 */
    HEADING_LEVEL_SKIP("Heading level skip"),

    /** 未闭合的数学块（缺少闭合 $$）。 */
    UNCLOSED_MATH_BLOCK("Unclosed math block"),

    /** 图片缺少 alt 文本（可访问性问题）。 */
    MISSING_IMAGE_ALT("Image missing alt text"),

    /** 空链接目标。 */
    EMPTY_LINK_DESTINATION("Empty link destination"),

    /** link with no text content, e.g. [](url). */
    EMPTY_LINK_TEXT("Empty link text"),

    /** link text uses generic non-descriptive phrases like "click here". */
    LINK_TEXT_NOT_DESCRIPTIVE("Link text not descriptive"),

    /** fenced code block without language specified. */
    MISSING_LANG_IN_CODE_BLOCK("Missing language in code block"),

    /** table with empty header cells. */
    TABLE_MISSING_HEADER("Table missing header content"),

    /** image alt text exceeding 125 characters. */
    LONG_ALT_TEXT("Long alt text"),
}

/**
 * 单条诊断信息。
 *
 * @property severity 严重级别
 * @property code 规则代码
 * @property message 人类可读的诊断消息
 * @property line 所在行号（0-based）
 * @property column 所在列号（0-based）
 * @property position 精确的源文本位置（可选，块级诊断可能没有精确列）
 */
data class Diagnostic(
    val severity: DiagnosticSeverity,
    val code: DiagnosticCode,
    val message: String,
    val line: Int,
    val column: Int = 0,
    val position: SourcePosition? = null,
)

/**
 * 诊断结果集合。
 *
 * 附加在 [com.hrm.markdown.parser.ast.Document] 上，
 * 通过 [com.hrm.markdown.parser.MarkdownParser.diagnostics] 访问。
 */
class DiagnosticResult {
    private val _diagnostics = mutableListOf<Diagnostic>()

    /** 所有诊断信息（按行号排序）。 */
    val diagnostics: List<Diagnostic> get() = _diagnostics

    /** 错误级别的诊断数量。 */
    val errorCount: Int get() = _diagnostics.count { it.severity == DiagnosticSeverity.ERROR }

    /** 警告级别的诊断数量。 */
    val warningCount: Int get() = _diagnostics.count { it.severity == DiagnosticSeverity.WARNING }

    /** 是否有任何诊断信息。 */
    val hasIssues: Boolean get() = _diagnostics.isNotEmpty()

    /** 是否有错误级别的诊断。 */
    val hasErrors: Boolean get() = errorCount > 0

    fun add(diagnostic: Diagnostic) {
        _diagnostics.add(diagnostic)
    }

    fun addAll(diagnostics: Collection<Diagnostic>) {
        _diagnostics.addAll(diagnostics)
    }

    /** 按行号排序。 */
    fun sort() {
        _diagnostics.sortWith(compareBy({ it.line }, { it.column }))
    }

    /** 按严重级别过滤。 */
    fun filter(severity: DiagnosticSeverity): List<Diagnostic> =
        _diagnostics.filter { it.severity == severity }

    /** 按规则代码过滤。 */
    fun filter(code: DiagnosticCode): List<Diagnostic> =
        _diagnostics.filter { it.code == code }

    override fun toString(): String {
        if (_diagnostics.isEmpty()) return "No issues found."
        return buildString {
            appendLine("Found ${_diagnostics.size} issue(s):")
            for (d in _diagnostics) {
                appendLine("  [${d.severity}] Line ${d.line + 1}: ${d.message} (${d.code})")
            }
        }
    }
}
