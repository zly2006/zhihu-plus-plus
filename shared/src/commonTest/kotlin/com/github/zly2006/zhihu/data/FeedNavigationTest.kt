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

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FeedNavigationTest {
    @Test
    fun filteredCardsDoNotTreatFilterReasonsAsContentPreviews() {
        val route = Article(type = ArticleType.Answer, id = 123, title = "实际标题")
        val item = FeedDisplayItem(
            title = "已屏蔽",
            summary = "规则：未关注作者",
            details = "",
            feed = null,
            isFiltered = true,
            navDestinationJson = route.toFeedDisplayItemNavDestinationJson(),
        )
        val model = ArticleViewModel(assertIs<Article>(item.navDestination), null)
        assertEquals("实际标题", model.title)
        assertEquals("", model.previewExcerpt)
    }

    @Test
    fun urlOnlyListDestinationsUseVisibleMetadataOnTheDetailFirstFrame() {
        listOf(ArticleType.Answer, ArticleType.Article).forEach { type ->
            val route = Article(type = type, id = 123)
            val item = FeedDisplayItem(
                title = "列表标题",
                summary = "列表摘要",
                details = "",
                feed = null,
                authorName = "列表作者",
                avatarSrc = "https://example.com/avatar.png",
                navDestinationJson = route.toFeedDisplayItemNavDestinationJson(),
            )
            val model = ArticleViewModel(assertIs<Article>(item.navDestination), null)
            assertEquals(item.title, model.title)
            assertEquals(item.authorName, model.authorName)
            assertEquals(item.avatarSrc, model.authorAvatarSrc)
            assertEquals(item.summary, model.previewExcerpt)
            assertEquals("", model.content)
            assertEquals(route.toFeedDisplayItemNavDestinationJson(), item.stableKey)
        }
        val question = FeedDisplayItem(
            title = "问题标题",
            summary = "",
            details = "",
            feed = null,
            navDestinationJson = Question(456).toFeedDisplayItemNavDestinationJson(),
        )
        assertEquals("问题标题", assertIs<Question>(question.navDestination).title)
    }

    @Test
    fun listMetadataDoesNotOverwriteKnownDestinationMetadata() {
        val route = Article(
            type = ArticleType.Answer,
            id = 123,
            title = "原始标题",
            authorName = "真实作者",
            authorBio = "签名",
            avatarSrc = "avatar",
            excerpt = "原始摘要",
            authorId = "author-id",
            authorUrlToken = "author-token",
            questionId = 456,
            readingQueueSourceId = "source",
        )
        val item = FeedDisplayItem(
            title = "列表展示标题",
            summary = "列表展示摘要",
            details = "",
            feed = null,
            authorName = "展示作者",
            avatarSrc = "展示头像",
            navDestinationJson = route.toFeedDisplayItemNavDestinationJson(),
        )
        val restored = assertIs<Article>(item.navDestination)
        assertEquals(route.toFeedDisplayItemNavDestinationJson(), restored.toFeedDisplayItemNavDestinationJson())
    }

    @Test
    fun feedDisplayItemRestoresExplicitNavigationJson() {
        val destination = Search(query = "fixture")
        val item = FeedDisplayItem(
            title = "标题",
            summary = "摘要",
            details = "详情",
            feed = null,
            navDestinationJson = destination.toFeedDisplayItemNavDestinationJson(),
        )

        assertEquals(destination, item.navDestination)
        assertEquals(destination.toFeedDisplayItemNavDestinationJson(), item.stableKey)
    }
}
