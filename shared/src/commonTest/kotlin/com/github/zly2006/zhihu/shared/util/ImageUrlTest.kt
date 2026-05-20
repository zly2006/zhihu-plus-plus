package com.github.zly2006.zhihu.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImageUrlTest {
    @Test
    fun prefersOriginalToken() {
        val url = extractImageUrl(
            mapOf(
                "data-original-token" to "v2-token",
                "data-original" to "https://example.com/original.jpg",
            )::getValue,
        )

        assertEquals("https://pic1.zhimg.com/v2-token", url)
    }

    @Test
    fun fallsBackByAttributePriority() {
        val url = extractImageUrl(
            mapOf(
                "data-actualsrc" to "https://example.com/actual.jpg",
                "data-thumbnail" to "https://example.com/thumb.jpg",
                "src" to "https://example.com/src.jpg",
            ).withDefault { "" }::getValue,
        )

        assertEquals("https://example.com/actual.jpg", url)
    }

    @Test
    fun returnsNullWhenAllAttributesBlank() {
        assertNull(extractImageUrl { "" })
    }
}
