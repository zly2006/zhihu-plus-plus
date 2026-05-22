package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.nlp.KeywordWithWeight
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import kotlinx.coroutines.launch

data class FeedBlockActions(
    val handleBlockUser: (
        viewModel: BaseFeedViewModel,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<String, String>) -> Unit,
    ) -> Unit,
    val handleBlockTopic: (
        viewModel: BaseFeedViewModel,
        topicId: String,
        topicName: String,
    ) -> Unit,
    val handleBlockByKeywords: (
        viewModel: BaseFeedViewModel,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<FeedDisplayItem, Triple<String, String, String?>>) -> Unit,
    ) -> Unit,
)

@Composable
expect fun rememberFeedBlockActions(): FeedBlockActions

data class BlockByKeywordsRuntime(
    val extractKeywords: suspend (
        title: String,
        excerpt: String?,
    ) -> List<KeywordWithWeight>,
    val addNlpPhrase: suspend (String) -> Unit,
)

@Composable
expect fun rememberBlockByKeywordsRuntime(): BlockByKeywordsRuntime

@Composable
expect fun BlockUserConfirmDialog(
    showDialog: Boolean,
    userToBlock: Pair<String, String>?,
    displayItems: List<FeedDisplayItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
)

@Composable
fun BlockByKeywordsDialog(
    showDialog: Boolean,
    feedTitle: String,
    feedExcerpt: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val runtime = rememberBlockByKeywordsRuntime()

    var extractedKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var keywordInfoList by remember { mutableStateOf<List<KeywordWithWeight>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isAdding by remember { mutableStateOf(false) }

    LaunchedEffect(showDialog, feedTitle, feedExcerpt) {
        if (showDialog) {
            isLoading = true
            try {
                val keywordsWithWeight = runtime.extractKeywords(feedTitle, feedExcerpt)
                keywordInfoList = keywordsWithWeight
                extractedKeywords = keywordsWithWeight.take(8).map { it.keyword }
            } catch (e: Exception) {
                e.printStackTrace()
                userMessages.showShortMessage("提取关键词失败: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    BlockByKeywordsDialogContent(
        showDialog = showDialog,
        feedTitle = feedTitle,
        feedExcerpt = feedExcerpt,
        extractedKeywords = extractedKeywords,
        keywordInfoList = keywordInfoList,
        isLoading = isLoading,
        isAdding = isAdding,
        onDismiss = onDismiss,
        onConfirmPhrase = { phrase ->
            isAdding = true
            coroutineScope.launch {
                try {
                    runtime.addNlpPhrase(phrase)
                    userMessages.showShortMessage("已添加NLP屏蔽短语: $phrase")
                    onConfirm()
                } catch (e: Exception) {
                    e.printStackTrace()
                    userMessages.showShortMessage("添加失败: ${e.message}")
                } finally {
                    isAdding = false
                }
            }
        },
    )
}
