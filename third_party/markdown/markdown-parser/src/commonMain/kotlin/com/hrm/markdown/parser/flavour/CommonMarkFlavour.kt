package com.hrm.markdown.parser.flavour

import com.hrm.markdown.parser.block.postprocessors.*
import com.hrm.markdown.parser.block.starters.*

/**
 * CommonMark 标准 Markdown 方言。
 *
 * 实现 [CommonMark 规范](https://spec.commonmark.org/) 的核心语法，
 * 不包含任何扩展特性（如 GFM 表格、删除线等）。
 *
 * ## 支持的语法
 *
 * ### 块级元素
 * - ATX 标题 (`# heading`)
 * - Setext 标题 (`heading\n===`)
 * - 围栏代码块 (` ``` ` / `~~~`)
 * - 缩进代码块（4 空格缩进）
 * - 块引用 (`>`)
 * - 有序列表 (`1. item`)
 * - 无序列表 (`- item` / `* item` / `+ item`)
 * - 主题分隔线 (`---` / `***` / `___`)
 * - 段落
 * - HTML 块
 *
 * ### 内联元素
 * - 强调 (`*text*` / `_text_`)
 * - 加粗 (`**text**` / `__text__`)
 * - 行内代码 (`` `code` ``)
 * - 链接 (`[text](url)` / `[text][ref]`)
 * - 图片 (`![alt](url)`)
 * - 自动链接 (`<url>` / `<email>`)
 * - 行内 HTML
 * - 转义字符 (`\*`)
 * - HTML 实体 (`&amp;`)
 *
 * ## 使用示例
 *
 * ```kotlin
 * val parser = MarkdownParser(CommonMarkFlavour)
 * val document = parser.parse("# Hello\n\nWorld")
 * ```
 */
object CommonMarkFlavour : MarkdownFlavour {

    /**
     * CommonMark 核心块级解析器。
     *
     * 按 CommonMark 规范的优先级顺序排列：
     * 1. Setext 标题（优先级 100）
     * 2. ATX 标题（优先级 110）
     * 3. 主题分隔线（优先级 210）
     * 4. 围栏代码块（优先级 310）
     * 5. HTML 块（优先级 400）
     * 6. 块引用（优先级 410）
     * 7. 列表项（优先级 500）
     * 8. 缩进代码块（优先级 600，最低）
     */
    override val blockStarters: List<BlockStarter> = listOf(
        SetextHeadingStarter(),       // 100
        HeadingStarter(),              // 110
        ThematicBreakStarter(),        // 210
        FencedCodeBlockStarter(),      // 310
        HtmlBlockStarter(),            // 400
        BlockQuoteStarter(),           // 410
        ListItemStarter(),             // 500
        IndentedCodeBlockStarter(),    // 600
    )

    /**
     * CommonMark 后处理器（空列表）。
     *
     * CommonMark 核心规范不需要后处理器，
     * 所有语法都在解析阶段完成。
     */
    override val postProcessors: List<PostProcessor> = emptyList()

    override val enableGfmAutolinks: Boolean = false
    override val enableExtendedInline: Boolean = false
    override val enableStrikethrough: Boolean = false
}
