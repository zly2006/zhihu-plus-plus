package com.github.zly2006.zhihu.viewmodel

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.signFetchRequest
import com.github.zly2006.zhihu.ui.Collection
import com.github.zly2006.zhihu.viewmodel.CollectionContentViewModel.CollectionItem
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel.FeedDisplayItem
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.typeOf

class CollectionContentViewModel(
    val collectionId: String,
) : PaginationViewModel<CollectionItem>(typeOf<CollectionItem>()) {
    val displayItems = mutableStateListOf<FeedDisplayItem>()
    var collection by mutableStateOf<Collection?>(null)
    val title by derivedStateOf {
        collection?.title ?: "收藏夹"
    }

    @Serializable
    class CollectionItem(
        val created: String,
        val content: Feed.Target
    )

    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/collections/$collectionId/items"

    override fun processResponse(context: Context, data: List<CollectionItem>, rawData: JsonArray) {
        super.processResponse(context, data, rawData)
        displayItems.addAll(data.map { createDisplayItem(it) }) // 展示用的已flatten数据
    }

    private fun createDisplayItem(item: CollectionItem): FeedDisplayItem {
        return FeedDisplayItem(
            title = item.content.title,
            summary = item.content.excerpt,
            details = item.content.detailsText,
            navDestination = item.content.navDestination,
            feed = null,
            avatarSrc = when (item.content) {
                is Feed.AnswerTarget -> item.content.author?.avatarUrl
                is Feed.ArticleTarget -> item.content.author.avatarUrl
                is Feed.QuestionTarget -> item.content.author?.avatarUrl
                else -> null
            }
        )
    }

    override fun refresh(context: Context) {
        if (isLoading) return
        displayItems.clear()
        viewModelScope.launch {
            val httpClient = httpClient(context)
            val jsonObject = httpClient.get("https://www.zhihu.com/api/v4/collections/$collectionId") {
                signFetchRequest(context)
            }.body<JsonObject>()
            collection = AccountData.decodeJson<Collection>(jsonObject["collection"]!!)
        }
        super.refresh(context)
    }
}
