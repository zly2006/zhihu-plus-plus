package com.hrm.markdown.renderer.internal.layout.inline

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density

internal class InlineFlowLayoutCache(
    private val maxEntries: Int = DefaultMaxEntries,
) {
    private val entries = mutableMapOf<InlineFlowLayoutCacheKey, InlineFlowLayout>()

    fun getOrPut(
        epoch: InlineLayoutEpoch,
        layoutRevision: Long,
        widthPx: Float,
        maxLines: Int,
        style: TextStyle,
        density: Density,
        textMeasurer: TextMeasurer,
        compute: () -> InlineFlowLayout,
    ): InlineFlowLayout {
        val key = InlineFlowLayoutCacheKey(
            epoch = epoch,
            layoutRevision = layoutRevision,
            widthBits = widthPx.toBits(),
            maxLines = maxLines,
            styleHash = style.hashCode(),
            densityBits = density.density.toBits(),
            fontScaleBits = density.fontScale.toBits(),
            textMeasurerHash = textMeasurer.hashCode(),
        )
        entries[key]?.let { layout ->
            touch(key)
            return layout
        }
        if (entries.size >= maxEntries) {
            evictEldest()
        }
        return compute().also { layout ->
            entries[key] = layout
            accessOrder += key
        }
    }

    fun clear() {
        entries.clear()
        accessOrder.clear()
    }

    private val accessOrder = ArrayList<InlineFlowLayoutCacheKey>(maxEntries)

    private fun touch(key: InlineFlowLayoutCacheKey) {
        accessOrder.remove(key)
        accessOrder += key
    }

    private fun evictEldest() {
        val eldest = accessOrder.removeFirstOrNull() ?: entries.keys.firstOrNull() ?: return
        entries.remove(eldest)
    }

    private data class InlineFlowLayoutCacheKey(
        val epoch: InlineLayoutEpoch,
        val layoutRevision: Long,
        val widthBits: Int,
        val maxLines: Int,
        val styleHash: Int,
        val densityBits: Int,
        val fontScaleBits: Int,
        val textMeasurerHash: Int,
    )

    private companion object {
        const val DefaultMaxEntries = 2048
    }
}
