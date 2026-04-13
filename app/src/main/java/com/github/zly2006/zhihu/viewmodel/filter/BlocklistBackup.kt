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
