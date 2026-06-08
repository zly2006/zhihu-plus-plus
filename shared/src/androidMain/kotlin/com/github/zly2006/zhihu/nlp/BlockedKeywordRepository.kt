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

import android.content.Context
import com.github.zly2006.zhihu.viewmodel.filter.BlockedContentRecord
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeywordService
import com.github.zly2006.zhihu.viewmodel.filter.KeywordSemanticMatcher
import com.github.zly2006.zhihu.viewmodel.filter.KeywordType
import com.github.zly2006.zhihu.viewmodel.filter.MatchedKeywordInfo
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android facade for blocked keyword storage.
 * Core database semantics live in [BlockedKeywordService]; NLP matching is injected by app variants.
 */
class BlockedKeywordRepository(
    context: Context,
    semanticMatcher: KeywordSemanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
) {
    private val database = getContentFilterDatabase(context)
    private val service = BlockedKeywordService(
        keywordDao = database.blockedKeywordDao(),
        recordDao = database.blockedContentRecordDao(),
        semanticMatcher = semanticMatcher,
    )

    suspend fun getAllKeywords(): List<BlockedKeyword> = withContext(Dispatchers.IO) {
        service.getAllKeywords()
    }

    suspend fun getExactMatchKeywords(): List<BlockedKeyword> = withContext(Dispatchers.IO) {
        service.getExactMatchKeywords()
    }

    suspend fun getNLPSemanticKeywords(): List<BlockedKeyword> = withContext(Dispatchers.IO) {
        service.getNLPSemanticKeywords()
    }

    suspend fun addKeyword(
        keyword: String,
        keywordType: KeywordType = KeywordType.NLP_SEMANTIC,
    ): Long = withContext(Dispatchers.IO) {
        service.addKeyword(keyword, keywordType)
    }

    suspend fun addExactMatchKeyword(
        keyword: String,
        caseSensitive: Boolean = false,
        isRegex: Boolean = false,
    ): Long = withContext(Dispatchers.IO) {
        service.addExactMatchKeyword(keyword, caseSensitive, isRegex)
    }

    suspend fun addNLPPhrase(phrase: String): Long = withContext(Dispatchers.IO) {
        service.addNLPPhrase(phrase)
    }

    suspend fun updateKeyword(keyword: BlockedKeyword) {
        withContext(Dispatchers.IO) {
            service.updateKeyword(keyword)
        }
    }

    suspend fun deleteKeyword(keyword: BlockedKeyword) {
        withContext(Dispatchers.IO) {
            service.deleteKeyword(keyword)
        }
    }

    suspend fun deleteKeywordById(id: Long) {
        withContext(Dispatchers.IO) {
            service.deleteKeywordById(id)
        }
    }

    suspend fun clearAllKeywords() {
        withContext(Dispatchers.IO) {
            service.clearAllKeywords()
        }
    }

    suspend fun getKeywordCount(): Int = withContext(Dispatchers.IO) {
        service.getKeywordCount()
    }

    suspend fun checkNLPBlockingWithWeight(
        title: String,
        excerpt: String?,
        content: String?,
        threshold: Double = 0.8,
    ): Pair<Boolean, List<MatchedKeywordInfo>> = withContext(Dispatchers.IO) {
        service.checkNLPBlockingWithWeight(title, excerpt, content, threshold)
    }

    suspend fun recordBlockedContent(
        contentId: String,
        contentType: String,
        title: String,
        excerpt: String?,
        authorName: String?,
        authorId: String?,
        matchedKeywords: List<MatchedKeywordInfo>,
    ) {
        withContext(Dispatchers.IO) {
            service.recordBlockedContent(
                contentId = contentId,
                contentType = contentType,
                title = title,
                excerpt = excerpt,
                authorName = authorName,
                authorId = authorId,
                matchedKeywords = matchedKeywords,
            )
        }
    }

    suspend fun getRecentBlockedRecords(limit: Int = 100): List<BlockedContentRecord> =
        withContext(Dispatchers.IO) {
            service.getRecentBlockedRecords(limit)
        }

    suspend fun deleteBlockedRecord(id: Long) {
        withContext(Dispatchers.IO) {
            service.deleteBlockedRecord(id)
        }
    }

    suspend fun clearAllBlockedRecords() {
        withContext(Dispatchers.IO) {
            service.clearAllBlockedRecords()
        }
    }

    fun parseMatchedKeywords(json: String): List<MatchedKeywordInfo> = service.parseMatchedKeywords(json)
}
