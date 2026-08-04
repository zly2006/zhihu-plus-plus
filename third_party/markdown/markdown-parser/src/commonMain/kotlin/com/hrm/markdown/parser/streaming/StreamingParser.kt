package com.hrm.markdown.parser.streaming

import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.core.SourceText
import com.hrm.markdown.parser.flavour.ExtendedFlavour
import com.hrm.markdown.parser.flavour.MarkdownFlavour
import com.hrm.markdown.parser.incremental.IncrementalEngine
import com.hrm.markdown.parser.lint.LintingPostProcessor
import com.hrm.markdown.parser.log.HLog

/**
 * 面向 LLM 流式输出的高性能增量解析器。
 *
 * 核心设计：
 * - **append-only** 输入模型，无需调用方维护偏移量。
 * - 只重新解析 **尾部脏区域**（从最后一个未关闭块的起始行到文本末尾），
 *   前面已稳定的块直接复用，每次 append 的解析代价为 O(尾部块大小)。
 * - 自动修复未关闭的语法结构（围栏代码块缺少 ` ``` `、强调缺少 `**` 等），
 *   保障即使大模型遗漏结束符也能正常展示。
 * - 通过 [contentHash] 实现细粒度的 Compose 重组优化：
 *   只有内容变化的块会触发重新渲染。
 *
 * 内部委托给 [IncrementalEngine] 实现所有解析逻辑。
 *
 * ## 使用方式
 * ```kotlin
 * val parser = StreamingParser()
 * parser.beginStream()
 * // LLM token 到达时
 * parser.append("# Hello")
 * parser.append(" World\n\n")
 * parser.append("This is **bold")
 * val doc = parser.document // "bold" 会自动补全 **
 * parser.endStream()
 * val finalDoc = parser.document // 最终文档（不做修复）
 * ```
 *
 * @param flavour Markdown 方言，控制支持的语法特性
 */
class StreamingParser(
    flavour: MarkdownFlavour = ExtendedFlavour,
    customEmojiMap: Map<String, String> = emptyMap(),
    enableAsciiEmoticons: Boolean = false,
    lintingProcessor: LintingPostProcessor? = null,
    /** 流式 append 合并阈值。详见 [IncrementalEngine.appendCoalesceThreshold]。 */
    appendCoalesceThreshold: Int = 0,
) {
    companion object {
        private const val TAG = "StreamingParser"
    }

    private val engine = IncrementalEngine(
        flavour,
        customEmojiMap,
        enableAsciiEmoticons,
        lintingProcessor = lintingProcessor,
        appendCoalesceThreshold = appendCoalesceThreshold,
    )

    /** 当前文档 AST */
    val document: Document get() = engine.document

    /** 当前源文本 */
    val sourceText: SourceText get() = engine.sourceText

    /** 当前是否处于流式接收中 */
    val isStreaming: Boolean get() = engine.isStreaming

    /**
     * 开始一次新的流式会话。清空之前的状态。
     */
    fun beginStream() {
        HLog.i(TAG, "beginStream")
        engine.beginStream()
    }

    /**
     * 追加一段文本（通常是 LLM 的一个 token 或一个 chunk）。
     * 追加后立即触发增量解析。
     *
     * @return 更新后的 Document
     */
    fun append(chunk: String): Document {
        val doc = engine.append(chunk)
        HLog.d(TAG) { "append chunk=${chunk.length} chars, children=${doc.children.size}" }
        return doc
    }

    /**
     * 流结束。执行最终解析（不再做未完成块修复）。
     *
     * @return 最终的 Document
     */
    fun endStream(): Document {
        HLog.i(TAG, "endStream")
        return engine.endStream()
    }

    /**
     * 中断流（用户取消等）。以当前状态做最终快照。
     *
     * @return 当前状态的 Document（含修复）
     */
    fun abort(): Document {
        HLog.w(TAG, "abort")
        return engine.abort()
    }

    /**
     * 获取当前完整文本。
     */
    fun currentText(): String = engine.currentText()

    /**
     * 对给定输入执行完整解析（非流式模式）。
     */
    fun fullParse(input: String): Document {
        return engine.fullParse(input)
    }
}