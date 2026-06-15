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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@Serializable
data class BlocklistBackup(
    val version: Int = 2,
    val exportTime: Long = Clock.System.now().toEpochMilliseconds(),
    val keywords: List<KeywordBackup> = emptyList(),
    val nlpKeywords: List<NlpKeywordBackup> = emptyList(),
    val users: List<UserBackup> = emptyList(),
    val topics: List<TopicBackup> = emptyList(),
    val mcnOrganizations: List<McnOrganizationBackup> = emptyList(),
)

@Serializable
data class KeywordBackup(
    val keyword: String,
    val caseSensitive: Boolean = false,
    val isRegex: Boolean = false,
)

@Serializable
data class NlpKeywordBackup(
    val keyword: String,
)

@Serializable
data class UserBackup(
    val userId: String,
    val userName: String,
    val urlToken: String = "",
    val avatarUrl: String = "",
)

@Serializable
data class TopicBackup(
    val topicId: String,
    val topicName: String,
)

@Serializable
data class McnOrganizationBackup(
    val organizationName: String,
)

data class BlocklistStats(
    val keywordCount: Int,
    val userCount: Int,
    val topicCount: Int,
    val mcnOrganizationCount: Int = 0,
)

private val blocklistBackupJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

suspend fun encodeBlocklistBackup(
    keywordDao: BlockedKeywordDao,
    userDao: BlockedUserDao,
    topicDao: BlockedTopicDao,
    mcnOrganizationDao: BlockedMcnOrganizationDao? = null,
): String {
    val allKeywords = keywordDao.getAllKeywords()
    val users = userDao.getAllUsers()
    val topics = topicDao.getAllTopics()
    val mcnOrganizations = mcnOrganizationDao?.getAllOrganizations().orEmpty()

    val backup = BlocklistBackup(
        keywords = allKeywords
            .filter { it.getKeywordTypeEnum() == KeywordType.EXACT_MATCH }
            .map { KeywordBackup(it.keyword, it.caseSensitive, it.isRegex) },
        nlpKeywords = allKeywords
            .filter { it.getKeywordTypeEnum() == KeywordType.NLP_SEMANTIC }
            .map { NlpKeywordBackup(it.keyword) },
        users = users.map { UserBackup(it.userId, it.userName, it.urlToken ?: "", it.avatarUrl ?: "") },
        topics = topics.map { TopicBackup(it.topicId, it.topicName) },
        mcnOrganizations = mcnOrganizations.map { McnOrganizationBackup(it.organizationName) },
    )

    return blocklistBackupJson.encodeToString(BlocklistBackup.serializer(), backup)
}

suspend fun importBlocklistBackupFromJsonText(
    keywordDao: BlockedKeywordDao,
    userDao: BlockedUserDao,
    topicDao: BlockedTopicDao,
    mcnOrganizationDao: BlockedMcnOrganizationDao? = null,
    text: String,
): String {
    val backup = blocklistBackupJson.decodeFromString(BlocklistBackup.serializer(), text)

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
    backup.users.filter { it.userId.isNotBlank() }.forEach { user ->
        userDao.insertUser(
            BlockedUser(
                userId = user.userId,
                userName = user.userName,
                urlToken = user.urlToken.takeIf { it.isNotBlank() },
                avatarUrl = user.avatarUrl.takeIf { it.isNotBlank() },
            ),
        )
    }
    backup.topics.filter { it.topicId.isNotBlank() }.forEach { topic ->
        topicDao.insertTopic(BlockedTopic(topicId = topic.topicId, topicName = topic.topicName))
    }
    backup.mcnOrganizations
        .mapNotNull { it.organizationName.normalizeMcnCompany() }
        .forEach { organizationName ->
            mcnOrganizationDao?.insertOrganization(BlockedMcnOrganization(organizationName))
        }

    return "关键词 ${backup.keywords.size} · NLP ${backup.nlpKeywords.size} · 用户 ${backup.users.size} · 主题 ${backup.topics.size} · MCN ${backup.mcnOrganizations.size}"
}
