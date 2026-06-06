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

package com.github.zly2006.zhihu.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ImagePreviewGalleryTest {
    @Test
    fun clicked_document_image_keeps_gallery_order_and_initial_page() {
        val urls = imagePreviewUrlsForClicked(
            documentImageUrls = listOf(
                "https://pic1.zhimg.com/v2-a.jpg",
                "https://pic1.zhimg.com/v2-b.jpg",
                "https://pic1.zhimg.com/v2-c.jpg",
            ),
            clickedUrl = "https://pic1.zhimg.com/v2-b.jpg",
        )

        assertEquals(
            listOf(
                "https://pic1.zhimg.com/v2-a.jpg",
                "https://pic1.zhimg.com/v2-b.jpg",
                "https://pic1.zhimg.com/v2-c.jpg",
            ),
            urls,
        )
        assertEquals(1, imagePreviewInitialPage(urls, urls.indexOf("https://pic1.zhimg.com/v2-b.jpg")))
    }

    @Test
    fun clicked_image_missing_from_document_is_appended_once() {
        val urls = imagePreviewUrlsForClicked(
            documentImageUrls = listOf(
                "https://pic1.zhimg.com/v2-a.jpg",
                "https://pic1.zhimg.com/v2-a.jpg",
                "data:image/png;base64,abc",
            ),
            clickedUrl = "https://pic1.zhimg.com/v2-b.jpg",
        )

        assertEquals(
            listOf(
                "https://pic1.zhimg.com/v2-a.jpg",
                "https://pic1.zhimg.com/v2-b.jpg",
            ),
            urls,
        )
    }

    @Test
    fun initial_page_is_clamped_for_single_image_and_empty_gallery() {
        assertEquals(0, imagePreviewInitialPage(listOf("https://pic1.zhimg.com/v2-a.jpg"), 5))
        assertEquals(0, imagePreviewInitialPage(emptyList(), 5))
    }
}
