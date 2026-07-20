package com.hrm.markdown.renderer.internal.layout.engine

import com.hrm.markdown.renderer.internal.core.compile.RenderBlockCatalog
import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity
import com.hrm.markdown.renderer.internal.core.model.InternalRenderBlockModel
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutBlockModel

internal sealed interface LazyLayoutBlockResult {
    data class Ready(
        val block: InternalLayoutBlockModel,
    ) : LazyLayoutBlockResult

    data class Failed(
        val identity: RenderIdentity,
        val message: String,
    ) : LazyLayoutBlockResult
}

internal class LazyMarkdownLayoutSession(
    private val catalog: RenderBlockCatalog,
    private val maxCachedBlocks: Int = 64,
    private val layoutBlock: (InternalRenderBlockModel) -> InternalLayoutBlockModel,
) {
    private data class CacheEntry(
        val identity: RenderIdentity,
        val result: LazyLayoutBlockResult,
    )

    private val cache = LinkedHashMap<Int, CacheEntry>()
    private val accessOrder = ArrayDeque<Int>()

    var layoutInvocationCount: Int = 0
        private set

    val cachedBlockCount: Int
        get() = cache.size

    init {
        require(maxCachedBlocks > 0) { "maxCachedBlocks must be positive" }
    }

    fun layout(index: Int): LazyLayoutBlockResult? {
        val identity = catalog.identityAt(index)
        cache[index]?.takeIf { it.identity == identity }?.let { entry ->
            recordAccess(index)
            return entry.result
        }

        cache.remove(index)
        accessOrder.remove(index)
        val renderBlock = catalog.compile(index) ?: return null
        layoutInvocationCount++
        val result = try {
            LazyLayoutBlockResult.Ready(layoutBlock(renderBlock))
        } catch (error: Exception) {
            LazyLayoutBlockResult.Failed(
                identity = identity,
                message = error.message ?: error::class.simpleName.orEmpty(),
            )
        }
        cache[index] = CacheEntry(identity, result)
        recordAccess(index)
        trimCache()
        return result
    }

    private fun recordAccess(index: Int) {
        accessOrder.remove(index)
        accessOrder.addLast(index)
    }

    private fun trimCache() {
        while (cache.size > maxCachedBlocks) {
            val evictedIndex = accessOrder.removeFirst()
            cache.remove(evictedIndex)
            catalog.evict(evictedIndex)
        }
    }
}
