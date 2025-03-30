package com.github.zly2006.zhihu.v2.viewmodel.feed

import android.content.Context
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.Question

class HistoryViewModel : BaseFeedViewModel() {
    override val initialUrl: String
        get() = error("不需要URL")

    override val isEnd: Boolean
        get() = displayItems.isNotEmpty()

    override fun refresh(context: Context) {
        if (isLoading) return
        isLoading = true
        errorMessage = null

        val history = (context as MainActivity).history
        displayItems.clear()

        history.history.forEach { dest ->
            val displayItem = when (dest) {
                is Article -> {
                    when (dest.type) {
                        "answer" -> FeedDisplayItem(
                            title = dest.title,
                            summary = dest.excerpt ?: "",
                            details = dest.authorName,
                            feed = null,
                            avatarSrc = dest.avatarSrc,
                            navDestination = dest,
                        )

                        "article" -> FeedDisplayItem(
                            title = dest.title,
                            summary = dest.excerpt ?: "",
                            details = dest.authorName,
                            feed = null,
                            avatarSrc = dest.avatarSrc,
                            navDestination = dest,
                        )

                        else -> null
                    }
                }
                is Question -> {
                    FeedDisplayItem(
                        title = dest.title,
                        details = "问题",
                        feed = null,
                        navDestination = dest,
                        summary = ""
                    )
                }

                else -> null
            }

            displayItem?.let {
                displayItems.add(it)
            }
        }

        isLoading = false
    }

    override suspend fun fetchFeeds(context: Context) {
    }

    override fun loadMore(context: Context) {
        // 不需要loadMore，所有数据一次性加载
    }
}
