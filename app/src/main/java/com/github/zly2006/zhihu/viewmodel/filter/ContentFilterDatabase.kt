/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ContentViewRecord::class, BlockedKeyword::class, BlockedUser::class, BlockedContentRecord::class, BlockedTopic::class, BlockedFeedRecord::class],
    version = 5,
    exportSchema = false,
)
abstract class ContentFilterDatabase : RoomDatabase() {
    abstract fun contentFilterDao(): ContentFilterDao

    abstract fun blockedKeywordDao(): BlockedKeywordDao

    abstract fun blockedUserDao(): BlockedUserDao

    abstract fun blockedContentRecordDao(): BlockedContentRecordDao

    abstract fun blockedTopicDao(): BlockedTopicDao

    abstract fun blockedFeedRecordDao(): BlockedFeedRecordDao

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
                    """.trimIndent(),
                )

                // Add keywordType column to blocked_keywords table with default value
                // Using enum constant to ensure type safety
                val defaultKeywordType = KeywordType.EXACT_MATCH.name
                db.execSQL(
                    """
                    ALTER TABLE `blocked_keywords` 
                    ADD COLUMN `keywordType` TEXT NOT NULL DEFAULT '$defaultKeywordType'
                    """.trimIndent(),
                )
            }
        }

        /**
         * Migration from version 3 to version 4
         * Changes:
         * 1. Add new table: blocked_topics
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create blocked_topics table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `blocked_topics` (
                        `topicId` TEXT NOT NULL PRIMARY KEY,
                        `topicName` TEXT NOT NULL,
                        `addedTime` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Migration from version 4 to version 5
         * Changes:
         * 1. Add new table: blocked_feed_records
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `${BlockedFeedRecord.TABLE_NAME}` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `questionId` INTEGER,
                        `authorName` TEXT,
                        `authorId` TEXT,
                        `url` TEXT,
                        `content` TEXT,
                        `blockedReason` TEXT NOT NULL,
                        `navDestinationJson` TEXT,
                        `feedJson` TEXT,
                        `blockedTime` INTEGER NOT NULL
                    )
                    """.trimIndent(),
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
                    ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true)
                    .build()
            INSTANCE = instance
            instance
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
}
