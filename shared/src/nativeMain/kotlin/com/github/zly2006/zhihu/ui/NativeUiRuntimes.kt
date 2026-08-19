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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import com.github.zly2006.zhihu.account.IosAccountStore
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.platform.UserMessageSink
import com.github.zly2006.zhihu.platform.nativeAppPrivateDirectoryPath
import com.github.zly2006.zhihu.platform.nativeAppVersionName
import com.github.zly2006.zhihu.platform.nativeBundledResourcePath
import com.github.zly2006.zhihu.platform.nativeChooseBlocklistImportFilePath
import com.github.zly2006.zhihu.platform.nativeIsDesktop
import com.github.zly2006.zhihu.platform.openNativeExternalUrl
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.platform.requestNativeQrLogin
import com.github.zly2006.zhihu.ui.subscreens.DUO3_TIQIAN_MARKDOWN_PREFERENCE_KEY
import com.github.zly2006.zhihu.viewmodel.NativePaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationEnvironment
import com.github.zly2006.zhihu.viewmodel.filter.encodeBlocklistBackup
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.importBlocklistBackupFromJsonText
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import org.jetbrains.skia.Image as SkiaImage

@Composable
actual fun rememberArticleTtsState(): TtsState = NativeArticleSpeechController.currentState

@Composable
actual fun rememberArticleSpeechToggler(): (title: String, content: String) -> Unit {
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val ttsState = NativeArticleSpeechController.currentState
    return remember(userMessages, coroutineScope, ttsState) {
        { title, content ->
            if (ttsState.isSpeaking) {
                NativeArticleSpeechController.stopSpeaking()
            } else if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                coroutineScope.launch {
                    try {
                        val textToRead = withContext(Dispatchers.Default) {
                            articleSpeechText(title, content)
                        }
                        if (textToRead.isNotBlank()) {
                            if (NativeArticleSpeechController.startSpeaking(textToRead)) {
                                userMessages.showMessage("开始朗读：$title")
                            } else {
                                userMessages.showMessage("朗读启动失败")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            userMessages.showMessage("朗读失败：${e.message}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
actual fun rememberArticleBrowserOpener(): (Article) -> Unit = remember {
    { article -> openNativeExternalUrl(articleWebUrl(article)) }
}

@Composable
actual fun rememberNotificationEnvironment(
    settingsStore: NotificationSettingsStore,
): NotificationEnvironment = remember(settingsStore) { NativePaginationEnvironment(notificationSettingsStore = settingsStore) }

@Composable
actual fun rememberArticleHost(): ArticleHost? = null

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
) {
    RenderMarkdown(
        html = html,
        modifier = Modifier,
        selectable = true,
        enableScroll = false,
        header = {},
        footer = {},
        useTiqianRenderer = !nativeIsDesktop &&
            rememberSettingsStore().getBoolean(DUO3_TIQIAN_MARKDOWN_PREFERENCE_KEY, false),
    )
}

actual fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = this

@Composable
actual fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> =
    remember(emojiKeys) {
        emojiKeys
            .mapNotNull { emojiKey ->
                val imageBytes = nativeEmojiBytesByInlineKey(emojiKey) ?: return@mapNotNull null
                emojiKey to InlineTextContent(
                    placeholder = Placeholder(
                        width = 1.3.em,
                        height = 1.3.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    ),
                ) {
                    val image = remember(imageBytes) {
                        runCatching { SkiaImage.makeFromEncoded(imageBytes).toComposeImageBitmap() }.getOrNull()
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
    nativeEmojiMapping.mapNotNull { (placeholder, fileName) ->
        val inlineKey = "emoji_$fileName"
        nativeEmojiBytesByInlineKey(inlineKey)?.let {
            CommentEmoji(placeholder = placeholder, inlineKey = inlineKey)
        }
    }
}

actual fun commentEmojiInlineKey(placeholder: String): String? =
    nativeEmojiMapping[placeholder]?.let { fileName -> "emoji_$fileName" }

actual fun Modifier.commentSelectionWorkaround(): Modifier = this

private val nativeEmojiMapping: Map<String, String> by lazy {
    val mappingPath = nativeBundledResourcePath("misc/emoji_mapping.json") ?: return@lazy emptyMap()
    val mappingBytes = readNativeFileBytes(mappingPath) ?: return@lazy emptyMap()
    runCatching {
        Json.decodeFromString<Map<String, String>>(mappingBytes.decodeToString())
    }.getOrDefault(emptyMap())
}

private fun nativeEmojiBytesByInlineKey(emojiKey: String): ByteArray? {
    val fileName = emojiKey.removePrefix("emoji_")
    val imagePath = nativeBundledResourcePath("misc/emojis/$fileName") ?: return null
    return readNativeFileBytes(imagePath)
}

@OptIn(ExperimentalForeignApi::class)
private fun readNativeFileBytes(filePath: String): ByteArray? {
    val data = NSFileManager.defaultManager.contentsAtPath(filePath) ?: return null
    return data.bytes?.reinterpret<ByteVar>()?.readBytes(data.length.toInt())
}

@Composable
actual fun rememberHomeIsDebuggable(): Boolean = nativeIsDesktop

@Composable
actual fun rememberAccountSettingsAccountState(): androidx.compose.runtime.State<AccountSettingsAccountState> {
    val accountStore = remember { IosAccountStore() }
    val account = accountStore.accountState.collectAsState()
    return remember(account) {
        derivedStateOf {
            val session = account.value
            AccountSettingsAccountState(
                login = session.login,
                username = session.username,
                avatarUrl = session.profile?.avatarUrl,
                id = session.profile?.id ?: "",
                urlToken = session.profile?.urlToken,
            )
        }
    }
}

@Composable
actual fun rememberAccountQrLoginRequester(): () -> Unit = remember { ::requestNativeQrLogin }

@Composable
actual fun rememberAppVersionInfo(): String = nativeAppVersionName

@Composable
actual fun ZhihuHtmlWebViewContent(html: String) = Unit

actual fun supportsZhihuHtmlWebView(): Boolean = false

@Composable
actual fun rememberBlocklistRuleImporter(
    userMessages: UserMessageSink,
): (((String) -> Unit) -> Unit) {
    val database = remember { getContentFilterDatabase() }
    val coroutineScope = rememberCoroutineScope()
    return remember(database, coroutineScope, userMessages) {
        { onImported ->
            val selectedFilePath = nativeChooseBlocklistImportFilePath()
            if (selectedFilePath != null) {
                coroutineScope.launch {
                    try {
                        val text = readNativeFileBytes(selectedFilePath)?.decodeToString()
                            ?: error("读取文件失败")
                        val summary = importBlocklistBackupFromJsonText(
                            keywordDao = database.blockedKeywordDao(),
                            userDao = database.blockedUserDao(),
                            questionAuthorDao = database.blockedQuestionAuthorDao(),
                            topicDao = database.blockedTopicDao(),
                            text = text,
                        )
                        onImported(summary)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        userMessages.showShortMessage("导入失败：${error.message}")
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
actual fun rememberBlocklistRuleExporter(): suspend () -> String {
    val database = remember { getContentFilterDatabase() }
    return remember(database) {
        suspend {
            val text = encodeBlocklistBackup(
                keywordDao = database.blockedKeywordDao(),
                userDao = database.blockedUserDao(),
                questionAuthorDao = database.blockedQuestionAuthorDao(),
                topicDao = database.blockedTopicDao(),
            )
            val outputDirectory = nativeAppPrivateDirectoryPath()
            val outputFile = "$outputDirectory/zhihupp_blocklist.json"
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(outputDirectory)) {
                fileManager.createDirectoryAtPath(
                    outputDirectory,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null,
                )
            }
            val data = checkNotNull(
                NSString.create(string = text).dataUsingEncoding(NSUTF8StringEncoding),
            ) { "无法编码导出内容" }
            check(fileManager.createFileAtPath(outputFile, contents = data, attributes = null)) {
                "无法写入导出文件"
            }
            "已导出到 $outputFile"
        }
    }
}

@Composable
actual fun QuestionDetailWebViewContent(questionId: Long, html: String) = Unit

actual fun supportsQuestionDetailWebView(): Boolean = false

actual fun Modifier.questionSelectionWorkaround(): Modifier = this

@Composable
actual fun ArticleImmersiveModeEffect(immersive: Boolean) = Unit

@Composable
actual fun LeaveImmersiveModeCleanup() = Unit
