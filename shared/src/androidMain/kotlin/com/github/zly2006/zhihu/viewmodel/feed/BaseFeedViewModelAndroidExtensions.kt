/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.viewmodel.filter.getBlocklistManager
import com.github.zly2006.zhihu.viewmodel.paginationEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun BaseFeedViewModel.pullToRefresh(context: Context) {
    pullToRefresh(paginationEnvironment(context))
}

/**
 * 处理用户屏蔽（包含数据获取和屏蔽逻辑）
 */
fun BaseFeedViewModel.handleBlockUser(
    context: Context,
    feedItem: FeedDisplayItem,
    onShowDialog: (Pair<String, String>) -> Unit,
) {
    viewModelScope.launch {
        val authorInfo = ensureAuthorInfo(context, feedItem)
        if (authorInfo != null) {
            onShowDialog(authorInfo)
        } else {
            Toast.makeText(context, "无法获取屏蔽用户所需的数据，请尝试进入内容详情页操作", Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * 处理关键词屏蔽（包含数据获取和屏蔽逻辑）
 */
fun BaseFeedViewModel.handleBlockByKeywords(
    context: Context,
    feedItem: FeedDisplayItem,
    onShowDialog: (Pair<FeedDisplayItem, Triple<String, String, String?>>) -> Unit,
) {
    viewModelScope.launch {
        val contentInfo = ensureContentForKeywordBlocking(context, feedItem)
        if (contentInfo != null) {
            onShowDialog(feedItem to contentInfo)
        } else {
            Toast.makeText(context, "无法获取关键词屏蔽所需的数据，请尝试进入内容详情页操作", Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * 屏蔽主题
 */
fun BaseFeedViewModel.handleBlockTopic(
    context: Context,
    topicId: String,
    topicName: String,
) {
    viewModelScope.launch {
        try {
            val blocklistManager = getBlocklistManager(context)
            blocklistManager.addBlockedTopic(topicId, topicName)
            Toast.makeText(context, "已屏蔽主题「$topicName」", Toast.LENGTH_SHORT).show()
            displayItems.removeAll {
                val topics = when (val content = it.raw) {
                    is DataHolder.Answer -> content.question.topics
                    is DataHolder.Article -> content.topics
                    is DataHolder.Question -> content.topics
                    else -> null
                }
                topics?.any { topic -> topic.id == topicId } == true
            }
        } catch (e: Exception) {
            val message = "屏蔽失败: ${e.message}"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}

private suspend fun ensureAuthorInfo(
    context: Context,
    feedItem: FeedDisplayItem,
): Pair<String, String>? = withContext(Dispatchers.IO) {
    feedItem.feed?.target?.author?.let { author ->
        return@withContext Pair(author.id, author.name)
    }

    feedItem.navDestination?.let { navDest ->
        if (navDest is Article) {
            when (val fullContent = ContentDetailCache.getOrFetch(context, navDest)) {
                is DataHolder.Answer -> {
                    return@withContext fullContent.author.let { Pair(it.id, it.name) }
                }
                is DataHolder.Article -> {
                    return@withContext fullContent.author.let { Pair(it.id, it.name) }
                }
                else -> {}
            }
        }
    }

    null
}

private suspend fun ensureContentForKeywordBlocking(
    context: Context,
    feedItem: FeedDisplayItem,
): Triple<String, String, String?>? = withContext(Dispatchers.IO) {
    val title = feedItem.title
    val summary = feedItem.summary ?: feedItem.feed?.target?.excerpt ?: ""
    var content = feedItem.content

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
