package com.github.zly2006.zhihu.harmonyprobe.db.room3

import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedKeyword3Dao {
    @Insert
    suspend fun insert(entity: BlockedKeyword3Entity): Long

    @Query("SELECT * FROM blocked_keywords ORDER BY createdTime DESC")
    fun observeAll(): Flow<List<BlockedKeyword3Entity>>

    @Query("SELECT COUNT(*) FROM blocked_keywords")
    suspend fun count(): Int

    @Query("DELETE FROM blocked_keywords")
    suspend fun deleteAll()
}

@Dao
interface BlockedUser3Dao {
    @Upsert
    suspend fun upsert(entity: BlockedUser3Entity)

    /** 约束冲突测试用：主键冲突时抛 SQLiteConstraintException 而非覆盖。 */
    @Insert
    suspend fun insertRaw(entity: BlockedUser3Entity): Long

    @Query("SELECT * FROM blocked_users WHERE userId = :userId")
    suspend fun getByUserId(userId: String): BlockedUser3Entity?

    @Query("SELECT * FROM blocked_users ORDER BY createdTime DESC")
    fun observeAll(): Flow<List<BlockedUser3Entity>>

    @Query("SELECT COUNT(*) FROM blocked_users")
    suspend fun count(): Int
}

@Dao
interface ContentOpenEvent3Dao {
    @Insert
    suspend fun insert(entity: ContentOpenEvent3Entity): Long

    @Query("SELECT * FROM content_open_events ORDER BY openedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ContentOpenEvent3Entity>>

    @Query("SELECT COUNT(*) FROM content_open_events")
    suspend fun count(): Int
}

@Dao
interface BlockedKeyword3V1Dao {
    @Insert
    suspend fun insert(entity: BlockedKeyword3V1Entity): Long

    @Query("SELECT COUNT(*) FROM blocked_keywords")
    suspend fun count(): Int
}

// JVM 上 Room 通过反射实例化 _Impl，无需 @ConstructedBy/RoomDatabaseConstructor（那是 native 目标的要求）。
@Database(
    entities = [BlockedKeyword3Entity::class, BlockedUser3Entity::class, ContentOpenEvent3Entity::class],
    version = 2,
    exportSchema = true,
)
abstract class ContentFilterDb3 : RoomDatabase() {
    abstract fun keywordDao(): BlockedKeyword3Dao
    abstract fun userDao(): BlockedUser3Dao
    abstract fun openEventDao(): ContentOpenEvent3Dao
}

@Database(
    // V1 库同样包含全部三张表（对应生产 v2 之前的形态），只有 blocked_keywords 缺 keywordType 列，
    // 这样迁移语义与生产 migration2To3 一致，且迁移后 Room 的 schema 校验能通过。
    entities = [BlockedKeyword3V1Entity::class, BlockedUser3Entity::class, ContentOpenEvent3Entity::class],
    version = 1,
    exportSchema = true,
)
abstract class ContentFilterDb3V1 : RoomDatabase() {    abstract fun v1KeywordDao(): BlockedKeyword3V1Dao
    abstract fun userDao(): BlockedUser3Dao
    abstract fun openEventDao(): ContentOpenEvent3Dao
}
