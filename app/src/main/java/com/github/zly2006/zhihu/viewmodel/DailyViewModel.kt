package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.DailyStoriesResponse
import com.github.zly2006.zhihu.ui.DailySection
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch
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
    var nextDate by mutableStateOf<String?>(null)
        private set

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun loadLatest(httpClient: HttpClient, onFinished: (date: String) -> Unit = {}) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = httpClient.get("https://news-at.zhihu.com/api/4/stories/latest")
                val data = json.decodeFromString<DailyStoriesResponse>(response.body<String>())
                sections = listOf(DailySection(data.date, data.stories))
                nextDate = data.date
                error = null
                onFinished(data.date)
            } catch (e: Exception) {
                error = "加载失败: ${e.message}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMore(httpClient: HttpClient) {
        if (isLoadingMore || nextDate == null) return
        viewModelScope.launch {
            isLoadingMore = true
            try {
                val response = httpClient.get("https://news-at.zhihu.com/api/4/stories/before/$nextDate")
                val data = json.decodeFromString<DailyStoriesResponse>(response.body<String>())
                sections = sections + DailySection(data.date, data.stories)
                nextDate = data.date
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }
}
