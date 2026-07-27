/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.ui.components

import com.github.zly2006.zhihu.shared.data.DataHolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PinFeedImagesTest {
    @Test
    fun selectsImageLayoutFromRealPinImageCounts() {
        assertNull(pinFeedImageLayout(0))
        assertEquals(PinFeedImageLayout.SINGLE, pinFeedImageLayout(1))
        assertEquals(PinFeedImageLayout.MULTI_ROW, pinFeedImageLayout(2))
        assertEquals(PinFeedImageLayout.MULTI_ROW, pinFeedImageLayout(3))
        assertEquals(PinFeedImageLayout.MULTI_ROW, pinFeedImageLayout(4))
        assertEquals(PinFeedImageLayout.NINE_GRID, pinFeedImageLayout(5))
        assertEquals(PinFeedImageLayout.NINE_GRID, pinFeedImageLayout(9))
        assertEquals(PinFeedImageLayout.NINE_GRID, pinFeedImageLayout(18))
    }

    @Test
    fun usesDedicatedThumbnailAndFallsBackToFeedSizedUrl() {
        val image = DataHolder.Pin.ContentImage(
            url = "https://pic.example/100/image_720w.jpg",
            thumbnail = "https://pic.example/thumbnail.jpg",
            width = 1200,
            height = 800,
        )
        assertEquals("https://pic.example/thumbnail.jpg", image.feedThumbnailUrl)
        assertEquals(
            "https://pic.example/100/image_720w.jpg",
            image.copy(thumbnail = "").feedThumbnailUrl,
        )
    }
}
