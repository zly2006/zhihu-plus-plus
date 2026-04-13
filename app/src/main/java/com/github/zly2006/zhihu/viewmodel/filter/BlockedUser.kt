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
