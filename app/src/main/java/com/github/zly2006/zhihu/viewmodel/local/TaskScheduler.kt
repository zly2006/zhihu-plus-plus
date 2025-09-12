package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 智能任务调度器，负责管理爬虫任务的执行
 */
class TaskScheduler(
    private val context: Context,
) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }
    private val crawlingExecutor by lazy { CrawlingExecutor(context) }

    private var schedulerJob: Job? = null

    /**
     * 启动任务调度
     */
    fun startScheduling() {
        schedulerJob?.cancel()
        schedulerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    executePendingTasks()
                    cleanupOldData()
                    delay(60_000) // 每分钟检查一次
                } catch (e: Exception) {
                    // 记录错误但继续运行
                    delay(300_000) // 出错时等待5分钟再试
                }
            }
        }
    }

    /**
     * 停止调度
     */
    fun stopScheduling() {
        schedulerJob?.cancel()
        schedulerJob = null
    }

    /**
     * 执行待处理的任务
     */
    private suspend fun executePendingTasks() {
        val pendingTasks = dao.getTasksByStatus(CrawlingStatus.NotStarted)

        // 限制并发执行的任务数量
        pendingTasks.take(3).forEach { task ->
            try {
                crawlingExecutor.executeTask(task)
            } catch (e: Exception) {
                // 忽略单个任务的执行错误
            }
        }
    }

    /**
     * 清理旧数据
     */
    private suspend fun cleanupOldData() {
        val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val oneMonthAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L

        // 清理旧的已完成任务
        dao.cleanupOldTasks(CrawlingStatus.Completed, oneWeekAgo)
        dao.cleanupOldTasks(CrawlingStatus.Failed, oneWeekAgo)

        // 清理旧的爬虫结果
        dao.cleanupOldResults(oneMonthAgo)

        // 清理旧的推荐内容
        dao.cleanupOldFeeds(oneMonthAgo)

        // 清理旧的用户行为数据
        dao.cleanupOldBehaviors(oneMonthAgo)
    }
}
