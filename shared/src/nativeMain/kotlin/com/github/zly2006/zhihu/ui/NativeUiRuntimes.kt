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
import com.github.zly2006.zhihu.platform.nativeAppVersionName
import com.github.zly2006.zhihu.platform.nativeBundledResourcePath
import com.github.zly2006.zhihu.platform.nativeIsDesktop
import com.github.zly2006.zhihu.platform.nativePlatformName
import com.github.zly2006.zhihu.platform.openNativeExternalUrl
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.platform.requestNativeQrLogin
import com.github.zly2006.zhihu.ui.subscreens.DUO3_TIQIAN_MARKDOWN_PREFERENCE_KEY
import com.github.zly2006.zhihu.viewmodel.NativePaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationEnvironment
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.serialization.json.Json
import platform.Foundation.NSFileManager
import org.jetbrains.skia.Image as SkiaImage

@Composable
actual fun rememberArticleTtsState(): TtsState = TtsState.Ready

@Composable
actual fun rememberArticleSpeechToggler(): (title: String, content: String) -> Unit {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        { _, _ -> userMessages.showMessage("$nativePlatformName TTS 暂未实现") }
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
        useTiqianRenderer = rememberSettingsStore()
            .getBoolean(DUO3_TIQIAN_MARKDOWN_PREFERENCE_KEY, false),
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
actual fun ZhihuHtmlWebViewContent(html: String) = Unit // TODO: iOS HTML WebView 实现

actual fun supportsZhihuHtmlWebView(): Boolean = false

@Composable
actual fun rememberBlocklistRuleImporter(
    userMessages: UserMessageSink,
): (((String) -> Unit) -> Unit) = remember(userMessages) {
    { _ -> userMessages.showMessage("$nativePlatformName 导入规则暂未实现") }
}

@Composable
actual fun rememberBlocklistRuleExporter(): suspend () -> String = remember {
    { "" } // TODO: iOS 导出规则
}

@Composable
actual fun QuestionDetailWebViewContent(questionId: Long, html: String) = Unit // TODO: iOS 问题 WebView 实现

actual fun supportsQuestionDetailWebView(): Boolean = false

actual fun Modifier.questionSelectionWorkaround(): Modifier = this

@Composable
actual fun ArticleImmersiveModeEffect(immersive: Boolean) = Unit

@Composable
actual fun LeaveImmersiveModeCleanup() = Unit
