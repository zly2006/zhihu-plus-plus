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
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = ContentOpenEvent.TABLE_NAME,
    indices = [
        Index(value = ["contentType", "contentId"]),
        Index(value = ["openedAt"]),
    ],
)
data class ContentOpenEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contentType: String,
    val contentId: String,
    val questionId: Long? = null,
    val openFrom: String,
    val openedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val TABLE_NAME = "content_open_events"
    }
}
