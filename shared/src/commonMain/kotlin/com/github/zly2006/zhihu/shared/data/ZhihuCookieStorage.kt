package com.github.zly2006.zhihu.shared.data

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url

class ZhihuCookieStorage(
    private val cookies: MutableMap<String, String>,
    private val onCookieChanged: (() -> Unit)? = null,
) : CookiesStorage {
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        // https://github.com/zly2006/zhihu-plus-plus/issues/25#issuecomment-3311926550
        if (cookie.name == "z_c0" && cookie.value.isBlank()) {
            return
        }
        if (cookie.domain?.endsWith("zhihu.com") != false) {
            cookies[cookie.name] = cookie.value
            onCookieChanged?.invoke()
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = cookies.map {
        Cookie(it.key, it.value, CookieEncoding.RAW, domain = "www.zhihu.com")
    }

    override fun close() {
    }
}
