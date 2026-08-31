/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.hrm.latex.renderer.layout

import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.model.RenderContext

/**
 * NodeLayout 缓存：相同 AST 子树 + 相同 RenderContext → 复用 layout，避免重复测量。
 *
 * 缓存 key 设计：
 * - LatexNode 是 data class，其 equals/hashCode 基于结构内容（子节点、属性等）。
 * - RenderContext 是 data class，其 equals/hashCode 覆盖所有影响布局的属性
 *   （fontSize, fontFamily, mathStyle 等）。
 * - 二者组合后天然具备"相同输入 → 相同输出"的语义，无需自定义 key。
 *
 * 生命周期：
 * - 每次顶层 `LatexRenderer.measure()` 调用前创建新的 LayoutCache 实例。
 * - 同一个测量周期内，相同子树+上下文的重复测量（如同一符号在不同位置出现）
 *   命中缓存直接返回。
 * - 跨测量周期通过 `invalidate()` 选择性失效或整体丢弃。
 *
 * 容量策略：
 * - 默认最大容量 2048 个条目，覆盖绝大多数公式场景。
 * - 超出容量时采用 LRU 淘汰最久未使用的条目。
 *
 * 线程安全：
 * - LayoutCache 仅在 Composition 主线程使用（measureNode/measureGroup 在主线程调用），
 *   不需要同步。
 */
internal class LayoutCache(
    private val maxSize: Int = DEFAULT_MAX_SIZE
) {
    /**
     * 缓存 key：节点 + 影响布局的上下文。
     *
     * 使用 [LatexNode] 的 data class equals 和 [RenderContext] 的 data class equals。
     *
     * 注意：equationNumbering 字段被排除在 key 之外，因为：
     * 1. 它是 mutable class（非 data class），reference equality 导致跨测量周期永远不命中
     * 2. 公式编号不影响子节点的布局尺寸（仅影响外层 wrap）
     * 通过使用不含 equationNumbering 的 context 副本作为 key，确保增量场景命中率。
     */
    private data class CacheKey(
        val node: LatexNode,
        val context: RenderContext
    )

    /**
     * 节点列表的缓存 key，用于 measureGroup 级别的缓存。
     */
    private data class GroupCacheKey(
        val nodes: List<LatexNode>,
        val context: RenderContext
    )

    /**
     * 从 context 中移除不影响节点布局的临时字段，生成稳定的缓存 key 用 context。
     */
    private fun normalizeContext(context: RenderContext): RenderContext {
        return if (context.equationNumbering != null) {
            context.copy(equationNumbering = null)
        } else {
            context
        }
    }

    // 跨平台 LRU 缓存（LinkedHashMap 的 accessOrder 和 removeEldestEntry 是 JVM 特有 API）
    private val nodeCache = LruCache<CacheKey, NodeLayout>(maxSize)
    private val groupCache = LruCache<GroupCacheKey, NodeLayout>(maxSize / 4)

    // ── 统计信息（调试用）──
    var hits: Int = 0
        private set
    var misses: Int = 0
        private set

    /**
     * 查询单节点缓存。
     *
     * @return 缓存的 NodeLayout，未命中时返回 null
     */
    fun getNode(node: LatexNode, context: RenderContext): NodeLayout? {
        val key = CacheKey(node, normalizeContext(context))
        val cached = nodeCache[key]
        if (cached != null) {
            hits++
        } else {
            misses++
        }
        return cached
    }

    /**
     * 写入单节点缓存。
     */
    fun putNode(node: LatexNode, context: RenderContext, layout: NodeLayout) {
        val key = CacheKey(node, normalizeContext(context))
        nodeCache[key] = layout
    }

    /**
     * 查询节点组缓存。
     *
     * @return 缓存的 NodeLayout，未命中时返回 null
     */
    fun getGroup(nodes: List<LatexNode>, context: RenderContext): NodeLayout? {
        val key = GroupCacheKey(nodes, normalizeContext(context))
        return groupCache[key]
    }

    /**
     * 写入节点组缓存。
     */
    fun putGroup(nodes: List<LatexNode>, context: RenderContext, layout: NodeLayout) {
        val key = GroupCacheKey(nodes, normalizeContext(context))
        groupCache[key] = layout
    }

    /**
     * 清空所有缓存。
     * 在新的渲染周期开始时调用（字体、配置可能已变化）。
     */
    fun clear() {
        nodeCache.clear()
        groupCache.clear()
        hits = 0
        misses = 0
    }

    /**
     * 当前缓存大小。
     */
    val size: Int get() = nodeCache.size + groupCache.size

    companion object {
        /** 默认最大缓存条目数 */
        const val DEFAULT_MAX_SIZE = 2048
    }
}

/**
 * 跨平台 LRU 缓存实现。
 *
 * 使用 [LinkedHashMap] 的插入顺序（Kotlin 跨平台默认行为）+
 * 手动 access-order 提升（get 时 remove+put）实现 LRU 语义。
 * 容量超限时淘汰最早插入/最久未访问的条目。
 */
private class LruCache<K, V>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>()

    val size: Int get() = map.size

    operator fun get(key: K): V? {
        val value = map[key] ?: return null
        // 提升访问顺序：移除后重新插入到末尾
        map.remove(key)
        map[key] = value
        return value
    }

    operator fun set(key: K, value: V) {
        map.remove(key) // 确保新插入到末尾
        map[key] = value
        // 超容量时淘汰最老条目（迭代器第一个元素）
        if (map.size > maxSize) {
            val eldest = map.keys.iterator().next()
            map.remove(eldest)
        }
    }

    fun clear() {
        map.clear()
    }
}
