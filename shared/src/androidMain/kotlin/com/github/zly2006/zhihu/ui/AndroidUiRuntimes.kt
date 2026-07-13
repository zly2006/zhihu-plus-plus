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
import android.content.ContextWrapper
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.asApiEnvironment
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.shared.data.ZHIHU_ME_URL
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.components.CustomWebView
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.util.EmojiManager
import com.github.zly2006.zhihu.util.OpenInBrowser
import com.github.zly2006.zhihu.util.createEmojiInlineContent
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import com.github.zly2006.zhihu.viewmodel.filter.encodeBlocklistBackup
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.importBlocklistBackupFromJsonText
import com.github.zly2006.zhihu.viewmodel.notificationEnvironment
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File

private const val LOGIN_ACTIVITY_CLASS = "com.github.zly2006.zhihu.LoginActivity"
private const val QR_CODE_SCAN_ACTIVITY_CLASS = "com.github.zly2006.zhihu.QRCodeScanActivity"
private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"
private const val QR_SCAN_RESULT_EXTRA = "scan_result"

@Composable
actual fun rememberAccountSettingsAccountState(): androidx.compose.runtime.State<AccountSettingsAccountState> {
    val accountDataState = AccountData.asState()
    return remember(accountDataState.value) {
        androidx.compose.runtime.derivedStateOf {
            accountDataState.value.toAccountSettingsAccountState()
        }
    }
}

@Composable
actual fun rememberAccountProfileRefresher(): suspend () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        suspend {
            val data = AccountData.data
            if (data.login) {
                val response = context.asApiEnvironment().fetchJson(ZHIHU_ME_URL, "")!!
                val self = ZhihuJson.decodeJson<com.github.zly2006.zhihu.shared.data.Person>(response)
                AccountData.saveData(context, data.copy(self = self))
            }
        }
    }
}

@Composable
actual fun rememberAccountLoginRequester(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { context.startActivity(Intent().setClassName(context.packageName, LOGIN_ACTIVITY_CLASS)) }
    }
}

@Composable
actual fun rememberAccountQrLoginRequester(): () -> Unit {
    val context = LocalContext.current
    val scanActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) scan@{ result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = result.data?.getStringExtra(QR_SCAN_RESULT_EXTRA) ?: return@scan
            context.startActivity(
                Intent().apply {
                    setClassName(context.packageName, WEBVIEW_ACTIVITY_CLASS)
                    data = scanResult.toUri()
                },
            )
        }
    }
    return remember(context, scanActivityLauncher) {
        { scanActivityLauncher.launch(Intent().setClassName(context.packageName, QR_CODE_SCAN_ACTIVITY_CLASS)) }
    }
}

@Composable
actual fun rememberAccountLogoutAction(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { AccountData.delete(context) }
    }
}

@Composable
actual fun rememberAppVersionInfo(): String = LocalContext.current.zhihuVersionInfo()

@Composable
actual fun rememberMainTabSelector(): (TopLevelDestination) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { destination -> context.navigateMainTab(destination) }
    }
}

fun AccountData.Data.toAccountSettingsAccountState(): AccountSettingsAccountState = AccountSettingsAccountState(
    login = login,
    username = username,
    avatarUrl = self?.avatarUrl,
    id = self?.id ?: "",
    urlToken = self?.urlToken,
)

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

private fun Context.navigateMainTab(destination: TopLevelDestination) {
    val activity = findActivity() ?: return
    activity
        .javaClass
        .methods
        .firstOrNull { method ->
            method.name == "navigateMainTab" &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes.first().isAssignableFrom(destination::class.java)
        }?.invoke(activity, destination)
}

private fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun rememberArticleTtsState(): TtsState {
    val articleHost = LocalContext.current.articleHost()
    return articleHost?.articleTtsState ?: TtsState.Uninitialized
}

@Composable
actual fun rememberArticleSpeechToggler(): (title: String, content: String) -> Unit {
    val activityContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val articleHost = activityContext.articleHost()
    val ttsState = articleHost?.articleTtsState ?: TtsState.Uninitialized
    return remember(coroutineScope, userMessages, articleHost, ttsState) {
        { title, content ->
            if (ttsState.isSpeaking) {
                articleHost?.stopArticleSpeaking()
            } else if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                coroutineScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val textToRead = articleSpeechText(title, content)
                            withContext(Dispatchers.Main) {
                                if (textToRead.isNotBlank()) {
                                    articleHost?.speakArticleText(textToRead, title)
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

@Composable
actual fun rememberArticleBrowserOpener(): (Article) -> Unit {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    return remember(context, coroutineScope, userMessages) {
        { article ->
            coroutineScope.launch {
                OpenInBrowser.openUrlInBrowser(context, article)
                userMessages.showMessage("已发送到浏览器")
            }
        }
    }
}

@Composable
actual fun rememberArticleHost(): ArticleHost? = LocalContext.current.articleHost()

@Composable
actual fun ArticlePreviewPreloadEffect(
    cached: com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent?,
    isNext: Boolean,
    title: String,
    onImageLoadFailed: () -> Unit,
) {
    val context = LocalContext.current
    val articleHost = context.articleHost()
    LaunchedEffect(cached?.article?.id, cached?.content, isNext, title, articleHost) {
        cached ?: return@LaunchedEffect
        if (cached.content.isBlank()) return@LaunchedEffect
        val previewWebViewStore = articleHost?.articleAnswerSwitchState as? ArticlePreviewWebViewStore
            ?: return@LaunchedEffect
        val wv = previewWebViewStore.getOrCreatePreviewWebView(context, isNext, cached.article.id)
        val articleId = cached.article.id.toString()
        if (wv.contentId != articleId) {
            wv.contentId = articleId
            wv.loadZhihu(
                "https://www.zhihu.com/answer/${cached.article.id}",
                prepareContentDocument(cached.content, onImageLoadFailed),
                title,
            )
        }
    }
}

@Composable
actual fun ArticleWebViewContent(
    article: Article,
    html: String,
    title: String,
    modifier: Modifier,
    scrollState: ScrollState,
    rememberedScrollY: Int,
    rememberedScrollYSync: Boolean,
    onRememberedScrollYSyncChange: (Boolean) -> Unit,
    onImageLoadFailed: () -> Unit,
    onDoubleTap: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    WebviewComp(
        modifier = modifier,
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
actual fun rememberHomeAccountState(): HomeAccountState {
    val accountData by AccountData.asState()
    return HomeAccountState(
        isLoggedIn = accountData.login,
        avatarUrl = accountData.self?.avatarUrl,
    )
}

@Composable
actual fun rememberHomeUpdateAnnouncement(): HomeUpdateAnnouncement? {
    val updateState by UpdateManager.updateState.collectAsState()
    return (updateState as? UpdateManager.UpdateState.UpdateAvailable)?.let {
        HomeUpdateAnnouncement(
            version = it.version.toString(),
            isNightly = it.isNightly,
        )
    }
}

@Composable
actual fun rememberHomeInstalledAtLeastThreeHours(): Boolean {
    val context = LocalContext.current
    val installTime = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
    return System.currentTimeMillis() - installTime >= 3 * 60 * 60 * 1000L
}

@Composable
actual fun rememberHomeIsDebuggable(): Boolean {
    val context = LocalContext.current
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

@Composable
actual fun rememberHomeLoginRequester(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            val intent = Intent().setClassName(context.packageName, "com.github.zly2006.zhihu.LoginActivity")
            context.startActivity(intent)
        }
    }
}

@Composable
actual fun rememberBlocklistRuleImporter(
    userMessages: UserMessageSink,
): (((String) -> Unit) -> Unit) {
    val context = LocalContext.current
    val database = remember(context) { getContentFilterDatabase(context) }
    val coroutineScope = rememberCoroutineScope()
    var importCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
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
                            topicDao = database.blockedTopicDao(),
                            text = text,
                        )
                    }
                    importCallback?.invoke(summary)
                } catch (e: Exception) {
                    Log.e("BlocklistSettings", "Failed to import blocklist", e)
                    userMessages.showShortMessage("导入失败: ${e.message}")
                }
            }
        }
    }
    return remember(context, database, userMessages, importLauncher) {
        { onImported ->
            importCallback = onImported
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
    }
}

@Composable
actual fun rememberBlocklistRuleExporter(): suspend () -> String {
    val context = LocalContext.current
    val database = remember(context) { getContentFilterDatabase(context) }
    return remember(context, database) {
        suspend {
            val file = withContext(Dispatchers.IO) {
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val file = File(dir, "zhihupp_blocklist.json")
                file.writeText(
                    encodeBlocklistBackup(
                        keywordDao = database.blockedKeywordDao(),
                        userDao = database.blockedUserDao(),
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
            "已导出到 ${file.absolutePath}"
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

actual fun supportsZhihuHtmlWebView(): Boolean = true

@Composable
actual fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> =
    remember(emojiKeys) { createEmojiInlineContent(emojiKeys) }

actual fun commentEmojiInlineKey(placeholder: String): String? {
    val emojiPath = EmojiManager.getEmojiPath(placeholder) ?: return null
    val emojiFileName = emojiPath.substringAfterLast('/')
    return "emoji_$emojiFileName"
}

actual fun Modifier.commentSelectionWorkaround(): Modifier = fuckHonorService()

@Composable
actual fun rememberNotificationEnvironment(
    viewModel: NotificationViewModel,
    settingsStore: NotificationSettingsStore,
): com.github.zly2006.zhihu.viewmodel.NotificationEnvironment {
    val context = LocalContext.current
    return remember(context, settingsStore, viewModel) {
        viewModel.notificationEnvironment(context, settingsStore)
    }
}

@Composable
actual fun rememberNotificationShowDebugCopy(): Boolean {
    val context = LocalContext.current
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

interface ArticlePreviewWebViewStore {
    fun getOrCreatePreviewWebView(
        context: Context,
        isNext: Boolean,
        answerId: Long,
    ): CustomWebView
}

fun Context.articleHost(): ArticleHost? =
    (this as? ArticleHost) ?: (this as? ContextWrapper)?.baseContext?.takeIf { it !== this }?.articleHost()

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

actual fun supportsQuestionDetailWebView(): Boolean = true

@Composable
actual fun rememberZhihuHttpClient(): HttpClient = AccountData.httpClient(LocalContext.current)

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
