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

package com.github.zly2006.zhihu.navigation

import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuestionAnswerNavigatorTest {
    @Test
    fun seededQuestionAnswerListKeepsClickedPositionForSwitching() = runTest {
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            initialNextAnswers = listOf(answer(103L), answer(104L)),
            initialPreviousAnswers = listOf(answer(101L), answer(100L)),
            initialNextUrl = "https://www.zhihu.com/api/v4/questions/1/feeds?limit=20&order=updated&offset=20",
            order = "updated",
            getAlreadyOpenedAnswerIds = { emptySet() },
            environment = NoopEnvironment,
        )

        assertEquals(101L, navigator.previousAnswerPreview?.article?.id)
        assertEquals(103L, navigator.loadNext()?.id)
        assertEquals(104L, navigator.loadNext()?.id)
    }

    @Test
    fun questionFeedFallbackUsesCurrentSortOrder() = runTest {
        val environment = RecordingEnvironment()
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            order = "updated",
            environment = environment,
        )

        assertNull(navigator.loadNext())
        assertEquals(
            "https://www.zhihu.com/api/v4/questions/1/feeds?limit=6&order=updated",
            environment.requestedUrls.single(),
        )
    }

    @Test
    fun seededQueuesIgnoreNonAnswerEntriesForPreviewAndNextNavigation() = runTest {
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            initialNextAnswers = listOf(
                article(201L),
                answer(103L),
                article(202L),
                answer(104L),
            ),
            initialPreviousAnswers = listOf(
                article(102L),
                answer(101L),
                answer(100L),
            ),
            getAlreadyOpenedAnswerIds = { emptySet() },
            environment = NoopEnvironment,
        )

        assertEquals(101L, navigator.previousAnswerPreview?.article?.id)
        assertEquals(103L, navigator.loadNext()?.id)
        assertEquals(104L, navigator.loadNext()?.id)
        assertNull(navigator.loadNext())
    }

    @Test
    fun emptyPreviousSeedDoesNotExposePreview() = runTest {
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            initialPreviousAnswers = listOf(article(201L)),
            getAlreadyOpenedAnswerIds = { emptySet() },
            environment = NoopEnvironment,
        )

        assertNull(navigator.previousAnswerPreview)
    }

    @Test
    fun seededNextAnswersAreCheckedAgainstOpenedHistoryBeforeDownNavigation() = runTest {
        val openedAnswerIds = setOf(104L, 106L)
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            initialNextAnswers = listOf(103L, 104L, 105L, 106L, 107L).map(::answer),
            getAlreadyOpenedAnswerIds = { ids -> ids.filter { it in openedAnswerIds }.toSet() },
            environment = NoopEnvironment,
        )

        assertEquals(103L, navigator.loadNext()?.id)
        assertEquals(105L, navigator.loadNext()?.id)
        assertEquals(107L, navigator.loadNext()?.id)
        assertNull(navigator.loadNext())
    }

    @Test
    fun repeatedFiveAnswerRoundsNeverNavigateDownToOpenedAnswers() = runTest {
        val rounds = listOf(
            listOf(101L, 102L, 103L, 104L, 105L) to setOf(102L, 104L),
            listOf(201L, 202L, 203L, 204L, 205L) to setOf(201L, 205L),
            listOf(301L, 302L, 303L, 304L, 305L) to setOf(303L),
        )

        rounds.forEachIndexed { roundIndex, (candidateIds, openedAnswerIds) ->
            val navigator = QuestionAnswerNavigator(
                questionId = roundIndex + 1L,
                initialNextAnswers = candidateIds.map(::answer),
                getAlreadyOpenedAnswerIds = { ids -> ids.filter { it in openedAnswerIds }.toSet() },
                environment = NoopEnvironment,
            )

            val expectedFreshIds = candidateIds.filterNot { it in openedAnswerIds }
            expectedFreshIds.forEach { expectedId ->
                assertEquals(expectedId, navigator.loadNext()?.id)
            }
            assertNull(navigator.loadNext())
        }
    }

    @Test
    fun directArticleScreenNavigatorSkipsOpenedAnswersWithoutQuestionScreenSeed() = runTest {
        val environment = FeedEnvironment(listOf(101L, 102L, 103L, 104L))
        val openedAnswerIds = mutableSetOf(101L, 102L)
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            getAlreadyOpenedAnswerIds = { ids -> ids.filterTo(mutableSetOf()) { it in openedAnswerIds } },
            environment = environment,
        )

        navigator.pushAnswer(answer(101L).toCachedContent())
        assertEquals(103L, navigator.loadNext()?.id)

        openedAnswerIds += 103L
        val reopenedNavigator = QuestionAnswerNavigator(
            questionId = 1L,
            getAlreadyOpenedAnswerIds = { ids -> ids.filterTo(mutableSetOf()) { it in openedAnswerIds } },
            environment = environment,
        )
        reopenedNavigator.pushAnswer(answer(101L).toCachedContent())
        assertEquals(104L, reopenedNavigator.loadNext()?.id)
        assertNull(reopenedNavigator.loadNext())
    }

    @Test
    fun directArticleScreenReentrySkipsAnswersViewedInPreviousSession() = runTest {
        val environment = FeedEnvironment(listOf(101L, 102L, 103L, 104L, 105L))
        val openedAnswerIds = mutableSetOf(101L, 102L)
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            getAlreadyOpenedAnswerIds = { ids -> ids.filterTo(mutableSetOf()) { it in openedAnswerIds } },
            environment = environment,
        )

        navigator.pushAnswer(answer(101L).toCachedContent())
        assertEquals(103L, navigator.loadNext()?.id)
        openedAnswerIds += 103L
        navigator.pushAnswer(answer(103L).toCachedContent())
        assertEquals(104L, navigator.loadNext()?.id)
        openedAnswerIds += 104L
        navigator.pushAnswer(answer(104L).toCachedContent())

        navigator.pushAnswer(answer(101L).toCachedContent())
        assertEquals(105L, navigator.loadNext()?.id)
        assertNull(navigator.loadNext())
    }

    private fun answer(id: Long) = Article(id = id, type = ArticleType.Answer)

    private fun article(id: Long) = Article(id = id, type = ArticleType.Article)

    private fun Article.toCachedContent() = com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent(
        article = this,
        title = "question",
        authorName = "author",
        authorBio = "",
        authorAvatarUrl = "",
        content = "",
        voteUpCount = 0,
        commentCount = 0,
        sourceLabel = "此问题",
    )

    private object NoopEnvironment : ZhihuApiEnvironment {
        override fun httpClient(): HttpClient = error("not used")

        override fun authenticatedCookies(): Map<String, String> = emptyMap()

        override suspend fun handleFetchFailure(
            tag: String?,
            error: Exception,
        ): Unit = throw error
    }

    private class RecordingEnvironment : ZhihuApiEnvironment {
        val requestedUrls = mutableListOf<String>()

        override fun httpClient(): HttpClient = error("not used")

        override fun authenticatedCookies(): Map<String, String> = emptyMap()

        override suspend fun fetchJson(
            url: String,
            include: String,
        ): JsonObject {
            requestedUrls += url
            return buildJsonObject {}
        }

        override suspend fun handleFetchFailure(
            tag: String?,
            error: Exception,
        ): Unit = throw error
    }

    private inner class FeedEnvironment(
        private val answerIds: List<Long>,
    ) : ZhihuApiEnvironment {
        override fun httpClient(): HttpClient = error("not used")

        override fun authenticatedCookies(): Map<String, String> = emptyMap()

        override suspend fun fetchJson(
            url: String,
            include: String,
        ): JsonObject = buildJsonObject {
            put(
                "data",
                buildJsonArray {
                    answerIds.forEach { answerId ->
                        add(answerFeed(answerId))
                    }
                },
            )
        }

        override suspend fun handleFetchFailure(
            tag: String?,
            error: Exception,
        ): Unit = throw error
    }

    private fun answerFeed(answerId: Long) = buildJsonObject {
        put("type", JsonPrimitive("feed"))
        put("target", answerTarget(answerId))
    }

    private fun answerTarget(answerId: Long) = buildJsonObject {
        put("type", JsonPrimitive("answer"))
        put("id", JsonPrimitive(answerId))
        put("url", JsonPrimitive("https://www.zhihu.com/question/1/answer/$answerId"))
        put("question", questionTarget())
    }

    private fun questionTarget() = buildJsonObject {
        put("type", JsonPrimitive("question"))
        put("id", JsonPrimitive(1L))
        put("title", JsonPrimitive("question"))
        put("url", JsonPrimitive("https://www.zhihu.com/question/1"))
    }
}
