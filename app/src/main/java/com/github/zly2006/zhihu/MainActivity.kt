package com.github.zly2006.zhihu

import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.HistoryStorage
import com.github.zly2006.zhihu.v2.theme.ZhihuTheme
import com.github.zly2006.zhihu.v2.ui.ZhihuMain
import com.github.zly2006.zhihu.v2.ui.components.setupUpWebview
import kotlinx.coroutines.CompletableDeferred
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    lateinit var webview: WebView
    lateinit var history: HistoryStorage

    lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        history = HistoryStorage(this)
        AccountData.loadData(this)
        webview = WebView(this)
        Log.i("MainActivity", "Webview created")
        setupUpWebview(webview, this)
        webview.settings.javaScriptEnabled = true
        webview.loadUrl("https://zhihu-plus.internal/assets/zse.html")

        setContent {
            navController = rememberNavController()
            ZhihuTheme {
                ZhihuMain(navController = navController)
            }
        }
    }

    var readClipboard = false
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        val uri = intent.data
        if (!readClipboard && hasFocus) {
            if (uri != null) {
                Log.i("MainActivity", "Intent data: $uri")
                val destination = resolveContent(uri)
                if (destination != null) {
                    navigate(destination)
                } else {
                    AlertDialog.Builder(this).apply {
                        setTitle("Unsupported URL")
                        setMessage("Unknown URL: $uri")
                        setPositiveButton("OK") { _, _ ->
                        }
                    }.create().show()
                }
            } else {
                // read clipboard
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text
                    if (text != null) {
                        val regex = Regex("""https?://[-a-zA-Z0-9()@:%_+.~#?&/=]*""")
                        regex.findAll(text).firstNotNullOfOrNull {
                            resolveContent(Uri.parse(it.value))
                        }?.let {
                            navigate(it)
                        }
                    }
                }
            }
        }
    }

    fun navigate(route: NavDestination) {
        navController.navigate(route)
        history.add(route)
    }

    suspend fun signRequest96(url: String): String {
        val dc0 = AccountData.data.cookies["d_c0"] ?: ""
        val zse93 = "101_3_3.0"
        val pathname = "/" + url.substringAfter("//").substringAfter('/')
        val signSource = "$zse93+$pathname+$dc0"
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
}
