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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Blocklist storage facade.
 * Core CRUD and matching semantics live in [BlocklistService].
 */
class BlocklistManager(
    private val service: BlocklistService,
) {
    suspend fun addBlockedKeyword(
        keyword: String,
        caseSensitive: Boolean = false,
        isRegex: Boolean = false,
    ): Long = withContext(Dispatchers.Default) {
        service.addBlockedKeyword(keyword, caseSensitive, isRegex)
    }

    suspend fun removeBlockedKeyword(keywordId: Long) {
        withContext(Dispatchers.Default) {
            service.removeBlockedKeyword(keywordId)
        }
    }

    suspend fun getAllBlockedKeywords(): List<BlockedKeyword> = withContext(Dispatchers.Default) {
        service.getAllBlockedKeywords()
    }

    suspend fun clearAllBlockedKeywords() {
        withContext(Dispatchers.Default) {
            service.clearAllBlockedKeywords()
        }
    }

    suspend fun containsBlockedKeyword(text: String?): Boolean = withContext(Dispatchers.Default) {
        service.containsBlockedKeyword(text)
    }

    suspend fun addBlockedUser(
        userId: String,
        userName: String,
        urlToken: String? = null,
        avatarUrl: String? = null,
    ) {
        withContext(Dispatchers.Default) {
            service.addBlockedUser(userId, userName, urlToken, avatarUrl)
        }
    }

    suspend fun removeBlockedUser(userId: String) {
        withContext(Dispatchers.Default) {
            service.removeBlockedUser(userId)
        }
    }

    suspend fun getAllBlockedUsers(): List<BlockedUser> = withContext(Dispatchers.Default) {
        service.getAllBlockedUsers()
    }

    suspend fun clearAllBlockedUsers() {
        withContext(Dispatchers.Default) {
            service.clearAllBlockedUsers()
        }
    }

    suspend fun isUserBlocked(userId: String?): Boolean = withContext(Dispatchers.Default) {
        service.isUserBlocked(userId)
    }

    suspend fun getBlocklistStats(): BlocklistStats = withContext(Dispatchers.Default) {
        service.getBlocklistStats()
    }

    suspend fun clearAllBlocklists() {
        withContext(Dispatchers.Default) {
            service.clearAllBlocklists()
        }
    }

    suspend fun addBlockedTopic(
        topicId: String,
        topicName: String,
    ): Long = withContext(Dispatchers.Default) {
        service.addBlockedTopic(topicId, topicName)
    }

    suspend fun removeBlockedTopic(topicId: String) {
        withContext(Dispatchers.Default) {
            service.removeBlockedTopic(topicId)
        }
    }

    suspend fun getAllBlockedTopics(): List<BlockedTopic> = withContext(Dispatchers.Default) {
        service.getAllBlockedTopics()
    }

    suspend fun clearAllBlockedTopics() {
        withContext(Dispatchers.Default) {
            service.clearAllBlockedTopics()
        }
    }

    suspend fun isTopicBlocked(topicId: String?): Boolean = withContext(Dispatchers.Default) {
        service.isTopicBlocked(topicId)
    }

    suspend fun countBlockedTopics(topicIds: List<String>?): Int = withContext(Dispatchers.Default) {
        service.countBlockedTopics(topicIds)
    }

    suspend fun exportAllBlocklistToJsonText(): String = withContext(Dispatchers.Default) {
        service.encodeAllBlocklistToJson()
    }

    suspend fun importAllBlocklistFromJsonText(text: String): String = withContext(Dispatchers.Default) {
        service.importAllBlocklistFromJsonText(text)
    }

    suspend fun getTopicName(topicId: String): String = withContext(Dispatchers.Default) {
        service.getTopicName(topicId)
    }
}

fun ContentFilterDatabase.createBlocklistManager(): BlocklistManager = BlocklistManager(
    BlocklistService(
        keywordDao = blockedKeywordDao(),
        userDao = blockedUserDao(),
        topicDao = blockedTopicDao(),
    ),
)

@Composable
expect fun rememberBlocklistManager(): BlocklistManager
