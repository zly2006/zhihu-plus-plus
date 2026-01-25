package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.OnlineHistoryResponse
import com.github.zly2006.zhihu.resolveContent
import com.github.zly2006.zhihu.util.signFetchRequest
import kotlinx.coroutines.launch

class OnlineHistoryViewModel : BaseFeedViewModel() {
    override val initialUrl: String = "https://api.zhihu.com/unify-consumption/read_history?offset=0&limit=10"

    private var nextUrl: String? = initialUrl

    override var isEnd: Boolean by mutableStateOf(false)

    override fun refresh(context: Context) {
        if (isLoading) return
        displayItems.clear()
        nextUrl = initialUrl
        isEnd = false
        loadMore(context)
    }

    override suspend fun fetchFeeds(context: Context) {
        // Not used directly, loadMore handles the logic
    }

    override fun loadMore(context: Context) {
        if (isLoading || isEnd) return
        val url = nextUrl ?: return

        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val jsonElement = AccountData.fetchGet(context, url) {
                    signFetchRequest(context)
                }

                val response = AccountData.decodeJson<OnlineHistoryResponse>(jsonElement)

                isEnd = response.paging.isEnd
                nextUrl = response.paging.next

                response.data.forEach { item ->
                    val navDest = try {
                        resolveContent(Uri.parse(item.data.action.url))
                    } catch (e: Exception) {
                        null
                    }

                    val detailsText = item.data.matrix
                        ?.firstOrNull()
                        ?.data
                        ?.text ?: item.data.extra.contentType

                    val displayItem = FeedDisplayItem(
                        title = item.data.header.title,
                        summary = item.data.content?.summary ?: "",
                        details = detailsText,
                        feed = null,
                        navDestination = navDest,
                        avatarSrc = item.data.header.icon,
                        authorName = item.data.content?.authorName,
                    )
                    displayItems.add(displayItem)
                }
            } catch (e: Exception) {
                errorMessage = e.message
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}
