package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberFeedBlockActions(): FeedBlockActions = remember {
    FeedBlockActions(
        handleBlockUser = { _, _, _ -> },
        handleBlockTopic = { _, _, _ -> },
        handleBlockByKeywords = { _, _, _ -> },
    )
} // TODO: iOS Feed 屏蔽操作

@Composable
actual fun rememberBlockByKeywordsRuntime(): BlockByKeywordsRuntime = remember {
    BlockByKeywordsRuntime(
        extractKeywords = { _, _ -> emptyList() },
        addNlpPhrase = { },
    )
} // TODO: iOS 关键词屏蔽运行时
