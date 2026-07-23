package com.hrm.markdown.parser.flavour

import com.hrm.markdown.parser.block.postprocessors.PostProcessor
import com.hrm.markdown.parser.block.starters.BlockStarter

/**
 * Markdown 方言（Flavour）描述符。
 *
 * 定义了特定 Markdown 方言的解析规则和扩展特性。
 * 通过组合不同的块解析器和后处理器，
 * 可以实现 CommonMark、GFM 或自定义 Markdown 语法。
 *
 * 所有已实现的解析能力默认全部启用，力争做最全的解析能力，
 * 不提供单独的功能开关——功能由方言的 [blockStarters] 和 [postProcessors] 组合决定。
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 使用 CommonMark
 * val parser = MarkdownParser(CommonMarkFlavour)
 *
 * // 使用 GFM（GitHub Flavored Markdown）
 * val parser = MarkdownParser(GFMFlavour)
 *
 * // 自定义方言
 * val customFlavour = object : MarkdownFlavour {
 *     override val blockStarters = CommonMarkFlavour.blockStarters + listOf(
 *         CalloutBlockStarter(),
 *         WikiLinkStarter()
 *     )
 *     override val postProcessors = CommonMarkFlavour.postProcessors
 * }
 * ```
 */
interface MarkdownFlavour {
    /**
     * 块级解析器（BlockStarter）列表。
     *
     * 按优先级顺序排列，用于识别和解析块级元素（标题、列表、代码块等）。
     * 解析器会按顺序尝试每个 Starter，直到找到匹配的块类型。
     *
     * @see com.hrm.markdown.parser.block.starters.BlockStarter
     */
    val blockStarters: List<BlockStarter>

    /**
     * 后处理器（PostProcessor）列表。
     *
     * 在 AST 构建完成后执行，用于处理引用链接、脚注、标题 ID 生成等需要全局信息的任务。
     * 后处理器按优先级顺序执行。
     *
     * @see com.hrm.markdown.parser.block.postprocessors.PostProcessor
     */
    val postProcessors: List<PostProcessor>

    val enableGfmAutolinks: Boolean get() = true
    val enableExtendedInline: Boolean get() = true

    /**
     * Whether to enable `~~strikethrough~~` syntax (double tilde).
     *
     * This is separate from [enableExtendedInline] because GFM includes
     * strikethrough as a core extension but not other extended inline features.
     * Defaults to true; CommonMark overrides to false.
     */
    val enableStrikethrough: Boolean get() = true

    /**
     * Whether to coalesce (flatten) redundant nested emphasis of the same type.
     *
     * When true, `<strong><strong>foo</strong></strong>` collapses to `<strong>foo</strong>`.
     * This matches GFM spec 0.29 behavior which differs from CommonMark 0.31.
     */
    val enableEmphasisCoalescing: Boolean get() = false
}
