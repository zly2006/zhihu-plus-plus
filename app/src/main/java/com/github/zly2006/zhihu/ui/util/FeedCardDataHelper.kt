package com.github.zly2006.zhihu.ui.util

import android.content.Context
import android.widget.Toast
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FeedCard 数据加载工具
 * 提供 lazy 加载机制，确保屏蔽操作能获取到必要的数据
 */
object FeedCardDataHelper {
    /**
     * 确保能获取作者信息，优先从 feed.target.author，失败时 lazy 加载完整内容
     */
    suspend fun ensureAuthorInfo(
        context: Context,
        feedItem: BaseFeedViewModel.FeedDisplayItem,
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        // 优先从 feed.target.author 获取
        feedItem.feed?.target?.author?.let { author ->
            return@withContext Pair(author.id, author.name)
        }

        // 尝试从 navDestination 的作者字段获取
        feedItem.navDestination?.let { navDest ->
            // 如果是 Article/Answer，尝试加载完整内容
            if (navDest is com.github.zly2006.zhihu.Article) {
                val fullContent = ContentDetailCache.getOrFetch(context, navDest)
                when (fullContent) {
                    is DataHolder.Answer -> {
                        return@withContext fullContent.author?.let { Pair(it.id, it.name) }
                    }
                    is DataHolder.Article -> {
                        return@withContext fullContent.author?.let { Pair(it.id, it.name) }
                    }
                    else -> {}
                }
            }
        }

        // 无法获取作者信息
        null
    }

    /**
     * 确保能获取关键词屏蔽所需的内容（标题+摘要+正文），优先从 feed.target，失败时 lazy 加载
     */
    suspend fun ensureContentForKeywordBlocking(
        context: Context,
        feedItem: BaseFeedViewModel.FeedDisplayItem,
    ): Triple<String, String, String?>? = withContext(Dispatchers.IO) {
        // 优先使用已有的数据
        val title = feedItem.title
        val summary = feedItem.summary ?: feedItem.feed?.target?.excerpt ?: ""
        var content = feedItem.content

        // 如果 content 为空，尝试 lazy 加载
        if (content == null) {
            feedItem.navDestination?.let { navDest ->
                val fullContent = ContentDetailCache.getOrFetch(context, navDest)
                content = when (fullContent) {
                    is DataHolder.Answer -> fullContent.content
                    is DataHolder.Article -> fullContent.content
                    is DataHolder.Question -> fullContent.detail
                    else -> null
                }
            }
        }

        if (title.isNotEmpty() || summary.isNotEmpty() || content != null) {
            Triple(title, summary, content)
        } else {
            null
        }
    }

    /**
     * 显示加载失败提示
     */
    fun showLoadFailedToast(context: Context, action: String) {
        Toast.makeText(context, "无法获取$action 所需的数据，请尝试进入内容详情页操作", Toast.LENGTH_LONG).show()
    }
}
