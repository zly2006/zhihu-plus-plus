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

package com.github.zly2006.zhihu.navigation

import android.content.Context
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.asApiEnvironment
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.CollectionItem
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.getOrFetchContentDetail

class AndroidAnswerNavigatorRepository(
    context: Context,
) : AnswerNavigatorRepository {
    private val appContext = context.applicationContext

    override suspend fun fetchCachedAnswerContent(article: Article): DataHolder.Answer? =
        appContext.asApiEnvironment().getOrFetchContentDetail(article) as? DataHolder.Answer

    override suspend fun fetchQuestionFeeds(
        questionId: Long,
        pageUrl: String?,
    ): AnswerNavigatorPage<Feed> {
        val url = pageUrl ?: zhihuQuestionFeedsUrl(questionId, limit = 6)
        val jojo = AccountData.fetchGet(appContext, url) { signFetchRequest() } ?: return AnswerNavigatorPage(
            items = emptyList(),
            nextUrl = "",
        )
        return answerNavigatorPageFromJson(jojo) { data ->
            ZhihuJson.decodeJson<List<Feed>>(data)
        }
    }

    override suspend fun fetchCollectionItems(pageUrl: String): AnswerNavigatorPage<CollectionItem> {
        val jojo = AccountData.fetchGet(appContext, pageUrl) { signFetchRequest() } ?: return AnswerNavigatorPage(
            items = emptyList(),
            nextUrl = "",
        )
        return answerNavigatorPageFromJson(jojo) { data ->
            ZhihuJson.decodeJson<List<CollectionItem>>(data)
        }
    }

    override suspend fun getAlreadyOpenedAnswerIds(answerIds: List<Long>): Set<Long> =
        ContentOpenEventSupport
            .getAlreadyOpenedContentIds(
                database = getContentFilterDatabase(appContext),
                content = answerIds.map { ContentType.ANSWER to it.toString() },
            ).mapNotNullTo(mutableSetOf()) { key ->
                key.substringAfter(':', "").toLongOrNull()
            }
}
