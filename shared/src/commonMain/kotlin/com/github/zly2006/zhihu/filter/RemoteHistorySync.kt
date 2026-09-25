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
import com.github.zly2006.zhihu.data.OnlineHistoryItem
import com.github.zly2006.zhihu.data.ZhihuJson.decodeJson
import com.github.zly2006.zhihu.data.ZhihuPaging
import com.github.zly2006.zhihu.data.navDestination
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenEvent
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenEventDao
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlin.time.Clock

internal const val REMOTE_HISTORY_FIRST_URL =
    "https://api.zhihu.com/unify-consumption/read_history?offset=0&limit=50"
internal const val REMOTE_HISTORY_WATERMARK_KEY = "remoteHistoryReadTime"
private const val LAST_ATTEMPT_KEY = "lastRemoteHistorySync"

/** One home-screen owner shares this job across recommendation modes and tab/lifecycle re-entry. */
class RemoteHistorySync(
    private val settings: SettingsStore,
    private val dao: ContentOpenEventDao = getContentFilterDatabase().contentOpenEventDao(),
) {
    private var job: Job? = null
    private var recentPage = CompletableDeferred(Unit)
    private val revision = MutableStateFlow(0)
    val importedRevision = revision.asStateFlow()

    fun start(scope: CoroutineScope, environment: PaginationEnvironment): Job? {
        if (job?.isActive == true) return job
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - settings.getLong(LAST_ATTEMPT_KEY, 0L) < 60_000L) return null
        settings.putLong(LAST_ATTEMPT_KEY, now)
        recentPage = CompletableDeferred()
        job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                sync(environment)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("RemoteHistorySync", "Failed to sync remote reading history", e)
            } finally {
                recentPage.complete(Unit)
            }
        }
        return job
    }

    fun feedEnvironment(environment: PaginationEnvironment): PaginationEnvironment =
        object : PaginationEnvironment by environment {
            override suspend fun applyForegroundHomeFeedFilter(items: List<FeedDisplayItem>): List<FeedDisplayItem> {
                awaitRecentPage()
                return environment.applyForegroundHomeFeedFilter(items)
            }
        }

    // Recommendation requests run immediately. A slow history request must not hold the feed indefinitely.
    suspend fun awaitRecentPage() {
        withTimeoutOrNull(300L) { recentPage.await() }
    }

    /** Reads only; unlike the foreground pipeline this does not record another exposure. */
    suspend fun filterRemoteReads(items: List<FeedDisplayItem>): List<FeedDisplayItem> {
        if (!settings.getBoolean("enableContentFilter", true) || settings.getBoolean("reverseBlock", false)) return items
        val identities = items.associateWith { item ->
            item.navDestination
                ?.let(ContentOpenEventSupport::toTrackedContentIdentity)
                ?.let { "${it.type}:${it.id}" }
        }
        val read = dao.getRemotelySyncedContentKeysByKeys(identities.values.filterNotNull()).toSet()
        return items.filter { item ->
            item.feed
                ?.target
                ?.author
                ?.isFollowing == true ||
                identities[item] !in read
        }
    }

    private suspend fun sync(environment: PaginationEnvironment) {
        val boundary = settings.getLong(REMOTE_HISTORY_WATERMARK_KEY, 0L)
        var newest = boundary
        var previousReadTime = Long.MAX_VALUE
        var ordered = true
        var url = REMOTE_HISTORY_FIRST_URL
        val requested = mutableSetOf<String>()
        var scanned = 0
        var imported = 0
        var unsupported = 0
        while (requested.add(url)) {
            val response = withTimeout(15_000L) { environment.fetchJson(url, "") }
                ?: error("远端历史没有返回 JSON")
            val rawItems = response["data"] as? JsonArray ?: error("远端历史缺少 data")
            val items = rawItems.map { decodeJson<OnlineHistoryItem>(it) }
            val paging = response["paging"]?.let { decodeJson<ZhihuPaging>(it) }
                ?: error("远端历史缺少 paging")
            items.forEach {
                val time = it.data.extra.readTime
                if (time > previousReadTime) ordered = false
                previousReadTime = time
                newest = maxOf(newest, time)
            }
            val events = items
                .mapNotNull { item ->
                    val destination = runCatching { resolveContent(item.data.action.url) }.getOrNull()
                        ?: return@mapNotNull null
                    val identity = ContentOpenEventSupport.toTrackedContentIdentity(destination) ?: return@mapNotNull null
                    ContentOpenEvent(
                        contentType = identity.type,
                        contentId = identity.id,
                        questionId = item.data.extra.questionToken
                            .toLongOrNull(),
                        openFrom = "remote_sync",
                        openedAt = item.data.extra.readTime * 1000,
                    )
                }.distinctBy { "${it.contentType}:${it.contentId}" }
            val keys = events.map { "${it.contentType}:${it.contentId}" }
            val known = if (keys.isEmpty()) emptySet() else dao.getRemotelySyncedContentKeysByKeys(keys).toSet()
            val unseen = events.filterNot { "${it.contentType}:${it.contentId}" in known }
            unseen.forEach { dao.insert(it) }
            scanned += items.size
            imported += unseen.size
            unsupported += items.size - events.size
            if (unseen.isNotEmpty()) revision.value++
            recentPage.complete(Unit)

            // The API is newest-read-first. Cross the previous successful time boundary, including
            // all ties; a page of familiar identities can still precede unread identities on page 2.
            val crossedBoundary = boundary > 0 && ordered && items.any { it.data.extra.readTime < boundary }
            if (paging.isEnd || crossedBoundary) {
                settings.putLong(REMOTE_HISTORY_WATERMARK_KEY, newest)
                Log.i("RemoteHistorySync", "Completed pages=${requested.size} records=$scanned imported=$imported unmappedOrDuplicate=$unsupported end=${paging.isEnd} incremental=${boundary > 0}")
                return
            }
            val next = Url(paging.next)
            require(
                next.protocol == URLProtocol.HTTPS &&
                    next.host == "api.zhihu.com" &&
                    next.encodedPath == "/unify-consumption/read_history" &&
                    paging.next !in requested,
            ) { "远端历史分页地址无效" }
            url = paging.next
        }
    }
}
