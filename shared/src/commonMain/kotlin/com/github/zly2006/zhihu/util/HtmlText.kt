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

package com.github.zly2006.zhihu.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

fun parseEmphasizedHtmlText(
    html: String,
    emphasisColor: Color,
): AnnotatedString {
    // Assume that the input contains only text and valid, non-nested <em> tags.
    return buildAnnotatedString {
        var cursor = 0
        var emphasisStart: Int? = null
        while (cursor < html.length) {
            when {
                html.startsWith("<em>", cursor) -> {
                    emphasisStart = length
                    cursor += 4
                }
                html.startsWith("</em>", cursor) -> {
                    emphasisStart?.let { start ->
                        if (start < length) {
                            addStyle(SpanStyle(color = emphasisColor), start, length)
                        }
                    }
                    emphasisStart = null
                    cursor += 5
                }
                html[cursor] == '&' -> {
                    val entityEnd = html.indexOf(';', cursor + 1)
                    val entity = entityEnd.takeIf { it != -1 }?.let { html.substring(cursor + 1, it) }
                    val decoded =
                        when (entity) {
                            "lt" -> "<"
                            "gt" -> ">"
                            "quot" -> "\""
                            "amp" -> "&"
                            else -> {
                                val radix =
                                    when {
                                        entity?.startsWith("#x", ignoreCase = true) == true -> 16
                                        entity?.startsWith('#') == true -> 10
                                        else -> null
                                    }
                                val codePoint =
                                    radix?.let {
                                        entity
                                            ?.substring(if (it == 16) 2 else 1)
                                            ?.toIntOrNull(it)
                                    }
                                when {
                                    codePoint == null || codePoint !in 0..0x10FFFF || codePoint in 0xD800..0xDFFF -> null
                                    codePoint <= 0xFFFF -> codePoint.toChar().toString()
                                    else -> {
                                        val offset = codePoint - 0x10000
                                        String(
                                            charArrayOf(
                                                ((offset shr 10) + 0xD800).toChar(),
                                                ((offset and 0x3FF) + 0xDC00).toChar(),
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    append(decoded ?: "&")
                    cursor = if (decoded == null) cursor + 1 else entityEnd + 1
                }
                else -> {
                    append(html[cursor])
                    cursor++
                }
            }
        }
    }
}

/**
 * Composable function to parse HTML text with Material Theme primary color for emphasis.
 * This is a convenience function that uses the current theme's primary color.
 *
 * @param html The HTML string to parse
 * @return AnnotatedString with styled text using theme colors
 */
@Composable
fun parseEmphasizedHtmlTextWithTheme(html: String): AnnotatedString {
    val emphasisColor = MaterialTheme.colorScheme.primary
    return parseEmphasizedHtmlText(html, emphasisColor)
}
