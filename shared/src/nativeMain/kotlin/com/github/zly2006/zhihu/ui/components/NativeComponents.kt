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
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink

@Composable
actual fun rememberFeedBlockActions(): FeedBlockActions = remember {
    FeedBlockActions(
        handleBlockUser = { _, _, _ -> },
        handleBlockQuestionAuthor = { _, _, _ -> },
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

@Composable
actual fun rememberShareDialogRuntime(): ShareDialogRuntime {
    val copyPlainText = rememberPlainTextClipboard()
    val userMessages = rememberUserMessageSink()
    return remember(copyPlainText, userMessages) { clipboardShareDialogRuntime(copyPlainText, userMessages) }
}

@Composable
actual fun rememberSegmentedTextRuntime(): SegmentedTextRuntime = remember {
    SegmentedTextRuntime(
        toggleSegmentLike = { error("Segment like not available on iOS") },
    )
} // TODO: iOS 分段文本运行时
