package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.nlp.KeywordAnalyzerCore
import com.github.zly2006.zhihu.shared.nlp.KeywordWithWeight
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeywordService
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistService
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import kotlinx.coroutines.launch
import java.io.File

@Composable
actual fun rememberFeedBlockActions(): FeedBlockActions {
    val blocklistService = rememberDesktopBlocklistService()
    return remember(blocklistService) {
        FeedBlockActions(
            handleBlockUser = { viewModel, feedItem, onShowDialog ->
                viewModel.viewModelScope.launch {
                    val authorInfo = ensureAuthorInfo(feedItem)
                    if (authorInfo != null) {
                        onShowDialog(authorInfo)
                    } else {
                        println("无法获取屏蔽用户所需的数据，请尝试进入内容详情页操作")
                    }
                }
            },
            handleBlockTopic = { viewModel, topicId, topicName ->
                viewModel.viewModelScope.launch {
                    try {
                        blocklistService.addBlockedTopic(topicId, topicName)
                        println("已屏蔽主题「$topicName」")
                        viewModel.displayItems.removeAll {
                            val topics = when (val content = it.raw) {
                                is DataHolder.Answer -> content.question.topics
                                is DataHolder.Article -> content.topics
                                is DataHolder.Question -> content.topics
                                else -> null
                            }
                            topics?.any { topic -> topic.id == topicId } == true
                        }
                    } catch (e: Exception) {
                        println("屏蔽失败: ${e.message}")
                    }
                }
            },
            handleBlockByKeywords = { viewModel, feedItem, onShowDialog ->
                viewModel.viewModelScope.launch {
                    val contentInfo = ensureContentForKeywordBlocking(feedItem)
                    if (contentInfo != null) {
                        onShowDialog(feedItem to contentInfo)
                    } else {
                        println("无法获取关键词屏蔽所需的数据，请尝试进入内容详情页操作")
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

@Composable
actual fun BlockUserConfirmDialog(
    showDialog: Boolean,
    userToBlock: Pair<String, String>?,
    displayItems: List<FeedDisplayItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val blocklistService = rememberDesktopBlocklistService()
    val coroutineScope = rememberCoroutineScope()
    BlockUserConfirmDialogContent(
        showDialog = showDialog,
        userToBlock = userToBlock,
        displayItems = displayItems,
        onDismiss = onDismiss,
        onConfirmBlock = { author ->
            coroutineScope.launch {
                try {
                    blocklistService.addBlockedUser(
                        userId = author.id,
                        userName = author.name,
                        urlToken = author.urlToken,
                        avatarUrl = author.avatarUrl,
                    )
                    onConfirm()
                    println("已屏蔽用户：${author.name}")
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("屏蔽用户失败: ${e.message}")
                }
            }
        },
    )
}

@Composable
private fun rememberDesktopBlocklistService(): BlocklistService = remember {
    val databaseFile = File(System.getProperty("user.home"), ".zhihu-plus/content-filter.db")
    databaseFile.parentFile?.mkdirs()
    val database = getContentFilterDatabase(databaseFile)
    BlocklistService(
        keywordDao = database.blockedKeywordDao(),
        userDao = database.blockedUserDao(),
        topicDao = database.blockedTopicDao(),
    )
}

private fun ensureAuthorInfo(feedItem: FeedDisplayItem): Pair<String, String>? {
    feedItem.feed?.target?.author?.let { author ->
        return Pair(author.id, author.name)
    }

    return when (val content = feedItem.raw) {
        is DataHolder.Answer -> content.author.let { Pair(it.id, it.name) }
        is DataHolder.Article -> content.author.let { Pair(it.id, it.name) }
        else -> null
    }
}

private fun ensureContentForKeywordBlocking(feedItem: FeedDisplayItem): Triple<String, String, String?>? {
    val title = feedItem.title
    val summary = feedItem.summary ?: feedItem.feed?.target?.excerpt ?: ""
    val content = feedItem.content ?: when (val fullContent = feedItem.raw) {
        is DataHolder.Answer -> fullContent.content
        is DataHolder.Article -> fullContent.content
        is DataHolder.Question -> fullContent.detail
        else -> null
    }

    return if (title.isNotEmpty() || summary.isNotEmpty() || content != null) {
        Triple(title, summary, content)
    } else {
        null
    }
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
    val databaseFile = File(System.getProperty("user.home"), ".zhihu-plus/content-filter.db")
    databaseFile.parentFile?.mkdirs()
    val database = getContentFilterDatabase(databaseFile)
    return BlockedKeywordService(
        keywordDao = database.blockedKeywordDao(),
        recordDao = database.blockedContentRecordDao(),
        semanticMatcher = { _, _, _ -> emptyList() },
    )
}
