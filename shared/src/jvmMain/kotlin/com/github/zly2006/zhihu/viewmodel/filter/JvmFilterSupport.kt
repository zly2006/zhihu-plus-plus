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

package com.github.zly2006.zhihu.viewmodel.filter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.room.Room
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDataFile
import java.io.File

internal val desktopKeywordSemanticMatcher = KeywordSemanticMatcher { text, blockedPhrases, threshold ->
    val normalizedText = text.lowercase()
    val textTokens = extractDesktopSemanticTokens(normalizedText).toSet()
    blockedPhrases.mapNotNull { phrase ->
        val normalizedPhrase = phrase.lowercase().trim()
        val similarity = when {
            normalizedPhrase.isBlank() -> 0.0
            normalizedText.contains(normalizedPhrase) -> 1.0
            else -> {
                val phraseTokens = extractDesktopSemanticTokens(normalizedPhrase)
                if (phraseTokens.isEmpty()) {
                    0.0
                } else {
                    phraseTokens.count { it in textTokens }.toDouble() / phraseTokens.size.toDouble()
                }
            }
        }
        if (similarity >= threshold) phrase to similarity else null
    }
}

private fun extractDesktopSemanticTokens(text: String): List<String> =
    Regex("[\\p{L}\\p{N}_\\u4e00-\\u9fff]{2,}")
        .findAll(text)
        .map { it.value.trim() }
        .filter { it.length >= 2 }
        .toList()

fun desktopContentFilterDatabaseFile(): File =
    desktopZhihuDataFile("content-filter.db")

@Composable
actual fun getContentFilterDatabase(): ContentFilterDatabase = remember {
    getContentFilterDatabase(desktopContentFilterDatabaseFile().also { it.parentFile?.mkdirs() })
}

fun getContentFilterDatabase(databaseFile: File): ContentFilterDatabase =
    buildContentFilterDatabase(
        Room.databaseBuilder<ContentFilterDatabase>(
            name = databaseFile.absolutePath,
        ),
    )
