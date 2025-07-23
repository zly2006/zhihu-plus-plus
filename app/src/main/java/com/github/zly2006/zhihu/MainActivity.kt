package com.github.zly2006.zhihu

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.HistoryStorage
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.ZhihuMain
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.enableEdgeToEdgeCompat
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterExtensions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import java.security.MessageDigest
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    class SharedData : ViewModel() {
        var clipboardDestination: NavDestination? = null
    }

    val sharedData by viewModels<SharedData>()
    lateinit var webview: WebView
    lateinit var history: HistoryStorage
    val httpClient by lazy {
        AccountData.httpClient(this)
    }

    // TTS服务实例
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private val maxChunkLength = 3000 // 每段最大字符数
    private var currentTextChunks = mutableListOf<String>()
    private var currentChunkIndex = 0

    lateinit var navController: NavHostController

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Log.e("MainActivity", "Uncaught exception", e)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.Builder().apply {
                    scheme("https")
                    authority("zhihu-plus.internal")
                    appendPath("error")
                    appendQueryParameter("title", "Uncaught exception: ${e.message}")
                    appendQueryParameter(
                        "message",
                        e.message
                    )
                    appendQueryParameter("stack", e.stackTraceToString())
                }.build(),
                this,
                MainActivity::class.java
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
                Log.i("MainActivity", "Content filter maintenance cleanup completed")
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to perform content filter cleanup", e)
            }
        }

        webview = WebView(this)
        Log.i("MainActivity", "Webview created")
        webview.setupUpWebviewClient()
        webview.settings.javaScriptEnabled = true
        webview.loadUrl("https://zhihu-plus.internal/assets/zse.html")

        setContent {
            navController = rememberNavController()
            ZhihuTheme {
                ZhihuMain(navController = navController)
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
                        AlertDialog.Builder(this).apply {
                            setTitle(title)
                            setMessage(stack)
                            setPositiveButton("OK") { _, _ ->
                            }
                            setNeutralButton("Copy") { _, _ ->
                                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = android.content.ClipData.newPlainText("error", "$stack")
                                clipboard.setPrimaryClip(clip)
                            }
                        }.create().show()
                    }
                }
            }
        }

        ImageLoader.Builder(this)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .build().also { loader ->
                SingletonImageLoader.setSafe {
                    loader
                }
            }

        // 初始化TTS
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // 设置语言
            val result = textToSpeech?.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("MainActivity", "Language not supported")
            } else {
                isTtsInitialized = true
            }
        } else {
            Log.e("MainActivity", "Initialization failed")
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            if (intent.data != null) {
                if (intent.data!!.authority != "zhihu-plus.internal") {
                    Log.i("MainActivity", "Intent data: ${intent.data}")
                    val destination = resolveContent(intent.data!!)
                    if (destination != null) {
                        if (destination != sharedData.clipboardDestination) {
                            sharedData.clipboardDestination = destination
                            navigate(destination, popup = true)
                        }
                    } else {
                        AlertDialog.Builder(this).apply {
                            setTitle("Unsupported URL")
                            setMessage("Unknown URL: ${intent.data}")
                            setPositiveButton("OK") { _, _ ->
                            }
                        }.create().show()
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
        navController.navigate(route) {
            if (popup) {
                launchSingleTop = true
                popUpTo(Home) {
                    // clear the back stack and viewModels
                    saveState = true
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun signRequest96(url: String): String {
        val dc0 = AccountData.data.cookies["d_c0"] ?: ""
        val pathname = "/" + url.substringAfter("//").substringAfter('/')
        val signSource = "$ZSE93+$pathname+$dc0"
        val md5 = MessageDigest.getInstance("MD5").digest(signSource.toByteArray()).toHexString()
        val timeStart = System.currentTimeMillis()
        val future = CompletableDeferred<String>()
        runOnUiThread {
            webview.evaluateJavascript("exports.encrypt('$md5')") {
                Log.i("MainActivity", "Sign request: $url")
                Log.i("MainActivity", "Sign source: $signSource")
                Log.i("MainActivity", "Sign result: $it")
                val time = System.currentTimeMillis() - timeStart
                Log.i("MainActivity", "Sign time: $time ms")
                future.complete(it.trim('"'))
            }
        }
        return "2.0_" + future.await()
    }

    fun postHistory(dest: NavDestination) {
        history.add(dest)
    }

    // TTS相关方法
    fun speakText(text: String) {
        if (isTtsInitialized && textToSpeech != null) {
            // 将长文本分段
            currentTextChunks = splitTextIntoChunks(text, maxChunkLength).toMutableList()
            currentChunkIndex = 0
            speakNextChunk()
        }
    }

    private fun speakNextChunk() {
        if (currentChunkIndex < currentTextChunks.size && isTtsInitialized) {
            val chunk = currentTextChunks[currentChunkIndex]
            textToSpeech?.speak(
                chunk,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "chunk_$currentChunkIndex"
            )
            currentChunkIndex++

            // 设置朗读完成监听器，自动播放下一段
            textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId?.startsWith("chunk_") == true) {
                        // 延迟一点再播放下一段，避免太快
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            speakNextChunk()
                        }, 500)
                    }
                }

                override fun onError(utteranceId: String?) {}
            })
        }
    }

    private fun splitTextIntoChunks(text: String, maxLength: Int): List<String> {
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
        currentTextChunks.clear()
        currentChunkIndex = 0
    }

    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }

    override fun onDestroy() {
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    companion object {
        const val ZSE93 = "101_3_3.0"
    }
}
