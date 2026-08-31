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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.data.ZhihuPrivateMessage
import io.ktor.http.Url

internal fun ZhihuPrivateMessage.displayContent(
    linkColor: Color,
    onLinkClick: (String) -> Unit,
): AnnotatedString {
    val source = plugin?.excerpt?.takeIf(String::isNotBlank)
        ?: content.takeIf(String::isNotBlank)
        ?: return AnnotatedString("暂不支持显示这条消息")
    val document = Ksoup.parseBodyFragment(source)
    val text = document.text()

    return buildAnnotatedString {
        var cursor = 0
        var anchorSearchStart = 0
        document.select("a[href]").forEach { anchor ->
            val linkText = anchor.text()
            val start = text.indexOf(linkText, anchorSearchStart).takeIf { it >= 0 }
                ?: return@forEach
            val end = start + linkText.length
            anchorSearchStart = end
            val url = anchor.attr("href").safeHttpUrl() ?: return@forEach
            if (linkText.isEmpty() || start < cursor) return@forEach
            append(text.substring(cursor, start))
            withLink(
                LinkAnnotation.Clickable(
                    tag = url,
                    styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                ) { onLinkClick(url) },
            ) {
                append(text.substring(start, end))
            }
            cursor = end
        }
        append(text.substring(cursor))
    }
}

private fun String.safeHttpUrl(): String? = runCatching { Url(trim()) }
    .getOrNull()
    ?.takeIf { it.protocol.name == "http" || it.protocol.name == "https" }
    ?.takeIf { it.host.isNotBlank() }
    ?.toString()
