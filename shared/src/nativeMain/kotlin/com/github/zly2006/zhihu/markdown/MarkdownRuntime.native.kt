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
import com.github.zly2006.zhihu.shared.account.IosAccountStore
import com.github.zly2006.zhihu.shared.data.toCookieHeaderString
import com.hrm.latex.renderer.font.MathFont

@Composable
actual fun rememberMarkdownRuntime(): MarkdownRuntime = remember {
    object : MarkdownRuntime {
        override val mathFont: MathFont? = null

        override suspend fun saveMarkdownImage(url: String) = Unit // TODO: iOS 保存Markdown图片

        override suspend fun shareMarkdownImage(url: String) = Unit // TODO: iOS 分享Markdown图片
    }
} // TODO: iOS Markdown 运行时完整实现

@Composable
actual fun rememberMarkdownImageRequestHeaders(): MarkdownImageRequestHeaders {
    val store = remember { IosAccountStore() }
    val session = remember(store) { store.load() }
    return MarkdownImageRequestHeaders(
        cookieHeader = session.cookies.toCookieHeaderString(),
        userAgent = session.userAgent,
    )
}
