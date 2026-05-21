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
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android facade for blocklist storage and import/export.
 * Core CRUD and matching semantics live in [BlocklistService].
 */
class BlocklistManager private constructor(
    context: Context,
) {
    private val database = getContentFilterDatabase(context)
    private val service = BlocklistService(
        keywordDao = database.blockedKeywordDao(),
        userDao = database.blockedUserDao(),
        topicDao = database.blockedTopicDao(),
    )

    companion object {
        @Volatile
        @Suppress("ktlint")
        private var INSTANCE: BlocklistManager? = null

        fun getInstance(context: Context): BlocklistManager = INSTANCE ?: synchronized(this) {
            val instance = BlocklistManager(context.applicationContext)
            INSTANCE = instance
            instance
        }
    }

    suspend fun addBlockedKeyword(
        keyword: String,
        caseSensitive: Boolean = false,
        isRegex: Boolean = false,
    ): Long = withContext(Dispatchers.IO) {
        service.addBlockedKeyword(keyword, caseSensitive, isRegex)
    }

    suspend fun removeBlockedKeyword(keywordId: Long) {
        withContext(Dispatchers.IO) {
            service.removeBlockedKeyword(keywordId)
        }
    }

    suspend fun getAllBlockedKeywords(): List<BlockedKeyword> = withContext(Dispatchers.IO) {
        service.getAllBlockedKeywords()
    }

    suspend fun clearAllBlockedKeywords() {
        withContext(Dispatchers.IO) {
            service.clearAllBlockedKeywords()
        }
    }

    suspend fun containsBlockedKeyword(text: String?): Boolean = withContext(Dispatchers.IO) {
        service.containsBlockedKeyword(text)
    }

    suspend fun addBlockedUser(
        userId: String,
        userName: String,
        urlToken: String? = null,
        avatarUrl: String? = null,
    ) {
        withContext(Dispatchers.IO) {
            service.addBlockedUser(userId, userName, urlToken, avatarUrl)
        }
    }

    suspend fun removeBlockedUser(userId: String) {
        withContext(Dispatchers.IO) {
            service.removeBlockedUser(userId)
        }
    }

    suspend fun getAllBlockedUsers(): List<BlockedUser> = withContext(Dispatchers.IO) {
        service.getAllBlockedUsers()
    }

    suspend fun clearAllBlockedUsers() {
        withContext(Dispatchers.IO) {
            service.clearAllBlockedUsers()
        }
    }

    suspend fun isUserBlocked(userId: String?): Boolean = withContext(Dispatchers.IO) {
        service.isUserBlocked(userId)
    }

    suspend fun getBlocklistStats(): BlocklistStats = withContext(Dispatchers.IO) {
        service.getBlocklistStats()
    }

    suspend fun clearAllBlocklists() {
        withContext(Dispatchers.IO) {
            service.clearAllBlocklists()
        }
    }

    suspend fun addBlockedTopic(
        topicId: String,
        topicName: String,
    ): Long = withContext(Dispatchers.IO) {
        service.addBlockedTopic(topicId, topicName)
    }

    suspend fun removeBlockedTopic(topicId: String) {
        withContext(Dispatchers.IO) {
            service.removeBlockedTopic(topicId)
        }
    }

    suspend fun getAllBlockedTopics(): List<BlockedTopic> = withContext(Dispatchers.IO) {
        service.getAllBlockedTopics()
    }

    suspend fun clearAllBlockedTopics() {
        withContext(Dispatchers.IO) {
            service.clearAllBlockedTopics()
        }
    }

    suspend fun isTopicBlocked(topicId: String?): Boolean = withContext(Dispatchers.IO) {
        service.isTopicBlocked(topicId)
    }

    suspend fun countBlockedTopics(topicIds: List<String>?): Int = withContext(Dispatchers.IO) {
        service.countBlockedTopics(topicIds)
    }

    suspend fun exportAllBlocklistToJson(context: Context): File = withContext(Dispatchers.IO) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, "zhihupp_blocklist.json")
        file.writeText(service.encodeAllBlocklistToJson())
        file
    }

    suspend fun importAllBlocklistFromJson(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val text = context.contentResolver
            .openInputStream(uri)
            ?.bufferedReader()
            ?.readText()
            ?: return@withContext "读取文件失败"
        service.importAllBlocklistFromJsonText(text)
    }

    suspend fun getTopicName(topicId: String): String = withContext(Dispatchers.IO) {
        service.getTopicName(topicId)
    }
}
