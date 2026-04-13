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
 * 关键词类型枚举
 */
enum class KeywordType {
    EXACT_MATCH, // 精确匹配（传统关键词）
    NLP_SEMANTIC, // NLP语义匹配（主题/短语）
}

/**
 * 屏蔽关键词实体
 * 用于屏蔽包含特定关键词的内容
 */
@Entity(tableName = BlockedKeyword.TABLE_NAME)
data class BlockedKeyword(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String, // 关键词内容（NLP模式下为空格分隔的短语）
    val keywordType: String = KeywordType.EXACT_MATCH.name, // 关键词类型
    val caseSensitive: Boolean = false, // 是否区分大小写（仅精确匹配模式）
    val isRegex: Boolean = false, // 是否为正则表达式（仅精确匹配模式）
    val createdTime: Long = System.currentTimeMillis(), // 创建时间
) {
    companion object {
        const val TABLE_NAME = "blocked_keywords"
    }

    // 辅助方法：获取关键词类型枚举
    fun getKeywordTypeEnum(): KeywordType = try {
        KeywordType.valueOf(keywordType)
    } catch (e: Exception) {
        KeywordType.EXACT_MATCH
    }

    // 辅助方法：获取NLP短语的各个关键词
    fun getNLPKeywords(): List<String> = if (getKeywordTypeEnum() == KeywordType.NLP_SEMANTIC) {
        keyword.split("\\s+".toRegex()).filter { it.isNotBlank() }
    } else {
        emptyList()
    }
}
