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

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentOpenEventSupportTest {
    @Test
    fun inferOpenFrom_mapsKnownSourceAndTargetPairs() {
        assertEquals(
            ContentOpenFrom.HOME_FEED,
            ContentOpenEventSupport.inferOpenFrom(
                source = Home,
                target = Article(type = ArticleType.Answer, id = 1L),
            ),
        )
        assertEquals(
            ContentOpenFrom.QUESTION_FEED,
            ContentOpenEventSupport.inferOpenFrom(
                source = Question(questionId = 10L),
                target = Article(type = ArticleType.Answer, id = 2L),
            ),
        )
        assertEquals(
            ContentOpenFrom.ANSWER_SWITCH,
            ContentOpenEventSupport.inferOpenFrom(
                source = Article(type = ArticleType.Answer, id = 2L),
                target = Article(type = ArticleType.Answer, id = 3L),
            ),
        )
        assertEquals(
            ContentOpenFrom.COLLECTION,
            ContentOpenEventSupport.inferOpenFrom(
                source = CollectionContent(collectionId = "fav"),
                target = Article(type = ArticleType.Answer, id = 4L),
            ),
        )
        assertEquals(
            ContentOpenFrom.HISTORY,
            ContentOpenEventSupport.inferOpenFrom(
                source = History,
                target = Pin(id = 5L),
            ),
        )
        assertEquals(
            ContentOpenFrom.NOTIFICATION,
            ContentOpenEventSupport.inferOpenFrom(
                source = Notification,
                target = Question(questionId = 6L),
            ),
        )
    }

    @Test
    fun toTrackedContentIdentity_returnsSupportedContentTypes() {
        assertEquals(
            TrackedContentIdentity(ContentType.ANSWER, "11"),
            ContentOpenEventSupport.toTrackedContentIdentity(Article(type = ArticleType.Answer, id = 11L)),
        )
        assertEquals(
            TrackedContentIdentity(ContentType.ARTICLE, "12"),
            ContentOpenEventSupport.toTrackedContentIdentity(Article(type = ArticleType.Article, id = 12L)),
        )
        assertEquals(
            TrackedContentIdentity(ContentType.QUESTION, "13"),
            ContentOpenEventSupport.toTrackedContentIdentity(Question(questionId = 13L)),
        )
        assertEquals(
            TrackedContentIdentity(ContentType.PIN, "14"),
            ContentOpenEventSupport.toTrackedContentIdentity(Pin(id = 14L)),
        )
        assertNull(
            ContentOpenEventSupport.toTrackedContentIdentity(
                Person(id = "u1", urlToken = "user-1"),
            ),
        )
    }

    @Test
    fun filterUnopenedAnswerArticles_excludesCurrentHistoryAndOpenedAnswers() {
        val filtered = ContentOpenEventSupport.filterUnopenedAnswerArticles(
            candidates = listOf(
                Article(type = ArticleType.Answer, id = 10L),
                Article(type = ArticleType.Answer, id = 11L),
                Article(type = ArticleType.Answer, id = 12L),
                Article(type = ArticleType.Article, id = 13L),
            ),
            openedContentKeys = setOf(ContentOpenEventSupport.buildContentKey(ContentType.ANSWER, "12")),
            currentArticleId = 10L,
            historyIds = setOf(11L),
        )

        assertEquals(emptyList<Article>(), filtered)
    }

    @Test
    fun partitionQuestionAnswerCandidates_movesOpenedAnswersToPreviousAndKeepsFreshNext() {
        val partition = ContentOpenEventSupport.partitionQuestionAnswerCandidates(
            candidates = listOf(
                Article(type = ArticleType.Answer, id = 10L),
                Article(type = ArticleType.Answer, id = 11L),
                Article(type = ArticleType.Answer, id = 12L),
                Article(type = ArticleType.Answer, id = 13L),
                Article(type = ArticleType.Article, id = 14L),
            ),
            openedAnswerIds = setOf(11L, 12L),
            currentArticleId = 10L,
        )

        assertEquals(
            listOf(
                Article(type = ArticleType.Answer, id = 11L),
                Article(type = ArticleType.Answer, id = 12L),
            ),
            partition.previousCandidates,
        )
        assertEquals(
            listOf(Article(type = ArticleType.Answer, id = 13L)),
            partition.nextCandidates,
        )
    }
}
