package com.hrm.markdown.parser

import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.core.SourceText
import com.hrm.markdown.parser.flavour.ExtendedFlavour
import com.hrm.markdown.parser.flavour.MarkdownFlavour
import com.hrm.markdown.parser.incremental.EditOperation
import com.hrm.markdown.parser.incremental.IncrementalEngine
import com.hrm.markdown.parser.lint.DiagnosticResult
import com.hrm.markdown.parser.lint.LintingPostProcessor
import com.hrm.markdown.parser.log.HLog
import com.hrm.markdown.parser.streaming.StreamingParser

/**
 * Markdown 解析器的主入口。
 *
 * 支持三种模式：
 * - **完整解析**：一次性解析整个 Markdown 文本。
 * - **流式解析**：面向 LLM 流式输出场景，支持 append-only 增量解析，
 *   自动修复未关闭的语法结构（围栏代码块、强调、链接等），保障正常展示。
 * - **编辑模式**：支持任意位置的 insert/delete/replace 操作，
 *   利用增量解析引擎精准定位脏区并增量更新 AST。
 *
 * ## 完整解析
 * ```kotlin
 * // 默认使用扩展版（包含所有特性）
 * val parser = MarkdownParser()
 * val document = parser.parse("# Hello\n\nWorld")
 *
 * // 使用特定方言
 * import com.hrm.markdown.parser.flavour.CommonMarkFlavour
 * import com.hrm.markdown.parser.flavour.GFMFlavour
 *
 * val commonMarkParser = MarkdownParser(CommonMarkFlavour)
 * val gfmParser = MarkdownParser(GFMFlavour)
 * ```
 *
 * ## 流式解析（LLM 场景）
 * ```kotlin
 * val parser = MarkdownParser()
 * parser.beginStream()
 * parser.append("# Hello")
 * parser.append(" World\n\n")
 * parser.append("This is **bold")
 * val doc = parser.document // "bold" 会自动补全 **
 * parser.endStream()
 * ```
 *
 * ## 编辑模式
 * ```kotlin
 * val parser = MarkdownParser()
 * parser.parse("# Hello\n\nWorld")
 * parser.insert(offset = 13, text = " of Markdown")
 * val doc = parser.document // 增量更新后的 AST
 * ```
 *
 * ## 语法验证（Linting）
 * ```kotlin
 * val parser = MarkdownParser(enableLinting = true)
 * val doc = parser.parse("# Title\n\n### Skipped h2\n\n[^missing]")
 * val diagnostics = parser.diagnostics
 * println(diagnostics) // 输出标题层级跳跃和无效脚注引用的诊断
 * ```
 *
 * @param flavour Markdown 方言，控制支持的语法特性。默认为 [ExtendedFlavour]（包含所有扩展）。
 * @param enableLinting 是否启用语法验证/Linting。默认为 false。
 */
class MarkdownParser(
    val flavour: MarkdownFlavour = ExtendedFlavour,
    /** 自定义 Emoji 别名映射（shortcode → unicode） */
    val customEmojiMap: Map<String, String> = emptyMap(),
    /** 是否启用 ASCII 表情自动转换（如 :) → 😊） */
    val enableAsciiEmoticons: Boolean = false,
    /** 是否启用语法验证/Linting */
    val enableLinting: Boolean = false,
    /**
     * 流式 [append] 合并阈值（字符数）。0 表示关闭，每次 [append] 立即增量解析（默认）。
     *
     * 大于 0 时：未跨换行符且未达阈值的 chunk 会被缓冲，多个小 token 合并为一次解析。
     * 典型 LLM 流式场景下 32-64 可减少 60-80% 的总流式耗时；代价是"正在写的最后一行"
     * AST 可能滞后最多该字符数（碰到 `\n` 必定 flush，所以已写完的行不受影响）。
     */
    val appendCoalesceThreshold: Int = 0,
) {
    private val lintingProcessor: LintingPostProcessor? = if (enableLinting) LintingPostProcessor() else null
    private val streamingParser = StreamingParser(
        flavour, customEmojiMap, enableAsciiEmoticons, lintingProcessor,
        appendCoalesceThreshold = appendCoalesceThreshold,
    )
    /**
     * Edit engine 仅在编辑 API（applyEdit / replace 等）首次被调用时才构造。
     * 大多数 LLM 流式场景不会触碰编辑路径，可省去一次 IncrementalEngine + FlavourCache 构造。
     */
    private val editEngine: IncrementalEngine by lazy {
        IncrementalEngine(flavour, customEmojiMap, enableAsciiEmoticons, lintingProcessor = lintingProcessor)
    }

    /**
     * 语法验证诊断结果。
     *
     * 仅在 `enableLinting = true` 时有内容。
     * 每次 [parse]、[endStream] 或编辑操作后自动更新。
     */
    val diagnostics: DiagnosticResult
        get() = lintingProcessor?.result ?: DiagnosticResult()

    /** 是否处于编辑模式（通过 edit API 操作过） */
    private var inEditMode = false

    /**
     * 当前文档 AST。每次解析/编辑后更新。
     */
    val document: Document
        get() = if (inEditMode) editEngine.document else streamingParser.document

    /**
     * 当前源文本。每次解析/编辑后更新。
     */
    val sourceText: SourceText
        get() = if (inEditMode) editEngine.sourceText else streamingParser.sourceText

    /**
     * 当前是否处于流式接收中。
     */
    val isStreaming: Boolean get() = streamingParser.isStreaming

    /**
     * 解析完整的 Markdown 输入文本。
     * 返回 AST 的根 Document 节点。
     */
    fun parse(input: String): Document {
        HLog.i(TAG) { "parse input=${input.length} chars" }
        inEditMode = false
        return streamingParser.fullParse(input)
    }

    // ────── 流式解析 API ──────

    /**
     * 开始一次新的流式会话。清空之前的状态。
     */
    fun beginStream() {
        HLog.i(TAG, "beginStream")
        inEditMode = false
        streamingParser.beginStream()
    }

    /**
     * 追加一段文本（通常是 LLM 的一个 token 或一个 chunk）。
     * 追加后立即触发增量解析，自动修复未关闭的语法结构。
     *
     * @return 更新后的 Document
     */
    fun append(chunk: String): Document {
        return streamingParser.append(chunk)
    }

    /**
     * 流结束。执行最终解析（不再做未完成块修复）。
     *
     * @return 最终的 Document
     */
    fun endStream(): Document {
        HLog.i(TAG, "endStream")
        return streamingParser.endStream()
    }

    /**
     * 中断流（用户取消等）。以当前状态做最终快照。
     *
     * @return 当前状态的 Document
     */
    fun abort(): Document {
        return streamingParser.abort()
    }

    /**
     * 获取当前完整文本。
     */
    fun currentText(): String {
        return if (inEditMode) editEngine.currentText() else streamingParser.currentText()
    }

    // ────── 编辑 API（新增） ──────

    /**
     * 在指定偏移量处插入文本。
     * 利用增量解析引擎，只重解析受影响的区域。
     *
     * @param offset 插入位置的字符偏移量
     * @param text 要插入的文本
     * @return 更新后的 Document
     */
    fun insert(offset: Int, text: String): Document {
        ensureEditMode()
        return editEngine.applyEdit(EditOperation.Insert(offset, text))
    }

    /**
     * 从指定偏移量开始删除指定长度的文本。
     *
     * @param offset 删除起始位置的字符偏移量
     * @param length 要删除的字符数
     * @return 更新后的 Document
     */
    fun delete(offset: Int, length: Int): Document {
        ensureEditMode()
        return editEngine.applyEdit(EditOperation.Delete(offset, length))
    }

    /**
     * 替换指定范围的文本。
     *
     * @param offset 替换起始位置的字符偏移量
     * @param length 要替换的原文长度
     * @param newText 新的文本内容
     * @return 更新后的 Document
     */
    fun replace(offset: Int, length: Int, newText: String): Document {
        ensureEditMode()
        return editEngine.applyEdit(EditOperation.Replace(offset, length, newText))
    }

    /**
     * 应用通用编辑操作。
     *
     * @param edit 编辑操作
     * @return 更新后的 Document
     */
    fun applyEdit(edit: EditOperation): Document {
        ensureEditMode()
        return editEngine.applyEdit(edit)
    }

    /**
     * 确保处于编辑模式。
     * 如果之前是解析模式，将当前文本同步到编辑引擎。
     */
    private fun ensureEditMode() {
        if (!inEditMode) {
            val currentText = streamingParser.currentText()
            editEngine.fullParse(currentText)
            inEditMode = true
        }
    }

    companion object {
        private const val TAG = "MarkdownParser"

        /**
         * 便捷方法：将 Markdown 输入解析为 Document AST。
         *
         * @param input Markdown 文本
         * @param flavour Markdown 方言，默认为 [ExtendedFlavour]
         */
        fun parseToDocument(input: String, flavour: MarkdownFlavour = ExtendedFlavour): Document {
            return MarkdownParser(flavour).parse(input)
        }
    }
}
