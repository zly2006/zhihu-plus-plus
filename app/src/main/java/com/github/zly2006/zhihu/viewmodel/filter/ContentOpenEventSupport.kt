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

import android.content.Context
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.NavDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport as SharedContentOpenEventSupport

typealias ContentOpenFrom = com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
typealias QuestionAnswerCandidatePartition = com.github.zly2006.zhihu.shared.filter.QuestionAnswerCandidatePartition
typealias TrackedContentIdentity = com.github.zly2006.zhihu.shared.filter.TrackedContentIdentity

object ContentOpenEventSupport {
    fun buildContentKey(type: String, id: String): String =
        SharedContentOpenEventSupport.buildContentKey(type, id)

    fun toTrackedContentIdentity(destination: NavDestination): TrackedContentIdentity? =
        SharedContentOpenEventSupport.toTrackedContentIdentity(destination)

    fun inferOpenFrom(source: NavDestination?, target: NavDestination): String =
        SharedContentOpenEventSupport.inferOpenFrom(source, target)

    suspend fun recordOpenEvent(
        context: Context,
        destination: NavDestination,
        questionId: Long? = null,
        openFrom: String = ContentOpenFrom.UNKNOWN,
    ) {
        val identity = toTrackedContentIdentity(destination) ?: return
        withContext(Dispatchers.IO) {
            getContentFilterDatabase(context)
                .contentOpenEventDao()
                .insert(
                    ContentOpenEvent(
                        contentType = identity.type,
                        contentId = identity.id,
                        questionId = questionId,
                        openFrom = openFrom,
                    ),
                )
        }
    }

    suspend fun getAlreadyOpenedContentIds(
        context: Context,
        content: List<Pair<String, String>>,
    ): Set<String> = withContext(Dispatchers.IO) {
        val idsToCheck = content.map { (targetType, targetId) ->
            buildContentKey(targetType, targetId)
        }
        getContentFilterDatabase(context)
            .contentOpenEventDao()
            .getOpenedContentKeysByKeys(idsToCheck)
            .toSet()
    }

    fun filterUnopenedAnswerArticles(
        candidates: List<Article>,
        openedContentKeys: Set<String>,
        currentArticleId: Long,
        historyIds: Set<Long> = emptySet(),
    ): List<Article> =
        SharedContentOpenEventSupport.filterUnopenedAnswerArticles(
            candidates = candidates,
            openedContentKeys = openedContentKeys,
            currentArticleId = currentArticleId,
            historyIds = historyIds,
        )

    fun partitionQuestionAnswerCandidates(
        candidates: List<com.github.zly2006.zhihu.navigation.Article>,
        openedAnswerIds: Set<Long>,
        currentArticleId: Long,
        historyIds: Set<Long> = emptySet(),
        previousIds: Set<Long> = emptySet(),
        nextIds: Set<Long> = emptySet(),
    ): QuestionAnswerCandidatePartition =
        SharedContentOpenEventSupport.partitionQuestionAnswerCandidates(
            candidates = candidates,
            openedAnswerIds = openedAnswerIds,
            currentArticleId = currentArticleId,
            historyIds = historyIds,
            previousIds = previousIds,
            nextIds = nextIds,
        )
}
