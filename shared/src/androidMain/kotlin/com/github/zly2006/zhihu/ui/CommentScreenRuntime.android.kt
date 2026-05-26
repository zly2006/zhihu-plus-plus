package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.components.OpenImageDialog
import com.github.zly2006.zhihu.util.EmojiManager
import com.github.zly2006.zhihu.util.createEmojiInlineContent
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.util.shareImage
import kotlinx.coroutines.launch

@Composable
actual fun rememberCommentScreenRuntime(): CommentScreenRuntime {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        object : CommentScreenRuntime {
            override fun openImage(imageUrl: String) {
                OpenImageDialog(context, AccountData.httpClient(context), imageUrl).show()
            }

            override fun openImageInBrowser(imageUrl: String) {
                luoTianYiUrlLauncher(context, imageUrl.toUri())
            }

            override fun saveImage(imageUrl: String) {
                scope.launch {
                    saveImageToGallery(context, AccountData.httpClient(context), imageUrl)
                }
            }

            override fun shareImage(imageUrl: String) {
                scope.launch {
                    shareImage(context, AccountData.httpClient(context), imageUrl)
                }
            }

            override fun openExternalUrl(url: String) {
                luoTianYiUrlLauncher(context, url.toUri())
            }
        }
    }
}

@Composable
actual fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> =
    remember(emojiKeys) { createEmojiInlineContent(emojiKeys) }

actual fun commentEmojiInlineKey(placeholder: String): String? {
    val emojiPath = EmojiManager.getEmojiPath(placeholder) ?: return null
    val emojiFileName = emojiPath.substringAfterLast('/')
    return "emoji_$emojiFileName"
}

actual fun Modifier.commentSelectionWorkaround(): Modifier = fuckHonorService()
