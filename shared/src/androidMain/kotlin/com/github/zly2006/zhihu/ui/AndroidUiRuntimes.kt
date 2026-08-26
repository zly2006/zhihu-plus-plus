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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.zly2006.zhihu.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.filter.ContentOpenFrom
import com.github.zly2006.zhihu.filter.TrackedContentIdentity
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.CommentHolder
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.platform.UserMessageSink
import com.github.zly2006.zhihu.platform.androidSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.reading.AndroidReadingPlayerBridge
import com.github.zly2006.zhihu.reading.ContentReadingService
import com.github.zly2006.zhihu.reading.ReadingContentType
import com.github.zly2006.zhihu.reading.ReadingPlaybackStatus
import com.github.zly2006.zhihu.reading.ReadingPreferences
import com.github.zly2006.zhihu.reading.ReadingQueueItem
import com.github.zly2006.zhihu.reading.ReadingStartRequest
import com.github.zly2006.zhihu.reading.ReadingTemplateField
import com.github.zly2006.zhihu.reading.loadReadingPlaybackSpeed
import com.github.zly2006.zhihu.ui.article.prepareContentDocument
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.EmojiManager
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.util.OpenInBrowser
import com.github.zly2006.zhihu.util.createEmojiInlineContent
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.viewmodel.SharedAndroidNotificationEnvironment
import com.github.zly2006.zhihu.viewmodel.filter.encodeBlocklistBackup
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.importBlocklistBackupFromJsonText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File

private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"

@Composable
actual fun rememberAppVersionInfo(): String = LocalContext.current.zhihuVersionInfo()

private fun Context.zhihuVersionInfo(): String {
    val versionName = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull() ?: "unknown"
    val appInfo = runCatching {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    }.getOrNull()
    val metaData = appInfo?.metaData
    val buildType = metaData?.getString("com.github.zly2006.zhihu.BUILD_TYPE")
        ?: if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) "debug" else "release"
    val gitHash = metaData?.getString("com.github.zly2006.zhihu.GIT_HASH") ?: "unknown"
    return "$versionName $buildType, $gitHash"
}

@Composable
actual fun rememberArticleTtsState(): TtsState {
    val state by AndroidReadingPlayerBridge.state.collectAsState()
    return when (state.status) {
        ReadingPlaybackStatus.Idle -> TtsState.Ready
        ReadingPlaybackStatus.Initializing -> TtsState.Initializing
        ReadingPlaybackStatus.Loading -> TtsState.LoadingText
        ReadingPlaybackStatus.Playing -> TtsState.Speaking
        ReadingPlaybackStatus.Paused -> TtsState.Paused
        ReadingPlaybackStatus.Error -> TtsState.Error
    }
}

@Composable
actual fun rememberArticleSpeechToggler(): ArticleSpeechToggler {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val ttsState = rememberArticleTtsState()
    return remember(context, coroutineScope, userMessages, ttsState) {
        object : ArticleSpeechToggler {
            override fun invoke(title: String, content: String) {
                if (ttsState.isSpeaking) {
                    context.startService(ContentReadingService.commandIntent(context, ContentReadingService.ACTION_STOP))
                } else if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                    coroutineScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val textToRead = articleSpeechText(title, content)
                                withContext(Dispatchers.Main) {
                                    if (textToRead.isNotBlank()) {
                                        AndroidReadingPlayerBridge.start(
                                            context,
                                            ReadingStartRequest(
                                                queue = listOf(
                                                    ReadingQueueItem(
                                                        contentType = ReadingContentType.Article,
                                                        id = title.hashCode().toLong() and 0xffffffffL,
                                                        title = title,
                                                        bodyHtml = textToRead,
                                                    ),
                                                ),
                                                preferences = ReadingPreferences(
                                                    fieldOrder = listOf(ReadingTemplateField.Body),
                                                    enabledFields = setOf(ReadingTemplateField.Body),
                                                    queueLimit = 1,
                                                    transitionText = "",
                                                ),
                                                playbackSpeed = loadReadingPlaybackSpeed(androidSettingsStore(context)),
                                            ),
                                        )
                                    }
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
}

@Composable
actual fun rememberArticleBrowserOpener(): ArticleBrowserOpener {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    return remember(context, coroutineScope, userMessages) {
        object : ArticleBrowserOpener {
            override fun invoke(article: Article) {
                coroutineScope.launch {
                    OpenInBrowser.openUrlInBrowser(context, article)
                    userMessages.showMessage("已发送到浏览器")
                }
            }
        }
    }
}

@Composable
actual fun consumePendingCommentId(content: com.github.zly2006.zhihu.navigation.NavDestination): String? = remember(content) { AndroidArticleNavigationHandoff.consumeCommentId(content) }

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
    val coroutineScope = rememberCoroutineScope()
    WebviewComp(
        onDoubleTap = onDoubleTap,
        scrollState = scrollState,
    ) {
        it.isVerticalScrollBarEnabled = false
        it.setupUpWebviewClient {
            if (!rememberedScrollYSync) {
                coroutineScope.launch {
                    while (scrollState.maxValue < rememberedScrollY) {
                        delay(100)
                    }
                    Log.i("zhihu-scroll", "scroll to $rememberedScrollY, max= ${scrollState.maxValue}, sync on")
                    scrollState.animateScrollTo(rememberedScrollY)
                    onRememberedScrollYSyncChange(true)
                }
            }
        }
        it.contentId = article.id.toString()
        it.loadZhihu(
            "https://www.zhihu.com/${article.type}/${article.id}",
            prepareContentDocument(html, onImageLoadFailed),
            title,
        )
    }
}

actual fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = fuckHonorService()

@Composable
actual fun rememberHomeIsDebuggable(): Boolean {
    val context = LocalContext.current
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

@Composable
actual fun rememberBlocklistRuleImporter(
    userMessages: UserMessageSink,
    onImported: (String) -> Unit,
): BlocklistRuleImporter {
    val context = LocalContext.current
    val database = remember(context) { getContentFilterDatabase(context) }
    val coroutineScope = rememberCoroutineScope()
    val currentOnImported by rememberUpdatedState(onImported)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val summary = withContext(Dispatchers.IO) {
                        val text = context.contentResolver
                            .openInputStream(uri)
                            ?.bufferedReader()
                            ?.readText()
                            ?: return@withContext "读取文件失败"
                        importBlocklistBackupFromJsonText(
                            keywordDao = database.blockedKeywordDao(),
                            userDao = database.blockedUserDao(),
                            questionAuthorDao = database.blockedQuestionAuthorDao(),
                            topicDao = database.blockedTopicDao(),
                            text = text,
                        )
                    }
                    currentOnImported(summary)
                } catch (e: Exception) {
                    Log.e("BlocklistSettings", "Failed to import blocklist", e)
                    userMessages.showShortMessage("导入失败: ${e.message}")
                }
            }
        }
    }
    return remember(importLauncher) {
        object : BlocklistRuleImporter {
            override fun invoke() = importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
    }
}

@Composable
actual fun rememberBlocklistRuleExporter(): BlocklistRuleExporter {
    val context = LocalContext.current
    val database = remember(context) { getContentFilterDatabase(context) }
    return remember(context, database) {
        object : BlocklistRuleExporter {
            override suspend fun invoke(): String {
                val file = withContext(Dispatchers.IO) {
                    val dir = context.getExternalFilesDir(null) ?: context.filesDir
                    val file = File(dir, "zhihupp_blocklist.json")
                    file.writeText(
                        encodeBlocklistBackup(
                            keywordDao = database.blockedKeywordDao(),
                            userDao = database.blockedUserDao(),
                            questionAuthorDao = database.blockedQuestionAuthorDao(),
                            topicDao = database.blockedTopicDao(),
                        ),
                    )
                    file
                }
                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file,
                        ),
                        "application/json",
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "查看屏蔽规则"))
                return "已导出到 ${file.absolutePath}"
            }
        }
    }
}

@Composable
actual fun ZhihuHtmlWebViewContent(html: String) {
    WebviewComp {
        it.isVerticalScrollBarEnabled = false
        it.setupUpWebviewClient()
        it.loadZhihu(
            "https://www.zhihu.com",
            Jsoup.parse(html),
        )
    }
}

actual val isLegacyWebViewSupported: Boolean = true

@Composable
actual fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> =
    remember(emojiKeys) { createEmojiInlineContent(emojiKeys) }

@Composable
actual fun rememberCommentEmojis(): List<CommentEmoji> {
    val placeholders by EmojiManager.placeholders.collectAsState()
    return remember(placeholders) {
        placeholders.mapNotNull { placeholder ->
            commentEmojiInlineKey(placeholder)?.let { inlineKey ->
                CommentEmoji(placeholder = placeholder, inlineKey = inlineKey)
            }
        }
    }
}

actual fun commentEmojiInlineKey(placeholder: String): String? {
    val emojiPath = EmojiManager.getEmojiPath(placeholder) ?: return null
    val emojiFileName = emojiPath.substringAfterLast('/')
    return "emoji_$emojiFileName"
}

actual fun Modifier.commentSelectionWorkaround(): Modifier = fuckHonorService()

@Composable
actual fun rememberNotificationEnvironment(
    settingsStore: NotificationSettingsStore,
): com.github.zly2006.zhihu.viewmodel.NotificationEnvironment {
    val context = LocalContext.current
    return remember(context, settingsStore) {
        SharedAndroidNotificationEnvironment(context, false, settingsStore)
    }
}

object AndroidArticleNavigationHandoff {
    private var pendingContentIdentity: TrackedContentIdentity? = null
    private var pendingContentOpenFrom: String? = null
    private var pendingComment: CommentHolder? = null
    var clipboardDestination: NavDestination? = null
        private set

    fun markClipboardDestination(destination: NavDestination) {
        clipboardDestination = destination
    }

    fun prepareComment(holder: CommentHolder) {
        pendingComment = holder
    }

    fun clearCommentUnless(destination: NavDestination) {
        if (pendingComment?.article != destination) pendingComment = null
    }

    fun consumeCommentId(destination: NavDestination): String? {
        val holder = pendingComment?.takeIf { it.article == destination } ?: return null
        pendingComment = null
        return holder.commentId
    }

    fun prepareContentOpen(
        destination: NavDestination,
        openFrom: String,
    ) {
        pendingContentIdentity = ContentOpenEventSupport.toTrackedContentIdentity(destination)
        pendingContentOpenFrom = openFrom.takeIf { pendingContentIdentity != null }
    }

    fun consumeContentOpenFrom(destination: NavDestination): String {
        val identity = ContentOpenEventSupport.toTrackedContentIdentity(destination) ?: return ContentOpenFrom.UNKNOWN
        if (identity != pendingContentIdentity) return ContentOpenFrom.UNKNOWN
        pendingContentIdentity = null
        return pendingContentOpenFrom.also { pendingContentOpenFrom = null } ?: ContentOpenFrom.UNKNOWN
    }
}

@Composable
actual fun QuestionDetailWebViewContent(
    questionId: Long,
    html: String,
) {
    WebviewComp {
        it.loadZhihu(
            "https://www.zhihu.com/question/$questionId",
            Jsoup.parse(html),
        )
    }
}

actual fun Modifier.questionSelectionWorkaround(): Modifier = fuckHonorService()

@Composable
actual fun ArticleImmersiveModeEffect(immersive: Boolean) {
    val context = LocalContext.current
    val window = remember(context) { (context as? Activity)?.window }
    LaunchedEffect(window, immersive) {
        window?.let { w ->
            val ctrl = WindowInsetsControllerCompat(w, w.decorView)
            if (immersive) {
                ctrl.hide(WindowInsetsCompat.Type.statusBars())
                ctrl.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                ctrl.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }
}

@Composable
actual fun LeaveImmersiveModeCleanup() {
    val context = LocalContext.current
    val window = remember(context) { (context as? Activity)?.window }
    LaunchedEffect(window) {
        window?.let { w ->
            WindowInsetsControllerCompat(w, w.decorView)
                .show(WindowInsetsCompat.Type.statusBars())
        }
    }
}
