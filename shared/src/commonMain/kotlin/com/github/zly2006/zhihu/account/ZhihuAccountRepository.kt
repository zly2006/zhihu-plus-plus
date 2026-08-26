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

package com.github.zly2006.zhihu.account

import com.github.zly2006.zhihu.data.ZhihuJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

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
    val mobileAccessToken: String? = null,
    val mobileRefreshToken: String? = null,
    val mobileTokenType: String? = null,
    val mobileTokenExpiresAt: Long? = null,
)

@Serializable
data class ZhihuSavedAccount(
    val id: String,
    val session: ZhihuAccountSession,
)

@Serializable
data class ZhihuAccounts(
    val activeAccountId: String? = null,
    val accounts: List<ZhihuSavedAccount> = emptyList(),
) {
    val session: ZhihuAccountSession
        get() = accounts.firstOrNull { it.id == activeAccountId }?.session ?: guestZhihuAccountSession
}

private val guestZhihuAccountSession = ZhihuAccountSession()

interface ZhihuAccountSessionStore {
    fun readText(): String?

    fun writeText(text: String)

    fun delete()
}

class ZhihuAccountRepository(
    private val store: ZhihuAccountSessionStore,
    private val json: Json = ZhihuJson.json,
) {
    fun loadAccounts(): ZhihuAccounts = runCatching {
        val text = store.readText()?.takeIf { it.isNotBlank() } ?: return@runCatching ZhihuAccounts()
        val element = json.parseToJsonElement(text)
        if (element is JsonObject && "accounts" in element) {
            json.decodeFromString<ZhihuAccounts>(text)
        } else {
            val session = json.decodeFromString<ZhihuAccountSession>(text)
            if (session.login) {
                val id = session.accountSlotId()
                ZhihuAccounts(id, listOf(ZhihuSavedAccount(id, session)))
            } else {
                ZhihuAccounts()
            }
        }
    }.getOrDefault(ZhihuAccounts())

    fun load(): ZhihuAccountSession = loadAccounts().session

    fun save(session: ZhihuAccountSession) {
        if (session.login) {
            val id = session.accountSlotId()
            saveAccounts(ZhihuAccounts(id, listOf(ZhihuSavedAccount(id, session))))
        } else {
            clear()
        }
    }

    fun saveAccounts(accounts: ZhihuAccounts) {
        store.writeText(json.encodeToString(accounts))
    }

    fun clear() {
        store.delete()
    }
}

internal fun ZhihuAccountSession.accountIdentityKey(): String? = profile?.id?.takeIf(String::isNotBlank)
    ?: profile?.urlToken?.takeIf(String::isNotBlank)
    ?: username.takeIf(String::isNotBlank)

internal fun ZhihuAccountSession.accountSlotId(): String = accountIdentityKey() ?: "account"
