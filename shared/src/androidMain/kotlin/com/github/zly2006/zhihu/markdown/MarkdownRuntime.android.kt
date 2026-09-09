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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.github.zly2006.zhihu.account.androidZhihuAccountStore
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.toCookieHeaderString
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.rememberObservedSetting
import com.github.zly2006.zhihu.ui.subscreens.PREF_CUSTOM_CONTENT_FONT_NAME
import com.hrm.latex.renderer.font.MathFont
import java.io.File

@Composable
actual fun rememberMarkdownMathFont(): MathFont? {
    val context = LocalContext.current
    val httpClient = androidZhihuAccountStore(context).client.httpClient()
    return rememberLatexFonts(context, httpClient).downloaded?.mathFont
}

/** 用户选中的字体文件被复制到应用私有目录的这个固定位置（见 WebViewCustomFontSettings）。 */
internal const val CUSTOM_CONTENT_FONT_FILE_NAME = "custom_font"

@Composable
actual fun rememberMarkdownContentFont(): FontFamily? {
    val context = LocalContext.current
    val fontName by rememberObservedSetting(rememberSettingsStore(), PREF_CUSTOM_CONTENT_FONT_NAME) {
        getStringOrNull(PREF_CUSTOM_CONTENT_FONT_NAME)
    }
    return remember(context, fontName) {
        if (fontName == null) {
            null
        } else {
            File(context.filesDir, CUSTOM_CONTENT_FONT_FILE_NAME)
                .takeIf(File::exists)
                ?.let { file -> runCatching { FontFamily(Font(file)) }.getOrNull() }
        }
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
