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

package com.github.zly2006.zhihu.viewmodel.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray

class LocalRecommendationEngine(
    private val dao: LocalContentDao,
    private val feedGenerator: FeedGenerator,
    private val userBehaviorAnalyzer: UserBehaviorAnalyzer,
    private val initializeContentIfNeeded: suspend () -> Unit,
    private val startScheduling: () -> Unit,
    private val stopScheduling: () -> Unit,
    private val executeTask: suspend (CrawlingTask) -> Unit,
    private val isNetworkAvailable: () -> Boolean = { true },
    private val logWarning: (String) -> Unit = {},
    private val logError: (String, Throwable) -> Unit = { _, _ -> },
) {
    @kotlin.concurrent.Volatile
    private var initialized = false

    suspend fun initialize() {
        if (initialized) {
            return
        }

        withContext(Dispatchers.Default) {
            if (initialized) {
                return@withContext
            }
            initializeContentIfNeeded()
            startScheduling()
            initialized = true
        }
    }

    suspend fun generateRecommendations(limit: Int = 20): List<LocalRecommendationEntry> = withContext(Dispatchers.Default) {
        initialize()

        var candidates = collectCandidateResults(dao, limit)
        if (candidates.size < limit / 2) {
            ensurePendingTasks(dao)
            executeHighPriorityTasks()
            waitForTaskCompletion(dao, 5_000L)
            candidates = collectCandidateResults(dao, limit)
        }

        if (candidates.isEmpty()) {
            return@withContext buildFallbackRecommendations(dao, userBehaviorAnalyzer, feedGenerator, limit)
        }

        val behaviorProfile = userBehaviorAnalyzer.buildBehaviorProfile()
        val ranked = candidates
            .mapNotNull { candidate ->
                rankCandidate(candidate, behaviorProfile)
            }.sortedByDescending { it.finalScore }

        if (ranked.isEmpty()) {
            return@withContext buildFallbackRecommendations(dao, userBehaviorAnalyzer, feedGenerator, limit)
        }

        applyReasonDiversity(ranked, limit).mapNotNull { rankedResult ->
            toRecommendationEntry(
                rankedResult = rankedResult,
                feedGenerator = feedGenerator,
            )
        }
    }

    suspend fun recordContentOpened(contentId: String, reason: CrawlingReason) {
        withContext(Dispatchers.Default) {
            userBehaviorAnalyzer.recordContentOpened(contentId, reason)
        }
    }

    suspend fun recordRecommendationFeedback(
        feedId: String?,
        contentId: String,
        reason: CrawlingReason,
        feedback: Double,
    ) {
        withContext(Dispatchers.Default) {
            feedId?.let { dao.updateFeedFeedback(it, feedback) }
            userBehaviorAnalyzer.recordRecommendationFeedback(contentId, reason, feedback)
        }
    }

    suspend fun refreshContent() {
        withContext(Dispatchers.Default) {
            insertRefreshTasks(dao)

            dao
                .getTasksByStatus(CrawlingStatus.NotStarted)
                .sortedByDescending { it.priority }
                .take(3)
                .forEach { task ->
                    runCatching { executeTask(task) }
                }
        }
    }

    fun getRecommendationStream(): Flow<List<LocalRecommendationEntry>> = flow {
        while (true) {
            try {
                emit(generateRecommendations())
                delay(30_000L)
            } catch (_: Exception) {
                delay(60_000L)
            }
        }
    }

    suspend fun cleanup() {
        withContext(Dispatchers.Default) {
            stopScheduling()
            cleanupLocalRecommendationData(dao)
        }
    }

    private suspend fun executeHighPriorityTasks() {
        if (!isNetworkAvailable()) {
            logWarning("No network connection, skipping task execution")
            return
        }

        dao
            .getTasksByStatus(CrawlingStatus.NotStarted)
            .sortedByDescending { it.priority }
            .take(3)
            .forEach { task ->
                try {
                    executeTask(task)
                    delay(1_000L)
                } catch (e: Exception) {
                    logError("Task execution failed: ${e.message}", e)
                }
            }
    }
}

fun buildLocalRecommendationEngine(
    dao: LocalContentDao,
    fetchFeedArray: suspend (String) -> JsonArray,
    isNetworkAvailable: () -> Boolean = { true },
    logWarning: (String) -> Unit = {},
    logError: (String, Throwable) -> Unit = { _, _ -> },
): LocalRecommendationEngine {
    val crawlingExecutor = CrawlingExecutor(
        dao = dao,
        fetchFeedArray = fetchFeedArray,
    )
    val taskScheduler = TaskScheduler(
        dao = dao,
        executeTask = { task -> crawlingExecutor.executeTask(task) },
    )
    val contentInitializer = LocalContentInitializer(dao)
    return LocalRecommendationEngine(
        dao = dao,
        feedGenerator = FeedGenerator(dao),
        userBehaviorAnalyzer = UserBehaviorAnalyzer(dao),
        initializeContentIfNeeded = { contentInitializer.initializeIfNeeded() },
        startScheduling = { taskScheduler.startScheduling() },
        stopScheduling = { taskScheduler.stopScheduling() },
        executeTask = { task -> crawlingExecutor.executeTask(task) },
        isNetworkAvailable = isNetworkAvailable,
        logWarning = logWarning,
        logError = logError,
    )
}
