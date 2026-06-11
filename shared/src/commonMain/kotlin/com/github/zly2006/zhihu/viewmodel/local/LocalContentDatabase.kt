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

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Builder
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.github.zly2006.zhihu.data.applyPlatformDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [CrawlingTask::class, CrawlingResult::class, LocalFeed::class, UserBehavior::class],
    version = 5, // 增加版本号，因为添加了UserBehavior表
    exportSchema = false,
)
@TypeConverters(LocalDatabaseConverters::class)
@ConstructedBy(LocalContentDatabaseConstructor::class)
abstract class LocalContentDatabase : RoomDatabase() {
    abstract fun contentDao(): LocalContentDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object LocalContentDatabaseConstructor : RoomDatabaseConstructor<LocalContentDatabase> {
    override fun initialize(): LocalContentDatabase
}

fun buildLocalContentDatabase(
    builder: Builder<LocalContentDatabase>,
): LocalContentDatabase = builder
    .applyPlatformDriver()
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()
