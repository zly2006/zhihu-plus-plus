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
import androidx.compose.runtime.remember
import io.ktor.client.HttpClient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Composable
actual fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment =
    remember(allowGuestAccess) { IosPaginationEnvironment() } // TODO: iOS 分页环境完整实现

private class IosPaginationEnvironment : PaginationEnvironment {
    override fun httpClient(): HttpClient = error("HTTP client not available on iOS") // TODO: iOS HTTP 客户端

    override suspend fun fetchJson(url: String, include: String): JsonObject? = null // TODO: iOS JSON 数据获取

    override fun logDecodeFailure(tag: String?, item: JsonElement, error: Exception) = Unit // TODO: iOS 解码失败日志

    override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit // TODO: iOS 获取失败处理
}
