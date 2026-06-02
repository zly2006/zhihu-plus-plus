/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.shared.data.fetchVerifiedZhihuSession
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopLoginRequests
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.desktop.saveImageToDownloads
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.PinLinkCardPreview
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.ui.subscreens.SystemUpdateState
import com.github.zly2006.zhihu.ui.subscreens.desktopSystemUpdateState
import com.github.zly2006.zhihu.viewmodel.DesktopPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.filter.createBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.local.LocalHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.AndroidHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
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
actual fun rememberArticleActionsRuntime(): ArticleActionsRuntime {
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val dialogShareRuntime = rememberShareDialogRuntime()
    return remember(userMessages, coroutineScope, dialogShareRuntime) {
        object : ArticleActionsRuntime {
            private var speechProcess: Process? = null
            private var currentTtsState by mutableStateOf(
                if (isDesktopSpeechCommandAvailable()) TtsState.Ready else TtsState.Error,
            )
            override val ttsState: TtsState
                get() = currentTtsState
            override val shareRuntime = dialogShareRuntime

            override fun toggleSpeech(
                title: String,
                content: String,
            ) {
                if (currentTtsState.isSpeaking) {
                    stopSpeaking()
                } else if (currentTtsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                    // 使用协程在后台处理文本提取，避免UI阻塞
                    coroutineScope.launch {
                        try {
                            // 在IO线程中处理文本提取和桌面 TTS 进程，保持 UI 线程可响应
                            val textToRead = withContext(Dispatchers.IO) {
                                articleSpeechText(title, content)
                            }
                            if (textToRead.isNotBlank()) {
                                speakText(textToRead, title)
                            }
                        } catch (e: Exception) {
                            currentTtsState = TtsState.Error
                            userMessages.showMessage("朗读失败：${e.message}")
                        }
                    }
                }
            }

            override fun openArticleInBrowser(article: Article) {
                if (openDesktopExternalUrl(articleWebUrl(article))) {
                    userMessages.showMessage("已发送到浏览器")
                }
            }

            private suspend fun speakText(
                text: String,
                title: String,
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
    }
}

private fun isDesktopSpeechCommandAvailable(): Boolean =
    runCatching {
        ProcessBuilder("sh", "-c", "command -v say >/dev/null 2>&1")
            .start()
            .waitFor() == 0
    }.getOrDefault(false)

@Composable
actual fun rememberCommentScreenRuntime(): CommentScreenRuntime {
    val scope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    return remember(scope, userMessages) {
        val store = DesktopAccountStore()
        object : CommentScreenRuntime {
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

private object JvmViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: kotlin.reflect.KClass<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T = modelClass.java.getDeclaredConstructor().newInstance()
}

@Composable
actual fun rememberHomeScreenRuntime(recommendationMode: RecommendationMode): HomeScreenRuntime {
    val accountStore = remember { DesktopAccountStore() }
    var account by remember { mutableStateOf(accountStore.load()) }
    val updateState by desktopSystemUpdateState.collectAsState()
    val viewModel: BaseFeedViewModel = when (recommendationMode) {
        RecommendationMode.ANDROID -> viewModel<AndroidHomeFeedViewModel>(factory = JvmViewModelFactory)
        RecommendationMode.LOCAL -> viewModel<LocalHomeFeedViewModel>(factory = JvmViewModelFactory)
        RecommendationMode.MIXED -> viewModel<MixedHomeFeedViewModel>(factory = JvmViewModelFactory)
        RecommendationMode.WEB -> viewModel<HomeFeedViewModel>(factory = JvmViewModelFactory)
    }
    val localHomeViewModel = viewModel as? LocalHomeFeedViewModel
    val updateAnnouncement = (updateState as? SystemUpdateState.UpdateAvailable)?.let {
        HomeUpdateAnnouncement(
            version = it.version,
            isNightly = it.isNightly,
        )
    }
    return HomeScreenRuntime(
        account = HomeAccountState(
            isLoggedIn = account.login,
            avatarUrl = account.profile?.avatarUrl,
        ),
        updateAnnouncement = updateAnnouncement,
        installedAtLeastThreeHours = false,
        isDebuggable = true,
        viewModel = viewModel,
        requestLogin = {
            DesktopLoginRequests.requestLogin()
            account = accountStore.load()
        },
        recordLocalItemOpened = { item ->
            localHomeViewModel?.onLocalItemOpened(item)
        },
        recordLocalItemFeedback = { item, feedback ->
            if (localHomeViewModel != null && item.localContentId != null) {
                localHomeViewModel.onLocalItemFeedback(item, feedback)
                true
            } else {
                false
            }
        },
    )
}

@Composable
actual fun rememberBlocklistSettingsPlatformRuntime(
    userMessages: UserMessageSink,
): BlocklistSettingsRuntime {
    val manager = remember {
        val databaseFile = blocklistDatabaseFile()
        databaseFile.parentFile?.mkdirs()
        val database = getContentFilterDatabase(databaseFile)
        database.createBlocklistManager()
    }
    val coroutineScope = rememberCoroutineScope()
    return remember(manager, userMessages) {
        BlocklistSettingsRuntime(
            requestImport = { onImported ->
                val selectedFile = chooseBlocklistImportFile()
                if (selectedFile != null) {
                    coroutineScope.launch {
                        try {
                            val summary = manager.importAllBlocklistFromJsonText(selectedFile.readText())
                            onImported(summary)
                        } catch (e: Exception) {
                            Log.e("BlocklistSettingsRuntime", "Failed to import blocklist", e)
                            userMessages.showShortMessage("导入失败: ${e.message}")
                        }
                    }
                }
            },
            exportRules = {
                val file = File(blocklistDatabaseFile().parentFile, "zhihupp_blocklist.json")
                file.writeText(manager.exportAllBlocklistToJsonText())
                "已导出到 ${file.absolutePath}"
            },
        )
    }
}

private fun blocklistDatabaseFile(): File = desktopContentFilterDatabaseFile()

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
actual fun rememberAccountSettingsPlatformRuntime(): AccountSettingsRuntime {
    val store = remember { DesktopAccountStore() }
    val accountState = remember { mutableStateOf(store.load().toAccountSettingsAccountState()) }
    return AccountSettingsRuntime(
        accountState = accountState,
        refreshProfile = {
            val account = store.load()
            val refreshed = store.createHttpClient(account.cookies).use { client ->
                fetchVerifiedZhihuSession(client, account.cookies, account.userAgent)
            }
            if (refreshed != null) {
                store.save(refreshed)
                accountState.value = refreshed.toAccountSettingsAccountState()
            } else {
                accountState.value = account.toAccountSettingsAccountState()
            }
        },
        requestLogin = {
            DesktopLoginRequests.requestLogin()
            accountState.value = store.load().toAccountSettingsAccountState()
        },
        requestQrLoginScan = {
            DesktopLoginRequests.requestLogin()
            accountState.value = store.load().toAccountSettingsAccountState()
        },
        logout = {
            store.clear()
            accountState.value = AccountSettingsAccountState()
        },
        appVersionInfo = { "desktop" },
        selectMainTab = { _: TopLevelDestination -> },
    )
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
actual fun rememberArticleScreenRuntime(): ArticleScreenRuntime = remember {
    object : ArticleScreenRuntime {
        override val articleHost: ArticleHost? = null
        override val previewPreloader = ArticlePreviewPreloader { _, _, _, _ -> }
    }
}

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
    ArticleMarkdownContent(
        html = html,
        modifier = Modifier,
        header = {},
        footer = {},
    )
}

actual fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = this

@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime {
    val store = remember { DesktopAccountStore() }
    val environment = remember(store) { DesktopPaginationEnvironment(store) }
    return remember(environment) {
        PinScreenRuntime(
            fetchLinkCardPreview = { linkCard ->
                fetchDesktopLinkCardPreview(environment, linkCard)
            },
        )
    }
}

@Composable
actual fun PinHtmlWebViewContent(html: String) = Unit // TODO: desktop Pin WebView

actual fun supportsPinHtmlWebView(): Boolean = false

private suspend fun fetchDesktopLinkCardPreview(
    environment: DesktopPaginationEnvironment,
    linkCard: DataHolder.Pin.ContentLinkCard,
): PinLinkCardPreview? = fetchPinLinkCardPreview(linkCard) { destination ->
    environment.getContentDetail(destination)
}

@Composable
actual fun rememberNotificationScreenRuntime(
    viewModel: NotificationViewModel,
    settingsStore: NotificationSettingsStore,
): NotificationScreenRuntime {
    val userMessages = rememberUserMessageSink()
    val store = remember { DesktopAccountStore() }
    val environment = remember(store, settingsStore, userMessages) {
        DesktopPaginationEnvironment(
            store = store,
            notificationSettingsStore = settingsStore,
            showFetchFailureMessage = userMessages::showMessage,
        )
    }
    return NotificationScreenRuntime(
        environment = environment,
        showDebugCopy = true,
    )
}

@Composable
actual fun rememberZhihuHttpClient(): HttpClient {
    val store = remember { DesktopAccountStore() }
    val session = remember { store.load() }
    val client = remember(store, session) { store.createHttpClient(session.cookies) }
    DisposableEffect(client) {
        onDispose { client.close() }
    }
    return client
}

@Composable
actual fun QuestionDetailWebViewContent(
    questionId: Long,
    html: String,
) = Unit // TODO: desktop question WebView

actual fun supportsQuestionDetailWebView(): Boolean = false

actual fun Modifier.questionSelectionWorkaround(): Modifier = this
