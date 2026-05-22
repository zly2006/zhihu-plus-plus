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
import com.github.zly2006.zhihu.navigation.NavDestination
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

interface PaginationEnvironment {
    fun httpClient(): HttpClient

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

    fun configureSignedRequest(builder: HttpRequestBuilder) {
    }

    fun feedDisplaySettings(): FeedDisplaySettings = FeedDisplaySettings()

    fun localHistory(): List<NavDestination> = emptyList()
}

data class FeedDisplaySettings(
    val enableQualityFilter: Boolean = true,
    val reverseBlock: Boolean = false,
)

@Composable
expect fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment
