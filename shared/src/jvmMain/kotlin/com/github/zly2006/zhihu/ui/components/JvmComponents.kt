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

package com.github.zly2006.zhihu.ui.components
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.shared.data.SegmentInfoMeta
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.signedFetchJson
import com.github.zly2006.zhihu.shared.nlp.KeywordAnalyzerCore
import com.github.zly2006.zhihu.shared.nlp.KeywordWithWeight
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.SegmentHighlightSpan
import com.github.zly2006.zhihu.viewmodel.DesktopPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.removeFeedItemsByBlockedTopic
import com.github.zly2006.zhihu.viewmodel.feed.resolveFeedBlockAuthorInfo
import com.github.zly2006.zhihu.viewmodel.feed.resolveFeedKeywordBlockingContent
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeywordService
import com.github.zly2006.zhihu.viewmodel.filter.ContentDetailProvider
import com.github.zly2006.zhihu.viewmodel.filter.createBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.desktopKeywordSemanticMatcher
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.getOrFetchContentDetail
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.launch

@Composable
actual fun rememberFeedBlockActions(): FeedBlockActions {
    val blocklistManager = getContentFilterDatabase().createBlocklistManager()
    val userMessages = rememberUserMessageSink()
    val store = remember { DesktopAccountStore() }
    val contentDetailProvider = remember(store) { desktopFeedBlockContentDetailProvider(store) }
    return remember(blocklistManager, userMessages, contentDetailProvider) {
        FeedBlockActions(
            handleBlockUser = { viewModel, feedItem, onShowDialog ->
                viewModel.viewModelScope.launch {
                    val authorInfo = resolveFeedBlockAuthorInfo(feedItem, contentDetailProvider)
                    if (authorInfo != null) {
                        onShowDialog(authorInfo)
                    } else {
                        userMessages.showShortMessage("无法获取屏蔽用户所需的数据，请尝试进入内容详情页操作")
                    }
                }
            },
            handleBlockTopic = { viewModel, topicId, topicName ->
                viewModel.viewModelScope.launch {
                    try {
                        blocklistManager.addBlockedTopic(topicId, topicName)
                        userMessages.showShortMessage("已屏蔽主题「$topicName」")
                        removeFeedItemsByBlockedTopic(viewModel, topicId)
                    } catch (e: Exception) {
                        userMessages.showShortMessage("屏蔽失败: ${e.message}")
                    }
                }
            },
            handleBlockByKeywords = { viewModel, feedItem, onShowDialog ->
                viewModel.viewModelScope.launch {
                    val contentInfo = resolveFeedKeywordBlockingContent(feedItem, contentDetailProvider)
                    if (contentInfo != null) {
                        onShowDialog(feedItem to contentInfo)
                    } else {
                        userMessages.showShortMessage("无法获取关键词屏蔽所需的数据，请尝试进入内容详情页操作")
                    }
                }
            },
        )
    }
}

@Composable
actual fun rememberBlockByKeywordsRuntime(): BlockByKeywordsRuntime = remember {
    val blockedKeywordService = createDesktopBlockedKeywordService()
    BlockByKeywordsRuntime(
        extractKeywords = { title, excerpt ->
            KeywordAnalyzerCore.extractFromFeedWithWeight(
                title = title,
                excerpt = excerpt,
                content = null,
                topN = 10,
                extractor = ::extractDesktopKeywordsWithWeight,
            )
        },
        addNlpPhrase = { phrase ->
            blockedKeywordService.addNLPPhrase(phrase)
        },
    )
}

private fun desktopFeedBlockContentDetailProvider(
    store: DesktopAccountStore,
): ContentDetailProvider {
    val environment = DesktopPaginationEnvironment(store)
    return ContentDetailProvider { destination -> environment.getOrFetchContentDetail(destination) }
}

private fun extractDesktopKeywordsWithWeight(
    text: String,
    topN: Int,
): List<KeywordWithWeight> {
    val words = Regex("[\\p{L}\\p{N}_\\u4e00-\\u9fff]{2,}")
        .findAll(text)
        .map { it.value.trim() }
        .filter { it.length >= 2 }
        .filter { !KeywordAnalyzerCore.isStopWord(it) }
        .filter { !KeywordAnalyzerCore.isNumberOnly(it) }
        .groupingBy { it }
        .eachCount()

    return words
        .map { (keyword, count) -> KeywordWithWeight(keyword, count.toDouble()) }
        .sortedByDescending { it.weight }
        .take(topN)
}

private fun createDesktopBlockedKeywordService(): BlockedKeywordService {
    val databaseFile = desktopContentFilterDatabaseFile()
    databaseFile.parentFile?.mkdirs()
    val database = getContentFilterDatabase(databaseFile)
    return BlockedKeywordService(
        keywordDao = database.blockedKeywordDao(),
        recordDao = database.blockedContentRecordDao(),
        semanticMatcher = desktopKeywordSemanticMatcher,
    )
}

@Composable
actual fun rememberSegmentedTextRuntime(): SegmentedTextRuntime = remember {
    val store = DesktopAccountStore()
    SegmentedTextRuntime(
        toggleSegmentLike = { highlight ->
            toggleSegmentLike(store, highlight)
        },
    )
}

private suspend fun toggleSegmentLike(
    store: DesktopAccountStore,
    highlight: SegmentHighlightSpan,
): SegmentInfoMeta {
    val contentId = highlight.contentId ?: return highlight.meta
    val targetType = highlight.contentType ?: return highlight.meta
    val url = "https://www.zhihu.com/api/v4/reaction/${targetType}s/$contentId/segment_reaction"
    if (store.load().cookies["d_c0"] == null) return highlight.meta

    return if (highlight.meta.isLike) {
        val body = buildSegmentUnlikeBody(highlight)
        store.signedFetchJson(url) {
            method = HttpMethod.Delete
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        updateSegmentMetaAfterUnlike(highlight)
    } else {
        val body = buildSegmentLikeBody(highlight)
        val response = store.signedFetchJson(url) {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        updateSegmentMetaAfterLike(highlight, response)
    }
}

@Composable
actual fun rememberShareDialogRuntime(): ShareDialogRuntime {
    val copyPlainText = rememberPlainTextClipboard()
    val userMessages = rememberUserMessageSink()
    return remember(copyPlainText, userMessages) { clipboardShareDialogRuntime(copyPlainText, userMessages) }
}
