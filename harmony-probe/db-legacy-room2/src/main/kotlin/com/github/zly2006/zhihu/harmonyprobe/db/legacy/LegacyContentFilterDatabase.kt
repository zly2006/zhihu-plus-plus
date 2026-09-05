package com.github.zly2006.zhihu.harmonyprobe.db.legacy

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.github.zly2006.zhihu.viewmodel.filter.BlockedContentRecord
import com.github.zly2006.zhihu.viewmodel.filter.BlockedFeedRecord
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.BlockedTopic
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenEvent
import com.github.zly2006.zhihu.viewmodel.filter.ContentViewRecord
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import kotlinx.coroutines.flow.Flow

/**
 * 与生产 shared-local-db ContentFilterDatabase（v6）完全相同的实体集合与版本号，
 * 但只声明测试所需的最小 DAO（schema 由实体决定，DAO 不影响 DDL）。
 * Schema JSON 会导出到 db-legacy-room2/schemas，作为生产格式的权威记录。
 */
@Database(
    entities = [
        ContentViewRecord::class,
        BlockedKeyword::class,
        BlockedUser::class,
        BlockedContentRecord::class,
        BlockedTopic::class,
        BlockedFeedRecord::class,
        ContentOpenEvent::class,
    ],
    version = 6,
    exportSchema = true,
)
// JVM 上 Room 通过反射实例化 _Impl，无需 @ConstructedBy/RoomDatabaseConstructor（那是 native 目标的要求）。
abstract class LegacyContentFilterDatabase : RoomDatabase() {
    abstract fun legacyKeywordDao(): LegacyKeywordDao
    abstract fun legacyUserDao(): LegacyUserDao
    abstract fun legacyOpenEventDao(): LegacyOpenEventDao
}

@Dao
interface LegacyKeywordDao {
    @Insert
    suspend fun insert(entity: BlockedKeyword): Long

    @Query("SELECT * FROM blocked_keywords ORDER BY createdTime DESC")
    suspend fun selectAll(): List<BlockedKeyword>

    @Query("SELECT COUNT(*) FROM blocked_keywords")
    suspend fun count(): Int
}

@Dao
interface LegacyUserDao {
    @Insert
    suspend fun insert(entity: BlockedUser): Long

    @Query("SELECT * FROM blocked_users WHERE userId = :userId")
    suspend fun getByUserId(userId: String): BlockedUser?
}

@Dao
interface LegacyOpenEventDao {
    @Insert
    suspend fun insert(entity: ContentOpenEvent): Long

    @Query("SELECT COUNT(*) FROM content_open_events")
    suspend fun count(): Int
}
