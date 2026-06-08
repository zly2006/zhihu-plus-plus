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

package com.github.zly2006.zhihu.shared.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.zly2006.zhihu.shared.data.DailySection
import com.github.zly2006.zhihu.shared.data.fetchDailyStoriesBefore
import com.github.zly2006.zhihu.shared.data.fetchDailyStoriesForDate
import com.github.zly2006.zhihu.shared.data.fetchLatestDailyStories
import com.github.zly2006.zhihu.shared.util.Log
import io.ktor.client.HttpClient

class DailyViewModel : ViewModel() {
    var sections by mutableStateOf<List<DailySection>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var nextDate: String? = null

    suspend fun loadLatest(httpClient: HttpClient) {
        isLoading = true
        try {
            val data = fetchLatestDailyStories(httpClient)
            sections = listOf(DailySection(data.date, data.stories))
            nextDate = data.date
            error = null
        } catch (e: Exception) {
            error = "加载失败: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    suspend fun loadDate(httpClient: HttpClient, date: String) {
        isLoading = true
        sections = emptyList()
        try {
            val data = fetchDailyStoriesForDate(httpClient, date)
            sections = listOf(DailySection(data.date, data.stories))
            nextDate = data.date
            error = null
        } catch (e: Exception) {
            error = "加载失败: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    suspend fun loadMore(httpClient: HttpClient) {
        val date = nextDate ?: return
        if (isLoadingMore) return
        isLoadingMore = true
        try {
            val data = fetchDailyStoriesBefore(httpClient, date)
            sections = sections + DailySection(data.date, data.stories)
            nextDate = data.date
        } catch (e: Exception) {
            Log.e("DailyViewModel", "Failed to load more daily stories", e)
        } finally {
            isLoadingMore = false
        }
    }
}
