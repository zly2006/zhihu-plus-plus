package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ContentViewRecord::class, BlockedKeyword::class, BlockedUser::class],
    version = 2,
    exportSchema = false,
)
abstract class ContentFilterDatabase : RoomDatabase() {
    abstract fun contentFilterDao(): ContentFilterDao
    abstract fun blockedKeywordDao(): BlockedKeywordDao
    abstract fun blockedUserDao(): BlockedUserDao

    companion object {
        @Volatile
        @Suppress("ktlint")
        private var INSTANCE: ContentFilterDatabase? = null

        fun getDatabase(context: Context): ContentFilterDatabase = INSTANCE ?: synchronized(this) {
            val instance =
                Room
                    .databaseBuilder(
                        context.applicationContext,
                        ContentFilterDatabase::class.java,
                        "content_filter_database",
                    ).fallbackToDestructiveMigration(true) // 临时数据库，可以重置
                    .build()
            INSTANCE = instance
            instance
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
}
