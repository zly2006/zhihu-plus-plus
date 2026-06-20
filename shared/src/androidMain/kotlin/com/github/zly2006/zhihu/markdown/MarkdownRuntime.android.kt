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
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.latex.rememberLatexFonts
import com.github.zly2006.zhihu.shared.data.toCookieHeaderString
import com.hrm.latex.renderer.font.MathFont

@Composable
actual fun rememberMarkdownRuntime(): MarkdownRuntime {
    val context = LocalContext.current
    val httpClient = AccountData.httpClient(context)
    val fontResult = rememberLatexFonts(context, httpClient)

    return object : MarkdownRuntime {
        override val mathFont: MathFont? = fontResult.downloaded?.mathFont
    }
}

@Composable
actual fun rememberMarkdownImageRequestHeaders(): MarkdownImageRequestHeaders {
    val userAgent = AccountData.data.userAgent
    return MarkdownImageRequestHeaders(
        cookieHeader = AccountData.data.cookies.toCookieHeaderString(),
        userAgent = userAgent,
    )
}
