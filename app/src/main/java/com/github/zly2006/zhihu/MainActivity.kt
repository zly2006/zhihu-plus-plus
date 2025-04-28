package com.github.zly2006.zhihu

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.HistoryStorage
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.ZhihuMain
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import kotlinx.coroutines.CompletableDeferred
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    class SharedData : ViewModel() {
        var clipboardDestination: NavDestination? = null
    }

    val sharedData by viewModels<SharedData>()
    lateinit var webview: WebView
    lateinit var history: HistoryStorage
    val httpClient by lazy {
        AccountData.httpClient(this)
    }

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
        enableEdgeToEdge()
        history = HistoryStorage(this)
        AccountData.loadData(this)
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
            if (intent.data != null) {
                if (intent.data!!.authority == "zhihu-plus.internal") {
                    if (intent.data!!.path == "/error") {
                        val title = intent.data!!.getQueryParameter("title")
                        val message = intent.data!!.getQueryParameter("message")
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
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            if (intent.data != null) {
                if (intent.data!!.authority != "zhihu-plus.internal") {
                    Log.i("MainActivity", "Intent data: ${intent.data}")
                    val destination = resolveContent(intent.data!!)
                    if (destination != null) {
                        navigate(destination, popup = true)
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
                            resolveContent(Uri.parse(it.value))
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
            launchSingleTop = true
            if (popup) {
                popUpTo(Home) {
                    // clear the back stack and viewModels
                    saveState = true
                }
            }
        }
    }

    suspend fun signRequest96(url: String): String {
        val dc0 = AccountData.data.cookies["d_c0"] ?: ""
        val pathname = "/" + url.substringAfter("//").substringAfter('/')
        val signSource = "$ZSE93+$pathname+$dc0"
        val md5 = MessageDigest.getInstance("MD5").digest(signSource.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
        val future = CompletableDeferred<String>()
        runOnUiThread {
            webview.evaluateJavascript("exports.encrypt('$md5')") {
                Log.i("MainActivity", "Sign request: $url")
                Log.i("MainActivity", "Sign source: $signSource")
                Log.i("MainActivity", "Sign result: $it")
                future.complete(it.trim('"'))
            }
        }
        return "2.0_" + future.await()
    }

    fun postHistory(dest: NavDestination) {
        history.add(dest)
    }

    companion object {
        const val ZSE93 = "101_3_3.0"
    }
}
