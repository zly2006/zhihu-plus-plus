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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.getContentDetail
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.shared.data.ZHIHU_ME_URL
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.PinLinkCardPreview
import com.github.zly2006.zhihu.ui.components.CustomWebView
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.util.EmojiManager
import com.github.zly2006.zhihu.util.OpenInBrowser
import com.github.zly2006.zhihu.util.createEmojiInlineContent
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.util.shareImage
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.filter.exportAllBlocklistToJson
import com.github.zly2006.zhihu.viewmodel.filter.getBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.importAllBlocklistFromJson
import com.github.zly2006.zhihu.viewmodel.local.LocalHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.notificationPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.za.AndroidHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

private const val LOGIN_ACTIVITY_CLASS = "com.github.zly2006.zhihu.LoginActivity"
private const val QR_CODE_SCAN_ACTIVITY_CLASS = "com.github.zly2006.zhihu.QRCodeScanActivity"
private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"
private const val QR_SCAN_RESULT_EXTRA = "scan_result"

@Composable
actual fun rememberAccountSettingsPlatformRuntime(): AccountSettingsRuntime {
    val context = LocalContext.current
    val accountDataState = AccountData.asState()
    val scanActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) scan@{ result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = result.data?.getStringExtra(QR_SCAN_RESULT_EXTRA) ?: return@scan
            context.startActivity(context.webviewActivityIntent(scanResult))
        }
    }
    val accountState = remember(accountDataState.value) {
        androidx.compose.runtime.derivedStateOf {
            accountDataState.value.toAccountSettingsAccountState()
        }
    }
    return AccountSettingsRuntime(
        accountState = accountState,
        refreshProfile = {
            val data = AccountData.data
            if (data.login) {
                val response = AccountData.fetchGet(context, ZHIHU_ME_URL) { signFetchRequest() }!!
                val self = ZhihuJson.decodeJson<com.github.zly2006.zhihu.shared.data.Person>(response)
                AccountData.saveData(context, data.copy(self = self))
            }
        },
        requestLogin = { context.startActivity(context.loginActivityIntent()) },
        requestQrLoginScan = { scanActivityLauncher.launch(context.qrCodeScanActivityIntent()) },
        logout = { AccountData.delete(context) },
        appVersionInfo = { context.zhihuVersionInfo() },
        selectMainTab = { destination -> context.navigateMainTab(destination) },
    )
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

private fun Context.loginActivityIntent(): Intent = Intent().setClassName(packageName, LOGIN_ACTIVITY_CLASS)

private fun Context.qrCodeScanActivityIntent(): Intent = Intent().setClassName(packageName, QR_CODE_SCAN_ACTIVITY_CLASS)

private fun Context.webviewActivityIntent(url: String): Intent = Intent().apply {
    setClassName(packageName, WEBVIEW_ACTIVITY_CLASS)
    data = url.toUri()
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
actual fun rememberArticleActionsRuntime(): ArticleActionsRuntime {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val dialogShareRuntime = rememberShareDialogRuntime()
    val articleHost = context.articleHost()
    val ttsState = articleHost?.articleTtsState ?: TtsState.Uninitialized
    return remember(context, coroutineScope, userMessages, dialogShareRuntime, articleHost, ttsState) {
        object : ArticleActionsRuntime {
            override val ttsState: TtsState = ttsState
            override val shareRuntime = dialogShareRuntime

            override fun toggleSpeech(
                title: String,
                content: String,
            ) {
                if (ttsState.isSpeaking) {
                    articleHost?.stopArticleSpeaking()
                } else if (ttsState !in listOf(TtsState.Error, TtsState.Uninitialized, TtsState.Initializing)) {
                    // 使用协程在后台处理文本提取，避免UI阻塞
                    coroutineScope.launch {
                        try {
                            // 在IO线程中处理文本提取
                            withContext(Dispatchers.IO) {
                                val textToRead = articleSpeechText(title, content)

                                // 回到主线程执行TTS
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

            override fun openArticleInBrowser(article: Article) {
                coroutineScope.launch {
                    OpenInBrowser.openUrlInBrowser(context, article)
                    userMessages.showMessage("已发送到浏览器")
                }
            }
        }
    }
}

@Composable
actual fun rememberArticleScreenRuntime(): ArticleScreenRuntime {
    val context = LocalContext.current
    return remember(context) {
        val articleHost = context.articleHost()
        object : ArticleScreenRuntime {
            override val articleHost: ArticleHost? = articleHost
            override val previewPreloader: ArticlePreviewPreloader = ArticlePreviewPreloader { cached, isNext, title, onImageLoadFailed ->
                val previewWebViewStore = articleHost?.articleAnswerSwitchState as? ArticlePreviewWebViewStore
                    ?: return@ArticlePreviewPreloader
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
    val coroutineScope = rememberCoroutineScope()
    com.github.zly2006.zhihu.ui.components.WebviewComp(
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
actual fun rememberHomeScreenRuntime(recommendationMode: RecommendationMode): HomeScreenRuntime {
    val context = LocalContext.current
    val accountData by AccountData.asState()
    val updateState by UpdateManager.updateState.collectAsState()
    val viewModel: BaseFeedViewModel = when (recommendationMode) {
        RecommendationMode.WEB -> viewModel<HomeFeedViewModel>()
        RecommendationMode.ANDROID -> viewModel<AndroidHomeFeedViewModel>()
        RecommendationMode.LOCAL -> viewModel<LocalHomeFeedViewModel>()
        RecommendationMode.MIXED -> viewModel<MixedHomeFeedViewModel>()
    }
    val localHomeViewModel = viewModel as? LocalHomeFeedViewModel
    val installTime = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
    val updateAnnouncement = (updateState as? UpdateManager.UpdateState.UpdateAvailable)?.let {
        HomeUpdateAnnouncement(
            version = it.version.toString(),
            isNightly = it.isNightly,
        )
    }

    return HomeScreenRuntime(
        account = HomeAccountState(
            isLoggedIn = accountData.login,
            avatarUrl = accountData.self?.avatarUrl,
        ),
        updateAnnouncement = updateAnnouncement,
        installedAtLeastThreeHours = System.currentTimeMillis() - installTime >= 3 * 60 * 60 * 1000L,
        isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
        viewModel = viewModel,
        requestLogin = {
            val intent = Intent().setClassName(context.packageName, "com.github.zly2006.zhihu.LoginActivity")
            context.startActivity(intent)
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
    val context = LocalContext.current
    val manager = remember(context) { getBlocklistManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var importCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val summary = manager.importAllBlocklistFromJson(context, uri)
                    importCallback?.invoke(summary)
                } catch (e: Exception) {
                    Log.e("BlocklistSettingsRuntime", "Failed to import blocklist", e)
                    userMessages.showShortMessage("导入失败: ${e.message}")
                }
            }
        }
    }
    return remember(context, manager, userMessages, importLauncher) {
        BlocklistSettingsRuntime(
            requestImport = { onImported ->
                importCallback = onImported
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            exportRules = {
                val file = manager.exportAllBlocklistToJson(context)
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
            },
        )
    }
}

@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime {
    val context = LocalContext.current
    return remember(context) {
        PinScreenRuntime(
            fetchLinkCardPreview = { linkCard ->
                fetchAndroidLinkCardPreview(context, linkCard)
            },
        )
    }
}

@Composable
actual fun PinHtmlWebViewContent(html: String) {
    WebviewComp {
        it.isVerticalScrollBarEnabled = false
        it.setupUpWebviewClient()
        it.loadZhihu(
            "https://www.zhihu.com",
            Jsoup.parse(html),
        )
    }
}

actual fun supportsPinHtmlWebView(): Boolean = true

private suspend fun fetchAndroidLinkCardPreview(
    context: Context,
    linkCard: DataHolder.Pin.ContentLinkCard,
): PinLinkCardPreview? = fetchPinLinkCardPreview(linkCard) { destination ->
    when (destination) {
        is Article -> {
            DataHolder.getContentDetail(context, destination)
        }
        is Question -> {
            DataHolder.getContentDetail(context, destination)
        }
        is Pin -> {
            DataHolder.getContentDetail(context, destination)
        }
        else -> null
    }
}

@Composable
actual fun rememberCommentScreenRuntime(): CommentScreenRuntime {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        object : CommentScreenRuntime {
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

@Composable
actual fun rememberNotificationScreenRuntime(
    viewModel: NotificationViewModel,
    settingsStore: NotificationSettingsStore,
): NotificationScreenRuntime {
    val context = LocalContext.current
    val environment = remember(context, settingsStore, viewModel) {
        viewModel.notificationPaginationEnvironment(context, settingsStore)
    }
    return NotificationScreenRuntime(
        environment = environment,
        showDebugCopy = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
    )
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
