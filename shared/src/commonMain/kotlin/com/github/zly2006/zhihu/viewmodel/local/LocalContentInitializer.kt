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

package com.github.zly2006.zhihu.viewmodel.local

import com.github.zly2006.zhihu.navigation.zhihuQuestionFeedsUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class LocalContentInitializer(
    private val dao: LocalContentDao,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    suspend fun initializeIfNeeded() {
        withContext(Dispatchers.Default) {
            val isInitialized = checkDatabaseHasSufficientData()
            if (!isInitialized) {
                initializeFromHistory()
            } else {
                // 检查是否需要补充爬虫任务
                ensureSufficientTasks()
            }
        }
    }

    private suspend fun checkDatabaseHasSufficientData(): Boolean {
        // 检查每种推荐原因是否都有足够的任务或结果
        val minDataPerReason = 2 // 每种原因至少需要2个任务或结果

        return CrawlingReason.entries.all { reason ->
            val pendingTasks = dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.NotStarted)
            val completedResults = dao.getResultCountByReason(reason)
            (pendingTasks + completedResults) >= minDataPerReason
        }
    }

    private suspend fun initializeFromHistory() {
        val tasks = mutableListOf<CrawlingTask>()

        repeat(3) { index ->
            tasks.add(
                CrawlingTask(
                    url = zhihuFollowingRecommendUrl(offset = index * 10),
                    reason = CrawlingReason.Following,
                    priority = 8,
                ),
            )
        }

        repeat(3) { index ->
            tasks.add(
                CrawlingTask(
                    url = zhihuTopstoryRecommendUrl(limit = 20, offset = index * 20),
                    reason = CrawlingReason.Trending,
                    priority = 7,
                ),
            )
        }

        val recentLikes = dao.getBehaviorsByActionSince("like", nowMillis() - 30 * 24 * 60 * 60 * 1000L)
        val questionIds = recentLikes
            .mapNotNull { behavior -> extractQuestionIdFromContentId(behavior.contentId) }
            .distinct()
            .take(5)
        questionIds.forEach { questionId ->
            tasks.add(
                CrawlingTask(
                    url = zhihuQuestionFeedsUrl(questionId, limit = 20),
                    reason = CrawlingReason.UpvotedQuestion,
                    priority = 6,
                ),
            )
        }
        if (questionIds.isEmpty()) {
            repeat(2) { index ->
                tasks.add(
                    CrawlingTask(
                        url = zhihuTopstoryRecommendUrl(limit = 10, offset = index * 10),
                        reason = CrawlingReason.UpvotedQuestion,
                        priority = 6,
                    ),
                )
            }
        }

        repeat(2) { index ->
            tasks.add(
                CrawlingTask(
                    url = zhihuFollowingUpvoteRecommendUrl(limit = 20, offset = index * 20),
                    reason = CrawlingReason.FollowingUpvote,
                    priority = 5,
                ),
            )
        }

        repeat(2) { index ->
            tasks.add(
                CrawlingTask(
                    url = zhihuTopstoryRecommendUrl(limit = 15, offset = index * 15),
                    reason = CrawlingReason.CollaborativeFiltering,
                    priority = 4,
                ),
            )
        }

        if (tasks.isNotEmpty()) {
            dao.insertTasks(tasks)
        }
    }

    private suspend fun ensureSufficientTasks() {
        val minTasksPerReason = 3
        val tasks = mutableListOf<CrawlingTask>()

        CrawlingReason.entries.forEach { reason ->
            val pendingCount = dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.NotStarted)
            val completedCount = dao.getResultCountByReason(reason)

            // 如果该类型的任务或结果不足，则创建新任务
            if (pendingCount < minTasksPerReason && completedCount < 10) {
                val additionalTasks = when (reason) {
                    CrawlingReason.Following -> (0 until (minTasksPerReason - pendingCount)).map { index ->
                        CrawlingTask(
                            url = zhihuFollowingRecommendUrl(offset = index * 10),
                            reason = CrawlingReason.Following,
                            priority = 8,
                        )
                    }
                    CrawlingReason.Trending -> (0 until (minTasksPerReason - pendingCount)).map { index ->
                        CrawlingTask(
                            url = zhihuTopstoryRecommendUrl(limit = 20, offset = index * 20),
                            reason = CrawlingReason.Trending,
                            priority = 7,
                        )
                    }
                    CrawlingReason.UpvotedQuestion -> {
                        val count = minTasksPerReason - pendingCount
                        val upvotedQuestionTasks = mutableListOf<CrawlingTask>()
                        dao.getMostLikedContent(count).forEach { (contentId, _) ->
                            val questionId = extractQuestionIdFromContentId(contentId)
                            if (questionId != null) {
                                upvotedQuestionTasks.add(
                                    CrawlingTask(
                                        url = zhihuQuestionFeedsUrl(questionId, limit = 20),
                                        reason = CrawlingReason.UpvotedQuestion,
                                        priority = 6,
                                    ),
                                )
                            }
                        }
                        while (upvotedQuestionTasks.size < count) {
                            upvotedQuestionTasks.add(
                                CrawlingTask(
                                    url = zhihuTopstoryRecommendUrl(limit = 10, offset = upvotedQuestionTasks.size * 10),
                                    reason = CrawlingReason.UpvotedQuestion,
                                    priority = 6,
                                ),
                            )
                        }
                        upvotedQuestionTasks
                    }
                    CrawlingReason.FollowingUpvote -> (0 until (minTasksPerReason - pendingCount)).map { index ->
                        CrawlingTask(
                            url = zhihuFollowingUpvoteRecommendUrl(limit = 20, offset = index * 20),
                            reason = CrawlingReason.FollowingUpvote,
                            priority = 5,
                        )
                    }
                    CrawlingReason.CollaborativeFiltering -> (0 until (minTasksPerReason - pendingCount)).map { index ->
                        CrawlingTask(
                            url = zhihuTopstoryRecommendUrl(limit = 15, offset = index * 15),
                            reason = CrawlingReason.CollaborativeFiltering,
                            priority = 4,
                        )
                    }
                }
                tasks.addAll(additionalTasks)
            }
        }

        if (tasks.isNotEmpty()) {
            dao.insertTasks(tasks)
        }
    }
}
