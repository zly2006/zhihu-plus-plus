package com.github.zly2006.zhihu.v2.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.v2.viewmodel.CollectionContentViewModel.CollectionItem
import com.github.zly2006.zhihu.v2.viewmodel.feed.BaseFeedViewModel.FeedDisplayItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

class CollectionContentViewModel(
    val collectionId: String,
) : PaginationViewModel<CollectionItem>(typeOf<CollectionItem>()) {
    val displayItems = mutableStateListOf<FeedDisplayItem>()
    @Serializable
    class CollectionItem(
        val created: String,
        val content: Feed.Target
    )

    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/collections/$collectionId/items"

    override fun processResponse(data: List<CollectionItem>, rawData: JsonArray) {
        super.processResponse(data, rawData)
        displayItems.addAll(data.map { createDisplayItem(it) }) // 展示用的已flatten数据
    }

    private fun createDisplayItem(item: CollectionItem): FeedDisplayItem {
        return FeedDisplayItem(
            title = item.content.title,
            summary = null,
            details = item.content.detailsText,
            navDestination = item.content.navDestination,
            feed = null,
            avatarSrc = when (item.content) {
                is Feed.AnswerTarget -> item.content.author.avatar_url
                is Feed.ArticleTarget -> item.content.author.avatar_url
                is Feed.QuestionTarget -> item.content.author?.avatar_url
                else -> null
            }
        )
    }

    override fun refresh(context: Context) {
        if (isLoading) return
        displayItems.clear()
        super.refresh(context)
    }
}
