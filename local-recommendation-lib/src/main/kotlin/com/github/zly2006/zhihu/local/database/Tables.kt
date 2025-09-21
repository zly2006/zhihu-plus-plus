package com.github.zly2006.zhihu.local.database

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object CrawlingTasks : LongIdTable("crawling_tasks") {
    val url = varchar("url", 2048)
    val reason = enumeration("reason", com.github.zly2006.zhihu.local.CrawlingReason::class)
    val status = enumeration("status", com.github.zly2006.zhihu.local.CrawlingStatus::class)
    val priority = integer("priority").default(0)
    val createdAt = timestamp("created_at").default(Instant.now())
    val executedAt = timestamp("executed_at").nullable()
    val errorMessage = text("error_message").nullable()
}

object CrawlingResults : LongIdTable("crawling_results") {
    val taskId = long("task_id")
    val contentId = varchar("content_id", 255)
    val title = text("title")
    val summary = text("summary")
    val url = varchar("url", 2048)
    val reason = enumeration("reason", com.github.zly2006.zhihu.local.CrawlingReason::class)
    val score = double("score").default(0.0)
    val createdAt = timestamp("created_at").default(Instant.now())
}

object LocalFeeds : LongIdTable("local_feeds") {
    val feedId = varchar("feed_id", 255).uniqueIndex()
    val resultId = long("result_id")
    val title = text("title")
    val summary = text("summary")
    val reasonDisplay = varchar("reason_display", 255)
    val navDestination = varchar("nav_destination", 1024).nullable()
    val userFeedback = double("user_feedback").default(0.0)
    val createdAt = timestamp("created_at").default(Instant.now())
}

object UserBehaviors : LongIdTable("user_behaviors") {
    val contentId = varchar("content_id", 255)
    val action = varchar("action", 50)
    val timestamp = timestamp("timestamp").default(Instant.now())
    val duration = long("duration").nullable()
}
