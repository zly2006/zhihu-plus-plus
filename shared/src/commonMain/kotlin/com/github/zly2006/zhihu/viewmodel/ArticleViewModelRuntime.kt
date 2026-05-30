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
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.ui.ArticleAnswerSwitchState
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder

interface ArticleViewModelRuntime {
    suspend fun getContentDetail(article: Article): DataHolder.Content?

    suspend fun recordOpenEvent(
        destination: Article,
        questionId: Long?,
    )

    fun answerNavigatorRepository(): AnswerNavigatorRepository

    fun articleAnswerSwitchState(): ArticleAnswerSwitchState?

    fun postHistoryDestination(destination: Article)

    fun configureSignedRequest(builder: HttpRequestBuilder)

    fun setPlainTextClipboard(
        label: String,
        text: String,
    )

    fun xsrfToken(): String

    fun hasImageExportPermission(): Boolean

    fun requiresHtmlExportPermission(): Boolean

    fun requestImageExportPermission()

    fun accountHttpClient(): HttpClient

    fun loadExportAssetText(fileName: String): String

    fun buildArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        extraSectionsHtml: String,
    ): String

    suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String

    fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String

    fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    )

    fun articleImageExportRenderer(loadAssetText: (String) -> String): ArticleImageExportRenderer
}
