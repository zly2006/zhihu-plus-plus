package com.hrm.markdown.parser.ast

// ─────────────── 行内级节点 ───────────────

/**
 * 纯文本内容。
 */
class Text(
    override var literal: String = ""
) : LeafNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitText(this)
}

/**
 * 软换行（源码中的单个换行符）。
 */
class SoftLineBreak : LeafNode() {
    override val literal: String get() = "\n"
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitSoftLineBreak(this)
}

/**
 * 硬换行（两个空格+换行，或反斜杠+换行）。
 */
class HardLineBreak : LeafNode() {
    override val literal: String get() = "\n"
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitHardLineBreak(this)
}

/**
 * 强调（斜体）：`*text*` 或 `_text_`。
 */
class Emphasis : ContainerNode() {
    var delimiter: Char = '*'
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitEmphasis(this)
}

/**
 * 加重强调（粗体）：`**text**` 或 `__text__`。
 */
class StrongEmphasis : ContainerNode() {
    var delimiter: Char = '*'
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitStrongEmphasis(this)
}

/**
 * 删除线（GFM 扩展）：`~~text~~`。
 */
class Strikethrough : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitStrikethrough(this)
}

/**
 * 行内代码：`` `code` ``。
 */
class InlineCode(
    override var literal: String = ""
) : LeafNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitInlineCode(this)
}

/**
 * 行内链接：`[text](url "title")`。
 *
 * 支持扩展属性语法：
 * - `[text](url){rel="nofollow" target="_blank"}` SEO/安全属性
 * - `[text](url){download="file.pdf"}` 下载属性
 */
class Link(
    var destination: String = "",
    var title: String? = null,
    /** 自定义属性映射，如 rel, target, download, class, id 等 */
    var attributes: Map<String, String> = emptyMap(),
) : ContainerNode() {
    /** CSS class 列表 */
    val cssClasses: List<String>
        get() = attributes["class"]?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()

    /** CSS ID */
    val cssId: String?
        get() = attributes["id"]

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitLink(this)
}

/**
 * 图片：`![alt](url "title")`。
 *
 * 支持扩展语法：
 * - `![alt](url =200x300)` 指定宽高（像素）
 * - `![alt](url =200x)` 仅指定宽度
 * - `![alt](url =x300)` 仅指定高度
 * - `![alt](url){.rounded #img1 loading=lazy align=right}` 自定义属性
 */
class Image(
    var destination: String = "",
    var title: String? = null,
    /** 图片宽度（像素），null 表示未指定 */
    var imageWidth: Int? = null,
    /** 图片高度（像素），null 表示未指定 */
    var imageHeight: Int? = null,
    /** 自定义属性映射，如 class, id, loading, align 等 */
    var attributes: Map<String, String> = emptyMap(),
) : ContainerNode() {
    /** CSS class 列表（从 attributes 或 `.class` 语法提取） */
    val cssClasses: List<String>
        get() = attributes["class"]?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()

    /** CSS ID（从 attributes 或 `#id` 语法提取） */
    val cssId: String?
        get() = attributes["id"]

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitImage(this)
}

/**
 * 自动链接：`<url>` 或 `<email>`。
 */
class Autolink(
    var destination: String = "",
    var isEmail: Boolean = false,
    var rawText: String = ""
) : LeafNode() {
    override val literal: String get() = rawText.ifEmpty { destination }
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitAutolink(this)
}

/**
 * 行内 HTML：`<tag>`、`</tag>`、`<!-- comment -->` 等。
 */
class InlineHtml(
    override var literal: String = ""
) : LeafNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitInlineHtml(this)
}

/**
 * HTML 实体：`&amp;`、`&#123;`、`&#x1F4A9;`。
 */
class HtmlEntity(
    override var literal: String = "",
    var resolved: String = ""
) : LeafNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitHtmlEntity(this)
}

/**
 * 转义字符：`\*`、`\[` 等。
 */
class EscapedChar(
    override var literal: String = ""
) : LeafNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitEscapedChar(this)
}

/**
 * 脚注引用：`[^label]`。
 */
class FootnoteReference(
    var label: String = "",
    var index: Int = 0
) : LeafNode() {
    override val literal: String get() = label
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitFootnoteReference(this)
}

/**
 * 行内数学公式：`$...$` 或 `$$...$$`。
 */
class InlineMath(
    override var literal: String = ""
) : LeafNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitInlineMath(this)
}

/**
 * 高亮：`==text==`。
 */
class Highlight : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitHighlight(this)
}

/**
 * 上标：`^text^`。
 */
class Superscript : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitSuperscript(this)
}

/**
 * 下标：`~text~`。
 */
class Subscript : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitSubscript(this)
}

/**
 * 插入文本：`++text++`。
 */
class InsertedText : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitInsertedText(this)
}

/**
 * Emoji 指令：`:smile:`。
 *
 * 支持增强功能：
 * - 标准指令映射到 Unicode：`:smile:` → 😄
 * - 自定义别名映射：`:my-emoji:` → 用户定义的字符
 * - ASCII 表情自动转换：`:)` → 😊（由解析器在扫描阶段处理）
 */
class Emoji(
    var shortcode: String = "",
    override var literal: String = ""
) : LeafNode() {
    /** Unicode 字符（若映射成功） */
    var unicode: String? = null

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitEmoji(this)
}

/**
 * 自定义行内样式：`[文本]{.class style="..."}` 或 `[文本]{.red .bold}`。
 *
 * 用于为行内文本添加自定义 CSS class、style 或其他属性，
 * 补充 `==高亮==` 的局限性。
 */
class StyledText(
    /** 自定义属性映射，如 class, style, id 等 */
    var attributes: Map<String, String> = emptyMap(),
) : ContainerNode() {
    /** CSS class 列表 */
    val cssClasses: List<String>
        get() = attributes["class"]?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()

    /** CSS ID */
    val cssId: String?
        get() = attributes["id"]

    /** CSS style 字符串 */
    val style: String?
        get() = attributes["style"]

    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitStyledText(this)
}

/**
 * 缩写：在正文中出现的缩写词，关联到 AbbreviationDefinition。
 */
class Abbreviation(
    var abbreviation: String = "",
    var fullText: String = ""
) : LeafNode() {
    override val literal: String get() = abbreviation
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitAbbreviation(this)
}

/**
 * 键盘按键：`<kbd>text</kbd>`。
 */
class KeyboardInput(
    override var literal: String = ""
) : LeafNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitKeyboardInput(this)
}

/**
 * inline directive: `{% tag arg1 "arg2" key=value %}`.
 *
 * inline directives cannot have content (no end tag) and are leaf nodes.
 */
class DirectiveInline(
    /** directive tag name */
    var tagName: String = "",
    /** positional and keyword arguments */
    var args: Map<String, String> = emptyMap(),
) : LeafNode() {
    override val literal: String get() = tagName
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitDirectiveInline(this)
}

/**
 * 参考文献引用：`[@key]`。
 *
 * 引用 `[^bibliography]` 定义中的文献条目。
 * 渲染为上标链接，如 [Smith2020]。
 */
class CitationReference(
    /** 引用键，如 "smith2020" */
    var key: String = "",
) : LeafNode() {
    override val literal: String get() = key
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitCitationReference(this)
}

/**
 * 剧透/折叠文本：`>!spoiler text!<`。
 *
 * 点击才可见的剧透文本（Discord / Reddit 风格）。
 */
class Spoiler : ContainerNode() {
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitSpoiler(this)
}

/**
 * Wiki 链接：`[[page]]` 或 `[[page|显示文本]]`。
 *
 * Obsidian 风格内部链接语法，适用于知识库/笔记场景。
 * - `[[page]]` — 链接目标和显示文本均为 "page"
 * - `[[page|显示文本]]` — 链接目标为 "page"，显示 "显示文本"
 */
class WikiLink(
    /** 链接目标（页面名称），如 "my-note" */
    var target: String = "",
    /** 显示文本，如果为 null 则使用 target */
    var label: String? = null,
) : LeafNode() {
    override val literal: String get() = label ?: target
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitWikiLink(this)
}

/**
 * Ruby 注音标注：`{漢字|かんじ}`。
 *
 * 中日文注音标注，渲染为 `<ruby>` HTML 元素。
 * - `{漢字|かんじ}` — 在"漢字"上方标注"かんじ"
 */
class RubyText(
    /** 基础文本（被注音的文字） */
    var base: String = "",
    /** 注音文本（标注在上方的文字） */
    var annotation: String = "",
) : LeafNode() {
    override val literal: String get() = base
    override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visitRubyText(this)
}
