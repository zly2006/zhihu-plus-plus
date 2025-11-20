package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 屏蔽关键词实体
 * 用于屏蔽包含特定关键词的内容
 */
@Entity(tableName = BlockedKeyword.TABLE_NAME)
data class BlockedKeyword(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String, // 关键词内容
    val caseSensitive: Boolean = false, // 是否区分大小写
    val isRegex: Boolean = false, // 是否为正则表达式
    val createdTime: Long = System.currentTimeMillis(), // 创建时间
) {
    companion object {
        const val TABLE_NAME = "blocked_keywords"
    }
}
