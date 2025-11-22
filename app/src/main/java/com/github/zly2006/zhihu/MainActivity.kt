@file:Suppress("PropertyName")

package com.github.zly2006.zhihu

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.Navigator
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.HistoryStorage
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.ZhihuMainVoyager
import com.github.zly2006.zhihu.ui.screens.toScreen
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.util.enableEdgeToEdgeCompat
import com.github.zly2006.zhihu.util.telemetry
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterExtensions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Locale

class MainActivity : ComponentActivity() {
    class SharedData : ViewModel() {
        var clipboardDestination: NavDestination? = null
    }

    val TAG = "MainActivity"
    val sharedData by viewModels<SharedData>()
    lateinit var webview: WebView
    lateinit var history: HistoryStorage
    val httpClient by lazy {
        AccountData.httpClient(this)
    }

    // TTS服务实例
    var textToSpeech: TextToSpeech? = null

    enum class TtsEngine {
        Uninitialized, // 未初始化
        Pico,
        Google,
        Sherpa,
    }

    @Suppress("unused")
    enum class TtsState(
        val isSpeaking: Boolean = false,
    ) {
        Uninitialized, // 未初始化
        Initializing, // 初始化中
        Ready, // 已初始化
        Error, // 失败，需要重新初始化
        LoadingText, // 正在加载文本
        Speaking(true), // 正在朗读
        Paused, // 暂停朗读
        SwitchingChunk(true), // 切换朗读段落
    }

    private val _ttsState = mutableStateOf(TtsState.Uninitialized)
    var ttsState: TtsState
        get() = _ttsState.value
        private set(value) {
            if (_ttsState.value != value) {
                val oldState = _ttsState.value
                _ttsState.value = value
                Log.i(TAG, "TTS State: $oldState -> $value")
            }
        }

    var ttsEngine: TtsEngine = TtsEngine.Uninitialized // 默认使用Pico TTS引擎
    private var isTtsInitialized = false

    var navigator: Navigator? = null

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
        enableEdgeToEdgeCompat()
        history = HistoryStorage(this)
        AccountData.loadData(this)

        // 应用启动时执行内容过滤数据库清理
        lifecycleScope.launch {
            try {
                ContentFilterExtensions.performMaintenanceCleanup(this@MainActivity)
                Log.i(TAG, "Content filter maintenance cleanup completed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform content filter cleanup", e)
            }
        }

        webview = WebView(this)
        Log.i(TAG, "Webview created")
        webview.setupUpWebviewClient()
        webview.settings.javaScriptEnabled = true
        webview.loadUrl("https://zhihu-plus.internal/assets/zse.html")

        setContent {
            ZhihuTheme {
                ZhihuMainVoyager()
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
                                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("error", "$stack")
                                    clipboard.setPrimaryClip(clip)
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
            .memoryCache {
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

        // 初始化TTS
        ttsState = TtsState.Initializing
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // 设置使用Pico TTS引擎
                val picoEngine = "com.svox.pico"
                val sherpaEngine = "com.k2fsa.sherpa.onnx.tts.engine"
                val availableEngines = textToSpeech?.engines?.map { it.name } ?: emptyList()

                Log.i(TAG, "availableEngines:$availableEngines")

                if (availableEngines.contains(picoEngine)) {
                    // 如果Pico TTS可用，切换到Pico引擎
                    textToSpeech?.shutdown()
                    ttsState = TtsState.Initializing
                    textToSpeech = TextToSpeech(this, { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            initializeTtsSettings()
                            ttsEngine = TtsEngine.Pico
                            Log.i(TAG, "Using Pico TTS engine")
                            ttsState = TtsState.Ready
                        } else {
                            Log.e(TAG, "Pico TTS engine Initialization failed")
                            ttsState = TtsState.Error
                        }
                    }, picoEngine)
                } else if (availableEngines.contains(sherpaEngine)) {
                    // Sherpa TTS可用，切换到Sherpa引擎
                    textToSpeech?.shutdown()
                    ttsState = TtsState.Initializing
                    textToSpeech = TextToSpeech(this, { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            initializeTtsSettings()
                            ttsEngine = TtsEngine.Sherpa
                            Log.i(TAG, "Using Sherpa TTS engine")
                            ttsState = TtsState.Ready
                        } else {
                            Log.e(TAG, "Sherpa TTS engine Initialization failed")
                            ttsState = TtsState.Error
                        }
                    }, sherpaEngine)
                } else {
                    Log.w(TAG, "Pico TTS not available, using default engine")
                    // 继续使用默认引擎的初始化
                    initializeTtsSettings()
                    ttsEngine = TtsEngine.Google
                }
            } else {
                Log.e(TAG, "TTS Initialization failed")
                ttsState = TtsState.Error
            }
        }

        // 自动检查更新（在应用启动时）
        if (savedInstanceState == null) {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                try {
                    val hasUpdate = UpdateManager.autoCheckForUpdate(this@MainActivity)
                    if (hasUpdate) {
                        val updateState = UpdateManager.updateState.value
                        if (updateState is UpdateManager.UpdateState.UpdateAvailable) {
                            showUpdateDialog(updateState.version.toString(), updateState.isNightly)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check for updates", e)
                }
            }
        }
    }

    private fun initializeTtsSettings() {
        // 设置语言
        val result = textToSpeech?.setLanguage(Locale.CHINESE)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // 如果中文不支持，尝试英文
            val englishResult = textToSpeech?.setLanguage(Locale.ENGLISH)
            if (englishResult == TextToSpeech.LANG_MISSING_DATA || englishResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported")
                ttsState = TtsState.Error
            } else {
                Log.i(TAG, "Using English language for TTS")
                isTtsInitialized = true
                ttsState = TtsState.Ready
            }
        } else {
            Log.i(TAG, "Using Chinese language for TTS")
            isTtsInitialized = true
            ttsState = TtsState.Ready
        }

        // 设置语音参数
        when (ttsEngine) {
            TtsEngine.Sherpa -> {
                textToSpeech?.setSpeechRate(1.1f) // 稍微慢一点的语速
                textToSpeech?.setPitch(1.0f) // 正常音调
            }
            else -> {
                textToSpeech?.setSpeechRate(0.9f) // 稍微慢一点的语速
                textToSpeech?.setPitch(1.0f) // 正常音调
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            if (intent.data != null) {
                if (intent.data!!.authority != "zhihu-plus.internal") {
                    Log.i(TAG, "Intent data: ${intent.data}")
                    val destination = resolveContent(intent.data!!)
                    if (destination != null) {
                        if (destination != sharedData.clipboardDestination) {
                            sharedData.clipboardDestination = destination
                            navigate(destination, popup = true)
                        }
                    } else {
                        AlertDialog
                            .Builder(this)
                            .apply {
                                setTitle("Unsupported URL")
                                setMessage("Unknown URL: ${intent.data}")
                                setPositiveButton("OK") { _, _ ->
                                }
                            }.create()
                            .show()
                    }
                }
            } else {
                // read clipboard
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text
                    if (text != null) {
                        val regex = Regex("""https?://[-a-zA-Z0-9()@:%_+.~#?&/=]*""")
                        val destination = regex.findAll(text).firstNotNullOfOrNull {
                            resolveContent(it.value.toUri())
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

    fun navigate(route: NavDestination, popup: Boolean = false) {
        history.add(route)
        if (popup) {
            // For popup navigation, replace current screen instead of pushing
            navigator?.replace(route.toScreen())
        } else {
            navigator?.push(route.toScreen())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun signRequest96(url: String, body: String?): String {
        val dc0 = AccountData.data.cookies["d_c0"] ?: ""
        val pathname = "/" + url.substringAfter("//").substringAfter('/')
        val signSource = listOfNotNull(
            ZSE93,
            pathname,
            dc0,
            body,
        ).joinToString("+")
        val md5 = MessageDigest.getInstance("MD5").digest(signSource.toByteArray()).toHexString()
        val timeStart = System.currentTimeMillis()
        val future = CompletableDeferred<String>()
        runOnUiThread {
            webview.evaluateJavascript("exports.encrypt('$md5')") {
                if (BuildConfig.DEBUG) {
                    val time = System.currentTimeMillis() - timeStart
                    Log.i(TAG, "Sign request: $url")
                    Log.i(TAG, "Sign source: $signSource")
                    Log.i(TAG, "Sign input: $md5")
                    Log.i(TAG, "Sign result: $it")
                    Log.i(TAG, "Sign time: $time ms")
                }
                future.complete(it.trim('"'))
            }
        }
        return "2.0_" + future.await()
    }

    fun postHistory(dest: NavDestination) {
        history.add(dest)
    }

    /**
     * 显示更新提醒对话框
     */
    private fun showUpdateDialog(version: String, isNightly: Boolean) {
        val versionType = if (isNightly) "Nightly" else "正式版本"
        val currentVersion = BuildConfig.VERSION_NAME

        runOnUiThread {
            AlertDialog
                .Builder(this)
                .apply {
                    setTitle("发现新版本")
                    setMessage("当前版本：$currentVersion\n新版本：$version ($versionType)\n\n是否立即更新？")
                    setCancelable(false)

                    // 立即更新按钮
                    setPositiveButton("立即更新") { dialog, _ ->
                        dialog.dismiss()
                        showDownloadProgressDialog(version)
                    }

                    // 跳过此版本按钮
                    setNeutralButton("跳过此版本") { _, _ ->
                        Log.i(TAG, "User chose to skip version $version")
                        UpdateManager.skipVersion(this@MainActivity, version)
                    }

                    // 稍后提醒按钮
                    setNegativeButton("稍后提醒") { _, _ ->
                        Log.i(TAG, "User chose to be reminded later")
                        // 不做任何操作，下次启动时会再次检查
                    }
                }.show()
        }
    }

    /**
     * 显示下载进度对话框
     */
    private fun showDownloadProgressDialog(version: String) {
        val progressDialog = AlertDialog
            .Builder(this)
            .apply {
                setTitle("正在下载更新")
                setMessage("正在下载版本 $version，请稍候...")
                setCancelable(false)
                setNegativeButton("取消") { dialog, _ ->
                    dialog.dismiss()
                    // 这里可以添加取消下载的逻辑
                }
            }.create()

        progressDialog.show()

        lifecycleScope.launch {
            try {
                Log.i(TAG, "Starting download for version $version")
                UpdateManager.downloadUpdate(this@MainActivity)

                // 监听下载状态
                UpdateManager.updateState.collect { state ->
                    when (state) {
                        is UpdateManager.UpdateState.Downloading -> {
                            runOnUiThread {
                                progressDialog.setMessage("正在下载版本 $version，��稍候...")
                            }
                        }
                        is UpdateManager.UpdateState.Downloaded -> {
                            Log.i(TAG, "Update downloaded, installing...")
                            runOnUiThread {
                                progressDialog.dismiss()
                                showInstallDialog(state.file)
                            }
                            return@collect // 结束收集
                        }
                        is UpdateManager.UpdateState.Error -> {
                            Log.e(TAG, "Update download failed: ${state.message}")
                            runOnUiThread {
                                progressDialog.dismiss()
                                AlertDialog
                                    .Builder(this@MainActivity)
                                    .apply {
                                        setTitle("下载失败")
                                        setMessage("更���下载失败：${state.message}")
                                        setPositiveButton("确定", null)
                                    }.show()
                            }
                            return@collect // 结束收集
                        }
                        else -> {
                            // 其他状态暂不处理
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download update", e)
                runOnUiThread {
                    progressDialog.dismiss()
                    AlertDialog
                        .Builder(this@MainActivity)
                        .apply {
                            setTitle("下载失败")
                            setMessage("更新下载失败：${e.message}")
                            setPositiveButton("确定", null)
                        }.show()
                }
            }
        }
    }

    /**
     * 显示安����认对话框
     */
    private fun showInstallDialog(file: java.io.File) {
        AlertDialog
            .Builder(this)
            .apply {
                setTitle("下载完成")
                setMessage("更新已下载完成，是否立即安装？")
                setCancelable(false)
                setPositiveButton("立即安装") { _, _ ->
                    try {
                        UpdateManager.installUpdate(this@MainActivity, file)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to install update", e)
                        AlertDialog
                            .Builder(this@MainActivity)
                            .apply {
                                setTitle("安装失败")
                                setMessage("无法启动安装程序：${e.message}")
                                setPositiveButton("确定", null)
                            }.show()
                    }
                }
                setNegativeButton("稍后安装") { _, _ ->
                    // 用户选择稍后安装，不做任何操作
                    Log.i(TAG, "User chose to install later")
                }
            }.show()
    }

    override fun onDestroy() {
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    // TTS相关方法
    fun speakText(text: String, title: String) {
        if (!isTtsInitialized || textToSpeech == null) return

        ttsState = TtsState.LoadingText

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            val maxChunkLength = 100
            val textChunks = splitTextIntoChunks(text, maxChunkLength)

            // 使用闭包来管理状态和递归调用
            lateinit var speakNextChunk: (Int) -> Unit
            speakNextChunk = { currentIndex ->
                if (currentIndex < textChunks.size) {
                    val chunk = textChunks[currentIndex]

                    runOnUiThread {
                        ttsState = TtsState.Speaking

                        textToSpeech?.speak(
                            chunk,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "chunk_$currentIndex",
                        )

                        // 设置朗读完成监听器，自动播放下一段
                        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                if (currentIndex == 0) {
                                    Toast
                                        .makeText(this@MainActivity, "开始朗读：$title", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                if (utteranceId == "chunk_$currentIndex") {
                                    ttsState = TtsState.Speaking
                                }
                            }

                            override fun onDone(utteranceId: String?) {
                                if (utteranceId == "chunk_$currentIndex") {
                                    if (currentIndex + 1 < textChunks.size) {
                                        ttsState = TtsState.SwitchingChunk
                                        // 延迟一点再播放下一段，避免太快
                                        @OptIn(DelicateCoroutinesApi::class)
                                        GlobalScope.launch {
                                            when (ttsEngine) {
                                                TtsEngine.Sherpa -> {
                                                    // 无需延迟, Sherpa 本身不会太快
                                                }
                                                else -> {
                                                    kotlinx.coroutines.delay(500)
                                                }
                                            }
                                            runOnUiThread {
                                                speakNextChunk(currentIndex + 1)
                                            }
                                        }
                                    } else {
                                        ttsState = TtsState.Ready
                                    }
                                }
                            }

                            @Suppress("OVERRIDE_DEPRECATION")
                            override fun onError(p0: String?) { }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                if (utteranceId == "chunk_$currentIndex") {
                                    ttsState = TtsState.Error
                                }
                            }
                        })
                    }
                } else {
                    runOnUiThread {
                        ttsState = TtsState.Ready
                    }
                }
            }

            // 开始播放第一段
            runOnUiThread {
                speakNextChunk(0)
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun splitTextIntoChunks(text: String, maxLength: Int = 100): List<String> {
        if (text.length <= maxLength) return listOf(text)

        val chunks = mutableListOf<String>()
        var currentPos = 0

        while (currentPos < text.length) {
            val endPos = minOf(currentPos + maxLength, text.length)
            var chunk = text.substring(currentPos, endPos)

            // 如果不是最后一段，尝试在句号、感叹号、问号处分割
            if (endPos < text.length) {
                val lastSentenceEnd = chunk.lastIndexOfAny(listOf("。", "！", "？", ".", "!", "?"))
                if (lastSentenceEnd > chunk.length / 2) {
                    chunk = chunk.substring(0, lastSentenceEnd + 1)
                }
            }

            chunks.add(chunk.trim())
            currentPos += chunk.length
        }

        return chunks
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        ttsState = TtsState.Ready
    }

    fun isSpeaking(): Boolean = textToSpeech?.isSpeaking ?: false

    @Suppress("unused")
    companion object {
        const val IOS = "5_2.0"
        const val ANDROID = "4_2.0"
        const val WEB = "3_2.0"
        const val ZSE93 = "101_3_3.0"
    }
}
