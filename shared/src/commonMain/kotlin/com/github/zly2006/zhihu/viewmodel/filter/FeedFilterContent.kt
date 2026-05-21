/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import kotlinx.serialization.json.Json

/**
 * 从 feed item 提炼出的内容快照。
 * 这个结构只在 feed 过滤流水线内部流转，用来承接关键词、NLP、作者、主题等内容级规则。
 */
data class FilterableContent(
    val title: String,
    val summary: String?,
    val content: String?,
    val authorName: String?,
    val authorId: String?,
    val contentId: String,
    val contentType: String,
    val raw: DataHolder.Content,
    val isFollowing: Boolean = false,
    val questionId: Long? = null,
    val url: String? = null,
    val feedJson: String? = null,
    val navDestinationJson: String? = null,
)

data class FeedContentIdentity(
    val type: String,
    val id: String,
)

fun FeedDisplayItem.resolveContentIdentity(): FeedContentIdentity {
    val identity = navDestination?.let(ContentOpenEventSupport::toTrackedContentIdentity)
    return if (identity != null) {
        FeedContentIdentity(identity.type, identity.id)
    } else {
        FeedContentIdentity("unknown", navDestination.hashCode().toString())
    }
}

fun FeedDisplayItem.toFilterableContent(
    identity: FeedContentIdentity,
    rawContent: DataHolder.Content,
): FilterableContent = FilterableContent(
    title = title,
    summary = summary,
    content = when (rawContent) {
        is DataHolder.Answer -> rawContent.content
        is DataHolder.Article -> rawContent.content
        is DataHolder.Pin -> rawContent.contentHtml
        else -> null
    } ?: content ?: summary,
    authorName = authorName,
    authorId = rawContent.author?.id,
    contentId = identity.id,
    contentType = identity.type,
    raw = rawContent,
    isFollowing = rawContent.author?.isFollowing ?: false,
    questionId = (rawContent as? DataHolder.Answer)?.question?.id,
    url = feed?.target?.url,
    feedJson = feed?.let { runCatching { feedFilterRecordJson.encodeToString(it) }.getOrNull() },
    navDestinationJson = navDestination?.let { runCatching { feedFilterRecordJson.encodeToString(it) }.getOrNull() },
)

/** 从内容实体中提取主题 ID 列表，供 feed 过滤阶段的主题规则使用。 */
fun extractTopicIds(raw: DataHolder.Content): List<String>? = when (raw) {
    is DataHolder.Answer -> raw.question.topics.map { it.id }
    is DataHolder.Question -> raw.topics.map { it.id }
    is DataHolder.Article -> raw.topics?.map { it.id }
    else -> null
}

private val DataHolder.Content.author: DataHolder.Author?
    get() = when (this) {
        is DataHolder.Answer -> this.author
        is DataHolder.Article -> this.author
        is DataHolder.Pin -> this.author
        is DataHolder.Question -> this.author
        else -> null
    }

private val feedFilterRecordJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}
