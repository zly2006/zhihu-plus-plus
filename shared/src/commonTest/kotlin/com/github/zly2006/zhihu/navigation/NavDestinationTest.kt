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

package com.github.zly2006.zhihu.navigation

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NavDestinationTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun serializesSearchDestinationFromCommonCode() {
        val destination: NavDestination = Search(query = "kmp")

        val decoded = json.decodeFromString<NavDestination>(
            json.encodeToString<NavDestination>(destination),
        )

        assertEquals(destination, decoded)
    }

    @Test
    fun resolvesZhihuAnswerUrlFromCommonCode() {
        val destination = resolveContent("https://www.zhihu.com/question/1/answer/42")

        val article = assertIs<Article>(destination)
        assertEquals(ArticleType.Answer, article.type)
        assertEquals(42L, article.id)
    }

    @Test
    fun articleNavigationIdentityIgnoresMutableDisplayFields() {
        val article = Article(type = ArticleType.Answer, id = 42L, title = "loading...")
        val hash = article.hashCode()
        val key = article.toString()

        article.title = "loaded title"
        article.authorName = "loaded author"
        article.avatarSrc = "https://example.invalid/avatar.png"

        assertEquals(Article(type = ArticleType.Answer, id = 42L), article)
        assertEquals(hash, article.hashCode())
        assertEquals(key, article.toString())
    }

    @Test
    fun personNavigationIdentitySurvivesProfileBackfillAndSerialization() {
        val route = Person(
            id = Person.EMPTY_ID,
            urlToken = "initial-token",
            name = "loading...",
        )
        val hash = route.hashCode()
        val key = route.toString()

        route.id = "loaded-id"
        route.urlToken = "loaded-token"

        assertEquals(hash, route.hashCode())
        assertEquals(key, route.toString())

        val decoded = assertIs<Person>(
            json.decodeFromString<NavDestination>(
                json.encodeToString<NavDestination>(route),
            ),
        )
        assertEquals(route, decoded)
        assertEquals(key, decoded.toString())
    }

    @Test
    fun resolvesAnswerCommentDeepLinkWithAnchor() {
        val destination = resolveContent(
            "zhihu://comment/list/answer/42?anchor_comment_id=123456&is_child=false",
        )

        val holder = assertIs<CommentHolder>(destination)
        val article = assertIs<Article>(holder.article)
        assertEquals(ArticleType.Answer, article.type)
        assertEquals(42L, article.id)
        assertEquals("123456", holder.commentId)
    }

    @Test
    fun resolvesArticleCommentDeepLinkWithAnchor() {
        val destination = resolveContent(
            "zhihu://comment/list/article/43?anchor_comment_id=123457&is_child=false",
        )

        val holder = assertIs<CommentHolder>(destination)
        val article = assertIs<Article>(holder.article)
        assertEquals(ArticleType.Article, article.type)
        assertEquals(43L, article.id)
        assertEquals("123457", holder.commentId)
    }

    @Test
    fun resolvesPinCommentDeepLinkWithAnchor() {
        val destination = resolveContent(
            "zhihu://comment/list/pin/44?anchor_comment_id=123458&is_child=true",
        )

        val holder = assertIs<CommentHolder>(destination)
        val pin = assertIs<Pin>(holder.article)
        assertEquals(44L, pin.id)
        assertEquals("123458", holder.commentId)
    }

    @Test
    fun resolvesQuestionCommentDeepLinkWithAnchor() {
        val destination = resolveContent(
            "zhihu://comment/list/question/45?anchor_comment_id=123459&is_child=false",
        )

        val holder = assertIs<CommentHolder>(destination)
        val question = assertIs<Question>(holder.article)
        assertEquals(45L, question.questionId)
        assertEquals("123459", holder.commentId)
    }

    @Test
    fun resolvesCommentDeepLinkWithExtraAndroidParameters() {
        val destination = resolveContent(
            "zhihu://comment/list/answer/46?anchor_comment_id=123460&list_height_ratio=0.66&dragIconVisible=true&segment=%7B%22id%22%3A1%7D",
        )

        val holder = assertIs<CommentHolder>(destination)
        val article = assertIs<Article>(holder.article)
        assertEquals(ArticleType.Answer, article.type)
        assertEquals(46L, article.id)
        assertEquals("123460", holder.commentId)
    }

    @Test
    fun resolvesZhihuAppViewPinUrlFromCommonCode() {
        val destination = resolveContent("https://www.zhihu.com/appview/pin/2059710318939301395")

        val pin = assertIs<Pin>(destination)
        assertEquals(2059710318939301395L, pin.id)
    }

    @Test
    fun resolvesZhihuAppViewAnswerUrlFromCommonCode() {
        val destination = resolveContent("https://www.zhihu.com/appview/answer/2040633177593619876")

        val article = assertIs<Article>(destination)
        assertEquals(ArticleType.Answer, article.type)
        assertEquals(2040633177593619876L, article.id)
    }

    @Test
    fun resolvesZhihuAppViewArticleUrlFromCommonCode() {
        val destination = resolveContent("https://www.zhihu.com/appview/p/1981671287999981270")

        val article = assertIs<Article>(destination)
        assertEquals(ArticleType.Article, article.type)
        assertEquals(1981671287999981270L, article.id)
    }

    @Test
    fun resolvesNotificationTimelineEntryFromOfficialMessageLink() {
        val destination = resolveContent(
            "https://www.zhihu.com/notifications/v3/timeline/entry/system?title=%E7%B3%BB%E7%BB%9F%E6%B6%88%E6%81%AF",
        )

        val entry = assertIs<Notification.Entry>(destination)
        assertEquals("system", entry.entryName)
        assertEquals("系统消息", entry.title)
    }

    @Test
    fun resolvesInvitationAnswerPageFromOfficialMessageLink() {
        val destination = resolveContent(
            "https://www.zhihu.com/compose_answer_tab?default_selected_page=2&title=%E9%82%80%E8%AF%B7%E5%9B%9E%E7%AD%94",
        )

        assertEquals(Notification.Invitations, destination)
    }

    @Test
    fun resolvesPrivateMessageFromOfficialInboxLink() {
        val destination = resolveContent(
            "https://www.zhihu.com/inbox/peer-token?title=%E7%9F%A5%E4%B9%8E%E5%B0%8F%E7%AE%A1%E5%AE%B6&source_type=message_list",
        )

        val message = assertIs<Notification.Message>(destination)
        assertEquals("peer-token", message.peerId)
        assertEquals("知乎小管家", message.name)
    }
}
