package com.hrm.markdown.renderer.internal.layout.inline

import androidx.compose.ui.text.TextStyle
import com.hrm.markdown.renderer.inline.InlineRenderResult

internal class InlineRenderResultCache(
    private val maxEntries: Int = DefaultMaxEntries,
) {
    private val entries = mutableMapOf<InlineRenderResultCacheKey, InlineRenderResult>()
    private val accessOrder = ArrayList<InlineRenderResultCacheKey>(maxEntries)

    fun getOrPut(
        epoch: InlineLayoutEpoch,
        stableId: Long,
        contentRevision: Long,
        style: TextStyle,
        compute: () -> InlineRenderResult,
    ): InlineRenderResult {
        val key = InlineRenderResultCacheKey(
            epoch = epoch,
            stableId = stableId,
            contentRevision = contentRevision,
            styleHash = style.hashCode(),
        )
        entries[key]?.let { result ->
            touch(key)
            return result
        }
        if (entries.size >= maxEntries) {
            evictEldest()
        }
        return compute().also { result ->
            entries[key] = result
            accessOrder += key
        }
    }

    fun clear() {
        entries.clear()
        accessOrder.clear()
    }

    private fun touch(key: InlineRenderResultCacheKey) {
        accessOrder.remove(key)
        accessOrder += key
    }

    private fun evictEldest() {
        val eldest = accessOrder.removeFirstOrNull() ?: entries.keys.firstOrNull() ?: return
        entries.remove(eldest)
    }

    private data class InlineRenderResultCacheKey(
        val epoch: InlineLayoutEpoch,
        val stableId: Long,
        val contentRevision: Long,
        val styleHash: Int,
    )

    private companion object {
        const val DefaultMaxEntries = 2048
    }
}
