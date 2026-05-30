package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface CommentScreenRuntime {
    fun saveImage(imageUrl: String)

    fun shareImage(imageUrl: String)
}

@Composable
expect fun rememberCommentScreenRuntime(): CommentScreenRuntime

@Composable
expect fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent>

expect fun commentEmojiInlineKey(placeholder: String): String?

expect fun Modifier.commentSelectionWorkaround(): Modifier
