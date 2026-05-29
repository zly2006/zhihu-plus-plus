package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink

@Composable
actual fun rememberCommentScreenRuntime(): CommentScreenRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        object : CommentScreenRuntime {
            override fun openImage(imageUrl: String) = openIosUrl(imageUrl)

            override fun openImageInBrowser(imageUrl: String) = openIosUrl(imageUrl)

            // TODO: iOS 图片保存
            override fun saveImage(imageUrl: String) {
                userMessages.showMessage("iOS 图片保存暂未实现")
            }

            // TODO: iOS 图片分享
            override fun shareImage(imageUrl: String) {
                userMessages.showMessage("iOS 图片分享暂未实现")
            }

            override fun openExternalUrl(url: String) = openIosUrl(url)
        }
    }
}

// TODO: iOS 表情内联内容
@Composable
actual fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> = emptyMap()

// TODO: iOS 表情内联 key
actual fun commentEmojiInlineKey(placeholder: String): String? = null

actual fun Modifier.commentSelectionWorkaround(): Modifier = this
