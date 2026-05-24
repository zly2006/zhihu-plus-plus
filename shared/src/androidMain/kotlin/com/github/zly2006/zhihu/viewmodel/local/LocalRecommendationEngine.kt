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

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.github.zly2006.zhihu.navigation.NavDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.jvm.java

class LocalRecommendationEngine(
    private val context: Context,
) {
    data class LocalRecommendationEntry(
        val feed: LocalFeed,
        val result: CrawlingResult,
        val navDestination: NavDestination?,
    )

    private val database by lazy { getLocalContentDatabase(context) }
    private val dao by lazy { database.contentDao() }
    private val feedGenerator by lazy { FeedGenerator(context) }
    private val userBehaviorAnalyzer by lazy { UserBehaviorAnalyzer(context) }
    private val contentInitializer by lazy { LocalContentInitializer(context) }
    private val taskScheduler by lazy { TaskScheduler(context) }
    private val crawlingExecutor by lazy { CrawlingExecutor(context) }

    @Volatile
    private var initialized = false

    suspend fun initialize() {
        if (initialized) {
            return
        }

        withContext(Dispatchers.IO) {
            if (initialized) {
                return@withContext
            }
            contentInitializer.initializeIfNeeded()
            taskScheduler.startScheduling()
            initialized = true
        }
    }

    suspend fun generateRecommendations(limit: Int = 20): List<LocalRecommendationEntry> = withContext(Dispatchers.IO) {
        initialize()

        var candidates = collectCandidateResults(limit)
        if (candidates.size < limit / 2) {
            ensurePendingTasks()
            executeHighPriorityTasks()
            waitForTaskCompletion(5_000L)
            candidates = collectCandidateResults(limit)
        }

        if (candidates.isEmpty()) {
            return@withContext buildFallbackRecommendations(limit)
        }

        val behaviorProfile = userBehaviorAnalyzer.buildBehaviorProfile()
        val ranked = candidates
            .mapNotNull { candidate ->
                rankCandidate(candidate, behaviorProfile)
            }.sortedByDescending { it.finalScore }

        if (ranked.isEmpty()) {
            return@withContext buildFallbackRecommendations(limit)
        }

        applyReasonDiversity(ranked, limit).mapNotNull { rankedResult ->
            toRecommendationEntry(rankedResult)
        }
    }

    suspend fun recordContentOpened(contentId: String, reason: CrawlingReason) {
        withContext(Dispatchers.IO) {
            userBehaviorAnalyzer.recordContentOpened(contentId, reason)
        }
    }

    suspend fun recordRecommendationFeedback(
        feedId: String?,
        contentId: String,
        reason: CrawlingReason,
        feedback: Double,
    ) {
        withContext(Dispatchers.IO) {
            feedId?.let { dao.updateFeedFeedback(it, feedback) }
            userBehaviorAnalyzer.recordRecommendationFeedback(contentId, reason, feedback)
        }
    }

    suspend fun refreshContent() {
        withContext(Dispatchers.IO) {
            val tasks = mutableListOf<CrawlingTask>()

            CrawlingReason.entries.forEach { reason ->
                val pendingCount = dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.NotStarted)
                if (pendingCount < 2) {
                    tasks.add(createTaskForReason(reason))
                }
            }

            if (tasks.isNotEmpty()) {
                dao.insertTasks(tasks)
            }

            dao
                .getTasksByStatus(CrawlingStatus.NotStarted)
                .sortedByDescending { it.priority }
                .take(3)
                .forEach { task ->
                    runCatching { crawlingExecutor.executeTask(task) }
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
        withContext(Dispatchers.IO) {
            taskScheduler.stopScheduling()

            val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            val oneMonthAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L

            dao.cleanupOldTasks(CrawlingStatus.Completed, oneWeekAgo)
            dao.cleanupOldTasks(CrawlingStatus.Failed, oneWeekAgo)
            dao.cleanupOldResults(oneMonthAgo)
            dao.cleanupOldFeeds(oneMonthAgo)
            dao.cleanupOldBehaviors(oneMonthAgo)
        }
    }

    private suspend fun collectCandidateResults(limit: Int): List<CrawlingResult> = withContext(Dispatchers.IO) {
        CrawlingReason.entries
            .flatMap { reason ->
                dao.getResultsByReason(reason).take(limit * 2)
            }.mapNotNull { candidate ->
                val identity = parseLocalContentIdentity(candidate.contentId, candidate.url) ?: return@mapNotNull null
                if (candidate.contentId == identity.value) {
                    candidate
                } else {
                    candidate.copy(contentId = identity.value)
                }
            }.distinctBy { it.contentId }
    }

    private suspend fun buildFallbackRecommendations(limit: Int): List<LocalRecommendationEntry> = withContext(Dispatchers.IO) {
        val behaviorProfile = userBehaviorAnalyzer.buildBehaviorProfile()

        dao
            .getRecentResults(limit * 3)
            .mapNotNull { candidate ->
                val identity = parseLocalContentIdentity(candidate.contentId, candidate.url) ?: return@mapNotNull null
                if (candidate.contentId == identity.value) {
                    candidate
                } else {
                    candidate.copy(contentId = identity.value)
                }
            }.distinctBy { it.contentId }
            .mapNotNull { candidate ->
                rankCandidate(
                    candidate = candidate,
                    behaviorProfile = behaviorProfile,
                )
            }.sortedByDescending { it.finalScore }
            .take(limit)
            .mapNotNull { rankedResult ->
                toRecommendationEntry(rankedResult)
            }
    }

    private suspend fun toRecommendationEntry(rankedResult: RankedLocalResult): LocalRecommendationEntry? {
        val identity = parseLocalContentIdentity(rankedResult.result.contentId, rankedResult.result.url) ?: return null
        val navDestination = identity.toNavDestination(rankedResult.result.title) ?: return null
        val localFeed = feedGenerator.generateFeedFromResult(rankedResult.result, rankedResult.reasonDisplay)
        return LocalRecommendationEntry(
            feed = localFeed,
            result = rankedResult.result,
            navDestination = navDestination,
        )
    }

    private suspend fun ensurePendingTasks() {
        val tasks = mutableListOf<CrawlingTask>()

        CrawlingReason.entries.forEach { reason ->
            val pendingCount = dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.NotStarted)
            val inProgressCount = dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.InProgress)

            if (pendingCount + inProgressCount < 2) {
                repeat(3 - pendingCount - inProgressCount) {
                    tasks.add(createTaskForReason(reason))
                }
            }
        }

        if (tasks.isNotEmpty()) {
            dao.insertTasks(tasks)
        }
    }

    private suspend fun executeHighPriorityTasks() {
        if (!isNetworkAvailable()) {
            Log.w("LocalRecommendationEngine", "No network connection, skipping task execution")
            return
        }

        dao
            .getTasksByStatus(CrawlingStatus.NotStarted)
            .sortedByDescending { it.priority }
            .take(3)
            .forEach { task ->
                try {
                    crawlingExecutor.executeTask(task)
                    delay(1_000L)
                } catch (e: Exception) {
                    Log.e("LocalRecommendationEngine", "Task execution failed: ${e.message}", e)
                }
            }
    }

    private fun isNetworkAvailable(): Boolean = try {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        networkCapabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } catch (_: Exception) {
        false
    }

    private suspend fun waitForTaskCompletion(maxWaitTimeMs: Long) {
        val startTime = System.currentTimeMillis()
        val checkInterval = 500L

        while (System.currentTimeMillis() - startTime < maxWaitTimeMs) {
            if (dao.getTasksByStatus(CrawlingStatus.InProgress).isEmpty()) {
                break
            }
            delay(checkInterval)
        }
    }
}
