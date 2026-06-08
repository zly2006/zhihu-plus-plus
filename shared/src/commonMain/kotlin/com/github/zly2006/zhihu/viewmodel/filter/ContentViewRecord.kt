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
import kotlin.time.Clock

/**
 * 内容曝光记录实体。
 * 记录某个内容身份在 feed 中被展示/交互的次数，用于“重复曝光但一直不点开”的 feed 过滤规则。
 */
@Entity(tableName = ContentViewRecord.TABLE_NAME)
data class ContentViewRecord(
    @PrimaryKey val id: String, // 使用 targetType:targetId 作为主键
    val targetType: String, // answer, article, etc.
    val targetId: String, // 目标内容ID
    val viewCount: Int = 0, // 展示次数
    val hasInteraction: Boolean = false, // 是否有过交互（点击、点赞等）
    val firstViewTime: Long = currentEpochMillis(), // 首次展示时间
    val lastViewTime: Long = currentEpochMillis(), // 最后展示时间
    val lastInteractionTime: Long? = null, // 最后交互时间
) {
    companion object {
        const val TABLE_NAME = "content_view_records"
        const val MAX_VIEW_COUNT_WITHOUT_INTERACTION = 2 // 展示次数大于此值且无交互时，后续 feed 过滤会拦截

        // 生成主键的辅助方法
        fun generateId(targetType: String, targetId: String): String = "$targetType:$targetId"
    }
}

private fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
