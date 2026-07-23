package com.hrm.markdown.parser.flavour

import com.hrm.markdown.parser.block.postprocessors.*
import com.hrm.markdown.parser.block.starters.*

/**
 * 扩展 Markdown 方言。
 *
 * 包含 CommonMark + GFM + 所有额外扩展特性，
 * 适用于功能丰富的文档系统、笔记应用等场景。
 *
 * ## 扩展特性
 *
 * ### 基于 GFM
 * - ✅ 表格
 * - ✅ 任务列表
 * - ✅ 删除线
 * - ✅ 自动链接
 *
 * ### 额外扩展
 *
 * #### 块级元素
 * - **前置元数据**：`---\ntitle: xxx\n---`（YAML）或 `+++\ntitle = xxx\n+++`（TOML）
 * - **自定义容器**：`::: warning\n内容\n:::`
 * - **数学块**：`$$\ny = ax + b\n$$`
 * - **脚注定义**：`[^1]: 脚注内容`
 * - **定义列表**：`Term\n: Definition`
 *
 * #### 内联元素
 * - **数学公式**：`$E=mc^2$`
 * - **脚注引用**：`[^1]`
 * - **上标**：`x^2^`
 * - **下标**：`H~2~O`
 * - **高亮**：`==marked text==`
 * - **插入文本**：`++inserted++`
 * - **Emoji**：`:smile:` → 😊
 *
 * #### 后处理
 * - **标题 ID**：自动生成 `# Title` → `id="title"`
 * - **缩写替换**：`*[HTML]: HyperText Markup Language` → `<abbr>`
 * - **图表渲染**：Mermaid/PlantUML 代码块转换
 *
 * ## 使用示例
 *
 * ```kotlin
 * val parser = MarkdownParser(ExtendedFlavour)
 * val document = parser.parse("""
 *     ---
 *     title: Example Document
 *     ---
 *
 *     # Heading {#custom-id}
 *
 *     ::: warning
 *     This is a warning!
 *     :::
 *
 *     Math: $E=mc^2$ and display mode:
 *
 *     $$
 *     \int_0^1 x^2 dx = \frac{1}{3}
 *     $$
 *
 *     - [x] Task completed
 *     - [ ] Task pending
 *
 *     ==Highlighted text== and ++inserted text++
 *
 *     H~2~O and X^2^
 *
 *     Footnote[^1]
 *
 *     [^1]: Footnote content
 * """.trimIndent())
 * ```
 */
object ExtendedFlavour : MarkdownFlavour {

    /**
     * 扩展版块级解析器。
     *
     * 包含 CommonMark + GFM + 所有扩展解析器。
     * 按优先级排序：
     * 0. 前置元数据（10）
     * 1. Setext 标题（100）
     * 2. ATX 标题（110）
     * 3. 表格（200）
     * 4. 分页符（205）
     * 5. 主题分隔线（210）
     * 6. 自定义容器（300）
     * 7. 围栏代码块（310）
     * 8. 数学块（320）
     * 9. HTML 块（400）
     * 10. 块引用（410）
     * 11. 列表项（500）
     * 12. 脚注定义（510）
     * 13. 定义描述（520）
     * 14. 缩进代码块（600，在定义列表/脚注内部自动让步）
     */
    override val blockStarters: List<BlockStarter> = listOf(
        FrontMatterStarter(),          // 10
        SetextHeadingStarter(),        // 100
        HeadingStarter(),              // 110
        TableStarter(),                // 200
        PageBreakStarter(),            // 205
        ThematicBreakStarter(),        // 210
        DirectiveBlockStarter(),       // 250
        TabBlockStarter(),             // 295
        CustomContainerStarter(),      // 300
        FencedCodeBlockStarter(),      // 310
        MathBlockStarter(),            // 320
        HtmlBlockStarter(),            // 400
        BlockQuoteStarter(),           // 410
        ListItemStarter(),             // 500
        FootnoteDefinitionStarter(),   // 510
        DefinitionDescriptionStarter(), // 520
        IndentedCodeBlockStarter(),    // 600（在定义列表/脚注内部自动让步）
    )

    /**
     * 扩展版后处理器。
     *
     * 执行顺序：
     * 1. 标题 ID 生成（100）
     * 2. 块级属性解析（150）
     * 3. 缩写替换（200）
     * 4. 图表渲染（300）
     * 5. 多列布局转换（350）
     * 6. HTML 过滤（400）
     * 7. Figure 转换（450）
     */
    override val postProcessors: List<PostProcessor> = listOf(
        HeadingIdProcessor(),          // 100
        BlockAttributeProcessor(),     // 150
        BibliographyProcessor(),       // 180
        AbbreviationProcessor(),       // 200
        DiagramProcessor(),            // 300
        ColumnsLayoutProcessor(),      // 350
        HtmlFilterProcessor(),         // 400
        FigureProcessor(),             // 450
    )

}
