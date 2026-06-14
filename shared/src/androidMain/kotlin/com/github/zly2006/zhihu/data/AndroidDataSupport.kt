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

package com.github.zly2006.zhihu.data

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.util.signFetchRequest
import kotlinx.coroutines.CancellationException

suspend fun DataHolder.getContentDetail(
    context: Context,
    destination: NavDestination,
): DataHolder.Content? = fetchAndroidContentDetail(context, destination)

suspend fun ContentDetailCache.getOrFetch(
    context: Context,
    navDestination: NavDestination,
): DataHolder.Content? = getOrFetch(navDestination) { destination ->
    fetchAndroidContentDetail(context, destination)
}

private suspend fun fetchAndroidContentDetail(
    context: Context,
    destination: NavDestination,
): DataHolder.Content? =
    runCatching {
        fetchZhihuContentDetail(destination) { url ->
            AccountData.fetchGet(context, url) {
                signFetchRequest()
            }
        }
    }.getOrElse { e ->
        if (e !is CancellationException) {
            Log.e("getContentDetail", "Failed to fetch content detail for $destination", e)
        }
        null
    }

actual fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyPlatformDriver(): RoomDatabase.Builder<T> = this
