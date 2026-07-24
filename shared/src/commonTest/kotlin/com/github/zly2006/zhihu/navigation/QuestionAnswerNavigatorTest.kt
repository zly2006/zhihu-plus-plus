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

import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
    fun remainingAnswerSnapshotDoesNotConsumeSwitchingQueue() = runTest {
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            initialNextAnswers = listOf(answer(103L), answer(104L)),
            getAlreadyOpenedAnswerIds = { emptySet() },
            environment = NoopEnvironment,
        )

        assertEquals(listOf(103L), navigator.remainingAnswersSnapshot(102L, limit = 1).map(Article::id))
        assertEquals(103L, navigator.loadNext()?.id)
        assertEquals(listOf(104L), navigator.remainingAnswersSnapshot(103L, limit = 8).map(Article::id))
    }

    @Test
    fun remainingAnswerSnapshotKeepsForwardHistoryBeforeSourceQueueWithoutConsumingEither() = runTest {
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            initialNextAnswers = listOf(answer(104L), answer(105L)),
            getAlreadyOpenedAnswerIds = { emptySet() },
            environment = NoopEnvironment,
        )
        navigator.pushAnswer(answer(101L).toCachedContent())
        navigator.pushAnswer(answer(102L).toCachedContent())
        navigator.pushAnswer(answer(103L).toCachedContent())
        assertEquals(102L, navigator.goToPrevious()?.article?.id)

        val expected = listOf(103L, 104L, 105L)
        assertEquals(expected, navigator.remainingAnswersSnapshot(102L, limit = 8).map(Article::id))
        assertEquals(expected, navigator.remainingAnswersSnapshot(102L, limit = 8).map(Article::id))
        assertEquals(103L, navigator.goToNext()?.article?.id)
        assertEquals(104L, navigator.loadNext()?.id)
        assertEquals(105L, navigator.loadNext()?.id)
    }

    @Test
    fun remainingAnswerSnapshotFiltersOpenedInitialCandidates() = runTest {
        val openedAnswerIds = setOf(104L, 106L)
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            initialNextAnswers = listOf(103L, 104L, 105L, 106L, 107L).map(::answer),
            getAlreadyOpenedAnswerIds = { ids -> ids.filter { it in openedAnswerIds }.toSet() },
            environment = NoopEnvironment,
        )

        assertEquals(
            listOf(103L, 105L, 107L),
            navigator.remainingAnswersSnapshot(102L, limit = 8).map(Article::id),
        )
    }

    @Test
    fun remainingAnswerSnapshotFollowsSeededNextUrlUntilLimitIsFilled() = runTest {
        val firstNextUrl = "https://www.zhihu.com/api/v4/questions/1/feeds?offset=2"
        val secondNextUrl = "https://www.zhihu.com/api/v4/questions/1/feeds?offset=4"
        val environment = PagedFeedEnvironment(
            mapOf(
                firstNextUrl to feedPage(listOf(104L), secondNextUrl),
                secondNextUrl to feedPage(listOf(105L, 106L), ""),
            ),
        )
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            initialNextAnswers = listOf(answer(103L)),
            initialNextUrl = firstNextUrl,
            getAlreadyOpenedAnswerIds = { ids -> ids.filterTo(mutableSetOf()) { it == 104L } },
            environment = environment,
        )

        assertEquals(
            listOf(103L, 105L, 106L),
            navigator.remainingAnswersSnapshot(102L, limit = 3).map(Article::id),
        )
        assertEquals(listOf(firstNextUrl, secondNextUrl), environment.requestedUrls)
        assertEquals(103L, navigator.loadNext()?.id)
        assertEquals(105L, navigator.loadNext()?.id)
        assertEquals(106L, navigator.loadNext()?.id)
    }

    @Test
    fun failedInitialCandidateLookupCanBeRetried() = runTest {
        var lookupAttempts = 0
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            initialNextAnswers = listOf(answer(103L), answer(104L)),
            getAlreadyOpenedAnswerIds = {
                lookupAttempts++
                if (lookupAttempts == 1) error("lookup failed")
                emptySet()
            },
            environment = NoopEnvironment,
        )

        assertFailsWith<IllegalStateException> {
            navigator.remainingAnswersSnapshot(102L, limit = 2)
        }
        assertEquals(
            listOf(103L, 104L),
            navigator.remainingAnswersSnapshot(102L, limit = 2).map(Article::id),
        )
        assertEquals(2, lookupAttempts)
    }

    @Test
    fun newlyDiscoveredPreviousAnswerInvalidatesCachedPreviousContent() = runTest {
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            initialNextAnswers = listOf(answer(103L)),
            getAlreadyOpenedAnswerIds = { setOf(103L) },
            environment = NoopEnvironment,
        )
        navigator.previousAnswerContent = answer(101L).toCachedContent()

        assertEquals(emptyList(), navigator.remainingAnswersSnapshot(102L, limit = 1))

        assertNull(navigator.previousAnswerContent)
        assertEquals(103L, navigator.previousAnswerPreview?.article?.id)
    }

    @Test
    fun snapshotWaitsForInFlightInitialCandidateFiltering() = runTest {
        val lookupStarted = CompletableDeferred<Unit>()
        val finishLookup = CompletableDeferred<Unit>()
        val navigator = QuestionAnswerNavigator(
            questionId = 1L,
            initialNextAnswers = listOf(answer(103L), answer(104L)),
            getAlreadyOpenedAnswerIds = {
                lookupStarted.complete(Unit)
                finishLookup.await()
                emptySet()
            },
            environment = NoopEnvironment,
        )

        val prefetch = async { navigator.prefetchNext(102L) }
        lookupStarted.await()
        val snapshot = async { navigator.remainingAnswersSnapshot(102L, limit = 8).map(Article::id) }
        yield()
        assertFalse(snapshot.isCompleted)

        finishLookup.complete(Unit)
        assertEquals(listOf(103L, 104L), snapshot.await())
        prefetch.await()
        assertEquals(103L, navigator.loadNext()?.id)
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

    private inner class PagedFeedEnvironment(
        private val pages: Map<String, JsonObject>,
    ) : ZhihuApiEnvironment {
        val requestedUrls = mutableListOf<String>()

        override fun httpClient(): HttpClient = error("not used")

        override fun authenticatedCookies(): Map<String, String> = emptyMap()

        override suspend fun fetchJson(
            url: String,
            include: String,
        ): JsonObject {
            requestedUrls += url
            return pages.getValue(url)
        }

        override suspend fun handleFetchFailure(
            tag: String?,
            error: Exception,
        ): Unit = throw error
    }

    private fun feedPage(
        answerIds: List<Long>,
        nextUrl: String,
    ) = buildJsonObject {
        put(
            "data",
            buildJsonArray {
                answerIds.forEach { answerId -> add(answerFeed(answerId)) }
            },
        )
        put(
            "paging",
            buildJsonObject {
                put("next", JsonPrimitive(nextUrl))
            },
        )
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
