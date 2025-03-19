package com.github.zly2006.zhihu.v2

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.HistoryStorage
import com.github.zly2006.zhihu.ui.home.setupUpWebview
import com.github.zly2006.zhihu.v2.theme.ZhihuTheme
import com.github.zly2006.zhihu.v2.ui.ZhihuMain
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
        val data = AccountData.getData(this)
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

    fun navigate(route: NavDestination) {
        navController.navigate(route)
    }

    suspend fun signRequest96(url: String): String {
        val dc0 = AccountData.getData().cookies["d_c0"] ?: ""
        val zse93 = "101_3_3.0"
        val pathname = "/" + url.substringAfter("//").substringAfter('/')
        val signSource = "$zse93+$pathname+$dc0"
        val md5 = MessageDigest.getInstance("MD5").digest(signSource.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
        val future = CompletableDeferred<String>()
        webview.evaluateJavascript("exports.encrypt('$md5')") {
            Log.i("MainActivity", "Sign request: $url")
            Log.i("MainActivity", "Sign source: $signSource")
            Log.i("MainActivity", "Sign result: $it")
            future.complete(it.trim('"'))
        }
        return "2.0_" + future.await()
    }

    fun postHistory(dest: NavDestination) {
        history.post(dest)
    }
}

