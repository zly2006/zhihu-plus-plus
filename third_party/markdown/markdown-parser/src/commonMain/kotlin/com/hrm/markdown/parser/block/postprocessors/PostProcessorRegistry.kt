package com.hrm.markdown.parser.block.postprocessors

import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.ast.Node
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * 后处理器注册表。
 *
 * 管理所有已注册的 [PostProcessor]，按优先级顺序执行。
 *
 * ## 使用方式
 * ```kotlin
 * // 使用内置默认处理器
 * val registry = PostProcessorRegistry.withDefaults()
 *
 * // 添加自定义处理器
 * registry.register(MyCustomPostProcessor())
 *
 * // 空注册表（不执行任何后处理）
 * val empty = PostProcessorRegistry()
 * ```
 */
@OptIn(ExperimentalAtomicApi::class)
class PostProcessorRegistry {
    private data class State(
        val processors: List<PostProcessor>,
        val sortedProcessors: List<PostProcessor>?,
    )

    private val state = AtomicReference(State(processors = emptyList(), sortedProcessors = null))

    fun register(processor: PostProcessor) {
        updateState { current ->
            current.copy(
                processors = current.processors + processor,
                sortedProcessors = null,
            )
        }
    }

    fun registerAll(vararg processors: PostProcessor) {
        if (processors.isEmpty()) return
        val additions = processors.toList()
        updateState { current ->
            current.copy(
                processors = current.processors + additions,
                sortedProcessors = null,
            )
        }
    }

    /**
     * 按优先级顺序执行所有后处理器。
     *
     * 使用 Copy-on-Write 模式保证线程安全：
     * - `processors` 与 `sortedProcessors` 始终作为同一个原子状态更新
     * - 遍历的是不可变列表快照，不会被并发修改
     * - 并发注册不会丢失更新，旧排序缓存也不会覆盖新状态
     */
    fun processAll(document: Document) {
        val snapshot = sortedSnapshot()
        for (processor in snapshot) {
            processor.process(document)
        }
    }

    private fun sortedSnapshot(): List<PostProcessor> {
        while (true) {
            val current = state.load()
            current.sortedProcessors?.let { return it }

            val sorted = current.processors.sortedBy { it.priority }
            val updated = current.copy(sortedProcessors = sorted)
            if (state.compareAndSet(current, updated)) {
                return sorted
            }
        }
    }

    private inline fun updateState(transform: (State) -> State) {
        while (true) {
            val current = state.load()
            val updated = transform(current)
            if (state.compareAndSet(current, updated)) {
                return
            }
        }
    }

    companion object {
        /**
         * 创建预注册了所有内置后处理器的注册表。
         *
         * 内置处理器按优先级排列：
         * 1. [HeadingIdProcessor] (100) — 自动生成标题 ID (slug)
         * 2. [HtmlFilterProcessor] (200) — GFM 禁止的 HTML 标签过滤
         * 3. [AbbreviationProcessor] (300) — 缩写替换
         * 4. [DiagramProcessor] (400) — 围栏代码块 → 图表块转换
         */
        fun withDefaults(): PostProcessorRegistry {
            return PostProcessorRegistry().apply {
                register(HeadingIdProcessor())
                register(HtmlFilterProcessor())
                register(AbbreviationProcessor())
                register(DiagramProcessor())
            }
        }

        /**
         * 从节点中提取纯文本的便捷方法。
         * 委托给 [HeadingIdProcessor.extractPlainText]。
         */
        fun extractPlainText(node: Node): String {
            return HeadingIdProcessor.extractPlainText(node)
        }
    }
}
