package com.github.zly2006.zhihu.local.engine

import com.github.zly2006.zhihu.local.*
import com.github.zly2006.zhihu.local.database.LocalContentDao
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

class TaskScheduler(private val dao: LocalContentDao) {
    private val logger = LoggerFactory.getLogger(TaskScheduler::class.java)
    private val isRunning = AtomicBoolean(false)
    private var schedulerJob: Job? = null
    private val crawlingExecutor = CrawlingExecutor(dao)

    fun startScheduling() {
        if (isRunning.compareAndSet(false, true)) {
            logger.info("Starting task scheduler...")
            schedulerJob = CoroutineScope(Dispatchers.IO).launch {
                while (isRunning.get()) {
                    try {
                        processNextBatch()
                        delay(60_000L) // 每分钟检查一次
                    } catch (e: Exception) {
                        logger.error("Error in task scheduler: ${e.message}", e)
                        delay(120_000L) // 出错时等待2分钟
                    }
                }
            }
        }
    }

    fun stopScheduling() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("Stopping task scheduler...")
            schedulerJob?.cancel()
            schedulerJob = null
        }
    }

    private suspend fun processNextBatch() {
        val pendingTasks = dao.getTasksByStatus(CrawlingStatus.NotStarted)
            .sortedByDescending { it.priority }
            .take(3) // 限制并发数量

        if (pendingTasks.isNotEmpty()) {
            logger.debug("Processing ${pendingTasks.size} pending tasks")

            pendingTasks.forEach { task ->
                try {
                    crawlingExecutor.executeTask(task)
                    delay(2000L) // 任务间延迟
                } catch (e: Exception) {
                    logger.error("Failed to execute task ${task.id}: ${e.message}", e)
                }
            }
        }
    }
}
