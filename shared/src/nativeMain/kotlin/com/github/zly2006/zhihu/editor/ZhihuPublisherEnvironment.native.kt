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

package com.github.zly2006.zhihu.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.account.IosAccountStore
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient

@Composable
internal fun rememberZhihuPublisherEnvironment(): ZhihuApiEnvironment {
    val store = remember { IosAccountStore() }
    return remember(store) {
        IosZhihuPublisherEnvironment(store)
    }
}

private class IosZhihuPublisherEnvironment(
    private val store: IosAccountStore,
) : ZhihuApiEnvironment {
    override fun httpClient(): HttpClient = store.httpClient()

    override fun authenticatedCookies(): Map<String, String> = store.load().cookies

    override fun xsrfToken(): String = store.load().cookies["_xsrf"] ?: ""

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        Log.e(tag ?: "ZhihuPublisherEnvironment", "Failed to fetch Zhihu API", error)
    }
}
