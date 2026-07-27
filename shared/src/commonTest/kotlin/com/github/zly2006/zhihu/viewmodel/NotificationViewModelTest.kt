/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationViewModelTest {
    @Test
    fun marksOnlyTheRequestedMobileNotificationCategoryAsRead() = runTest {
        val requests = mutableListOf<Pair<HttpMethod, String>>()
        val client = HttpClient(
            MockEngine { request ->
                requests += request.method to request.url.toString()
                respond("", HttpStatusCode.NoContent)
            },
        )
        val environment = object : MobileHomeFeedEnvironment {
            override fun httpClient() = client

            override fun mobileHomeFeedHttpClient() = client

            override fun authenticatedCookies() = emptyMap<String, String>()

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }
        val viewModel = NotificationViewModel()
        MobileNotificationCategory.entries.forEach { category ->
            viewModel.categoryUnreadCounts[category] = 1
        }

        for ((index, category) in MobileNotificationCategory.entries.withIndex()) {
            assertTrue(viewModel.markCategoryAsRead(category, environment))
            assertEquals(0, viewModel.categoryUnreadCounts.getValue(category))
            assertEquals(MobileNotificationCategory.entries.size - index - 1, viewModel.unreadCount)
        }

        assertEquals(
            MobileNotificationCategory.entries.map { category ->
                HttpMethod.Post to category.readAllUrl
            },
            requests,
        )
    }
}
