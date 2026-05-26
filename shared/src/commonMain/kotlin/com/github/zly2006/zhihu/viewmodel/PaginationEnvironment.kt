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

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.AnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.viewmodel.local.LocalRecommendationEngine
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

interface PaginationEnvironment {
    fun httpClient(): HttpClient

    fun mobileHomeFeedHttpClient(): HttpClient = httpClient()

    suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject?

    fun logDecodeFailure(
        tag: String?,
        item: JsonElement,
        error: Exception,
    )

    suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    )

    suspend fun handleMobileHomeFeedFailure(error: Exception) {
        handleFetchFailure("AndroidHomeFeedViewModel", error)
    }

    fun configureSignedRequest(builder: HttpRequestBuilder) {
    }

    fun feedDisplaySettings(): FeedDisplaySettings = FeedDisplaySettings()

    fun localHistory(): List<NavDestination> = emptyList()

    suspend fun addReadHistory(
        contentToken: String,
        contentTypeName: String,
    ) {
    }

    suspend fun followQuestion(
        questionId: Long,
        follow: Boolean,
    ) {
    }

    suspend fun applyHomeFeedFilters(items: List<FeedDisplayItem>): HomeFeedFilterResult =
        HomeFeedFilterResult(
            foregroundItems = items,
            filteredItems = items,
            reverseBlock = feedDisplaySettings().reverseBlock,
        )

    suspend fun sendFeedReadStatus(feed: Feed) {
    }

    suspend fun recordContentInteraction(feed: Feed) {
    }

    suspend fun markItemsAsTouched(items: Set<Pair<String, String>>): Set<Pair<String, String>> = emptySet()

    suspend fun clearAllHistory() {
    }

    fun localRecommendationEngine(): LocalRecommendationEngine? = null

    suspend fun handleLocalRecommendationFailure(error: Exception) {
        handleFetchFailure("LocalHomeFeedViewModel", error)
    }

    suspend fun showLocalRecommendationDatabaseError() {
    }

    fun answerNavigatorRepository(): AnswerNavigatorRepository? = null
}

data class FeedDisplaySettings(
    val enableQualityFilter: Boolean = true,
    val reverseBlock: Boolean = false,
)

data class HomeFeedFilterResult(
    val foregroundItems: List<FeedDisplayItem>,
    val filteredItems: List<FeedDisplayItem>,
    val reverseBlock: Boolean,
)

fun zhihuQuestionFollowersUrl(questionId: Long): String =
    "https://www.zhihu.com/api/v4/questions/$questionId/followers"

@Composable
expect fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment
