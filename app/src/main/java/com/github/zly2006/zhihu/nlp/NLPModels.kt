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

/**
 * 关键词及其权重信息
 */
data class KeywordWithWeight(
    val keyword: String,
    val weight: Double,
)

// 汇总调试信息，便于断点查看
data class NlpDebugTrace(
    val normalizedText: String,
    val candidateKeywords: List<String> = emptyList(),
    val segments: List<SegmentDebugInfo> = emptyList(),
    val mmrIterations: MutableList<KeywordSelectionDebug> = mutableListOf(),
    val selectedKeywords: MutableList<KeywordWithWeight> = mutableListOf(),
)

data class SegmentDebugInfo(
    val index: Int,
    val text: String,
    val length: Int,
    val embeddingDim: Int?,
    val embeddingPreview: String,
)

data class KeywordSelectionDebug(
    val iteration: Int,
    val candidate: String,
    val relevance: Double,
    val redundancy: Double,
    val score: Double,
)

data class SegmentMatchDebug(
    val index: Int,
    val segmentTextPreview: String,
    val segmentLength: Int,
    val embeddingDim: Int?,
    val score: Double?,
)

data class PhraseMatchResult(
    val phrase: String,
    val normalizedPhrase: String,
    val keywords: List<String>,
    val phraseEmbeddingDim: Int?,
    val bestScore: Double,
    val segmentDebug: List<SegmentMatchDebug>,
)

sealed interface ModelState {
    data object Uninitialized : ModelState

    data object Loading : ModelState

    data class Downloading(
        val progress: Float,
    ) : ModelState

    data object Ready : ModelState

    data class Error(
        val message: String,
    ) : ModelState
}
