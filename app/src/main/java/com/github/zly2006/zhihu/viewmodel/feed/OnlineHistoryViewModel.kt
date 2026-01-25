package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.OnlineHistoryItem
import com.github.zly2006.zhihu.resolveContent
import kotlinx.serialization.json.JsonArray

class OnlineHistoryViewModel : BaseFeedViewModel() {
    override val initialUrl: String = "https://api.zhihu.com/unify-consumption/read_history?offset=0&limit=10"

    override fun processResponse(context: Context, data: List<Feed>, rawData: JsonArray) {
        val history = (context as MainActivity).history

        val response = rawData.mapNotNull {
            runCatching {
                AccountData.decodeJson<OnlineHistoryItem>(it)
            }.getOrNull()
        }

        response.forEach { item ->
            val navDest = try {
                resolveContent(Uri.parse(item.data.action.url))
            } catch (e: Exception) {
                null
            }

            val detailsText = item.data.matrix
                ?.firstOrNull()
                ?.data
                ?.text ?: item.data.extra.contentType

            val matchedItem = history.history.firstOrNull {
                it == navDest
            }
            val displayItem = FeedDisplayItem(
                title = item.data.header.title,
                summary = item.data.content?.summary ?: "",
                details = detailsText,
                feed = null,
                navDestination = navDest,
                avatarSrc = when (matchedItem) {
                    is Article -> matchedItem.avatarSrc
                    else -> null
                },
                authorName = item.data.content?.authorName,
            )
            displayItems.add(displayItem)
        }
    }
}
