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

data class FeedContentFilterResult(
    val kept: List<FilterableContent>,
    val blocked: List<Pair<FilterableContent, String>>,
)

class FeedContentFilterPipeline(
    private val settings: FeedFilterSettings,
    private val blocklistService: BlocklistService,
    private val blockedKeywordService: BlockedKeywordService,
    private val htmlToText: (String) -> String = { it },
    private val onNlpBlocked: suspend (List<FilterableContent>) -> Unit = {},
) {
    suspend fun filter(contents: List<FilterableContent>): FeedContentFilterResult {
        val blocked = mutableListOf<Pair<FilterableContent, String>>()
        var filteredContents = contents

        if (settings.enableUserBlocking) {
            val (kept, removed) = filteredContents.partition { !blocklistService.isUserBlocked(it.authorId) }
            removed.forEach { blocked.add(it to "屏蔽作者：${it.authorName ?: it.authorId}") }
            filteredContents = kept
        }

        if (settings.enableKeywordBlocking) {
            val (kept, removed) = filteredContents.partition { content ->
                !blocklistService.containsBlockedKeyword(content.title) &&
                    !blocklistService.containsBlockedKeyword(content.summary ?: "") &&
                    !blocklistService.containsBlockedKeyword(content.content ?: "")
            }
            removed.forEach { blocked.add(it to "关键词屏蔽") }
            filteredContents = kept
        }

        if (settings.enableNlpBlocking) {
            val blockedThisRound = mutableListOf<FilterableContent>()
            val finalFilteredContents = mutableListOf<FilterableContent>()

            for (content in filteredContents) {
                val (shouldBlock, matchedKeywords) = blockedKeywordService.checkNLPBlockingWithWeight(
                    title = content.title,
                    excerpt = content.summary,
                    content = content.content?.let(htmlToText),
                    threshold = settings.nlpSimilarityThreshold,
                )

                if (!shouldBlock) {
                    finalFilteredContents.add(content)
                } else {
                    blockedKeywordService.recordBlockedContent(
                        contentId = content.contentId,
                        contentType = content.contentType,
                        title = content.title,
                        excerpt = content.summary ?: "",
                        authorName = content.authorName,
                        authorId = content.authorId,
                        matchedKeywords = matchedKeywords,
                    )
                    val keywordNames = matchedKeywords.joinToString("、") { it.keyword }
                    blocked.add(content to "NLP语义屏蔽：$keywordNames")
                    blockedThisRound.add(content)
                }
            }

            if (blockedThisRound.isNotEmpty()) {
                onNlpBlocked(blockedThisRound)
            }

            filteredContents = finalFilteredContents
        }

        if (settings.enableTopicBlocking) {
            filteredContents = filteredContents.filter { content ->
                val topicIds = extractTopicIds(content.raw)
                val kept = blocklistService.countBlockedTopics(topicIds) < settings.topicBlockingThreshold
                if (!kept) {
                    val topicName = topicIds
                        ?.first { topicId ->
                            blocklistService.isTopicBlocked(topicId)
                        }?.let { topicId ->
                            blocklistService.getTopicName(topicId)
                        }
                    blocked.add(content to "屏蔽主题：$topicName")
                }
                kept
            }
        }

        return FeedContentFilterResult(filteredContents, blocked)
    }
}
