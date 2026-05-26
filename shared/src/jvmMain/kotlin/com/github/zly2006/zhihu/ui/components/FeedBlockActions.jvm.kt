package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.nlp.KeywordAnalyzerCore
import com.github.zly2006.zhihu.shared.nlp.KeywordWithWeight
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.fetchDesktopPinDetail
import com.github.zly2006.zhihu.ui.fetchDesktopQuestionDetailForFeedBlock
import com.github.zly2006.zhihu.viewmodel.DesktopArticleViewModelRuntime
import com.github.zly2006.zhihu.viewmodel.feed.removeFeedItemsByBlockedTopic
import com.github.zly2006.zhihu.viewmodel.feed.resolveFeedBlockAuthorInfo
import com.github.zly2006.zhihu.viewmodel.feed.resolveFeedKeywordBlockingContent
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeywordService
import com.github.zly2006.zhihu.viewmodel.filter.ContentDetailProvider
import com.github.zly2006.zhihu.viewmodel.filter.createBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.desktopKeywordSemanticMatcher
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import kotlinx.coroutines.launch

@Composable
actual fun rememberFeedBlockActions(): FeedBlockActions {
    val blocklistManager = rememberDesktopBlocklistManager()
    val userMessages = rememberUserMessageSink()
    val store = remember { DesktopAccountStore() }
    val contentDetailProvider = remember(store, userMessages) {
        desktopFeedBlockContentDetailProvider(store, userMessages)
    }
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

@Composable
actual fun rememberBlockUserConfirmRuntime(): BlockUserConfirmRuntime {
    val blocklistManager = rememberDesktopBlocklistManager()
    return remember(blocklistManager) {
        BlockUserConfirmRuntime(
            blockUser = { author ->
                blocklistManager.addBlockedUser(
                    userId = author.id,
                    userName = author.name,
                    urlToken = author.urlToken,
                    avatarUrl = author.avatarUrl,
                )
            },
        )
    }
}

@Composable
private fun rememberDesktopBlocklistManager() = remember {
    val databaseFile = desktopContentFilterDatabaseFile()
    databaseFile.parentFile?.mkdirs()
    val database = getContentFilterDatabase(databaseFile)
    database.createBlocklistManager()
}

private fun desktopFeedBlockContentDetailProvider(
    store: DesktopAccountStore,
    userMessages: UserMessageSink,
): ContentDetailProvider {
    val articleRuntime = DesktopArticleViewModelRuntime(store, userMessages)
    return ContentDetailProvider { destination ->
        fetchDesktopFeedBlockContentDetail(store, articleRuntime, destination)
    }
}

private suspend fun fetchDesktopFeedBlockContentDetail(
    store: DesktopAccountStore,
    articleRuntime: DesktopArticleViewModelRuntime,
    destination: NavDestination,
) = when (destination) {
    is Article -> articleRuntime.getContentDetail(destination)
    is Question -> fetchDesktopQuestionDetailForFeedBlock(store, destination)
    is Pin -> fetchDesktopPinDetail(store, destination)
    else -> null
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
