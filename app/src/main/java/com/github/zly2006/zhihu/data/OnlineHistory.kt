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

package com.github.zly2006.zhihu.data

import kotlinx.serialization.Serializable

@Serializable
data class OnlineHistoryItem(
    val cardType: String,
    val data: OnlineHistoryData,
)

@Serializable
data class OnlineHistoryData(
    val header: OnlineHistoryHeader,
    val content: OnlineHistoryContent? = null,
    val action: OnlineHistoryAction,
    val extra: OnlineHistoryExtra,
    val matrix: List<OnlineHistoryMatrixItem>? = null,
)

@Serializable
data class OnlineHistoryMatrixItem(
    val type: String,
    val data: OnlineHistoryMatrixData,
)

@Serializable
data class OnlineHistoryMatrixData(
    val text: String,
)

@Serializable
data class OnlineHistoryHeader(
    val icon: String,
    val title: String,
    val action: OnlineHistoryAction? = null,
)

@Serializable
data class OnlineHistoryContent(
    val authorName: String? = null,
    val summary: String? = null,
    val coverImage: String? = null,
)

@Serializable
data class OnlineHistoryAction(
    val type: String,
    val url: String,
)

@Serializable
data class OnlineHistoryExtra(
    val contentToken: String,
    val contentType: String,
    val readTime: Long,
    val questionToken: String,
)
