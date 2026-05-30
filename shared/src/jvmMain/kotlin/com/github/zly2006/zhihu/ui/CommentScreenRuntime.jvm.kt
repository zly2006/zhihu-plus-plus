package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.desktop.saveImageToDownloads
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberImagePreviewOpener
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import javax.imageio.ImageIO

@Composable
actual fun rememberCommentScreenRuntime(): CommentScreenRuntime {
    val scope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val openExternalUrl = rememberExternalUrlOpener()
    val openImagePreview = rememberImagePreviewOpener()
    return remember(scope, userMessages, openExternalUrl, openImagePreview) {
        val store = DesktopAccountStore()
        object : CommentScreenRuntime {
            override fun openImage(imageUrl: String) {
                openImagePreview(imageUrl)
            }

            override fun openImageInBrowser(imageUrl: String) {
                openExternalUrl(imageUrl)
            }

            override fun saveImage(imageUrl: String) {
                scope.launch {
                    runCatching {
                        store.saveImageToDownloads(imageUrl, "comment_image")
                    }.onSuccess { file ->
                        userMessages.showShortMessage("已保存图片: ${file.absolutePath}")
                    }.onFailure { error ->
                        userMessages.showShortMessage("保存失败: ${error.message}")
                    }
                }
            }

            override fun shareImage(imageUrl: String) {
                runCatching {
                    copyDesktopPlainText(imageUrl)
                    userMessages.showShortMessage("已复制图片链接")
                }.onFailure { error ->
                    userMessages.showShortMessage("分享失败: ${error.message}")
                }
            }

            override fun openExternalUrl(url: String) {
                openExternalUrl(url)
            }
        }
    }
}

@Composable
actual fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> =
    remember(emojiKeys) {
        emojiKeys
            .mapNotNull { emojiKey ->
                val imageFile = desktopEmojiFileByInlineKey(emojiKey) ?: return@mapNotNull null
                emojiKey to InlineTextContent(
                    placeholder = Placeholder(
                        width = 1.3.em,
                        height = 1.3.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    ),
                ) {
                    val image = remember(imageFile) {
                        runCatching {
                            ImageIO.read(imageFile)?.toComposeImageBitmap()
                        }.getOrNull()
                    }
                    image?.let {
                        Image(
                            bitmap = it,
                            contentDescription = emojiKey,
                            modifier = Modifier,
                        )
                    }
                }
            }.toMap()
    }

actual fun commentEmojiInlineKey(placeholder: String): String? {
    val fileName = desktopEmojiMapping()[placeholder] ?: return null
    return "emoji_$fileName"
}

actual fun Modifier.commentSelectionWorkaround(): Modifier = this

private fun desktopEmojiFileByInlineKey(emojiKey: String): File? {
    val fileName = emojiKey.removePrefix("emoji_")
    return desktopEmojiDirectories()
        .map { directory -> File(directory, fileName) }
        .firstOrNull { it.isFile }
}

private fun desktopEmojiMapping(): Map<String, String> {
    val mappingFile = desktopEmojiMappingFiles().firstOrNull { it.isFile } ?: return emptyMap()
    return runCatching {
        Json.decodeFromString<Map<String, String>>(mappingFile.readText())
    }.getOrDefault(emptyMap())
}

private fun desktopEmojiMappingFiles(): List<File> =
    desktopProjectRoots().map { root -> File(root, "misc/emoji_mapping.json") }

private fun desktopEmojiDirectories(): List<File> =
    desktopProjectRoots().map { root -> File(root, "misc/emojis") }

private fun desktopProjectRoots(): List<File> {
    val userDir = File(System.getProperty("user.dir")).absoluteFile
    return generateSequence(userDir) { it.parentFile }
        .take(6)
        .toList()
}
