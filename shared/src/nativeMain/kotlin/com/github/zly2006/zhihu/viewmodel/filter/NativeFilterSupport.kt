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

import androidx.room.InvalidationTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual fun getContentFilterDatabase(): ContentFilterDatabase = emptyContentFilterDatabase

private val emptyContentFilterDatabase = object : ContentFilterDatabase() {
    override fun createInvalidationTracker(): InvalidationTracker =
        InvalidationTracker(
            database = this,
            shadowTablesMap = emptyMap(),
            viewTables = emptyMap(),
            tableNames = emptyArray(),
        )

    override fun contentFilterDao(): ContentFilterDao = emptyContentFilterDao

    override fun contentOpenEventDao(): ContentOpenEventDao = emptyContentOpenEventDao

    override fun blockedKeywordDao(): BlockedKeywordDao = emptyBlockedKeywordDao

    override fun blockedUserDao(): BlockedUserDao = emptyBlockedUserDao

    override fun blockedContentRecordDao(): BlockedContentRecordDao = emptyBlockedContentRecordDao

    override fun blockedTopicDao(): BlockedTopicDao = emptyBlockedTopicDao

    override fun blockedFeedRecordDao(): BlockedFeedRecordDao = emptyBlockedFeedRecordDao

    override fun blockedMcnOrganizationDao(): BlockedMcnOrganizationDao = emptyBlockedMcnOrganizationDao

    override fun mcnAuthorCacheDao(): McnAuthorCacheDao = emptyMcnAuthorCacheDao
}

private val emptyContentFilterDao = object : ContentFilterDao {
    override suspend fun getViewRecord(id: String): ContentViewRecord? = null

    override suspend fun insertOrUpdateViewRecord(record: ContentViewRecord) = Unit

    override suspend fun incrementViewCount(id: String, currentTime: Long) = Unit

    override suspend fun markAsInteracted(id: String, currentTime: Long) = Unit

    override suspend fun getFilteredContent(maxCount: Int): List<ContentViewRecord> = emptyList()

    override suspend fun getViewedContentIdsByIds(ids: List<String>): List<String> = emptyList()

    override suspend fun cleanupOldRecords(cutoffTime: Long) = Unit

    override suspend fun clearAllRecords() = Unit

    override suspend fun getRecordCount(): Int = 0
}

private val emptyContentOpenEventDao = object : ContentOpenEventDao {
    override suspend fun insert(event: ContentOpenEvent): Long = 0L

    override suspend fun getOpenedContentKeysByKeys(keys: List<String>): List<String> = emptyList()
}

private val emptyBlockedKeywordDao = object : BlockedKeywordDao {
    override suspend fun getAllKeywords(): List<BlockedKeyword> = emptyList()

    override suspend fun insertKeyword(keyword: BlockedKeyword): Long = 0L

    override suspend fun deleteKeyword(keyword: BlockedKeyword) = Unit

    override suspend fun deleteKeywordById(id: Long) = Unit

    override suspend fun clearAllKeywords() = Unit

    override suspend fun getKeywordCount(): Int = 0
}

private val emptyBlockedUserDao = object : BlockedUserDao {
    override suspend fun getAllUsers(): List<BlockedUser> = emptyList()

    override suspend fun insertUser(user: BlockedUser) = Unit

    override suspend fun deleteUserById(userId: String) = Unit

    override suspend fun clearAllUsers() = Unit

    override suspend fun getUserCount(): Int = 0

    override suspend fun isUserBlocked(userId: String): Boolean = false
}

private val emptyBlockedContentRecordDao = object : BlockedContentRecordDao {
    override suspend fun getRecentBlockedRecords(limit: Int): List<BlockedContentRecord> = emptyList()

    override suspend fun insertRecord(record: BlockedContentRecord): Long = 0L

    override suspend fun deleteRecord(id: Long) = Unit

    override suspend fun clearAllRecords() = Unit

    override suspend fun getRecordCount(): Int = 0

    override suspend fun deleteOldestRecords(deleteCount: Int) = Unit
}

private val emptyBlockedTopicDao = object : BlockedTopicDao {
    override suspend fun insertTopic(topic: BlockedTopic): Long = 0L

    override suspend fun deleteTopicById(topicId: String) = Unit

    override suspend fun getAllTopics(): List<BlockedTopic> = emptyList()

    override suspend fun getTopicCount(): Int = 0

    override suspend fun isTopicBlocked(topicId: String): Boolean = false

    override suspend fun clearAllTopics() = Unit

    override suspend fun getBlockedTopicIds(topicIds: List<String>): List<String> = emptyList()

    override suspend fun getTopicNameById(topicId: String): String = ""
}

private val emptyBlockedFeedRecordDao = object : BlockedFeedRecordDao {
    override fun observeAll(): Flow<List<BlockedFeedRecord>> = emptyFlow()

    override suspend fun insert(record: BlockedFeedRecord): Long = 0L

    override suspend fun deleteById(id: Long) = Unit

    override suspend fun clearAll() = Unit

    override suspend fun maintainLimit() = Unit
}

private val emptyBlockedMcnOrganizationDao = object : BlockedMcnOrganizationDao {
    override suspend fun insertOrganization(organization: BlockedMcnOrganization) = Unit

    override suspend fun deleteOrganizationByName(organizationName: String) = Unit

    override suspend fun getAllOrganizations(): List<BlockedMcnOrganization> = emptyList()

    override suspend fun getOrganizationCount(): Int = 0

    override suspend fun hasOrganizations(): Boolean = false

    override suspend fun clearAllOrganizations() = Unit
}

private val emptyMcnAuthorCacheDao = object : McnAuthorCacheDao {
    override suspend fun insert(cache: McnAuthorCache) = Unit

    override suspend fun getByUrlToken(urlToken: String): McnAuthorCache? = null

    override suspend fun clearAll() = Unit
}
