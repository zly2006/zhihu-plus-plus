package com.hrm.markdown.parser.flavour

import com.hrm.markdown.parser.block.postprocessors.PostProcessor
import com.hrm.markdown.parser.block.starters.BlockStarter
import com.hrm.markdown.parser.block.starters.TableStarter
import com.hrm.markdown.parser.block.starters.ThematicBreakStarter

/**
 * GitHub Flavored Markdown (GFM) 方言。
 *
 * 基于 CommonMark 扩展，添加了 GitHub 特有的语法特性。
 * 遵循 [GFM 规范](https://github.github.com/gfm/)。
 *
 * ## GFM 扩展语法
 *
 * ### 块级扩展
 * - **表格**：`| A | B |\n| --- | --- |\n| 1 | 2 |`
 * - **任务列表**：`- [ ] todo` / `- [x] done`
 *
 * ### 内联扩展
 * - **删除线**：`~~text~~`
 * - **自动链接**：自动识别 URL 和邮箱（无需尖括号）
 *
 * ### 行为差异
 * - 表格优先级高于主题分隔线
 *
 * ## 使用示例
 *
 * ```kotlin
 * val parser = MarkdownParser(GFMFlavour)
 * val document = parser.parse("""
 *     # Task List
 *     - [x] Completed
 *     - [ ] Todo
 *
 *     | Name | Age |
 *     | ---- | --- |
 *     | Bob  | 30  |
 *
 *     ~~Deleted text~~
 * """.trimIndent())
 * ```
 */
object GFMFlavour : MarkdownFlavour {

    /**
     * GFM 块级解析器。
     *
     * 基于 CommonMark，添加了：
     * - 表格解析器（优先级 200，在主题分隔线之前）
     *
     * GFM spec 0.29 is based on CommonMark 0.29 and retains indented code blocks.
     */
    override val blockStarters: List<BlockStarter> = buildList {
        // CommonMark 解析器（保留所有 starters，包括缩进代码块）
        addAll(CommonMarkFlavour.blockStarters)

        // GFM 扩展：表格（优先级 200，插入到主题分隔线之前）
        val thematicBreakIndex = indexOfFirst {
            it is ThematicBreakStarter
        }
        if (thematicBreakIndex >= 0) {
            add(thematicBreakIndex, TableStarter())
        } else {
            add(TableStarter())
        }
    }

    /**
     * GFM 后处理器（空列表）。
     *
     * GFM 的任务列表、删除线等特性在解析阶段处理，
     * 不需要额外的后处理器。
     */
    override val postProcessors: List<PostProcessor> = emptyList()

    /**
     * GFM 0.29 collapses redundant nested emphasis.
     * E.g. `****foo****` renders as `<strong>foo</strong>` not `<strong><strong>foo</strong></strong>`.
     */
    override val enableEmphasisCoalescing: Boolean = true

    /**
     * Disable GFM bare-URL autolinks for spec compliance.
     *
     * The GFM spec 0.29 test suite only includes the standard CommonMark "Autolinks" section,
     * which explicitly states that bare URLs (without angle brackets) are NOT autolinks.
     * Extended autolink detection (bare http://, www., email) is available via ExtendedFlavour.
     */
    override val enableGfmAutolinks: Boolean = false

    /**
     * Disable extended inline syntax (highlight ==, insert ++, superscript ^, etc.)
     * for GFM spec compliance.
     *
     * The GFM spec 0.29 only adds strikethrough (~~) and tables to CommonMark.
     * Extended inline features like ==highlight== interfere with spec tests
     * (e.g., `====` being parsed as `<mark></mark>` instead of `<p>====</p>`).
     */
    override val enableExtendedInline: Boolean = false

}
