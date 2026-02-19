package com.github.zly2006.zhihu.viewmodel.filter

import kotlinx.serialization.Serializable

@Serializable
data class BlocklistBackup(
    val version: Int = 2,
    val exportTime: Long = System.currentTimeMillis(),
    val keywords: List<KeywordBackup> = emptyList(),
    val nlpKeywords: List<NlpKeywordBackup> = emptyList(),
    val users: List<UserBackup> = emptyList(),
    val topics: List<TopicBackup> = emptyList(),
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
