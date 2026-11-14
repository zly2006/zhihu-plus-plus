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

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import kotlinx.serialization.Serializable

fun Map<String, String>.toCookieHeaderString(): String =
    entries
        .asSequence()
        .mapNotNull { (name, value) ->
            value.takeIf { it.isNotBlank() }?.let { "$name=$it" }
        }.joinToString("; ")

class ZhihuCookieStorage(
    private val cookies: MutableMap<String, String>,
    private val onCookieChanged: () -> Unit = {},
) : CookiesStorage {
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        // https://github.com/zly2006/zhihu-plus-plus/issues/25#issuecomment-3311926550
        if (cookie.name == "z_c0" && cookie.value.isBlank()) {
            return
        }
        if (cookie.domain?.endsWith("zhihu.com") != false) {
            cookies[cookie.name] = cookie.value
            onCookieChanged()
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = cookies.map {
        Cookie(it.key, it.value, CookieEncoding.RAW, domain = "www.zhihu.com")
    }

    override fun close() = Unit
}

@Serializable
data class ZhihuPaging(
    val page: Int = -1,
    val isEnd: Boolean = false,
    val isStart: Boolean = false,
    val previous: String? = null,
    val totals: Int = 0,
    val next: String,
    val prev: String? = null,
)

@Serializable
data class ZhihuVotersResponse(
    val paging: ZhihuPaging,
    val data: List<DataHolder.Author> = emptyList(),
)

internal val ZHIHU_MOBILE_HEADERS = mapOf(
    "x-api-version" to "3.1.8",
    "x-app-version" to "10.61.0",
    "x-app-za" to
        "OS=Android&Release=12&Model=sdk_gphone64_arm64&VersionName=10.61.0&VersionCode=26107&Product=com.zhihu.android&Width=1440&Height=2952&Installer=%E7%81%B0%E5%BA%A6&DeviceType=AndroidPhone&Brand=google",
)

const val ZHIHU_ANDROID_USER_AGENT = "com.zhihu.android/Futureve/10.61.0 Mozilla/5.0 (Linux; Android 12; sdk_gphone64_arm64 " +
    "Build/SE1A.220630.001.A1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.1000.10 Mobile Safari/537.36"

internal fun HttpClientConfig<*>.installZhihuMobileClientProfile() {
    install(UserAgent) { agent = ZHIHU_ANDROID_USER_AGENT }
    defaultRequest {
        ZHIHU_MOBILE_HEADERS.forEach { (name, value) ->
            if (!headers.contains(name)) headers.append(name, value)
        }
    }
}
