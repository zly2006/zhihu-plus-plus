/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
