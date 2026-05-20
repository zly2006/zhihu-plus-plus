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

typealias KeywordWithWeight = com.github.zly2006.zhihu.shared.nlp.KeywordWithWeight
typealias NlpDebugTrace = com.github.zly2006.zhihu.shared.nlp.NlpDebugTrace
typealias SegmentDebugInfo = com.github.zly2006.zhihu.shared.nlp.SegmentDebugInfo
typealias KeywordSelectionDebug = com.github.zly2006.zhihu.shared.nlp.KeywordSelectionDebug
typealias SegmentMatchDebug = com.github.zly2006.zhihu.shared.nlp.SegmentMatchDebug
typealias PhraseMatchResult = com.github.zly2006.zhihu.shared.nlp.PhraseMatchResult

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
