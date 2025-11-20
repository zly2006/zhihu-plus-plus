package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 屏蔽用户实体
 * 用于屏蔽特定用户发布的内容
 */
@Entity(tableName = BlockedUser.TABLE_NAME)
data class BlockedUser(
    @PrimaryKey val userId: String, // 用户ID
    val userName: String, // 用户名（用于显示）
    val urlToken: String? = null, // 用户URL Token
    val avatarUrl: String? = null, // 用户头像URL
    val createdTime: Long = System.currentTimeMillis(), // 创建时间
) {
    companion object {
        const val TABLE_NAME = "blocked_users"
    }
}
