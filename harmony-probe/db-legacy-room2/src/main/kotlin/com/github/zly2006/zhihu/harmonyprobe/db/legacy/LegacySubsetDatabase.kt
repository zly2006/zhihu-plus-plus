package com.github.zly2006.zhihu.harmonyprobe.db.legacy

import androidx.room.ColumnInfo
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import kotlinx.coroutines.Dispatchers

/**
 * Room 2.8.4 版本线上的 3 表子集（DDL 与 db-room3 的 ContentFilterDb3 完全一致），
 * 用于「Room3 能否直接打开 Room 2.8.4 创建的文件」的 identity hash / 版本线实验。
 */
@Entity(tableName = "blocked_keywords")
data class LegacySubsetKeyword(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    @ColumnInfo(defaultValue = "EXACT_MATCH") val keywordType: String = "EXACT_MATCH",
    val caseSensitive: Boolean = false,
    val isRegex: Boolean = false,
    val createdTime: Long = 0,
)

@Entity(tableName = "blocked_users")
data class LegacySubsetUser(
    @PrimaryKey val userId: String,
    val userName: String,
    val urlToken: String? = null,
    val avatarUrl: String? = null,
    val createdTime: Long = 0,
)

@Entity(
    tableName = "content_open_events",
    indices = [
        Index(value = ["contentType", "contentId"], name = "index_content_open_events_contentType_contentId"),
        Index(value = ["openedAt"], name = "index_content_open_events_openedAt"),
    ],
)
data class LegacySubsetOpenEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentType: String,
    val contentId: String,
    val questionId: Long? = null,
    val openFrom: String,
    val openedAt: Long = 0,
)

@Dao
interface LegacySubsetKeywordDao {
    @Insert
    suspend fun insert(entity: LegacySubsetKeyword): Long

    @Query("SELECT COUNT(*) FROM blocked_keywords")
    suspend fun count(): Int
}

@Dao
interface LegacySubsetUserDao {
    @Insert
    suspend fun insert(entity: LegacySubsetUser): Long
}

@Dao
interface LegacySubsetOpenEventDao {
    @Insert
    suspend fun insert(entity: LegacySubsetOpenEvent): Long
}

@Database(
    entities = [LegacySubsetKeyword::class, LegacySubsetUser::class, LegacySubsetOpenEvent::class],
    version = 2,
    exportSchema = true,
)
abstract class LegacySubsetDatabase : RoomDatabase() {
    abstract fun subsetKeywordDao(): LegacySubsetKeywordDao
    abstract fun subsetUserDao(): LegacySubsetUserDao
    abstract fun subsetOpenEventDao(): LegacySubsetOpenEventDao
}

fun buildLegacySubsetDatabase(path: String): LegacySubsetDatabase =
    androidx.room.Room.databaseBuilder<LegacySubsetDatabase>(path)
        .setQueryCoroutineContext(Dispatchers.Default)
        .setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver())
        .build()
