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

package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException

private const val DAILY_PRIMARY_API_BASE = "https://news-at.zhihu.com/api/4/stories"
private const val DAILY_FALLBACK_API_BASE = "https://daily.zhihu.com/api/4/stories"

suspend fun HttpClient.fetchLatestDailyStories(): DailyStoriesResponse =
    fetchDailyStories("/latest")

suspend fun HttpClient.fetchDailyStoriesBefore(date: String): DailyStoriesResponse =
    fetchDailyStories("/before/$date")

private suspend fun HttpClient.fetchDailyStories(path: String): DailyStoriesResponse =
    try {
        get("$DAILY_PRIMARY_API_BASE$path").body()
    } catch (e: Exception) {
        if (e is CancellationException || !e.isHostResolutionFailure()) {
            throw e
        }
        get("$DAILY_FALLBACK_API_BASE$path").body()
    }

private fun Throwable.isHostResolutionFailure(): Boolean =
    this is UnresolvedAddressException ||
        this::class.simpleName == "UnknownHostException" ||
        message?.contains("Unable to resolve host", ignoreCase = true) == true ||
        message?.contains("No address associated with hostname", ignoreCase = true) == true ||
        message?.contains("Name or service not known", ignoreCase = true) == true ||
        message?.contains("nodename nor servname provided", ignoreCase = true) == true ||
        generateSequence(cause) { it.cause }.any {
            it is UnresolvedAddressException ||
                it::class.simpleName == "UnknownHostException"
        }
