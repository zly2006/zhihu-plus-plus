package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 屏蔽列表管理器
 * 负责管理屏蔽关键词和屏蔽用户
 */
class BlocklistManager private constructor(
    context: Context,
) {
    private val database = ContentFilterDatabase.getDatabase(context)
    private val keywordDao = database.blockedKeywordDao()
    private val userDao = database.blockedUserDao()

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

    // ==================== 关键词屏蔽 ====================

    /**
     * 添加屏蔽关键词
     */
    suspend fun addBlockedKeyword(
        keyword: String,
        caseSensitive: Boolean = false,
        isRegex: Boolean = false,
    ): Long = withContext(Dispatchers.IO) {
        val blockedKeyword = BlockedKeyword(
            keyword = keyword.trim(),
            caseSensitive = caseSensitive,
            isRegex = isRegex,
        )
        keywordDao.insertKeyword(blockedKeyword)
    }

    /**
     * 删除屏蔽关键词
     */
    suspend fun removeBlockedKeyword(keywordId: Long) {
        withContext(Dispatchers.IO) {
            keywordDao.deleteKeywordById(keywordId)
        }
    }

    /**
     * 获取所有屏蔽关键词
     */
    suspend fun getAllBlockedKeywords(): List<BlockedKeyword> = withContext(Dispatchers.IO) {
        keywordDao.getAllKeywords()
    }

    /**
     * 清空所有屏蔽关键词
     */
    suspend fun clearAllBlockedKeywords() {
        withContext(Dispatchers.IO) {
            keywordDao.clearAllKeywords()
        }
    }

    /**
     * 检查文本是否包含屏蔽关键词
     */
    suspend fun containsBlockedKeyword(text: String?): Boolean = withContext(Dispatchers.IO) {
        if (text.isNullOrBlank()) return@withContext false

        val keywords = keywordDao.getAllKeywords()
        keywords.any { blockedKeyword ->
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

    // ==================== 用户屏蔽 ====================

    /**
     * 添加屏蔽用户
     */
    suspend fun addBlockedUser(
        userId: String,
        userName: String,
        urlToken: String? = null,
        avatarUrl: String? = null,
    ) {
        withContext(Dispatchers.IO) {
            val blockedUser = BlockedUser(
                userId = userId,
                userName = userName,
                urlToken = urlToken,
                avatarUrl = avatarUrl,
            )
            userDao.insertUser(blockedUser)
        }
    }

    /**
     * 删除屏蔽用户
     */
    suspend fun removeBlockedUser(userId: String) {
        withContext(Dispatchers.IO) {
            userDao.deleteUserById(userId)
        }
    }

    /**
     * 获取所有屏蔽用户
     */
    suspend fun getAllBlockedUsers(): List<BlockedUser> = withContext(Dispatchers.IO) {
        userDao.getAllUsers()
    }

    /**
     * 清空所有屏蔽用户
     */
    suspend fun clearAllBlockedUsers() {
        withContext(Dispatchers.IO) {
            userDao.clearAllUsers()
        }
    }

    /**
     * 检查用户是否被屏蔽
     */
    suspend fun isUserBlocked(userId: String?): Boolean = withContext(Dispatchers.IO) {
        if (userId.isNullOrBlank()) return@withContext false
        userDao.isUserBlocked(userId)
    }

    /**
     * 获取屏蔽统计信息
     */
    suspend fun getBlocklistStats(): BlocklistStats = withContext(Dispatchers.IO) {
        BlocklistStats(
            keywordCount = keywordDao.getKeywordCount(),
            userCount = userDao.getUserCount(),
        )
    }

    /**
     * 清空所有屏蔽数据
     */
    suspend fun clearAllBlocklists() {
        withContext(Dispatchers.IO) {
            keywordDao.clearAllKeywords()
            userDao.clearAllUsers()
        }
    }
}

/**
 * 屏蔽列表统计信息
 */
data class BlocklistStats(
    val keywordCount: Int,
    val userCount: Int,
)
