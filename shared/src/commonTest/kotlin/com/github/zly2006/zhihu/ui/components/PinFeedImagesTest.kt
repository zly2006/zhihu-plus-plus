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

import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.shared.data.toDisplayItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PinFeedImagesTest {
    @Test
    fun selectsImageLayoutFromRealPinImageCounts() {
        assertNull(pinFeedImageLayout(0))
        assertEquals(PinFeedImageLayout.SINGLE, pinFeedImageLayout(1))
        assertEquals(PinFeedImageLayout.MULTI_ROW, pinFeedImageLayout(3))
        assertEquals(PinFeedImageLayout.MULTI_ROW, pinFeedImageLayout(4))
        assertEquals(PinFeedImageLayout.NINE_GRID, pinFeedImageLayout(5))
        assertEquals(PinFeedImageLayout.NINE_GRID, pinFeedImageLayout(9))
        assertEquals(PinFeedImageLayout.NINE_GRID, pinFeedImageLayout(18))
    }

    @Test
    fun mapsTypedPinImagesToFeedDisplayItemInApiOrder() {
        val item = CommonFeed(
            id = "pin-feed",
            target = pinTarget(
                id = NORMAL_MULTI_PIN_ID,
                imageCount = 3,
            ),
        ).toDisplayItem()

        assertEquals(3, item.pinImages.size)
        assertEquals(
            listOf(
                "https://pic.example/0.jpg",
                "https://pic.example/1.jpg",
                "https://pic.example/2.jpg",
            ),
            item.pinImages.map { it.url },
        )
    }

    @Test
    fun keepsAllNineGridImagesForRendering() {
        val item = CommonFeed(
            id = "pin-feed",
            target = pinTarget(
                id = NINE_GRID_PIN_ID,
                imageCount = 9,
            ),
        ).toDisplayItem()

        assertEquals(9, item.pinImages.size)
    }

    private fun pinTarget(
        id: Long,
        imageCount: Int,
    ) = Feed.PinTarget(
        id = id,
        url = "https://www.zhihu.com/pin/$id",
        author = Person(
            id = "author-id",
            url = "https://www.zhihu.com/people/author",
            userType = "people",
            urlToken = "author",
            name = "作者",
            headline = "",
            avatarUrl = "",
        ),
        content = buildList {
            add(DataHolder.Pin.ContentText(content = "正文", title = ""))
            repeat(imageCount) { index ->
                add(
                    DataHolder.Pin.ContentImage(
                        url = "https://pic.example/$index.jpg",
                        width = 1200,
                        height = 800,
                    ),
                )
            }
        },
    )

    private companion object {
        const val NORMAL_MULTI_PIN_ID = 2063687667297079630L
        const val NINE_GRID_PIN_ID = 2064056340616631575L
    }
}
