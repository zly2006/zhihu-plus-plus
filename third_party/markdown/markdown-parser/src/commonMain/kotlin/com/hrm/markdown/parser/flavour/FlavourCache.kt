package com.hrm.markdown.parser.flavour

import com.hrm.markdown.parser.block.postprocessors.PostProcessor
import com.hrm.markdown.parser.block.postprocessors.PostProcessorRegistry
import com.hrm.markdown.parser.block.starters.BlockStarter
import com.hrm.markdown.parser.block.starters.BlockStarterRegistry

/**
 * Flavour 配置快照。
 *
 * 对 [MarkdownFlavour] 的 [BlockStarter] 列表和 [PostProcessor] 列表进行快照缓存，
 * 避免在高频调用场景下重复初始化，降低开销。
 *
 * ## 缓存策略
 * - 对于**已注册的单例方言**（内置的 CommonMarkFlavour、GFMFlavour、ExtendedFlavour
 *   默认注册，第三方 `object` 方言可通过 [registerSingleton] 注册），
 *   通过 [of] 获取**全局共享**的缓存实例，生命周期与应用相同。
 *   多个解析器共享同一份配置，零额外开销。
 * - 对于**未注册的自定义方言实例**，[of] 每次**直接创建新的 FlavourCache**（不放入全局缓存）。
 *   其生命周期与持有者（如 `IncrementalEngine`）绑定，持有者被 GC 回收时自动释放，
 *   **无需调用方手动清理，不存在内存泄漏风险**。
 *
 * > 设计理由：自定义方言通常是匿名 `object : MarkdownFlavour {}`，每次创建的是不同引用，
 * > 全局缓存以引用为 key 永远无法命中，既浪费内存又阻止 GC。
 * > 因此未注册的方言不纳入全局缓存，而是作为调用方的局部变量，随持有者自然回收。
 * > KMP 无 `WeakHashMap`，此设计从根源消除内存泄漏可能。
 *
 * ## 缓存内容
 * - [BlockStarter] 列表快照（不可变）
 * - [PostProcessor] 列表快照（不可变）
 * - 预构建的 [BlockStarterRegistry]（排序后冻结，避免重复排序）
 *
 * ## 使用方式
 * ```kotlin
 * // 推荐：通过 of() 获取（自动区分单例/自定义）
 * val cache = FlavourCache.of(flavour)
 * val registry = cache.blockStarterRegistry  // 缓存的注册表，直接使用
 *
 * // 也可直接构造（不经过全局缓存，适合明确需要独立实例的场景）
 * val cache = FlavourCache(myCustomFlavour)
 * ```
 *
 * ## 线程安全
 * 单个 FlavourCache 实例是不可变的，线程安全。
 * [of] 方法中的全局缓存为非线程安全设计（与 Kotlin/Native 单线程模型一致）。
 * 若需在多线程环境使用 [of]，调用方应自行加锁。
 */
class FlavourCache(
    val flavour: MarkdownFlavour,
) {
    /** 缓存的块级解析器列表（不可变快照）。 */
    val blockStarters: List<BlockStarter> = flavour.blockStarters.toList()

    /** 缓存的后处理器列表（不可变快照）。 */
    val postProcessors: List<PostProcessor> = flavour.postProcessors.toList()

    /**
     * 预构建并缓存的 [BlockStarterRegistry]。
     *
     * 内部的排序操作只执行一次，后续直接复用。
     * 该实例已被 [BlockStarterRegistry.freeze] 冻结，任何修改操作（`register`、`registerAll`）
     * 都会抛出 [IllegalStateException]，确保缓存安全。
     * 如需可修改的副本，请通过 [newBlockStarterRegistry] 获取。
     */
    val blockStarterRegistry: BlockStarterRegistry = BlockStarterRegistry().apply {
        blockStarters.forEach { register(it) }
        freeze()
    }

    /**
     * 创建一个新的 [BlockStarterRegistry] 副本。
     *
     * 与 [blockStarterRegistry] 的区别：返回新实例，可安全添加额外的 starter。
     */
    fun newBlockStarterRegistry(): BlockStarterRegistry {
        return BlockStarterRegistry().apply {
            blockStarters.forEach { register(it) }
        }
    }

    /**
     * 创建一个新的 [PostProcessorRegistry]。
     *
     * 每次调用返回新实例，以便调用方可以追加自定义后处理器。
     */
    fun newPostProcessorRegistry(): PostProcessorRegistry {
        return PostProcessorRegistry().apply {
            postProcessors.forEach { register(it) }
        }
    }

    companion object {
        /**
         * `object` 单例方言的全局缓存。
         *
         * 仅缓存已注册的单例方言（内置 + 通过 [registerSingleton] 注册的），
         * 这些方言的生命周期与应用相同，全局只需一份配置。
         * 条目数量有限（等于已注册单例的个数），不会无序增长。
         */
        private val singletonCache = mutableMapOf<MarkdownFlavour, FlavourCache>()

        /**
         * 已注册为"可全局缓存"的单例方言集合。
         *
         * 使用显式注册而非反射检测，确保跨平台行为一致。
         * 内置的 [CommonMarkFlavour]、[GFMFlavour]、[ExtendedFlavour] 默认注册。
         * 第三方 `object` 方言可通过 [registerSingleton] 注册。
         */
        private val registeredSingletons = mutableSetOf<MarkdownFlavour>(
            CommonMarkFlavour,
            GFMFlavour,
            ExtendedFlavour,
            MarkdownExtraFlavour,
        )

        /**
         * 注册一个 `object` 单例方言，使其可被全局缓存。
         *
         * 适用于第三方库提供的 `object` 方言。注册后，[of] 会为其提供全局缓存。
         */
        fun registerSingleton(flavour: MarkdownFlavour) {
            registeredSingletons.add(flavour)
        }

        private fun isSingleton(flavour: MarkdownFlavour): Boolean {
            return flavour in registeredSingletons
        }

        /**
         * 获取指定 Flavour 的配置快照。
         *
         * - 对于已注册的单例方言：从全局缓存获取，多次调用返回同一实例。
         * - 对于未注册的自定义方言实例：**每次创建新的 FlavourCache**，不进入全局缓存。
         *   返回的实例由调用方持有，随调用方 GC 自动释放，无内存泄漏风险。
         */
        fun of(flavour: MarkdownFlavour): FlavourCache {
            return if (isSingleton(flavour)) {
                singletonCache.getOrPut(flavour) { FlavourCache(flavour) }
            } else {
                // 自定义方言：直接创建，不缓存。生命周期由调用方管理。
                FlavourCache(flavour)
            }
        }

        /**
         * 清除指定单例 Flavour 的缓存。
         * 下次访问时会重新创建。对自定义方言调用无效果（它们不在全局缓存中）。
         */
        fun invalidate(flavour: MarkdownFlavour) {
            singletonCache.remove(flavour)
        }

        /**
         * 清除所有全局缓存。
         */
        fun clearAll() {
            singletonCache.clear()
        }

        /**
         * 当前全局缓存的 Flavour 数量（仅含单例，用于测试/调试）。
         */
        val cacheSize: Int get() = singletonCache.size
    }
}
