package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import android.util.Log
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.SearchResult
import com.github.zly2006.zhihu.ui.raiseForStatus
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.parameters
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import java.net.URLEncoder

class SearchViewModel(
    val searchQuery: String,
) : BaseFeedViewModel() {
    override val initialUrl: String
        get() {
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            return "https://www.zhihu.com/api/v4/search_v3?gk_version=gz-gaokao&t=general&q=$encodedQuery&correction=1&search_source=Normal&limit=10"
        }

    // Override include to request necessary fields for search results
    override val include = "data[*].highlight,object,type"

    override suspend fun fetchFeeds(context: Context) {
        try {
            val url = lastPaging?.next ?: initialUrl
            val jojo = AccountData.fetchGet(context, url) {
                url {
                    parameters.append("include", include)
                }
                signFetchRequest(context)
            }
            val jsonArray = jojo["data"]!!.jsonArray

            // Parse search results and convert to Feed objects
            val feeds = jsonArray.mapNotNull { element ->
                try {
                    val searchResult = AccountData.decodeJson<SearchResult>(element)
                    searchResult.toFeed()
                } catch (e: Exception) {
                    Log.e("SearchViewModel", "Failed to decode search result: $element", e)
                    null
                }
            }

            processResponse(context, feeds, jsonArray)

            // Handle pagination
            if ("paging" in jojo) {
                lastPaging = AccountData.decodeJson(jojo["paging"]!!)
            }
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Failed to fetch search results", e)
            throw e
        } finally {
            isLoading = false
        }
    }
}
