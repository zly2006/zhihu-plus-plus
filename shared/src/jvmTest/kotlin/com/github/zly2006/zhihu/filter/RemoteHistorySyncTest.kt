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

package com.github.zly2006.zhihu.filter

import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.platform.MapSettingsStore
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RemoteHistorySyncTest {
    @Test
    fun fullSyncThenIncrementalCrossesTimeBoundaryAndIncludesTies() = runTest {
        val fixture = Fixture()
        try {
            fixture.pages = listOf(page(listOf(1 to 100, 2 to 90), NEXT), page(listOf(3 to 80), null))
            fixture.start(this)!!.join()
            assertEquals(2, fixture.requests.size)
            assertEquals(100L, fixture.settings.getLong(REMOTE_HISTORY_WATERMARK_KEY, 0))
            assertEquals(listOf("answer:1", "answer:2", "answer:3"), fixture.dao.getRemotelySyncedContentKeysByKeys(listOf("answer:1", "answer:2", "answer:3")))
            assertNull(fixture.start(this)) // rapid tab/lifecycle re-entry does not request again

            fixture.retryWith(listOf(page(listOf(1 to 110, 2 to 110), NEXT), page(listOf(4 to 100, 3 to 80), null)))
            fixture.start(this)!!.join()
            assertEquals(2, fixture.requests.size) // familiar first page must not hide new item on page 2
            assertEquals(listOf("answer:4"), fixture.dao.getRemotelySyncedContentKeysByKeys(listOf("answer:4")))
            assertEquals(110L, fixture.settings.getLong(REMOTE_HISTORY_WATERMARK_KEY, 0))

            fixture.retryWith(listOf(page(listOf(1 to 110, 2 to 110), NEXT), page(listOf(5 to 110, 3 to 80), null)))
            fixture.start(this)!!.join()
            assertEquals(2, fixture.requests.size) // equal timestamps can span the page boundary
            assertEquals(listOf("answer:5"), fixture.dao.getRemotelySyncedContentKeysByKeys(listOf("answer:5")))

            fixture.retryWith(listOf(page(listOf(1 to 110, 2 to 109), NEXT)))
            fixture.start(this)!!.join()
            assertEquals(1, fixture.requests.size) // normal unchanged history uses one request
        } finally {
            fixture.close()
        }
    }

    @Test
    fun failedPageDoesNotAdvanceBoundaryAndRetryImportsMissingItems() = runTest {
        val fixture = Fixture()
        try {
            fixture.settings.putLong(REMOTE_HISTORY_WATERMARK_KEY, 100)
            fixture.pages = listOf(page(listOf(1 to 120), NEXT), "{}")
            fixture.start(this)!!.join()
            assertEquals(100L, fixture.settings.getLong(REMOTE_HISTORY_WATERMARK_KEY, 0))
            fixture.retryWith(listOf(page(listOf(1 to 120), NEXT), page(listOf(2 to 110, 3 to 99), null)))
            fixture.start(this)!!.join()
            assertEquals(120L, fixture.settings.getLong(REMOTE_HISTORY_WATERMARK_KEY, 0))
            assertEquals(3, fixture.dao.getRemotelySyncedContentKeysByKeys(listOf("answer:1", "answer:2", "answer:3")).size)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun slowHistoryDoesNotBlockFeedOrDuplicateRequestsAndLateResultFiltersLocally() = runTest {
        val fixture = Fixture()
        try {
            fixture.gate = CompletableDeferred()
            fixture.pages = listOf(page(listOf(1 to 100), null))
            val job = fixture.start(this)!!
            assertSame(job, fixture.start(this))
            val card = FeedDisplayItem(title = "read", summary = null, details = "", feed = null, navDestinationJson = Article(id = 1L, type = ArticleType.Answer).toFeedDisplayItemNavDestinationJson())
            val result = async { fixture.feedEnvironment.applyForegroundHomeFeedFilter(listOf(card)) }
            runCurrent()
            advanceTimeBy(299)
            assertFalse(result.isCompleted)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(listOf(card), result.await())
            assertEquals(1, fixture.requests.size)
            fixture.gate!!.complete(Unit)
            job.join()
            assertEquals(emptyList(), fixture.sync.filterRemoteReads(listOf(card)))
            assertEquals(1, fixture.requests.size) // applying the late result performs no recommendation fetch
        } finally {
            fixture.close()
        }
    }

    @Test
    fun invalidPagingAndDecodeFailuresNeverClaimCompletion() = runTest {
        val fixture = Fixture()
        try {
            fixture.pages = listOf(page(listOf(1 to 100), "https://example.com/steal"))
            fixture.start(this)!!.join()
            assertEquals(listOf(REMOTE_HISTORY_FIRST_URL), fixture.requests)
            assertEquals(0L, fixture.settings.getLong(REMOTE_HISTORY_WATERMARK_KEY, 0))
            fixture.retryWith(listOf("""{"data":[{}],"paging":{"is_end":true,"next":""}}"""))
            fixture.start(this)!!.join()
            assertEquals(0L, fixture.settings.getLong(REMOTE_HISTORY_WATERMARK_KEY, 0))
        } finally {
            fixture.close()
        }
    }

    private class Fixture {
        val database = getContentFilterDatabase(createTempDirectory("remote-history").resolve("filter.db").toFile())
        val dao = database.contentOpenEventDao()
        val settings = MapSettingsStore()
        val requests = mutableListOf<String>()
        var pages = emptyList<String>()
        var gate: CompletableDeferred<Unit>? = null
        private val client = HttpClient(MockEngine { respond("{}") })
        private val environment = object : PaginationEnvironment {
            override fun httpClient() = client

            override fun authenticatedCookies() = emptyMap<String, String>()

            override suspend fun fetchJson(url: String, include: String): JsonObject {
                assertEquals("", include)
                requests += url
                gate?.await()
                return ZhihuJson.json.parseToJsonElement(pages[requests.lastIndex]).jsonObject
            }

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }
        val sync = RemoteHistorySync(settings, dao)
        val feedEnvironment = sync.feedEnvironment(environment)

        fun start(scope: CoroutineScope) = sync.start(scope, environment)

        fun retryWith(responses: List<String>) {
            settings.remove("lastRemoteHistorySync")
            requests.clear()
            pages = responses
        }

        fun close() {
            database.close()
            client.close()
        }
    }

    private fun page(entries: List<Pair<Int, Int>>, next: String?): String {
        val items = entries.joinToString(",") { (id, time) ->
            """{"card_type":"read_history","data":{"header":{"icon":"","title":"t"},"action":{"type":"open_url","url":"zhihu://answers/$id"},"extra":{"content_token":"$id","content_type":"answer","read_time":$time,"question_token":"1"}}}"""
        }
        return """{"data":[$items],"paging":{"is_end":${next == null},"next":"${next ?: ""}"}}"""
    }

    companion object {
        private const val NEXT = "https://api.zhihu.com/unify-consumption/read_history?offset=50&limit=50"
    }
}
