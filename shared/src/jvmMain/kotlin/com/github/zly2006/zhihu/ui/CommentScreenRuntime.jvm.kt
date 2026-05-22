package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import java.awt.Desktop
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
actual fun rememberCommentScreenRuntime(): CommentScreenRuntime =
    remember {
        object : CommentScreenRuntime {
            override fun openImage(imageUrl: String) {
                openExternalUrl(imageUrl)
            }

            override fun openImageInBrowser(imageUrl: String) {
                openExternalUrl(imageUrl)
            }

            override fun saveImage(imageUrl: String) {
            }

            override fun shareImage(imageUrl: String) {
            }

            override fun openExternalUrl(url: String) {
                runCatching {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(url))
                    }
                }
            }
        }
    }

@Composable
actual fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> = emptyMap()

actual fun commentEmojiInlineKey(placeholder: String): String? = null

actual fun Modifier.commentSelectionWorkaround(): Modifier = this

actual object YMDHMS {
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

    actual fun format(date: Any): String = formatter.format(date as Date)
}
