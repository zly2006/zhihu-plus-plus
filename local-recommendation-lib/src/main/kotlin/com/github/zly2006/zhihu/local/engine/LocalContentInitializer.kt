package com.github.zly2006.zhihu.local.engine

import com.github.zly2006.zhihu.local.*
import com.github.zly2006.zhihu.local.database.LocalContentDao
import org.slf4j.LoggerFactory

class LocalContentInitializer(private val dao: LocalContentDao) {
    private val logger = LoggerFactory.getLogger(LocalContentInitializer::class.java)

    suspend fun initializeIfNeeded() {
        logger.info("Checking if content initialization is needed...")

        // 检查是否需要初始化基础数据
        val totalTasks = CrawlingReason.entries.sumOf { reason ->
            dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.NotStarted) +
            dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.InProgress)
        }

        if (totalTasks < 10) {
            logger.info("Initializing basic crawling tasks...")
            initializeBasicTasks()
        } else {
            logger.info("Content initialization not needed")
        }
    }

    private suspend fun initializeBasicTasks() {
        val initialTasks = mutableListOf<CrawlingTask>()

        // 为每种推荐原因创建初始任务
        CrawlingReason.entries.forEach { reason ->
            repeat(3) { // 每种原因创建3个初始任务
                initialTasks.add(createInitialTask(reason))
            }
        }

        dao.insertTasks(initialTasks)
        logger.info("Created ${initialTasks.size} initial crawling tasks")
    }

    private fun createInitialTask(reason: CrawlingReason): CrawlingTask {
        val (url, priority) = when (reason) {
            CrawlingReason.Following -> {
                "https://api.zhihu.com/moments_v3?feed_type=recommend" to 8
            }
            CrawlingReason.Trending -> {
                "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20" to 7
            }
            CrawlingReason.UpvotedQuestion -> {
                "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=15" to 6
            }
            CrawlingReason.FollowingUpvote -> {
                "https://www.zhihu.com/api/v3/feed/topstory/recommend?action_feed=True&limit=15" to 5
            }
            CrawlingReason.CollaborativeFiltering -> {
                "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10" to 4
            }
        }

        return CrawlingTask(
            url = url,
            reason = reason,
            priority = priority,
        )
    }
}
