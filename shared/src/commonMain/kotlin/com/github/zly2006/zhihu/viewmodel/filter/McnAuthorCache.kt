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
import com.github.zly2006.zhihu.shared.data.OfficialBadge
import kotlin.time.Clock

@Entity(tableName = McnAuthorCache.TABLE_NAME)
data class McnAuthorCache(
    @PrimaryKey val urlToken: String,
    val userName: String? = null,
    val mcnCompany: String? = null,
    val badgeTitle: String? = null,
    val badgeDescription: String? = null,
    val badgeIconUrl: String? = null,
    val badgeNightIconUrl: String? = null,
    val checkedTime: Long = Clock.System.now().toEpochMilliseconds(),
) {
    private val hasPositiveProfile: Boolean
        get() = !mcnCompany.isNullOrBlank() || !badgeIconUrl.isNullOrBlank()

    fun isExpired(nowMillis: Long = Clock.System.now().toEpochMilliseconds()): Boolean {
        val ttlMillis = if (hasPositiveProfile) POSITIVE_MCN_CACHE_TTL_MILLIS else NEGATIVE_MCN_CACHE_TTL_MILLIS
        return checkedTime <= 0L || nowMillis - checkedTime >= ttlMillis
    }

    val officialBadge: OfficialBadge?
        get() {
            val icon = badgeIconUrl?.takeIf { it.isNotBlank() } ?: return null
            return OfficialBadge(
                title = badgeTitle.orEmpty(),
                description = badgeDescription.orEmpty(),
                iconUrl = icon,
                nightIconUrl = badgeNightIconUrl.orEmpty(),
            )
        }

    companion object {
        const val TABLE_NAME = "mcn_author_cache"
    }
}

internal const val POSITIVE_MCN_CACHE_TTL_MILLIS: Long = 30L * 24 * 60 * 60 * 1000
internal const val NEGATIVE_MCN_CACHE_TTL_MILLIS: Long = 24L * 60 * 60 * 1000
