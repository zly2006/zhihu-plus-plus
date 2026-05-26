package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.imageio.ImageIO

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
