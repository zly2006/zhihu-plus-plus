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

import com.github.zly2006.zhihu.shared.filter.BlocklistBackup
import com.github.zly2006.zhihu.shared.filter.KeywordBackup
import com.github.zly2006.zhihu.shared.filter.NlpKeywordBackup
import com.github.zly2006.zhihu.shared.filter.TopicBackup
import com.github.zly2006.zhihu.shared.filter.UserBackup
import kotlinx.serialization.json.Json

/**
 * 屏蔽列表核心服务。
 * 只负责关键词、用户、主题屏蔽的本地数据语义，不处理平台上下文、URI 或文件路径。
 */
class BlocklistService(
    private val keywordDao: BlockedKeywordDao,
    private val userDao: BlockedUserDao,
    private val topicDao: BlockedTopicDao,
) {
    /**
     * 添加屏蔽关键词
     */
    suspend fun addBlockedKeyword(
        keyword: String,
        caseSensitive: Boolean = false,
        isRegex: Boolean = false,
    ): Long {
        val blockedKeyword = BlockedKeyword(
            keyword = keyword.trim(),
            keywordType = KeywordType.EXACT_MATCH.name,
            caseSensitive = caseSensitive,
            isRegex = isRegex,
        )
        return keywordDao.insertKeyword(blockedKeyword)
    }

    /**
     * 删除屏蔽关键词
     */
    suspend fun removeBlockedKeyword(keywordId: Long) {
        keywordDao.deleteKeywordById(keywordId)
    }

    /**
     * 获取所有屏蔽关键词
     */
    suspend fun getAllBlockedKeywords(): List<BlockedKeyword> = keywordDao.getAllKeywords()

    /**
     * 清空所有屏蔽关键词
     */
    suspend fun clearAllBlockedKeywords() {
        keywordDao.clearAllKeywords()
    }

    /**
     * 检查文本是否包含屏蔽关键词（仅精确匹配）
     */
    suspend fun containsBlockedKeyword(text: String?): Boolean {
        if (text.isNullOrBlank()) return false

        val keywords = keywordDao
            .getAllKeywords()
            .filter { it.getKeywordTypeEnum() == KeywordType.EXACT_MATCH }

        return keywords.any { blockedKeyword ->
            try {
                when {
                    blockedKeyword.isRegex -> {
                        val pattern = if (blockedKeyword.caseSensitive) {
                            Regex(blockedKeyword.keyword)
                        } else {
                            Regex(blockedKeyword.keyword, RegexOption.IGNORE_CASE)
                        }
                        pattern.containsMatchIn(text)
                    }
                    blockedKeyword.caseSensitive -> {
                        text.contains(blockedKeyword.keyword)
                    }
                    else -> {
                        text.contains(blockedKeyword.keyword, ignoreCase = true)
                    }
                }
            } catch (e: Exception) {
                // 正则表达式错误，忽略这个关键词
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * 添加屏蔽用户
     */
    suspend fun addBlockedUser(
        userId: String,
        userName: String,
        urlToken: String? = null,
        avatarUrl: String? = null,
    ) {
        val blockedUser = BlockedUser(
            userId = userId,
            userName = userName,
            urlToken = urlToken,
            avatarUrl = avatarUrl,
        )
        userDao.insertUser(blockedUser)
    }

    /**
     * 删除屏蔽用户
     */
    suspend fun removeBlockedUser(userId: String) {
        userDao.deleteUserById(userId)
    }

    /**
     * 获取所有屏蔽用户
     */
    suspend fun getAllBlockedUsers(): List<BlockedUser> = userDao.getAllUsers()

    /**
     * 清空所有屏蔽用户
     */
    suspend fun clearAllBlockedUsers() {
        userDao.clearAllUsers()
    }

    /**
     * 检查用户是否被屏蔽
     */
    suspend fun isUserBlocked(userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        return userDao.isUserBlocked(userId)
    }

    /**
     * 获取屏蔽统计信息
     */
    suspend fun getBlocklistStats(): BlocklistStats =
        BlocklistStats(
            keywordCount = keywordDao.getKeywordCount(),
            userCount = userDao.getUserCount(),
            topicCount = topicDao.getTopicCount(),
        )

    /**
     * 清空所有屏蔽数据
     */
    suspend fun clearAllBlocklists() {
        keywordDao.clearAllKeywords()
        userDao.clearAllUsers()
        topicDao.clearAllTopics()
    }

    /**
     * 添加屏蔽主题
     */
    suspend fun addBlockedTopic(
        topicId: String,
        topicName: String,
    ): Long {
        val blockedTopic = BlockedTopic(
            topicId = topicId,
            topicName = topicName,
        )
        return topicDao.insertTopic(blockedTopic)
    }

    /**
     * 删除屏蔽主题
     */
    suspend fun removeBlockedTopic(topicId: String) {
        topicDao.deleteTopicById(topicId)
    }

    /**
     * 获取所有屏蔽主题
     */
    suspend fun getAllBlockedTopics(): List<BlockedTopic> = topicDao.getAllTopics()

    /**
     * 清空所有屏蔽主题
     */
    suspend fun clearAllBlockedTopics() {
        topicDao.clearAllTopics()
    }

    /**
     * 检查主题是否被屏蔽
     */
    suspend fun isTopicBlocked(topicId: String?): Boolean {
        if (topicId.isNullOrBlank()) return false
        return topicDao.isTopicBlocked(topicId)
    }

    /**
     * 批量检查主题是否被屏蔽，返回被屏蔽的主题数量
     */
    suspend fun countBlockedTopics(topicIds: List<String>?): Int {
        if (topicIds.isNullOrEmpty()) return 0
        return topicDao.getBlockedTopicIds(topicIds).size
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun encodeAllBlocklistToJson(): String {
        val allKeywords = keywordDao.getAllKeywords()
        val users = userDao.getAllUsers()
        val topics = topicDao.getAllTopics()

        val backup = BlocklistBackup(
            keywords = allKeywords
                .filter { it.getKeywordTypeEnum() == KeywordType.EXACT_MATCH }
                .map { KeywordBackup(it.keyword, it.caseSensitive, it.isRegex) },
            nlpKeywords = allKeywords
                .filter { it.getKeywordTypeEnum() == KeywordType.NLP_SEMANTIC }
                .map { NlpKeywordBackup(it.keyword) },
            users = users.map { UserBackup(it.userId, it.userName, it.urlToken ?: "", it.avatarUrl ?: "") },
            topics = topics.map { TopicBackup(it.topicId, it.topicName) },
        )

        return json.encodeToString(BlocklistBackup.serializer(), backup)
    }

    suspend fun importAllBlocklistFromJsonText(text: String): String {
        val backup = json.decodeFromString(BlocklistBackup.serializer(), text)

        backup.keywords.filter { it.keyword.isNotBlank() }.forEach { kw ->
            keywordDao.insertKeyword(
                BlockedKeyword(
                    keyword = kw.keyword,
                    keywordType = KeywordType.EXACT_MATCH.name,
                    caseSensitive = kw.caseSensitive,
                    isRegex = kw.isRegex,
                ),
            )
        }
        backup.nlpKeywords.filter { it.keyword.isNotBlank() }.forEach { kw ->
            keywordDao.insertKeyword(
                BlockedKeyword(keyword = kw.keyword, keywordType = KeywordType.NLP_SEMANTIC.name),
            )
        }
        backup.users.filter { it.userId.isNotBlank() }.forEach { u ->
            userDao.insertUser(
                BlockedUser(
                    userId = u.userId,
                    userName = u.userName,
                    urlToken = u.urlToken.takeIf { it.isNotBlank() },
                    avatarUrl = u.avatarUrl.takeIf { it.isNotBlank() },
                ),
            )
        }
        backup.topics.filter { it.topicId.isNotBlank() }.forEach { t ->
            topicDao.insertTopic(BlockedTopic(topicId = t.topicId, topicName = t.topicName))
        }

        return "关键词 ${backup.keywords.size} · NLP ${backup.nlpKeywords.size} · 用户 ${backup.users.size} · 主题 ${backup.topics.size}"
    }

    suspend fun getTopicName(topicId: String): String = topicDao.getTopicNameById(topicId)
}

/**
 * 屏蔽列表统计信息
 */
data class BlocklistStats(
    val keywordCount: Int,
    val userCount: Int,
    val topicCount: Int,
)
