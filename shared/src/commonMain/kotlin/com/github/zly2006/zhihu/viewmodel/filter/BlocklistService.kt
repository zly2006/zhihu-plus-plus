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

import com.github.zly2006.zhihu.shared.util.Log

/**
 * 屏蔽列表核心服务。
 * 只负责关键词、用户、主题和 MCN 屏蔽的本地数据语义，不处理平台上下文、URI 或文件路径。
 */
class BlocklistService(
    private val keywordDao: BlockedKeywordDao,
    private val userDao: BlockedUserDao,
    private val topicDao: BlockedTopicDao,
    private val mcnOrganizationDao: BlockedMcnOrganizationDao? = null,
    private val mcnAuthorCacheDao: McnAuthorCacheDao? = null,
) {
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
                    blockedKeyword.caseSensitive -> text.contains(blockedKeyword.keyword)
                    else -> text.contains(blockedKeyword.keyword, ignoreCase = true)
                }
            } catch (e: Exception) {
                Log.e("BlocklistService", "Failed to match blocked keyword", e)
                false
            }
        }
    }

    suspend fun isUserBlocked(userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        return userDao.isUserBlocked(userId)
    }

    suspend fun countBlockedTopics(topicIds: List<String>?): Int {
        if (topicIds.isNullOrEmpty()) return 0
        return topicDao.getBlockedTopicIds(topicIds).size
    }

    suspend fun isTopicBlocked(topicId: String?): Boolean {
        if (topicId.isNullOrBlank()) return false
        return topicDao.isTopicBlocked(topicId)
    }

    suspend fun getTopicName(topicId: String): String = topicDao.getTopicNameById(topicId)

    suspend fun addBlockedMcnOrganization(organizationName: String) {
        val normalized = organizationName.normalizeMcnCompany() ?: return
        mcnOrganizationDao?.insertOrganization(BlockedMcnOrganization(normalized))
    }

    suspend fun removeBlockedMcnOrganization(organizationName: String) {
        mcnOrganizationDao?.deleteOrganizationByName(organizationName)
    }

    suspend fun getAllBlockedMcnOrganizations(): List<BlockedMcnOrganization> =
        mcnOrganizationDao?.getAllOrganizations().orEmpty()

    suspend fun clearAllBlockedMcnOrganizations() {
        mcnOrganizationDao?.clearAllOrganizations()
    }

    suspend fun hasBlockedMcnOrganizations(): Boolean = mcnOrganizationDao?.hasOrganizations() == true

    suspend fun isMcnOrganizationBlocked(organizationName: String?): Boolean {
        val normalized = organizationName.normalizeMcnCompany() ?: return false
        return getAllBlockedMcnOrganizations().any { blocked ->
            blocked.organizationName.matchesMcnOrganization(normalized)
        }
    }

    suspend fun getCachedMcnAuthor(urlToken: String): McnAuthorCache? = mcnAuthorCacheDao
        ?.getByUrlToken(urlToken)
        ?.takeUnless { it.isExpired() }

    suspend fun cacheMcnAuthorProfile(
        urlToken: String,
        userName: String?,
        profile: McnAuthorProfile,
    ) {
        val badge = profile.officialBadge
        mcnAuthorCacheDao?.insert(
            McnAuthorCache(
                urlToken = urlToken,
                userName = userName,
                mcnCompany = profile.mcnCompany.normalizeMcnCompany(),
                badgeTitle = badge?.title,
                badgeDescription = badge?.description,
                badgeIconUrl = badge?.iconUrl,
                badgeNightIconUrl = badge?.nightIconUrl,
            ),
        )
    }
}

fun ContentFilterDatabase.createBlocklistService(): BlocklistService = BlocklistService(
    keywordDao = blockedKeywordDao(),
    userDao = blockedUserDao(),
    topicDao = blockedTopicDao(),
    mcnOrganizationDao = blockedMcnOrganizationDao(),
    mcnAuthorCacheDao = mcnAuthorCacheDao(),
)
