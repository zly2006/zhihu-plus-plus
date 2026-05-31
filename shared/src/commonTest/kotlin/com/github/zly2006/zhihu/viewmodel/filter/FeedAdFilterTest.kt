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

package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.shared.data.DataHolder
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedAdFilterTest {
    @Test
    fun detectsConfiguredLinkBasedAds() {
        val content = filterable(
            raw = article("<p>read more at https://mp.weixin.qq.com/s/example</p>"),
        )

        assertEquals("微信公众号文章", getFeedAdBlockReason(content, FeedAdBlockSettings()))
        assertEquals(
            null,
            getFeedAdBlockReason(
                content,
                FeedAdBlockSettings(blockWeChatOfficialAccount = false),
            ),
        )
    }

    @Test
    fun detectsPaidContentWhenEnabled() {
        val content = filterable(
            raw = article(
                body = "<p>paid article</p>",
                paid = true,
            ),
        )

        assertTrue(isFeedAdOrPaidContent(content))
        assertEquals("知乎盐选付费内容", getFeedAdBlockReason(content, FeedAdBlockSettings()))
        assertFalse(getFeedAdBlockReason(content, FeedAdBlockSettings(blockPaidContent = false)) == "知乎盐选付费内容")
    }

    private fun filterable(raw: DataHolder.Content): FilterableContent = FilterableContent(
        title = "title",
        summary = null,
        content = null,
        authorName = "author",
        authorId = "author-id",
        contentId = "content-id",
        contentType = "article",
        raw = raw,
    )

    private fun article(
        body: String,
        paid: Boolean = false,
    ): DataHolder.Article = DataHolder.Article(
        id = 1L,
        author = author(),
        canComment = DataHolder.CanComment(status = true, reason = ""),
        title = "title",
        content = body,
        excerpt = "excerpt",
        type = "article",
        created = 1L,
        updated = 1L,
        url = "https://www.zhihu.com/p/1",
        voteupCount = 0,
        paidInfo = if (paid) buildJsonObject { } else null,
    )

    private fun author(): DataHolder.Author = DataHolder.Author(
        avatarUrl = "",
        gender = 0,
        headline = "",
        id = "author-id",
        isAdvertiser = false,
        isOrg = false,
        name = "author",
        type = "people",
        url = "",
        urlToken = "author",
        userType = "people",
    )
}
