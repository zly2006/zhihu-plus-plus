package com.github.zly2006.zhihu.viewmodel.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

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
