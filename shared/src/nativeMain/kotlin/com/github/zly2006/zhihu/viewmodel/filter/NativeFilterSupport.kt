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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
actual fun rememberBlocklistManager(): BlocklistManager = remember {
    // TODO: iOS BlocklistManager 完整实现
    BlocklistManager(
        BlocklistService(
            keywordDao = object : BlockedKeywordDao {
                override suspend fun getAllKeywords(): List<BlockedKeyword> = emptyList()

                override suspend fun getKeywordById(id: Long): BlockedKeyword? = null

                override suspend fun insertKeyword(keyword: BlockedKeyword): Long = 0L

                override suspend fun deleteKeyword(keyword: BlockedKeyword) = Unit

                override suspend fun deleteKeywordById(id: Long) = Unit

                override suspend fun clearAllKeywords() = Unit

                override suspend fun getKeywordCount(): Int = 0
            },
            userDao = object : BlockedUserDao {
                override suspend fun getAllUsers(): List<BlockedUser> = emptyList()

                override suspend fun getUserById(userId: String): BlockedUser? = null

                override suspend fun getAllUserIds(): List<String> = emptyList()

                override suspend fun insertUser(user: BlockedUser) = Unit

                override suspend fun deleteUser(user: BlockedUser) = Unit

                override suspend fun deleteUserById(userId: String) = Unit

                override suspend fun clearAllUsers() = Unit

                override suspend fun getUserCount(): Int = 0

                override suspend fun isUserBlocked(userId: String): Boolean = false
            },
            topicDao = object : BlockedTopicDao {
                override suspend fun insertTopic(topic: BlockedTopic): Long = 0L

                override suspend fun deleteTopicById(topicId: String) = Unit

                override suspend fun getAllTopics(): List<BlockedTopic> = emptyList()

                override suspend fun getTopicCount(): Int = 0

                override suspend fun isTopicBlocked(topicId: String): Boolean = false

                override suspend fun clearAllTopics() = Unit

                override suspend fun getBlockedTopicIds(topicIds: List<String>): List<String> = emptyList()

                override suspend fun getTopicNameById(topicId: String): String = ""
            },
        ),
    )
}

@Composable
actual fun rememberBlockedFeedRecordDao(): BlockedFeedRecordDao = remember {
    object : BlockedFeedRecordDao {
        override fun observeAll(): Flow<List<BlockedFeedRecord>> = emptyFlow() // TODO: iOS 观察所有记录

        override suspend fun getRecent(limit: Int): List<BlockedFeedRecord> = emptyList() // TODO: iOS 获取最近记录

        override suspend fun insert(record: BlockedFeedRecord): Long = 0L // TODO: iOS 插入记录

        override suspend fun deleteById(id: Long) = Unit // TODO: iOS 删除记录

        override suspend fun clearAll() = Unit // TODO: iOS 清空所有

        override suspend fun maintainLimit() = Unit // TODO: iOS 维护限制
    }
} // TODO: iOS 内容过滤数据库完整实现
