package com.hrm.markdown.renderer

import androidx.compose.runtime.Immutable
import com.hrm.markdown.parser.flavour.ExtendedFlavour
import com.hrm.markdown.parser.flavour.MarkdownFlavour

/**
 * Markdown 渲染器的解析配置。
 *
 * 将解析器的配置选项暴露给渲染层，允许外部控制使用的 Markdown 方言和解析行为。
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 默认配置（ExtendedFlavour，全功能）
 * Markdown(markdown = "# Hello")
 *
 * // 使用 GFM 方言
 * Markdown(
 *     markdown = "# Hello",
 *     config = MarkdownConfig(flavour = GFMFlavour),
 * )
 *
 * // 使用 CommonMark 方言
 * Markdown(
 *     markdown = "# Hello",
 *     config = MarkdownConfig(flavour = CommonMarkFlavour),
 * )
 *
 * // 自定义方言 + Emoji
 * Markdown(
 *     markdown = ":myemoji: Hello",
 *     config = MarkdownConfig(
 *         flavour = ExtendedFlavour,
 *         customEmojiMap = mapOf("myemoji" to "🎉"),
 *         enableAsciiEmoticons = true,
 *     ),
 * )
 * ```
 *
 * // 启用标题自动编号
 * Markdown(
 *     markdown = "# Intro\n## Section\n## Section 2",
 *     config = MarkdownConfig(enableHeadingNumbering = true),
 * )
 * ```
 *
 * @param flavour Markdown 方言，控制支持的语法特性。默认为 [ExtendedFlavour]（包含所有扩展）。
 * @param customEmojiMap 自定义 Emoji 别名映射（shortcode → unicode），默认为空。
 * @param enableAsciiEmoticons 是否启用 ASCII 表情自动转换（如 `:)` → 😊），默认关闭。
 * @param enableLinting 是否启用语法验证/Linting，默认关闭。
 * @param enableHeadingNumbering 是否启用标题自动编号（如 1, 1.1, 1.1.1），默认关闭。
 * @param appendCoalesceThreshold LLM 流式 [com.hrm.markdown.parser.MarkdownParser.append] 合并阈值（字符数）。
 *   `0`（默认）：每次 append 立即增量解析，行为与历史版本一致。
 *   `> 0`：未跨换行符且累积 < 阈值的小 chunk 会被缓冲，多个 token 合并为一次解析；
 *   推荐 LLM 流式场景设为 `16`，可减少 60-80% 的总流式耗时；代价是"正在写的最后一行"
 *   AST 最多滞后该字符数（碰到 `\n` 必定 flush，已写完的行不受影响）。
 */
@Immutable
data class MarkdownConfig(
    val flavour: MarkdownFlavour = ExtendedFlavour,
    val customEmojiMap: Map<String, String> = emptyMap(),
    val enableAsciiEmoticons: Boolean = false,
    val enableLinting: Boolean = false,
    val enableHeadingNumbering: Boolean = false,
    val appendCoalesceThreshold: Int = 0,
) {
    companion object {
        /** 默认配置：ExtendedFlavour，全功能。 */
        val Default = MarkdownConfig()

        /**
         * 面向 LLM 流式展示场景的推荐配置：开启 append 节流（合并阈值 16），
         * 在保证 UI 可见性的前提下大幅降低总流式耗时。
         */
        val LlmStreaming = MarkdownConfig(appendCoalesceThreshold = 16)
    }
}
