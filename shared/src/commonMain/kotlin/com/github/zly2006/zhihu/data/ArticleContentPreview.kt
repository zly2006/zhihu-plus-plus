/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.withReadingQueueSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** 点击与详情 ViewModel 在 UI 线程交接正文；不把长 HTML 写入导航参数、SavedState 或历史。 */
internal object ArticleContentPreview {
    private data class Entry(
        val article: Article,
        val html: String,
    )

    private val entries = linkedMapOf<String, Entry>()

    @OptIn(ExperimentalUuidApi::class)
    fun attach(article: Article, html: String): Article {
        if (html.isBlank()) return article
        // 通常由目标 ViewModel 立即取走；拦截导航或快速连续点击时也只保留有限条目。
        while (entries.size >= 8) entries.remove(entries.keys.first())
        val key = Uuid.random().toString()
        entries[key] = Entry(article, html)
        return article.copy(contentPreviewKey = key)
    }

    fun take(article: Article): String? {
        val entry = entries.remove(article.contentPreviewKey) ?: return null
        return entry.html.takeIf { entry.article == article }
    }
}

/** 只在实际点击时准备正文，列表测量、过滤和朗读队列读取导航目标不会创建交接条目。 */
fun FeedDisplayItem.prepareNavigation(readingQueueSourceId: String? = null): NavDestination? {
    val destination = navDestination?.withReadingQueueSource(readingQueueSourceId)
    if (isFiltered || destination !is Article) return destination
    val html = when (val source = raw) {
        is DataHolder.Answer -> source.content.takeIf { destination.type == ArticleType.Answer && source.id == destination.id }
        is DataHolder.Article -> source.content.takeIf { destination.type == ArticleType.Article && source.id == destination.id }
        else -> when (val target = feed?.target) {
            is Feed.AnswerTarget -> target.content.takeIf { destination.type == ArticleType.Answer && target.id == destination.id }
            is Feed.ArticleTarget -> target.content.takeIf { destination.type == ArticleType.Article && target.id == destination.id }
            else -> ""
        }
    }
    return ArticleContentPreview.attach(destination, html.orEmpty())
}
