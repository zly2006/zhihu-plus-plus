package com.github.zly2006.zhihu.local.database

import com.github.zly2006.zhihu.local.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class LocalContentDao(private val database: Database) {

    // CrawlingTask 相关操作
    fun getTasksByStatus(status: CrawlingStatus): List<CrawlingTask> = transaction(database) {
        CrawlingTasks.select { CrawlingTasks.status eq status }
            .orderBy(CrawlingTasks.priority to SortOrder.DESC, CrawlingTasks.createdAt to SortOrder.ASC)
            .map { rowToCrawlingTask(it) }
    }

    fun getTaskCountByReasonAndStatus(reason: CrawlingReason, status: CrawlingStatus): Int = transaction(database) {
        CrawlingTasks.select { (CrawlingTasks.reason eq reason) and (CrawlingTasks.status eq status) }
            .count().toInt()
    }

    fun insertTask(task: CrawlingTask): Long = transaction(database) {
        CrawlingTasks.insertAndGetId {
            it[url] = task.url
            it[reason] = task.reason
            it[status] = task.status
            it[priority] = task.priority
            it[createdAt] = Instant.ofEpochMilli(task.createdAt)
            task.executedAt?.let { executedAt -> it[CrawlingTasks.executedAt] = Instant.ofEpochMilli(executedAt) }
            it[errorMessage] = task.errorMessage
        }.value
    }

    fun insertTasks(tasks: List<CrawlingTask>) = transaction(database) {
        CrawlingTasks.batchInsert(tasks) { task ->
            this[CrawlingTasks.url] = task.url
            this[CrawlingTasks.reason] = task.reason
            this[CrawlingTasks.status] = task.status
            this[CrawlingTasks.priority] = task.priority
            this[CrawlingTasks.createdAt] = Instant.ofEpochMilli(task.createdAt)
            task.executedAt?.let { executedAt -> this[CrawlingTasks.executedAt] = Instant.ofEpochMilli(executedAt) }
            this[CrawlingTasks.errorMessage] = task.errorMessage
        }
    }

    fun updateTask(task: CrawlingTask) = transaction(database) {
        CrawlingTasks.update({ CrawlingTasks.id eq task.id }) {
            it[url] = task.url
            it[reason] = task.reason
            it[status] = task.status
            it[priority] = task.priority
            it[createdAt] = Instant.ofEpochMilli(task.createdAt)
            task.executedAt?.let { executedAt -> it[CrawlingTasks.executedAt] = Instant.ofEpochMilli(executedAt) }
            it[errorMessage] = task.errorMessage
        }
    }

    fun cleanupOldTasks(status: CrawlingStatus, before: Long) = transaction(database) {
        CrawlingTasks.deleteWhere {
            (CrawlingTasks.status eq status) and (CrawlingTasks.createdAt less Instant.ofEpochMilli(before))
        }
    }

    // CrawlingResult 相关操作
    fun getResultsByReason(reason: CrawlingReason): List<CrawlingResult> = transaction(database) {
        CrawlingResults.select { CrawlingResults.reason eq reason }
            .orderBy(CrawlingResults.score to SortOrder.DESC, CrawlingResults.createdAt to SortOrder.DESC)
            .map { rowToCrawlingResult(it) }
    }

    fun getResultCountByReason(reason: CrawlingReason): Int = transaction(database) {
        CrawlingResults.select { CrawlingResults.reason eq reason }.count().toInt()
    }

    fun getResultByContentId(contentId: String): CrawlingResult? = transaction(database) {
        CrawlingResults.select { CrawlingResults.contentId eq contentId }
            .singleOrNull()?.let { rowToCrawlingResult(it) }
    }

    fun insertResult(result: CrawlingResult): Long = transaction(database) {
        CrawlingResults.insertAndGetId {
            it[taskId] = result.taskId
            it[contentId] = result.contentId
            it[title] = result.title
            it[summary] = result.summary
            it[url] = result.url
            it[reason] = result.reason
            it[score] = result.score
            it[createdAt] = Instant.ofEpochMilli(result.createdAt)
        }.value
    }

    fun insertResults(results: List<CrawlingResult>) = transaction(database) {
        CrawlingResults.batchInsert(results) { result ->
            this[CrawlingResults.taskId] = result.taskId
            this[CrawlingResults.contentId] = result.contentId
            this[CrawlingResults.title] = result.title
            this[CrawlingResults.summary] = result.summary
            this[CrawlingResults.url] = result.url
            this[CrawlingResults.reason] = result.reason
            this[CrawlingResults.score] = result.score
            this[CrawlingResults.createdAt] = Instant.ofEpochMilli(result.createdAt)
        }
    }

    fun cleanupOldResults(before: Long) = transaction(database) {
        CrawlingResults.deleteWhere {
            CrawlingResults.createdAt less Instant.ofEpochMilli(before)
        }
    }

    // LocalFeed 相关操作
    fun getFeedByResultId(resultId: Long): LocalFeed? = transaction(database) {
        LocalFeeds.select { LocalFeeds.resultId eq resultId }
            .singleOrNull()?.let { rowToLocalFeed(it) }
    }

    fun getTopRatedFeeds(limit: Int): List<LocalFeed> = transaction(database) {
        LocalFeeds.selectAll()
            .orderBy(LocalFeeds.userFeedback to SortOrder.DESC)
            .limit(limit)
            .map { rowToLocalFeed(it) }
    }

    fun getRecentFeeds(limit: Int): List<LocalFeed> = transaction(database) {
        LocalFeeds.selectAll()
            .orderBy(LocalFeeds.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { rowToLocalFeed(it) }
    }

    fun updateFeedFeedback(feedId: String, feedback: Double) = transaction(database) {
        LocalFeeds.update({ LocalFeeds.feedId eq feedId }) {
            it[userFeedback] = feedback
        }
    }

    fun cleanupOldFeeds(before: Long) = transaction(database) {
        LocalFeeds.deleteWhere {
            LocalFeeds.createdAt less Instant.ofEpochMilli(before)
        }
    }

    // UserBehavior 相关操作
    fun insertBehavior(behavior: UserBehavior): Long = transaction(database) {
        UserBehaviors.insertAndGetId {
            it[contentId] = behavior.contentId
            it[action] = behavior.action
            it[timestamp] = Instant.ofEpochMilli(behavior.timestamp)
            it[duration] = behavior.duration
        }.value
    }

    fun cleanupOldBehaviors(before: Long) = transaction(database) {
        UserBehaviors.deleteWhere {
            UserBehaviors.timestamp less Instant.ofEpochMilli(before)
        }
    }

    // 工具方法：将数据库行转换为数据类
    private fun rowToCrawlingTask(row: ResultRow) = CrawlingTask(
        id = row[CrawlingTasks.id].value,
        url = row[CrawlingTasks.url],
        reason = row[CrawlingTasks.reason],
        status = row[CrawlingTasks.status],
        priority = row[CrawlingTasks.priority],
        createdAt = row[CrawlingTasks.createdAt].toEpochMilli(),
        executedAt = row[CrawlingTasks.executedAt]?.toEpochMilli(),
        errorMessage = row[CrawlingTasks.errorMessage]
    )

    private fun rowToCrawlingResult(row: ResultRow) = CrawlingResult(
        id = row[CrawlingResults.id].value,
        taskId = row[CrawlingResults.taskId],
        contentId = row[CrawlingResults.contentId],
        title = row[CrawlingResults.title],
        summary = row[CrawlingResults.summary],
        url = row[CrawlingResults.url],
        reason = row[CrawlingResults.reason],
        score = row[CrawlingResults.score],
        createdAt = row[CrawlingResults.createdAt].toEpochMilli()
    )

    private fun rowToLocalFeed(row: ResultRow) = LocalFeed(
        id = row[LocalFeeds.feedId],
        resultId = row[LocalFeeds.resultId],
        title = row[LocalFeeds.title],
        summary = row[LocalFeeds.summary],
        reasonDisplay = row[LocalFeeds.reasonDisplay],
        navDestination = row[LocalFeeds.navDestination],
        userFeedback = row[LocalFeeds.userFeedback],
        createdAt = row[LocalFeeds.createdAt].toEpochMilli()
    )
}
