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

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.ui.ArticleAnswerTransitionDirection
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestionAnswerNavigatorTest {
    @Test
    fun seededQuestionAnswerListKeepsClickedPositionForSwitching() = runTest {
        val navigator = navigatorFromIds(
            ids = listOf(100L, 101L, 102L, 103L, 104L),
            cursor = 2,
            feedsNextUrl = "https://www.zhihu.com/api/v4/questions/1/feeds?limit=20&order=updated&offset=20",
            order = "updated",
        )

        assertEquals(101L, navigator.previousAnswer?.article?.id)
        assertEquals(103L, navigator.loadNext()?.id)
        assertEquals(104L, navigator.loadNext()?.id)
    }

    @Test
    fun questionFeedFallbackUsesCurrentSortOrder() = runTest {
        val environment = RecordingEnvironment()
        val navigator = navigatorFromIds(
            ids = listOf(101L),
            cursor = 0,
            feedsNextUrl = "",
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
        val navigator = navigatorFromIds(
            ids = listOf(100L, 101L, 103L, 104L),
            cursor = 2,
            environment = RecordingEnvironment(),
        )

        assertEquals(101L, navigator.previousAnswer?.article?.id)
        assertEquals(104L, navigator.loadNext()?.id)
        assertNull(navigator.loadNext())
    }

    @Test
    fun emptyPreviousSeedDoesNotExposePreview() = runTest {
        val navigator = navigatorFromIds(
            ids = listOf(102L),
            cursor = 0,
        )

        assertNull(navigator.previousAnswer)
    }

    @Test
    fun feedsExtensionSkipsOpenedAnswersBeforeAppending() = runTest {
        val openedAnswerIds = setOf(104L, 106L)
        val navigator = navigatorFromIds(
            ids = listOf(103L),
            cursor = 0,
            environment = FeedEnvironment(listOf(103L, 104L, 105L, 106L, 107L)),
            getAlreadyOpenedAnswerIds = { ids -> ids.filter { it in openedAnswerIds }.toSet() },
        )

        assertEquals(105L, navigator.loadNext()?.id)
        assertEquals(107L, navigator.loadNext()?.id)
        assertNull(navigator.loadNext())
    }

    @Test
    fun repeatedFiveAnswerRoundsNeverAppendOpenedAnswersFromFeeds() = runTest {
        val rounds = listOf(
            listOf(101L, 102L, 103L, 104L, 105L) to setOf(102L, 104L),
            listOf(201L, 202L, 203L, 204L, 205L) to setOf(201L, 205L),
            listOf(301L, 302L, 303L, 304L, 305L) to setOf(303L),
        )

        rounds.forEachIndexed { roundIndex, (candidateIds, openedAnswerIds) ->
            val navigator = QuestionAnswerNavigator.fromQuestionList(
                questionId = roundIndex + 1L,
                orderedArticles = listOf(answer(candidateIds.first())),
                cursorIndex = 0,
                feedsNextUrl = "",
                order = null,
                environment = FeedEnvironment(candidateIds),
                getAlreadyOpenedAnswerIds = { ids -> ids.filter { it in openedAnswerIds }.toSet() },
            )

            val expectedFreshIds = candidateIds
                .filterNot { it in openedAnswerIds || it == candidateIds.first() }
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
        val navigator = navigatorFromIds(
            ids = listOf(101L),
            cursor = 0,
            environment = environment,
            getAlreadyOpenedAnswerIds = { ids -> ids.filterTo(mutableSetOf()) { it in openedAnswerIds } },
        )

        navigator.pushAnswer(answer(101L).toCachedContent())
        assertEquals(103L, navigator.loadNext()?.id)

        openedAnswerIds += 103L
        val reopenedNavigator = navigatorFromIds(
            ids = listOf(101L),
            cursor = 0,
            environment = environment,
            getAlreadyOpenedAnswerIds = { ids -> ids.filterTo(mutableSetOf()) { it in openedAnswerIds } },
        )
        reopenedNavigator.pushAnswer(answer(101L).toCachedContent())
        assertEquals(104L, reopenedNavigator.loadNext()?.id)
        assertNull(reopenedNavigator.loadNext())
    }

    @Test
    fun directArticleScreenReentrySkipsAnswersViewedInPreviousSession() = runTest {
        val environment = FeedEnvironment(listOf(101L, 102L, 103L, 104L, 105L))
        val openedAnswerIds = mutableSetOf(101L, 102L)
        val navigator = navigatorFromIds(
            ids = listOf(101L),
            cursor = 0,
            environment = environment,
            getAlreadyOpenedAnswerIds = { ids -> ids.filterTo(mutableSetOf()) { it in openedAnswerIds } },
        )

        navigator.pushAnswer(answer(101L).toCachedContent())
        assertEquals(103L, navigator.loadNext()?.id)
        openedAnswerIds += 103L
        navigator.pushAnswer(answer(103L).toCachedContent())
        assertEquals(104L, navigator.loadNext()?.id)
        openedAnswerIds += 104L
        navigator.pushAnswer(answer(104L).toCachedContent())

        navigator.pushAnswer(answer(101L).toCachedContent())
        assertEquals(103L, navigator.loadNext()?.id)
    }

    @Test
    fun firstEntry_previewEnablesSwipeBeforeNetwork() {
        val navigator = navigatorFromIds(
            ids = listOf(101L, 103L, 104L),
            cursor = 0,
        )

        assertTrue(navigator.hasNextCandidate)
        assertEquals(103L, navigator.neighborNextSlot?.article?.id)
        assertEquals(NeighborPhase.Preview, navigator.neighborNextSlot?.phase)
        assertNotNull(navigator.nextAnswer)
        assertTrue(navigator.nextAnswer?.content?.isEmpty() == true)
    }

    @Test
    fun staleNextPrefetchCacheIsClearedSoPrefetchIsNotSkipped() = runTest {
        val navigator = navigatorFromIds(
            ids = listOf(101L, 102L, 103L),
            cursor = 1,
        )
        navigator.pushAnswer(answer(101L).toCachedContent())
        navigator.pushAnswer(answer(102L).toCachedContent().copy(content = "body"))
        navigator.session.markNeighborReady(answer(102L).toCachedContent().copy(content = "body"))
        val revisionBefore = navigator.queueRevision

        navigator.prefetchNext(102L)

        assertTrue(navigator.queueRevision > revisionBefore)
        assertEquals(103L, navigator.neighborNextSlot?.article?.id)
    }

    @Test
    fun onPageSettledAfterPeekNavigationRestoresNextNeighbor() = runTest {
        val navigator = navigatorFromIds(
            ids = listOf(101L, 102L, 103L),
            cursor = 0,
        )
        navigator.pushAnswer(answer(101L).toCachedContent())
        val next = navigator.resolveNextForNavigation()
        assertNotNull(next)
        assertEquals(102L, next.article.id)
        navigator.session.markNeighborReady(next.copy(content = "body"))

        navigator.onPageSettled(
            articleId = 102L,
            direction = ArticleAnswerTransitionDirection.HORIZONTAL_NEXT,
            paginationInfo = null,
            schedulePrefetch = { currentId -> navigator.prefetchNext(currentId) },
        )

        assertTrue(navigator.hasNextCandidate)
        assertEquals(103L, navigator.neighborNextSlot?.article?.id)
    }

    @Test
    fun paginationOnlyNavigatorMergesPaginationOnSettle() = runTest {
        val navigator = PaginationAnswerNavigator.forDirectEntry(
            answerId = 201L,
            questionId = 1L,
            environment = NoopEnvironment,
        )

        navigator.onPageSettled(
            articleId = 201L,
            direction = null,
            paginationInfo = DataHolder.Answer.PaginationInfo(
                index = 1,
                prevAnswerIds = listOf(200L),
                nextAnswerIds = listOf(202L, 203L),
            ),
            schedulePrefetch = {},
        )

        assertEquals(listOf(200L, 201L, 202L, 203L), navigator.session.orderedIds)
        assertEquals(200L, navigator.previousAnswer?.article?.id)
        assertEquals(202L, navigator.nextAnswer?.article?.id)
    }

    @Test
    fun sessionRegistrySuspendAndResumePreservesNavigator() {
        val registry = AnswerSwitchSessionRegistry()
        val navigator = navigatorFromIds(ids = listOf(101L, 102L), cursor = 0)
        registry.active = navigator

        registry.suspend("entry-a", navigator)
        assertNull(registry.active)

        val restored = registry.resume("entry-a")
        assertNotNull(restored)
        assertEquals(102L, restored.nextAnswer?.article?.id)
    }

    private fun navigatorFromIds(
        ids: List<Long>,
        cursor: Int,
        feedsNextUrl: String = "",
        order: String? = "updated",
        environment: ZhihuApiEnvironment = NoopEnvironment,
        getAlreadyOpenedAnswerIds: suspend (List<Long>) -> Set<Long> = { emptySet() },
    ): QuestionAnswerNavigator = QuestionAnswerNavigator.fromQuestionList(
        questionId = 1L,
        orderedArticles = ids.map(::answer),
        cursorIndex = cursor,
        feedsNextUrl = feedsNextUrl,
        order = order,
        environment = environment,
        getAlreadyOpenedAnswerIds = getAlreadyOpenedAnswerIds,
    )

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
