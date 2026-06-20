/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.ZhihuVotersResponse

suspend fun loadVotersPage(
    environment: ZhihuApiEnvironment,
    initialUrl: String,
    nextUrl: String?,
    reset: Boolean,
): ZhihuVotersResponse {
    val url = (if (reset || nextUrl == null) initialUrl else nextUrl).replace("http://", "https://")
    val response = environment.fetchJson(url, "") ?: error("赞同者信息为空")
    return ZhihuJson.decodeJson(response)
}

fun MutableList<DataHolder.Author>.replaceOrAppendUniqueVoters(
    voters: List<DataHolder.Author>,
    reset: Boolean,
) {
    if (reset) {
        clear()
    }
    val existingIds = mapTo(mutableSetOf()) { it.id }
    addAll(voters.filter { existingIds.add(it.id) })
}

fun ZhihuVotersResponse.nextUrlOrNull(): String? =
    paging.next
        .takeUnless { paging.isEnd || it.isBlank() }
        ?.replace("http://", "https://")
