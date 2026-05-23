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

package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.navigation.AnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.shared.article.VoteUpState
import com.github.zly2006.zhihu.shared.data.CollectionResponse
import com.github.zly2006.zhihu.shared.data.DataHolder
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.json.JsonObject

interface ArticleViewModelRuntime {
    suspend fun getContentDetail(article: Article): DataHolder.Content?

    suspend fun recordOpenEvent(
        destination: Article,
        questionId: Long?,
    )

    fun answerNavigatorRepository(): AnswerNavigatorRepository

    suspend fun loadCollections(
        contentType: String,
        articleId: Long,
    ): CollectionResponse

    suspend fun createNewCollection(
        title: String,
        description: String,
        isPublic: Boolean,
    )

    suspend fun voteArticle(
        article: Article,
        newState: VoteUpState,
    ): JsonObject

    fun configureSignedRequest(builder: HttpRequestBuilder)

    fun xsrfToken(): String

    fun showMessage(message: String)
}
