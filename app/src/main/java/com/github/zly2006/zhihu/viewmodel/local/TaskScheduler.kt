package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import kotlinx.coroutines.*
import java.util.concurrent.Executors

/**
 * 智能任务调度器，负责管理爬虫任务的执行
 */
class TaskScheduler(private val context: Context) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }
    private val crawlingExecutor by lazy { CrawlingExecutor(context) }

    // 使用单线程执行器确保任务按优先级顺序执行
    private val scheduler = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 启动任务调度
     */
    fun startScheduling() {
        scope.launch {
            while (true) {
                try {
                    executeNextTask()
                    delay(5000) // 每5秒检查一次新任务
                } catch (e: Exception) {
                    delay(10000) // 出错时等待更长时间
                }
            }
        }
    }

    /**
     * 执行下一个优先级最高的任务
     */
    private suspend fun executeNextTask() {
        val pendingTasks = dao.getTasksByStatus(CrawlingStatus.NotStarted)
        val highPriorityTask = pendingTasks
            .filter { it.retryCount < 3 } // 过滤重试次数过多的任务
            .maxByOrNull { it.priority } // 选择优先级最高的任务

        highPriorityTask?.let { task ->
            try {
                crawlingExecutor.executeTask(task)
            } catch (e: Exception) {
                // 增加重试次数
                dao.updateTask(task.copy(
                    retryCount = task.retryCount + 1,
                    errorMessage = e.message
                ))
            }
        }
    }

    /**
     * 添加新任务
     */
    suspend fun addTask(task: CrawlingTask) {
        dao.insertTask(task)
    }

    /**
     * 停止调度
     */
    fun stopScheduling() {
        scope.cancel()
        scheduler.shutdown()
    }
}
