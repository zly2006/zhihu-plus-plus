package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CrawlingTask::class, CrawlingResult::class, LocalFeed::class, UserBehavior::class],
    version = 5, // 增加版本号，因为添加了UserBehavior表
    exportSchema = false,
)
@TypeConverters(LocalDatabaseConverters::class)
abstract class LocalContentDatabase : RoomDatabase() {
    abstract fun contentDao(): LocalContentDao

    companion object {
        @Volatile
        private var INSTANCE: LocalContentDatabase? = null

        fun getDatabase(context: Context): LocalContentDatabase = INSTANCE ?: synchronized(this) {
            val instance = Room
                .databaseBuilder(
                    context.applicationContext,
                    LocalContentDatabase::class.java,
                    "local_content_database",
                ).build()
            INSTANCE = instance
            instance
        }
    }
}
