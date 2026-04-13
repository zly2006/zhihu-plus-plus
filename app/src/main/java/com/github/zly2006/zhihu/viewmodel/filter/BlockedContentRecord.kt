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

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 被屏蔽内容记录实体
 * 记录被NLP语义匹配屏蔽的内容，供用户复查
 */
@Entity(tableName = BlockedContentRecord.TABLE_NAME)
data class BlockedContentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentId: String, // 内容ID
    val contentType: String, // 内容类型（answer/article/question等）
    val title: String, // 内容标题
    val excerpt: String?, // 内容摘要
    val authorName: String?, // 作者名称
    val authorId: String?, // 作者ID
    val blockedTime: Long = System.currentTimeMillis(), // 屏蔽时间
    val blockReason: String, // 屏蔽原因JSON（包含匹配的关键词和相似度）
    val matchedKeywords: String, // 匹配的关键词列表JSON格式：[{"keyword":"xxx","similarity":0.8}]
) {
    companion object {
        const val TABLE_NAME = "blocked_content_records"
        const val MAX_RECORDS = 100 // 最多保留100条记录
    }
}

/**
 * 匹配关键词信息
 */
@kotlinx.serialization.Serializable
data class MatchedKeywordInfo(
    val keyword: String,
    val similarity: Double,
)
