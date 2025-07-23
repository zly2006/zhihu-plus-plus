package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [ContentViewRecord::class],
    version = 1,
    exportSchema = false
)
abstract class ContentFilterDatabase : RoomDatabase() {
    abstract fun contentFilterDao(): ContentFilterDao

    companion object {
        @Volatile
        private var INSTANCE: ContentFilterDatabase? = null

        fun getDatabase(context: Context): ContentFilterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        ContentFilterDatabase::class.java,
                        "content_filter_database"
                    ).fallbackToDestructiveMigration(true) // 临时数据库，可以重置
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
}
