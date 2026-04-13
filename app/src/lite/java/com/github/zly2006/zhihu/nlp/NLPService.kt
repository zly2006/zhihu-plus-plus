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

package com.github.zly2006.zhihu.nlp

object NLPService {
    suspend fun extractKeywords(text: String, topN: Int = 5): List<String> = emptyList()

    suspend fun extractKeywordsWithWeight(text: String, topN: Int = 5): List<KeywordWithWeight> = emptyList()

    suspend fun checkBlockedPhrases(text: String, blockedPhrases: List<String>, threshold: Double = 0.8): List<Pair<String, Double>> = emptyList()

    suspend fun extractSummary(text: String, maxLength: Int = 100): String = text.take(maxLength)
}
