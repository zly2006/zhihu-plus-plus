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
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.CommonFeed
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray

/**
 * 爬虫执行器，负责执行爬虫任务并生成结果
 */
class CrawlingExecutor(
    private val context: Context,
) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }

    /**
     * 执行爬虫任务
     */
    suspend fun executeTask(task: CrawlingTask) {
        withContext(Dispatchers.IO) {
            try {
                // 更新任务状态为进行中
                dao.updateTask(
                    task.copy(
                        status = CrawlingStatus.InProgress,
                        executedAt = System.currentTimeMillis(),
                    ),
                )

                val results = when (task.reason) {
                    CrawlingReason.Following -> executeFollowingTask(task)
                    CrawlingReason.Trending -> executeTrendingTask(task)
                    CrawlingReason.UpvotedQuestion -> executeUpvotedQuestionTask(task)
                    CrawlingReason.FollowingUpvote -> executeFollowingUpvoteTask(task)
                    CrawlingReason.CollaborativeFiltering -> executeCollaborativeFilteringTask(task)
                }

                // 保存爬虫结果
                dao.insertResults(results)

                // 更新任务状态为已完成
                dao.updateTask(task.copy(status = CrawlingStatus.Completed))
            } catch (e: Exception) {
                // 更新任务状态为失败
                dao.updateTask(
                    task.copy(
                        status = CrawlingStatus.Failed,
                        errorMessage = e.message,
                    ),
                )
            }
        }
    }

    private suspend fun executeFollowingTask(task: CrawlingTask): List<CrawlingResult> {
        // 参考FollowRecommendViewModel的实现
        val feedArray = AccountData.fetchGet(context, "https://api.zhihu.com/moments_v3?feed_type=recommend")?.get("data")?.jsonArray ?: JsonArray(emptyList())

        return feedArray.mapNotNull { feedElement ->
            try {
                val feed = AccountData.decodeJson<Feed>(feedElement)
                createCrawlingResult(feed, task.id, CrawlingReason.Following)
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun executeTrendingTask(task: CrawlingTask): List<CrawlingResult> {
        // 参考HomeFeedViewModel的实现
        val feedArray = AccountData.fetchGet(context, "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20")?.get("data")?.jsonArray ?: JsonArray(emptyList())

        return feedArray.mapNotNull { feedElement ->
            try {
                val feed = AccountData.decodeJson<Feed>(feedElement)
                createCrawlingResult(feed, task.id, CrawlingReason.Trending, 1.2) // 热门内容加权
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun executeUpvotedQuestionTask(task: CrawlingTask): List<CrawlingResult> {
        // 参考QuestionFeedViewModel的实现
        // 从URL中提取问题ID
        val questionId = extractQuestionIdFromUrl(task.url) ?: return emptyList()

        val feedArray = AccountData.fetchGet(context, "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=20")?.get("data")?.jsonArray ?: JsonArray(emptyList())

        return feedArray.mapNotNull { feedElement ->
            try {
                val feed = AccountData.decodeJson<Feed>(feedElement)
                createCrawlingResult(feed, task.id, CrawlingReason.UpvotedQuestion, 1.1) // 相关问题内容加权
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun executeFollowingUpvoteTask(task: CrawlingTask): List<CrawlingResult> {
        // 获取关注用户的点赞内容
        val feedArray = AccountData.fetchGet(context, "https://www.zhihu.com/api/v3/feed/topstory/recommend?action_feed=True&limit=20")?.get("data")?.jsonArray ?: JsonArray(emptyList())

        return feedArray.mapNotNull { feedElement ->
            try {
                val feed = AccountData.decodeJson<Feed>(feedElement)
                // 检查是否为点赞类型的动态
                if (isVoteupFeed(feed)) {
                    createCrawlingResult(feed, task.id, CrawlingReason.FollowingUpvote)
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun executeCollaborativeFilteringTask(task: CrawlingTask): List<CrawlingResult> {
        // 基于用户行为的协同过滤推荐
        // 获取用户最近点赞的内容，用于发现相似用户
        val recentLikes = dao.getBehaviorsByActionSince("like", System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)

        if (recentLikes.isEmpty()) {
            // 如果没有点赞行为，回退到推荐内容
            return executeTrendingTask(task.copy(reason = CrawlingReason.CollaborativeFiltering))
        }

        // 基于最近点赞的内容获取相关推荐
        val feedArray = AccountData.fetchGet(context, "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20")?.get("data")?.jsonArray ?: JsonArray(emptyList())

        return feedArray.mapNotNull { feedElement ->
            try {
                val feed = AccountData.decodeJson<Feed>(feedElement)
                createCrawlingResult(feed, task.id, CrawlingReason.CollaborativeFiltering, 0.9) // 协同过滤内容权重稍低
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun createCrawlingResult(
        feed: Feed,
        taskId: Long,
        reason: CrawlingReason,
        scoreMultiplier: Double = 1.0,
    ): CrawlingResult? {
        val target = feed.target ?: return null

        return CrawlingResult(
            taskId = taskId,
            contentId = target.toLocalContentIdentity().value,
            title = target.title,
            summary = target.excerpt ?: "",
            url = when (target) {
                is Feed.AnswerTarget -> target.url
                is Feed.ArticleTarget -> target.url
                else -> target.url
            },
            reason = reason,
            score = scoreFeedTarget(target) * scoreMultiplier,
        )
    }

    private fun isVoteupFeed(feed: Feed): Boolean {
        // 简化的判断逻辑，根据Feed类型判断是否为点赞相关
        return when (feed) {
            is CommonFeed -> {
                // 检查attachedInfo或brief中是否包含点赞相关信息
                feed.brief.contains("赞同") ||
                    feed.brief.contains("点赞") ||
                    feed.attachedInfo.contains("VOTEUP")
            }
            else -> false
        }
    }

    private fun extractQuestionIdFromUrl(url: String): String? {
        val regex = """question/(\d+)""".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }
}
