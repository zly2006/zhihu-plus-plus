package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ContentViewRecord::class, BlockedKeyword::class, BlockedUser::class, BlockedContentRecord::class],
    version = 3,
    exportSchema = false,
)
abstract class ContentFilterDatabase : RoomDatabase() {
    abstract fun contentFilterDao(): ContentFilterDao

    abstract fun blockedKeywordDao(): BlockedKeywordDao

    abstract fun blockedUserDao(): BlockedUserDao

    abstract fun blockedContentRecordDao(): BlockedContentRecordDao

    companion object {
        @Volatile
        @Suppress("ktlint")
        private var INSTANCE: ContentFilterDatabase? = null

        /**
         * Migration from version 2 to version 3
         * Changes:
         * 1. Add new table: blocked_content_records
         * 2. Add keywordType column to blocked_keywords table with default value 'EXACT_MATCH'
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create blocked_content_records table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `blocked_content_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `contentId` TEXT NOT NULL,
                        `contentType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `excerpt` TEXT,
                        `authorName` TEXT,
                        `authorId` TEXT,
                        `blockedTime` INTEGER NOT NULL,
                        `blockReason` TEXT NOT NULL,
                        `matchedKeywords` TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                // Add keywordType column to blocked_keywords table
                // SQLite doesn't support adding columns with default values in older versions,
                // so we add the column and then update all rows
                db.execSQL(
                    """
                    ALTER TABLE `blocked_keywords` 
                    ADD COLUMN `keywordType` TEXT NOT NULL DEFAULT 'EXACT_MATCH'
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): ContentFilterDatabase = INSTANCE ?: synchronized(this) {
            val instance =
                Room
                    .databaseBuilder(
                        context.applicationContext,
                        ContentFilterDatabase::class.java,
                        "content_filter_database",
                    )
                    .addMigrations(MIGRATION_2_3)
                    .build()
            INSTANCE = instance
            instance
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
}
