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

package com.github.zly2006.zhihu.shared.account

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

const val DEFAULT_ZHIHU_USER_AGENT =
    "Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/540.0 (KHTML, like Gecko) Ubuntu/10.10 Chrome/9.1.0.0 Safari/540.0"

@Serializable
data class ZhihuAccountProfileSnapshot(
    val id: String = "",
    val name: String = "",
    val urlToken: String? = null,
    val userType: String = "",
    val avatarUrl: String? = null,
)

@Serializable
data class ZhihuAccountSession(
    val login: Boolean = false,
    val username: String = "",
    val cookies: MutableMap<String, String> = mutableMapOf(),
    val userAgent: String = DEFAULT_ZHIHU_USER_AGENT,
    val profile: ZhihuAccountProfileSnapshot? = null,
    val self: JsonElement? = null,
)

interface ZhihuAccountSessionStore {
    fun readText(): String?

    fun writeText(text: String)

    fun delete()
}

class ZhihuAccountRepository(
    private val store: ZhihuAccountSessionStore,
    private val json: Json = ZhihuJson.json,
) {
    fun load(): ZhihuAccountSession = runCatching {
        store
            .readText()
            ?.takeIf { it.isNotBlank() }
            ?.let { json.decodeFromString<ZhihuAccountSession>(it) }
            ?: ZhihuAccountSession()
    }.getOrDefault(ZhihuAccountSession())

    fun save(session: ZhihuAccountSession) {
        store.writeText(json.encodeToString(session))
    }

    fun clear() {
        store.delete()
    }
}
