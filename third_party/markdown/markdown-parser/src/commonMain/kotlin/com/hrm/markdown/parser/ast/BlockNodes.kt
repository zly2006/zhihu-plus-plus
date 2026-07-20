package com.hrm.markdown.parser.ast

import androidx.compose.runtime.Composable
import com.hrm.markdown.parser.core.Attributes
import com.hrm.markdown.parser.lint.DiagnosticResult

private var nextSyntheticStableKey = -1

private fun allocateSyntheticStableKey(): Int = nextSyntheticStableKey--

/**
 * 文档的根节点。
 */
class Document : ContainerNode() {
    /** 解析过程中收集的链接引用定义。 */
    val linkDefinitions: MutableMap<String, LinkReferenceDefinition> = mutableMapOf()

    /** 解析过程中收集的脚注定义。 */
    val footnoteDefinitions: MutableMap<String, FootnoteDefinition> = mutableMapOf()

    /** 解析过程中收集的缩写定义。 */
    val abbreviationDefinitions: MutableMap<String, AbbreviationDefinition> = mutableMapOf()

    /**
     * 语法验证/Linting 诊断结果。
     *
     * 当 [com.hrm.markdown.parser.MarkdownParser] 启用 `enableLinting = true` 时，
     * 解析完成后此字段包含检测到的语法问题（错误、警告、建议）。
     * 默认为空结果（无诊断信息）。
     */
    var diagnostics: DiagnosticResult = DiagnosticResult()

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDocument(this)
}

// ─────────────── 块级节点 ───────────────

/**
 * ATX 标题：`# heading` 到 `###### heading`。
 */
class Heading(
    var level: Int,
) : ContainerNode() {
    /** 来自 `{#id}` 语法的可选自定义 ID。 */
    var customId: String? = null

    /** 解析阶段捕获的原始内容（已去除块级标记如块引用前缀），供行内解析使用。 */
    var rawContent: String? = null

    /** 自动生成的标题 ID（基于标题文本的 slug）。若有 customId 则优先使用。 */
    var autoId: String? = null

    /** 获取最终使用的标题 ID（customId 优先，否则 autoId）。 */
    val id: String? get() = customId ?: autoId

    /** 块级属性 `{.class #id key=value}`（由 BlockAttributeProcessor 后处理设置）。 */
    var blockAttributes: Map<String, String> = emptyMap()

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitHeading(this)
}

/**
 * Setext 标题：`heading\n===` 或 `heading\n---`。
 */
class SetextHeading(
    var level: Int,
) : ContainerNode() {
    /** 自动生成的标题 ID（基于标题文本的 slug）。 */
    var autoId: String? = null

    /** parsed heading content (stripped of block-level markers like list prefixes) */
    var rawContent: String? = null

    /** 获取最终使用的标题 ID。 */
    val id: String? get() = autoId

    /** 块级属性 `{.class #id key=value}`（由 BlockAttributeProcessor 后处理设置）。 */
    var blockAttributes: Map<String, String> = emptyMap()

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitSetextHeading(this)
}

/**
 * 段落块：连续的非空行文本。
 */
class Paragraph : ContainerNode() {
    /** 解析阶段捕获的原始内容（已去除块级标记），供行内解析使用。 */
    var rawContent: String? = null

    /** 块级属性 `{.class #id key=value}`（由 BlockAttributeProcessor 后处理设置）。 */
    var blockAttributes: Map<String, String> = emptyMap()

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitParagraph(this)
}

/**
 * 主题分隔线（水平线）：`---`、`***` 或 `___`。
 */
class ThematicBreak(
    var char: Char = '-'
) : LeafNode() {
    override val literal: String get() = ""
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitThematicBreak(this)
}

/**
 * 围栏代码块：``` 或 ~~~。
 */
class FencedCodeBlock(
    var info: String = "",
    var language: String = "",
    var fenceChar: Char = '`',
    var fenceLength: Int = 3,
    var fenceIndent: Int = 0,
    override var literal: String = "",
    var attributes: Attributes = Attributes(),
) : LeafNode() {
    /** highlighted line ranges parsed from `hl_lines` attribute, e.g. "1 3-5" -> [1..1, 3..5] */
    var highlightLines: List<IntRange> = emptyList()

    /** whether to show line numbers, parsed from `linenums` attribute */
    var showLineNumbers: Boolean = true

    /** starting line number, parsed from `startline` attribute (default 1) */
    var startLineNumber: Int = 1

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitFencedCodeBlock(this)
}

/**
 * 缩进代码块：4 个空格或 1 个制表符缩进。
 */
class IndentedCodeBlock(
    override var literal: String = ""
) : LeafNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitIndentedCodeBlock(this)
}

/**
 * 块引用：以 `>` 为前缀的行。
 */
class BlockQuote : ContainerNode() {
    /** 块级属性 `{.class #id key=value}`（由 BlockAttributeProcessor 后处理设置）。 */
    var blockAttributes: Map<String, String> = emptyMap()

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBlockQuote(this)
}

/**
 * 列表（有序或无序）。
 */
class ListBlock(
    var ordered: Boolean = false,
    var startNumber: Int = 1,
    var bulletChar: Char = '-',
    var delimiter: Char = '.',
    var tight: Boolean = true
) : ContainerNode() {
    /** 块级属性 `{.class #id key=value}`（由 BlockAttributeProcessor 后处理设置）。 */
    var blockAttributes: Map<String, String> = emptyMap()

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitListBlock(this)
}

/**
 * 列表中的单个列表项。
 */
class ListItem(
    var markerIndent: Int = 0,
    var contentIndent: Int = 0
) : ContainerNode() {
    /** 用于任务列表。 */
    var taskListItem: Boolean = false
    var checked: Boolean = false
    /** tracks whether a blank line was seen between block children within this item */
    var containsBlankLine: Boolean = false

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitListItem(this)
}

/**
 * HTML 块（CommonMark 规范中的类型 1-7）。
 */
class HtmlBlock(
    var htmlType: Int = 7,
    override var literal: String = ""
) : LeafNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitHtmlBlock(this)
}

/**
 * 链接引用定义：`[label]: destination "title"`。
 */
class LinkReferenceDefinition(
    var label: String = "",
    var destination: String = "",
    var title: String? = null
) : LeafNode() {
    override val literal: String get() = ""
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitLinkReferenceDefinition(this)
}

/**
 * 表格块（GFM 扩展）。
 */
class Table : ContainerNode() {
    var columnAlignments: List<Alignment> = emptyList()

    /** 块级属性 `{.class #id key=value}`（由 BlockAttributeProcessor 后处理设置）。 */
    var blockAttributes: Map<String, String> = emptyMap()

    enum class Alignment { LEFT, CENTER, RIGHT, NONE }

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTable(this)
}

class TableHead : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTableHead(this)
}

class TableBody : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTableBody(this)
}

class TableRow : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTableRow(this)
}

class TableCell(
    var alignment: Table.Alignment = Table.Alignment.NONE,
    var isHeader: Boolean = false
) : ContainerNode() {
    /** 单元格的原始文本内容（由解析器在分割表格行时设置）。 */
    var rawContent: String = ""

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTableCell(this)
}

/**
 * 脚注定义：`[^label]: content`。
 */
class FootnoteDefinition(
    var label: String = "",
    var index: Int = 0
) : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitFootnoteDefinition(this)
}

/**
 * 数学公式块：`$$...$$`。
 *
 * 公式编号（`\tag{N}`）、环境自动编号（equation/align 等）、引用（`\ref`/`\eqref`）
 * 均由 LaTeX 渲染库原生处理，literal 保留完整的 LaTeX 源文本。
 */
class MathBlock(
    override var literal: String = ""
) : LeafNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitMathBlock(this)
}

/**
 * 定义列表（扩展语法）。
 */
class DefinitionList : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDefinitionList(this)
}

class DefinitionTerm : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDefinitionTerm(this)
}

class DefinitionDescription : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDefinitionDescription(this)
}

/**
 * 警告/提示块：`> [!NOTE]`、`> [!WARNING]` 等。
 */
class Admonition(
    var type: String = "NOTE",
    var title: String = ""
) : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitAdmonition(this)
}

/**
 * 前置元数据块：YAML `---` 或 TOML `+++`。
 */
class FrontMatter(
    var format: String = "yaml",
    override var literal: String = ""
) : LeafNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitFrontMatter(this)
}

/**
 * 原生 Compose 块。
 *
 * 该节点不由 Markdown 文本直接解析产生，而是用于外部手工构造 AST 时插入
 * 自定义 Compose 内容。块级渲染时会直接执行 [content]。
 */
class NativeBlock(
    val content: @Composable () -> Unit,
) : LeafNode() {
    override val literal: String get() = ""
    override val stableKey: Int = allocateSyntheticStableKey()

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitNativeBlock(this)
}

/**
 * 空行节点（内部用于松散列表检测）。
 */
class BlankLine : LeafNode() {
    override val literal: String get() = ""
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBlankLine(this)
}

/**
 * TOC 占位符：`[TOC]` 或 `[[toc]]`，渲染时自动生成目录。
 *
 * 支持高级配置参数（紧跟 `[TOC]` 之后的行）：
 * - `:depth=2-4`：标题深度范围
 * - `:exclude=#ignore`：排除指定 ID 的标题
 * - `:order=asc|desc`：排序方式
 */
class TocPlaceholder : LeafNode() {
    override val literal: String get() = ""

    /** 最小标题深度（默认 1）。 */
    var minDepth: Int = 1

    /** 最大标题深度（默认 6）。 */
    var maxDepth: Int = 6

    /** 要排除的标题 ID 列表。 */
    var excludeIds: List<String> = emptyList()

    /** 排序方式："asc"（默认，按文档顺序）或 "desc"（逆序）。 */
    var order: String = "asc"

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTocPlaceholder(this)
}

/**
 * 缩写定义：`*[abbr]: Full Text`。
 */
class AbbreviationDefinition(
    var abbreviation: String = "",
    var fullText: String = ""
) : LeafNode() {
    override val literal: String get() = ""
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitAbbreviationDefinition(this)
}

/**
 * 自定义容器块：`:::type` 到 `:::`。
 *
 * 语法：
 * ```
 * ::: warning "标题"
 * 容器内容（支持嵌套块级元素）
 * :::
 * ```
 *
 * 支持自定义类型、标题、CSS class/ID 属性，以及容器嵌套。
 */
class CustomContainer(
    /** 容器类型，如 "warning"、"note"、"card" 等 */
    var type: String = "",
    /** 可选标题（引号内的文本） */
    var title: String = "",
    /** CSS class 列表，从 `{.class1 .class2}` 中提取 */
    var cssClasses: List<String> = emptyList(),
    /** CSS ID，从 `{#my-id}` 中提取 */
    var cssId: String? = null,
) : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitCustomContainer(this)
}

/**
 * 图表块：Mermaid / PlantUML 等图表代码块。
 *
 * 由围栏代码块中 info string 为 `mermaid`、`plantuml` 等关键字时自动转换。
 * 内部不解析 Markdown 语法，保留原始图表代码供渲染引擎处理。
 */
class DiagramBlock(
    /** 图表类型：如 "mermaid"、"plantuml" */
    var diagramType: String = "",
    /** 原始图表代码 */
    override var literal: String = ""
) : LeafNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDiagramBlock(this)
}

/**
 * 多列布局容器：`:::columns` 到 `:::`。
 *
 * 语法：
 * ```
 * :::columns
 * :::column{width=50%}
 * 左列内容
 * :::column{width=50%}
 * 右列内容
 * :::
 * ```
 *
 * 包含多个 [ColumnItem] 子节点，每个子节点代表一列。
 */
class ColumnsLayout : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitColumnsLayout(this)
}

/**
 * 多列布局中的单列：`:::column{width=50%}`。
 *
 * @property width 列宽，如 "50%"、"300px"，为空时表示平均分配
 */
class ColumnItem(
    var width: String = "",
) : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitColumnItem(this)
}

/**
 * 分页符标记：`***pagebreak***`。
 *
 * 用于 PDF 导出/打印场景，渲染器据此插入分页样式。
 */
class PageBreak : LeafNode() {
    override val literal: String get() = ""
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitPageBreak(this)
}

/**
 * block-level directive: `{% tag arg1 "arg2" key=value %}...{% endtag %}`.
 *
 * self-closing directives (no end tag) are also represented as a block
 * with empty children.
 *
 * 该节点也是官方块级扩展承载协议：
 * parser 只负责把 directive 解析为纯 AST，
 * 外部特殊语法应在 runtime 层先转换为 directive，再由 renderer 插件分发。
 */
class DirectiveBlock(
    /** directive tag name, e.g. "youtube", "include" */
    var tagName: String = "",
    /** positional and keyword arguments */
    var args: Map<String, String> = emptyMap(),
) : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDirectiveBlock(this)
}

/**
 * 内容标签页块（MkDocs Material 风格）：`=== "Tab Title"`。
 *
 * 语法：
 * ```
 * === "Tab 1"
 *     Tab 1 内容
 *
 * === "Tab 2"
 *     Tab 2 内容
 * ```
 *
 * 包含多个 [TabItem] 子节点，每个子节点代表一个标签页。
 */
class TabBlock : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTabBlock(this)
}

/**
 * 标签页中的单个标签项：`=== "Title"`。
 *
 * @property title 标签页标题
 */
class TabItem(
    var title: String = "",
) : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitTabItem(this)
}

/**
 * 参考文献定义块：`[^bibliography]: key: Author, "Title", Year`。
 *
 * 以特殊脚注 `[^bibliography]` 为标志的参考文献数据库，
 * 包含一组键值对，每个键对应一条文献记录。
 */
class BibliographyDefinition : ContainerNode() {
    /** 所有文献条目：key → BibEntry */
    val entries: MutableMap<String, BibEntry> = mutableMapOf()

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitBibliographyDefinition(this)
}

/**
 * 单条参考文献记录。
 */
data class BibEntry(
    val key: String,
    val content: String,
)

/**
 * Figure 块：独立段落中的图片，渲染为 `<figure>` + `<figcaption>`。
 *
 * 当一个段落仅包含单个图片节点时（Pandoc implicit_figures 语义），
 * 由 [com.hrm.markdown.parser.block.postprocessors.FigureProcessor] 后处理器
 * 将该段落替换为 Figure 节点。
 *
 * - 图片的 alt 文本作为 figcaption（标题）
 * - 图片的 title 属性（如有）也可用于标题
 */
class Figure(
    /** 图片 URL */
    var imageUrl: String = "",
    /** 图片标题（figcaption），通常来自 alt 文本 */
    var caption: String = "",
    /** 图片宽度（像素），null 表示未指定 */
    var imageWidth: Int? = null,
    /** 图片高度（像素），null 表示未指定 */
    var imageHeight: Int? = null,
    /** 自定义属性映射 */
    var attributes: Map<String, String> = emptyMap(),
) : LeafNode() {
    override val literal: String get() = caption
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitFigure(this)
}
