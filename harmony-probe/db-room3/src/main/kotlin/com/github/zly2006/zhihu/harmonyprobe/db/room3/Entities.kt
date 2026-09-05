package com.github.zly2006.zhihu.harmonyprobe.db.room3

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * P3 最小 schema，对齐生产 shared-local-db ContentFilterDatabase（v6）的三张代表性表：
 * - blocked_keywords：自增整型主键 + 布尔列 + 枚举默认值（对应生产迁移 2→3）。
 * - blocked_users：文本主键。
 * - content_open_events：自增主键 + 复合索引（对应生产迁移 5→6）。
 *
 * 实体注解与生产实体逐字段一致（verbatim），仅换包名与 currentEpochMillis 依赖。
 */
@Entity(tableName = BlockedKeyword3Entity.TABLE_NAME)
data class BlockedKeyword3Entity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    // 与生产迁移 2→3 的 ALTER ... DEFAULT 'EXACT_MATCH' 保持一致，保证迁移后 schema 校验通过。
    @ColumnInfo(defaultValue = "EXACT_MATCH") val keywordType: String = KeywordType3.EXACT_MATCH.name,
    val caseSensitive: Boolean = false,
    val isRegex: Boolean = false,
    val createdTime: Long = nowMillis(),
) {
    companion object {
        const val TABLE_NAME = "blocked_keywords"
    }
}

enum class KeywordType3 {
    EXACT_MATCH,
    NLP_SEMANTIC,
}

@Entity(tableName = BlockedUser3Entity.TABLE_NAME)
data class BlockedUser3Entity(
    @PrimaryKey val userId: String,
    val userName: String,
    val urlToken: String? = null,
    val avatarUrl: String? = null,
    val createdTime: Long = nowMillis(),
) {
    companion object {
        const val TABLE_NAME = "blocked_users"
    }
}

@Entity(
    tableName = ContentOpenEvent3Entity.TABLE_NAME,
    indices = [
        Index(value = ["contentType", "contentId"], name = "index_content_open_events_contentType_contentId"),
        Index(value = ["openedAt"], name = "index_content_open_events_openedAt"),
    ],
)
data class ContentOpenEvent3Entity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentType: String,
    val contentId: String,
    val questionId: Long? = null,
    val openFrom: String,
    val openedAt: Long = nowMillis(),
) {
    companion object {
        const val TABLE_NAME = "content_open_events"
    }
}

/** v1 版本的 blocked_keywords（迁移测试用，缺 keywordType 列，对应生产 v2 之前的形态）。 */
@Entity(tableName = BlockedKeyword3Entity.TABLE_NAME)
data class BlockedKeyword3V1Entity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val caseSensitive: Boolean = false,
    val isRegex: Boolean = false,
    val createdTime: Long = nowMillis(),
)

@OptIn(kotlin.time.ExperimentalTime::class)
internal fun nowMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
