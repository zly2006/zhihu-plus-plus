package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.URLEncoder

class SearchViewModel : BaseFeedViewModel() {
    var searchQuery by mutableStateOf("")
        private set

    override val initialUrl: String
        get() {
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            return "https://www.zhihu.com/api/v4/search_v3?gk_version=gz-gaokao&t=general&q=$encodedQuery&correction=1&search_source=Normal&limit=10"
        }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun performSearch(context: Context) {
        if (searchQuery.isNotBlank()) {
            refresh(context)
        }
    }
}
