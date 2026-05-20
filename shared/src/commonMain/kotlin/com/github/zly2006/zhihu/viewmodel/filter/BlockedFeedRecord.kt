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
 * Feed 屏蔽记录实体。
 * 记录的是某条 feed 卡片为何没有进入首页展示结果，包含广告、已读、关键词、作者、主题、NLP 等 feed 阶段原因。
 */
@Entity(tableName = BlockedFeedRecord.TABLE_NAME)
data class BlockedFeedRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val questionId: Long?, // 回答所属问题 ID，文章/想法等则为 null
    val authorName: String?,
    val authorId: String?,
    val url: String?,
    val content: String?, // 文本正文（可能为 HTML）
    val blockedReason: String, // 人类可读屏蔽原因
    val navDestinationJson: String?, // NavDestination JSON 序列化
    val feedJson: String?, // Feed JSON 序列化
    val blockedTime: Long = currentEpochMillis(),
) {
    companion object {
        const val TABLE_NAME = "blocked_feed_records"
        const val MAX_RECORDS = 500
    }
}

private fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
