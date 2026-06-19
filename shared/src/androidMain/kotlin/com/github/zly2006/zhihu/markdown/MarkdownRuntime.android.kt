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

package com.github.zly2006.zhihu.markdown

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.latex.rememberLatexFonts
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.util.shareImage
import com.hrm.latex.renderer.font.MathFont

@Composable
actual fun rememberMarkdownRuntime(): MarkdownRuntime {
    val context = LocalContext.current
    val httpClient = AccountData.httpClient(context)
    val fontResult = rememberLatexFonts(context, httpClient)

    return object : MarkdownRuntime {
        override val mathFont: MathFont? = fontResult.downloaded?.mathFont

        override suspend fun saveMarkdownImage(url: String) = saveImageToGallery(context, httpClient, url)

        override suspend fun shareMarkdownImage(url: String) = shareImage(context, httpClient, url)
    }
}

// 在请求类似 https://pic-private.zhihu.com/的路径的时候，需要带上cookie
@Composable
actual fun rememberMarkdownImageModel(url: String): Any {
    val context = LocalContext.current
    val host = remember(url) { Uri.parse(url).host.orEmpty() }

    val cookies = AccountData.data.cookies
        .mapNotNull { (name, value) ->
            value.takeIf { it.isNotBlank() }?.let { "$name=$it" }
        }.joinToString("; ")
    val userAgent = AccountData.data.userAgent
    val headers = remember(cookies, userAgent) {
        NetworkHeaders
            .Builder()
            .set("Cookie", cookies)
            .set("User-Agent", userAgent)
            .build()
    }
    return remember(context, url, headers) {
        ImageRequest
            .Builder(context)
            .data(url)
            .httpHeaders(headers)
            .build()
    }
}
