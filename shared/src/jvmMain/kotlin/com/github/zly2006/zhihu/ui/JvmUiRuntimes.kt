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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopLoginRequests
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.desktop.saveImageToDownloads
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.subscreens.SystemUpdateState
import com.github.zly2006.zhihu.ui.subscreens.desktopSystemUpdateState
import com.github.zly2006.zhihu.viewmodel.DesktopPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.encodeBlocklistBackup
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.importBlocklistBackupFromJsonText
import io.ktor.client.HttpClient
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
actual fun rememberArticleSpeechToggler(): (title: String, content: String) -> Unit {
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    return remember(userMessages, coroutineScope) {
        { title, content ->
            DesktopArticleSpeechController.toggleSpeech(title, content, coroutineScope, userMessages)
        }
    }
}

@Composable
actual fun rememberArticleBrowserOpener(): (Article) -> Unit {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        { article ->
            if (openDesktopExternalUrl(articleWebUrl(article))) {
                userMessages.showMessage("已发送到浏览器")
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
actual fun rememberCommentImageSaver(): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val store = remember { DesktopAccountStore() }
    return remember(scope, userMessages, store) {
        { imageUrl ->
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
    }
}

@Composable
actual fun rememberCommentImageSharer(): (String) -> Unit {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        { imageUrl ->
            runCatching {
                copyDesktopPlainText(imageUrl)
                userMessages.showShortMessage("已复制图片链接")
            }.onFailure { error ->
                userMessages.showShortMessage("分享失败: ${error.message}")
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
actual fun rememberHomeAccountState(): HomeAccountState {
    val accountStore = remember { DesktopAccountStore() }
    val account = accountStore.load()
    return HomeAccountState(
        isLoggedIn = account.login,
        avatarUrl = account.profile?.avatarUrl,
    )
}

@Composable
actual fun rememberHomeUpdateAnnouncement(): HomeUpdateAnnouncement? {
    val updateState by desktopSystemUpdateState.collectAsState()
    return (updateState as? SystemUpdateState.UpdateAvailable)?.let {
        HomeUpdateAnnouncement(
            version = it.version,
            isNightly = it.isNightly,
        )
    }
}

@Composable
actual fun rememberHomeInstalledAtLeastThreeHours(): Boolean = false

@Composable
actual fun rememberHomeIsDebuggable(): Boolean = true

@Composable
actual fun rememberHomeLoginRequester(): () -> Unit = remember {
    { DesktopLoginRequests.requestLogin() }
}

@Composable
actual fun rememberBlocklistRuleImporter(
    userMessages: UserMessageSink,
): (((String) -> Unit) -> Unit) {
    val database = remember {
        val databaseFile = desktopContentFilterDatabaseFile()
        databaseFile.parentFile?.mkdirs()
        getContentFilterDatabase(databaseFile)
    }
    val coroutineScope = rememberCoroutineScope()
    return remember(database, userMessages) {
        { onImported ->
            val selectedFile = chooseBlocklistImportFile()
            if (selectedFile != null) {
                coroutineScope.launch {
                    try {
                        val summary = importBlocklistBackupFromJsonText(
                            keywordDao = database.blockedKeywordDao(),
                            userDao = database.blockedUserDao(),
                            topicDao = database.blockedTopicDao(),
                            text = selectedFile.readText(),
                        )
                        onImported(summary)
                    } catch (e: Exception) {
                        Log.e("BlocklistSettings", "Failed to import blocklist", e)
                        userMessages.showShortMessage("导入失败: ${e.message}")
                    }
                }
            }
        }
    }
}

@Composable
actual fun rememberBlocklistRuleExporter(): suspend () -> String {
    val database = remember {
        val databaseFile = desktopContentFilterDatabaseFile()
        databaseFile.parentFile?.mkdirs()
        getContentFilterDatabase(databaseFile)
    }
    return remember(database) {
        suspend {
            val file = File(desktopContentFilterDatabaseFile().parentFile, "zhihupp_blocklist.json")
            file.writeText(
                encodeBlocklistBackup(
                    keywordDao = database.blockedKeywordDao(),
                    userDao = database.blockedUserDao(),
                    topicDao = database.blockedTopicDao(),
                ),
            )
            "已导出到 ${file.absolutePath}"
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
actual fun rememberAccountSettingsAccountState(): androidx.compose.runtime.State<AccountSettingsAccountState> =
    DesktopAccountSettingsState.accountState

@Composable
actual fun rememberAccountProfileRefresher(): suspend () -> Unit = remember {
    {
        DesktopAccountSettingsState.refreshProfile()
    }
}

@Composable
actual fun rememberAccountLoginRequester(): () -> Unit = remember {
    {
        DesktopLoginRequests.requestLogin()
        DesktopAccountSettingsState.reload()
    }
}

@Composable
actual fun rememberAccountQrLoginRequester(): () -> Unit = rememberAccountLoginRequester()

@Composable
actual fun rememberAccountLogoutAction(): () -> Unit = remember {
    {
        DesktopAccountSettingsState.clear()
    }
}

@Composable
actual fun rememberAppVersionInfo(): String = "desktop"

@Composable
actual fun rememberMainTabSelector(): (TopLevelDestination) -> Unit = remember {
    { _: TopLevelDestination -> }
}

private object DesktopAccountSettingsState {
    private val store = DesktopAccountStore()
    val accountState = mutableStateOf(store.load().toAccountSettingsAccountState())

    suspend fun refreshProfile() {
        val account = store.load()
        val refreshed = store.refreshAndSaveProfile()
        accountState.value = if (refreshed != null) {
            refreshed.toAccountSettingsAccountState()
        } else {
            account.toAccountSettingsAccountState()
        }
    }

    fun reload() {
        accountState.value = store.load().toAccountSettingsAccountState()
    }

    fun clear() {
        store.clear()
        accountState.value = AccountSettingsAccountState()
    }
}

private fun com.github.zly2006.zhihu.shared.account.ZhihuAccountSession.toAccountSettingsAccountState(): AccountSettingsAccountState =
    AccountSettingsAccountState(
        login = login,
        username = username,
        avatarUrl = profile?.avatarUrl,
        id = profile?.id ?: "",
        urlToken = profile?.urlToken,
    )

@Composable
actual fun rememberArticleHost(): ArticleHost? = null

@Composable
actual fun ArticlePreviewPreloadEffect(
    cached: com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent?,
    isNext: Boolean,
    title: String,
    onImageLoadFailed: () -> Unit,
) = Unit

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
    )
}

actual fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = this

/**
 * 桌面端不支持 WebView
 */
@Composable
actual fun ZhihuHtmlWebViewContent(html: String) = Unit

actual fun supportsZhihuHtmlWebView(): Boolean = false

@Composable
actual fun rememberNotificationEnvironment(
    viewModel: NotificationViewModel,
    settingsStore: NotificationSettingsStore,
): com.github.zly2006.zhihu.viewmodel.NotificationEnvironment {
    val userMessages = rememberUserMessageSink()
    val store = remember { DesktopAccountStore() }
    return remember(store, settingsStore, userMessages) {
        DesktopPaginationEnvironment(
            store = store,
            notificationSettingsStore = settingsStore,
            showFetchFailureMessage = userMessages::showMessage,
        )
    }
}

@Composable
actual fun rememberNotificationShowDebugCopy(): Boolean = true

@Composable
actual fun rememberZhihuHttpClient(): HttpClient {
    val store = remember { DesktopAccountStore() }
    return store.httpClient()
}

@Composable
actual fun QuestionDetailWebViewContent(
    questionId: Long,
    html: String,
) = Unit // TODO: 桌面端问题 WebView

actual fun supportsQuestionDetailWebView(): Boolean = false

actual fun Modifier.questionSelectionWorkaround(): Modifier = this

@Composable
actual fun ArticleImmersiveModeEffect(immersive: Boolean) = Unit

@Composable
actual fun LeaveImmersiveModeCleanup() = Unit
