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

package com.github.zly2006.zhihu.data

import android.annotation.SuppressLint
import android.content.Context
import com.github.zly2006.zhihu.account.DEFAULT_ZHIHU_USER_AGENT
import com.github.zly2006.zhihu.account.androidZhihuAccountStore
import com.github.zly2006.zhihu.account.currentAndroidZhihuAccountStore
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.data.ZhihuJson

@SuppressLint("StaticFieldLeak")
object AccountData {
    data class Data(
        val login: Boolean = false,
        val username: String = "",
        val cookies: Map<String, String> = emptyMap(),
        val userAgent: String = DEFAULT_ZHIHU_USER_AGENT,
        val self: Person? = null,
    )

    fun loadData(context: Context): Data {
        androidZhihuAccountStore(context)
        return data
    }

    val data: Data
        get() = currentAndroidZhihuAccountStore().session.run {
            Data(
                login = login,
                username = username,
                cookies = cookies.toMap(),
                userAgent = userAgent,
                self = self?.let {
                    runCatching {
                        ZhihuJson.decodeJson<Person>(it)
                    }.getOrNull()
                },
            )
        }
}
