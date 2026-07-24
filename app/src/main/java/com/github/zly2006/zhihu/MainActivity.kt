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

package com.github.zly2006.zhihu

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.HistoryStorage
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.MainTabs
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.nlp.NLPService
import com.github.zly2006.zhihu.nlp.NlpServiceKeywordSemanticMatcher
import com.github.zly2006.zhihu.nlp.SentenceEmbeddingManager
import com.github.zly2006.zhihu.reading.AndroidReadingPlayerBridge
import com.github.zly2006.zhihu.reading.ContentReadingService
import com.github.zly2006.zhihu.reading.ReadingContentType
import com.github.zly2006.zhihu.reading.ReadingPlaybackStatus
import com.github.zly2006.zhihu.reading.ReadingPreferences
import com.github.zly2006.zhihu.reading.ReadingQueueItem
import com.github.zly2006.zhihu.reading.ReadingStartRequest
import com.github.zly2006.zhihu.reading.ReadingTemplateField
import com.github.zly2006.zhihu.reading.loadReadingPlaybackSpeed
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
import com.github.zly2006.zhihu.shared.filter.TrackedContentIdentity
import com.github.zly2006.zhihu.shared.nlp.KeywordWeightExtractor
import com.github.zly2006.zhihu.shared.platform.androidSettingsStore
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.shared.util.ZHIHU_WEB_ZSE93
import com.github.zly2006.zhihu.theme.AndroidThemeSettings
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.AndroidZhihuMain
import com.github.zly2006.zhihu.ui.ArticleAnswerSwitchState
import com.github.zly2006.zhihu.ui.ArticleHost
import com.github.zly2006.zhihu.ui.TtsState
import com.github.zly2006.zhihu.ui.components.getHighestQualityVideoUrl
import com.github.zly2006.zhihu.ui.subscreens.DeveloperRuntimeInfo
import com.github.zly2006.zhihu.ui.subscreens.DeveloperRuntimeInfoProvider
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.util.ContinuousUsageReminderManager
import com.github.zly2006.zhihu.util.EmojiManager
import com.github.zly2006.zhihu.util.PowerSaveModeCompat
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import com.github.zly2006.zhihu.util.clearShareImageCache
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.enableEdgeToEdgeCompat
import com.github.zly2006.zhihu.util.telemetry
import com.github.zly2006.zhihu.viewmodel.AndroidArticlesSharedData
import com.github.zly2006.zhihu.viewmodel.filter.AndroidContentFilterRuntime
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterManager
import com.github.zly2006.zhihu.viewmodel.filter.contentFilterSettings
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity :
    ComponentActivity(),
    ArticleHost,
    DeveloperRuntimeInfoProvider {
    class SharedData : ViewModel() {
        var clipboardDestination: NavDestination? = null
    }

    val sharedData by viewModels<SharedData>()
    override val articleNavController: NavHostController
        get() = navController
    override val articleAnswerSwitchState: ArticleAnswerSwitchState
        get() = ViewModelProvider(this)[AndroidArticlesSharedData::class.java]
    override val articleTtsState: TtsState
        get() = ttsState
    override var clipboardDestination: NavDestination?
        get() = sharedData.clipboardDestination
        set(value) {
            sharedData.clipboardDestination = value
        }
    lateinit var history: HistoryStorage
    val httpClient by lazy {
        AccountData.httpClient(this)
    }

    private val _ttsState = mutableStateOf(TtsState.Ready)
    var ttsState: TtsState
        get() = _ttsState.value
        private set(value) {
            if (_ttsState.value != value) {
                val oldState = _ttsState.value
                _ttsState.value = value
                Log.i(TAG, "TTS State: $oldState -> $value")
            }
        }

    lateinit var navController: NavHostController
    private lateinit var continuousUsageReminderManager: ContinuousUsageReminderManager
    private var pendingContentOpenIdentity: TrackedContentIdentity? = null
    private var pendingContentOpenFrom: String? = null
    private var currentMainTabOpenFrom: String? = null
    var mainTabNavigationTarget by mutableStateOf<TopLevelDestination?>(null)
        private set

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Log.e(TAG, "Uncaught exception", e)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri
                    .Builder()
                    .apply {
                        scheme("https")
                        authority("zhihu-plus.internal")
                        appendPath("error")
                        appendQueryParameter("title", "Uncaught exception: ${e.message}")
                        appendQueryParameter(
                            "message",
                            e.message,
                        )
                        appendQueryParameter("stack", e.stackTraceToString())
                    }.build(),
                this,
                MainActivity::class.java,
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
        super.onCreate(savedInstanceState)
        clearShareImageCache(this)
        enableEdgeToEdgeCompat()
        continuousUsageReminderManager = ContinuousUsageReminderManager(this)
        history = HistoryStorage(this)
        AccountData.loadData(this)
        AndroidThemeSettings.initialize(this)
        AndroidContentFilterRuntime.semanticMatcher = NlpServiceKeywordSemanticMatcher
        AndroidContentFilterRuntime.keywordWeightExtractor = KeywordWeightExtractor { text, topN ->
            NLPService.extractKeywordsWithWeight(text, topN)
        }
        getContentFilterDatabase(this)

        val settings = androidSettingsStore(this)
        val lastLaunchTimestamp = settings.getLong(KEY_LAST_LAUNCH_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        if (now - lastLaunchTimestamp >= TimeUnit.DAYS.toMillis(1)) {
            val client = httpClient
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val refreshToken = ZhihuCredentialRefresher.fetchRefreshToken(client)
                    ZhihuCredentialRefresher.refreshZhihuToken(refreshToken, client)
                    Log.i(TAG, "Zhihu token refreshed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to refresh Zhihu token", e)
                    withContext(Dispatchers.Main) {
                        androidUserMessageSink(this@MainActivity)
                            .showLongMessage("刷新登录状态失败，如多次看到此提示请重新登录")
                    }
                }
                if (!PowerSaveModeCompat.getPowerSaveMode(this@MainActivity).isPowerSaveMode) {
                    try {
                        SentenceEmbeddingManager.ensureModel(this@MainActivity)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to initialize NLP embedding model", e)
                    }
                }
            }
        }
        settings.putLong(KEY_LAST_LAUNCH_TIMESTAMP, now)

        // 应用启动时执行内容过滤数据库清理
        lifecycleScope.launch {
            try {
                if (contentFilterSettings().enableContentFilter) {
                    ContentFilterManager(getContentFilterDatabase(this@MainActivity).contentFilterDao()).cleanupOldData()
                }
                Log.i(TAG, "Content filter maintenance cleanup completed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform content filter cleanup", e)
            }
        }

        // 初始化emoji管理器
        lifecycleScope.launch {
            try {
                EmojiManager
                    .initialize(this@MainActivity)
                Log.i(TAG, "Emoji manager initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize emoji manager", e)
            }
        }

        setContent {
            navController = rememberNavController()
            ZhihuTheme {
                Box(Modifier.semantics { testTagsAsResourceId = true }) {
                    AndroidZhihuMain(navController = navController)
                }
            }
        }
        if (savedInstanceState == null) {
            telemetry(this, "start")
            if (intent.data != null) {
                if (intent.data!!.authority == "zhihu-plus.internal") {
                    if (intent.data!!.path == "/error") {
                        val title = intent.data!!.getQueryParameter("title")
//                        val message = intent.data!!.getQueryParameter("message")
                        val stack = intent.data!!.getQueryParameter("stack")
                        AlertDialog
                            .Builder(this)
                            .apply {
                                setTitle(title)
                                setMessage(stack)
                                setPositiveButton("OK") { _, _ ->
                                }
                                setNeutralButton("Copy") { _, _ ->
                                    val clip = ClipData.newPlainText("error", "$stack")
                                    clipboardManager.setPrimaryClip(clip)
                                }
                            }.create()
                            .show()
                    }
                }
            }
        }

        ImageLoader
            .Builder(this)
            .crossfade(true)
            .components {
                // add(SvgDecoder.Factory())
            }.memoryCache {
                MemoryCache
                    .Builder()
                    .maxSizePercent(this, 0.25)
                    .build()
            }.diskCache {
                DiskCache
                    .Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }.build()
            .also { loader ->
                SingletonImageLoader.setSafe {
                    loader
                }
            }

        lifecycleScope.launch {
            AndroidReadingPlayerBridge.state.collect { state ->
                ttsState = when (state.status) {
                    ReadingPlaybackStatus.Idle -> TtsState.Ready
                    ReadingPlaybackStatus.Initializing -> TtsState.Initializing
                    ReadingPlaybackStatus.Loading -> TtsState.LoadingText
                    ReadingPlaybackStatus.Playing -> TtsState.Speaking
                    ReadingPlaybackStatus.Paused -> TtsState.Paused
                    ReadingPlaybackStatus.Error -> TtsState.Error
                }
            }
        }

        // 自动检查更新（在应用启动时）
        if (savedInstanceState == null) {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                try {
                    UpdateManager.autoCheckForUpdate(this@MainActivity)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check for updates", e)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        continuousUsageReminderManager.onAppForeground()
    }

    override fun onStop() {
        continuousUsageReminderManager.onAppBackground()
        super.onStop()
    }

    override val developerRuntimeInfo: DeveloperRuntimeInfo
        get() = DeveloperRuntimeInfo(
            continuousUsageDurationMs = continuousUsageReminderManager.currentElapsedForegroundMs(),
            ttsState = ttsState,
            currentTtsEngineLabel = AndroidReadingPlayerBridge.state.value.engineLabel
                .ifBlank { "按需初始化" },
            availableTtsEngineLabels = AndroidReadingPlayerBridge.state.value.availableEngineLabels,
        )

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            if (!handleIntentData(intent)) {
                // read clipboard
                val clip = clipboardManager.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text
                    if (text != null) {
                        val regex = Regex("""https?://[-a-zA-Z0-9@:%_+.~#?&/=]*""")
                        val destination = regex.findAll(text).firstNotNullOfOrNull {
                            resolveContent(it.value)
                        }
                        if (destination != null && destination != sharedData.clipboardDestination) {
                            sharedData.clipboardDestination = destination
                            navigate(destination, popup = true)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::navController.isInitialized) {
            handleIntentData(intent)
        }
    }

    private fun handleIntentData(incomingIntent: Intent): Boolean {
        val data = incomingIntent.data ?: return false
        if (data.authority == "zhihu-plus.internal") return true
        val forceNavigation = incomingIntent.getBooleanExtra(ContentReadingService.READING_NOTIFICATION_INTENT_EXTRA, false)
        incomingIntent.removeExtra(ContentReadingService.READING_NOTIFICATION_INTENT_EXTRA)

        Log.i(TAG, "Intent data: $data")
        val destination = resolveContent(data.toString())
        if (destination != null) {
            if (forceNavigation || destination != sharedData.clipboardDestination) {
                sharedData.clipboardDestination = destination
                navigate(destination, popup = true)
            }
        } else {
            AlertDialog
                .Builder(this)
                .apply {
                    setTitle("Unsupported URL")
                    setMessage("Unknown URL: $data")
                    setPositiveButton("OK") { _, _ -> }
                }.create()
                .show()
        }
        return true
    }

    fun navigate(route: NavDestination, popup: Boolean = false) {
        preparePendingContentOpen(route)
        history.add(route)
        if (route is Video) {
            val current = runCatching {
                navController.currentBackStackEntry?.toRoute<Article>()
            }.getOrNull() ?: runCatching {
                navController.currentBackStackEntry?.toRoute<Question>()
            }.getOrNull()
            if (current == null) {
                androidUserMessageSink(this).showShortMessage("无法打开视频：未知的内容类型")
                return
            }
            val (contentId, contentType) = when (current) {
                is Article -> {
                    current.id.toString() to when (current.type) {
                        ArticleType.Answer -> "answer"
                        ArticleType.Article -> "article"
                    }
                }
                is Question -> {
                    current.questionId.toString() to "question"
                }
                else -> error("Unsupported content type for video: $current")
            }
            CoroutineScope(Dispatchers.Main).launch {
                val videoUrl = getHighestQualityVideoUrl(this@MainActivity, httpClient, route.id.toString(), contentId, contentType)
                if (videoUrl == null) {
                    androidUserMessageSink(this@MainActivity).showShortMessage("获取视频链接失败")
                    return@launch
                }
                startActivity(
                    Intent(this@MainActivity, VideoPlayerActivity::class.java).apply {
                        putExtra("video_url", videoUrl)
                        putExtra("video_id", route.id)
                    },
                )
            }
            return
        }
        if (route == MainTabs) {
            mainTabNavigationTarget = Home
            navigateToMainTabs()
            return
        }
        navController.navigate(route) {
            if (popup) {
                launchSingleTop = true
                popUpTo(MainTabs) {
                    // clear the back stack and viewModels
                    saveState = true
                }
            }
        }
    }

    override fun consumePendingContentOpenFrom(destination: NavDestination): String {
        val identity = ContentOpenEventSupport.toTrackedContentIdentity(destination) ?: return ContentOpenFrom.UNKNOWN
        if (identity != pendingContentOpenIdentity) {
            return ContentOpenFrom.UNKNOWN
        }
        val openFrom = pendingContentOpenFrom ?: ContentOpenFrom.UNKNOWN
        pendingContentOpenIdentity = null
        pendingContentOpenFrom = null
        return openFrom
    }

    private fun preparePendingContentOpen(target: NavDestination) {
        val identity = ContentOpenEventSupport.toTrackedContentIdentity(target)
        if (identity == null) {
            pendingContentOpenIdentity = null
            pendingContentOpenFrom = null
            return
        }
        pendingContentOpenIdentity = identity
        pendingContentOpenFrom = if (
            runCatching { navController.currentBackStackEntry?.toRoute<MainTabs>() }.getOrNull() != null
        ) {
            currentMainTabOpenFrom
        } else {
            null
        }
            ?: ContentOpenEventSupport.inferOpenFrom(currentContentOpenSource(), target)
    }

    private fun navigateToMainTabs() {
        navController.navigate(MainTabs) {
            launchSingleTop = true
            restoreState = true
            popUpTo(MainTabs) {
                saveState = true
            }
        }
    }

    fun navigateMainTab(destination: TopLevelDestination) {
        mainTabNavigationTarget = destination
        navigateToMainTabs()
    }

    fun setCurrentMainTabOpenFrom(openFrom: String?) {
        currentMainTabOpenFrom = openFrom
    }

    fun consumeMainTabNavigationTarget(destination: TopLevelDestination) {
        if (mainTabNavigationTarget == destination) {
            mainTabNavigationTarget = null
        }
    }

    private fun currentContentOpenSource(): NavDestination? {
        val currentEntry = navController.currentBackStackEntry
        return runCatching {
            currentEntry?.toRoute<Article>()
        }.getOrNull() ?: runCatching {
            currentEntry?.toRoute<Question>()
        }.getOrNull() ?: runCatching {
            currentEntry?.toRoute<Pin>()
        }.getOrNull() ?: runCatching {
            currentEntry?.toRoute<CollectionContent>()
        }.getOrNull() ?: runCatching {
            currentEntry?.toRoute<History>()
        }.getOrNull() ?: runCatching {
            currentEntry?.toRoute<Notification>()
        }.getOrNull()
    }

    override fun postHistoryDestination(destination: NavDestination) {
        history.add(destination)
    }

    override fun speakArticleText(
        text: String,
        title: String,
    ) {
        val request = ReadingStartRequest(
            queue = listOf(
                ReadingQueueItem(
                    contentType = ReadingContentType.Article,
                    id = title.hashCode().toLong() and 0xffffffffL,
                    title = title,
                    bodyHtml = text,
                ),
            ),
            preferences = ReadingPreferences(
                fieldOrder = listOf(ReadingTemplateField.Body),
                enabledFields = setOf(ReadingTemplateField.Body),
                queueLimit = 1,
                transitionText = "",
            ),
            playbackSpeed = loadReadingPlaybackSpeed(androidSettingsStore(this)),
        )
        AndroidReadingPlayerBridge.prepareStart(request)
        ContextCompat.startForegroundService(
            this,
            ContentReadingService.commandIntent(this, ContentReadingService.ACTION_START),
        )
    }

    override fun stopArticleSpeaking() {
        startService(ContentReadingService.commandIntent(this, ContentReadingService.ACTION_STOP))
    }

    override fun onDestroy() {
        continuousUsageReminderManager.onDestroy()
        super.onDestroy()
    }

    @Suppress("unused")
    companion object {
        private const val KEY_LAST_LAUNCH_TIMESTAMP = "last_main_launch_timestamp"
        const val IOS = "5_2.0"
        const val ANDROID = "4_2.0"
        const val WEB = "3_2.0"
        const val ZSE93 = ZHIHU_WEB_ZSE93
        const val TAG = "MainActivity"
    }
}
