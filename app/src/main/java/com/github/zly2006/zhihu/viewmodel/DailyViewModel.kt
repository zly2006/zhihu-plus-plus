package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.zly2006.zhihu.data.DailyStoriesResponse
import com.github.zly2006.zhihu.ui.DailySection
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.Json

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

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun loadLatest(httpClient: HttpClient) {
        isLoading = true
        try {
            val data = httpClient.get("https://news-at.zhihu.com/api/4/stories/latest").body<DailyStoriesResponse>()
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
        if (isLoadingMore || nextDate == null) return
        isLoadingMore = true
        try {
            val data = httpClient.get("https://news-at.zhihu.com/api/4/stories/before/$nextDate").body<DailyStoriesResponse>()
            sections = sections + DailySection(data.date, data.stories)
            nextDate = data.date
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingMore = false
        }
    }
}
