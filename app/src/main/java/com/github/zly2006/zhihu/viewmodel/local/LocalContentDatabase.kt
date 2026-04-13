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
        @Suppress("ktlint")
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
