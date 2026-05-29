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

package com.github.zly2006.zhihu.viewmodel.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonArray




class FollowViewModel : BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v3/moments?limit=10&desktop=true"
}

class FollowRecommendViewModel : BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://api.zhihu.com/moments_v3?feed_type=recommend"
}

class RecentMomentsViewModel : ViewModel() {
    @Serializable
    data class Actor(
        val id: String,
        val urlToken: String,
        val name: String,
        val avatarUrl: String,
    )

    @Serializable
    data class FollowingUserItem(
        val actor: Actor,
        val unreadCount: Int,
    )

    var users = mutableStateListOf<FollowingUserItem>()
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun load(environment: PaginationEnvironment) {
        if (isLoading || users.isNotEmpty()) return
        isLoading = true
        viewModelScope.launch {
            try {
                val json = environment.fetchJson("https://api.zhihu.com/moments/recent?type=raw", "") ?: return@launch
                val dataArray = json["data"]?.jsonArray ?: return@launch
                users.addAll(
                    dataArray.mapNotNull { item ->
                        try {
                            ZhihuJson.decodeJson<FollowingUserItem>(item)
                        } catch (e: Exception) {
                            environment.logDecodeFailure("RecentMomentsVM", item, e)
                            null
                        }
                    },
                )
            } catch (e: Exception) {
                environment.handleFetchFailure("RecentMomentsVM", e)
                errorMessage = "加载关注动态失败"
            } finally {
                isLoading = false
            }
        }
    }
}
