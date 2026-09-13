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

package com.github.zly2006.zhihu.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.hrm.latex.renderer.font.MathFont

@Composable
expect fun rememberMarkdownMathFont(): MathFont?

/**
 * 用户在"正文自定义字体"里选择的字体（偏好键
 * [com.github.zly2006.zhihu.ui.subscreens.PREF_CUSTOM_CONTENT_FONT_NAME]）。
 *
 * 返回 null 表示未设置、字体文件已失效，或该平台不支持自选字体文件；此时正文沿用平台默认字体。
 */
@Composable
expect fun rememberMarkdownContentFont(): FontFamily?

@Composable
fun rememberMarkdownImageModel(url: String): Any {
    val context = LocalPlatformContext.current
    val headerData = rememberMarkdownImageRequestHeaders()
    val headers = remember(headerData.cookieHeader, headerData.userAgent) {
        NetworkHeaders
            .Builder()
            .set("Cookie", headerData.cookieHeader)
            .set("User-Agent", headerData.userAgent)
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

data class MarkdownImageRequestHeaders(
    val cookieHeader: String,
    val userAgent: String,
)

@Composable
expect fun rememberMarkdownImageRequestHeaders(): MarkdownImageRequestHeaders
