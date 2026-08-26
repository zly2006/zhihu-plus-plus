/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import com.github.zly2006.zhihu.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.platform.UserMessageSink
import com.github.zly2006.zhihu.platform.platformName
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.subscreens.desktopVersionName
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.viewmodel.DesktopPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.encodeBlocklistBackup
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.importBlocklistBackupFromJsonText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberArticleTtsState(): TtsState = DesktopArticleSpeechController.currentTtsState

@Composable
actual fun rememberArticleSpeechToggler(): ArticleSpeechToggler {
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    return remember(userMessages, coroutineScope) {
        object : ArticleSpeechToggler {
            override fun invoke(title: String, content: String) {
                DesktopArticleSpeechController.toggleSpeech(title, content, coroutineScope, userMessages)
            }
        }
    }
}

@Composable
actual fun rememberArticleBrowserOpener(): ArticleBrowserOpener {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        object : ArticleBrowserOpener {
            override fun invoke(article: Article) {
                if (openDesktopExternalUrl(articleWebUrl(article))) {
                    userMessages.showMessage("已发送到浏览器")
                }
            }
        }
    }
}

private object DesktopArticleSpeechController {
    private var speechProcess: Process? = null
    var currentTtsState by mutableStateOf(
        if (isDesktopSpeechCommandAvailable()) TtsState.Ready else TtsState.Error,
    )
        private set

    fun toggleSpeech(
        title: String,
        content: String,
        coroutineScope: kotlinx.coroutines.CoroutineScope,
        userMessages: UserMessageSink,
    ) {
        if (currentTtsState.isSpeaking) {
            stopSpeaking()
        } else if (currentTtsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
            coroutineScope.launch {
                try {
                    val textToRead = withContext(Dispatchers.IO) {
                        articleSpeechText(title, content)
                    }
                    if (textToRead.isNotBlank()) {
                        speakText(textToRead, title, userMessages)
                    }
                } catch (e: Exception) {
                    currentTtsState = TtsState.Error
                    userMessages.showMessage("朗读失败：${e.message}")
                }
            }
        }
    }

    private suspend fun speakText(
        text: String,
        title: String,
        userMessages: UserMessageSink,
    ) {
        currentTtsState = TtsState.LoadingText
        val process = withContext(Dispatchers.IO) {
            ProcessBuilder("say")
                .redirectErrorStream(true)
                .start()
        }
        speechProcess = process
        currentTtsState = TtsState.Speaking
        userMessages.showMessage("开始朗读：$title")
        val exitCode = withContext(Dispatchers.IO) {
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(text)
            }
            process.waitFor()
        }
        if (speechProcess == process) {
            speechProcess = null
            currentTtsState = if (exitCode == 0) TtsState.Ready else TtsState.Error
        }
    }

    private fun stopSpeaking() {
        speechProcess?.destroy()
        speechProcess = null
        currentTtsState = TtsState.Ready
    }
}

private fun isDesktopSpeechCommandAvailable(): Boolean =
    runCatching {
        ProcessBuilder("sh", "-c", "command -v say >/dev/null 2>&1")
            .start()
            .waitFor() == 0
    }.getOrDefault(false)

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

@Composable
actual fun rememberCommentEmojis(): List<CommentEmoji> = remember {
    desktopEmojiMapping().mapNotNull { (placeholder, fileName) ->
        val inlineKey = "emoji_$fileName"
        desktopEmojiFileByInlineKey(inlineKey)?.let {
            CommentEmoji(placeholder = placeholder, inlineKey = inlineKey)
        }
    }
}

actual fun commentEmojiInlineKey(placeholder: String): String? =
    desktopEmojiMapping()[placeholder]?.let { fileName -> "emoji_$fileName" }

actual fun Modifier.commentSelectionWorkaround(): Modifier = this

private fun desktopEmojiFileByInlineKey(emojiKey: String): File? {
    val fileName = emojiKey.removePrefix("emoji_")
    return desktopProjectRoots()
        .map { root -> File(root, "misc/emojis/$fileName") }
        .firstOrNull { it.isFile }
}

private fun desktopEmojiMapping(): Map<String, String> {
    val mappingFile = desktopProjectRoots()
        .map { root -> File(root, "misc/emoji_mapping.json") }
        .firstOrNull { it.isFile } ?: return emptyMap()
    return runCatching {
        Json.decodeFromString<Map<String, String>>(mappingFile.readText())
    }.getOrDefault(emptyMap())
}

private fun desktopProjectRoots(): List<File> =
    generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .take(6)
        .toList()

@Composable
actual fun rememberHomeIsDebuggable(): Boolean = true

@Composable
actual fun rememberBlocklistRuleImporter(
    userMessages: UserMessageSink,
    onImported: (String) -> Unit,
): BlocklistRuleImporter {
    val database = remember {
        val databaseFile = desktopContentFilterDatabaseFile()
        databaseFile.parentFile?.mkdirs()
        getContentFilterDatabase(databaseFile)
    }
    val coroutineScope = rememberCoroutineScope()
    val currentOnImported by rememberUpdatedState(onImported)
    return remember(database, userMessages, coroutineScope) {
        object : BlocklistRuleImporter {
            override fun invoke() {
                val selectedFile = chooseBlocklistImportFile()
                if (selectedFile != null) {
                    coroutineScope.launch {
                        try {
                            val summary = importBlocklistBackupFromJsonText(
                                keywordDao = database.blockedKeywordDao(),
                                userDao = database.blockedUserDao(),
                                questionAuthorDao = database.blockedQuestionAuthorDao(),
                                topicDao = database.blockedTopicDao(),
                                text = selectedFile.readText(),
                            )
                            currentOnImported(summary)
                        } catch (e: Exception) {
                            Log.e("BlocklistSettings", "Failed to import blocklist", e)
                            userMessages.showShortMessage("导入失败: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
actual fun rememberBlocklistRuleExporter(): BlocklistRuleExporter {
    val database = remember {
        val databaseFile = desktopContentFilterDatabaseFile()
        databaseFile.parentFile?.mkdirs()
        getContentFilterDatabase(databaseFile)
    }
    return remember(database) {
        object : BlocklistRuleExporter {
            override suspend fun invoke(): String {
                val file = File(desktopContentFilterDatabaseFile().parentFile, "zhihupp_blocklist.json")
                file.writeText(
                    encodeBlocklistBackup(
                        keywordDao = database.blockedKeywordDao(),
                        userDao = database.blockedUserDao(),
                        questionAuthorDao = database.blockedQuestionAuthorDao(),
                        topicDao = database.blockedTopicDao(),
                    ),
                )
                return "已导出到 ${file.absolutePath}"
            }
        }
    }
}

private fun chooseBlocklistImportFile(): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "导入屏蔽规则"
        fileSelectionMode = JFileChooser.FILES_ONLY
        fileFilter = FileNameExtensionFilter("JSON 或文本文件", "json", "txt")
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else {
        null
    }
}

@Composable
actual fun rememberAppVersionInfo(): String = desktopVersionName()

@Composable
actual fun consumePendingCommentId(content: com.github.zly2006.zhihu.navigation.NavDestination): String? = null

@Composable
actual fun ArticleWebViewContent(
    article: Article,
    html: String,
    title: String,
    scrollState: ScrollState,
    rememberedScrollY: Int,
    rememberedScrollYSync: Boolean,
    onRememberedScrollYSyncChange: (Boolean) -> Unit,
    onImageLoadFailed: () -> Unit,
    onDoubleTap: () -> Unit,
): Unit = error("$platformName 暂不支持文章 WebView 渲染")

actual fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = this

/**
 * 桌面端不支持 WebView
 */
@Composable
actual fun ZhihuHtmlWebViewContent(html: String): Unit = error("$platformName 暂不支持 HTML WebView 渲染")

actual val isLegacyWebViewSupported: Boolean = false

@Composable
actual fun rememberNotificationEnvironment(
    settingsStore: NotificationSettingsStore,
): com.github.zly2006.zhihu.viewmodel.NotificationEnvironment = remember(settingsStore) {
    DesktopPaginationEnvironment(
        notificationSettingsStore = settingsStore,
    )
}

@Composable
actual fun QuestionDetailWebViewContent(
    questionId: Long,
    html: String,
) {
    error("$platformName 暂不支持问题详情 WebView 渲染")
}

actual fun Modifier.questionSelectionWorkaround(): Modifier = this

@Composable
actual fun ArticleImmersiveModeEffect(immersive: Boolean) = Unit

@Composable
actual fun LeaveImmersiveModeCleanup() = Unit
