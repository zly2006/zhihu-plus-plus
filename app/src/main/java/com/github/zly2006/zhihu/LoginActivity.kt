package com.github.zly2006.zhihu

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.enableEdgeToEdgeCompat
import com.github.zly2006.zhihu.util.telemetry
import kotlinx.coroutines.runBlocking

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeCompat()

        setContent {
            val scope = rememberCoroutineScope()
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            ) {
                WebviewComp(
                    onLoad = { webView ->
                        webView.setupUpWebviewClient()
                        @SuppressLint("SetJavaScriptEnabled")
                        webView.settings.javaScriptEnabled = true
                        CookieManager.getInstance().removeAllCookies { }
                        webView.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest,
                            ): Boolean {
                                if (request.url.toString() == "https://www.zhihu.com/") {
                                    webView.settings.userAgentString = AccountData.ANDROID_USER_AGENT
                                }
                                if (request.url?.scheme == "zhihu") {
                                    return true
                                }
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url == "https://www.zhihu.com/") {
                                    val cookies =
                                        CookieManager
                                            .getInstance()
                                            .getCookie("https://www.zhihu.com/")
                                            .orEmpty()
                                            .split(";")
                                            .associate {
                                                it.substringBefore("=").trim() to it.substringAfter("=")
                                            }
                                    runBlocking {
                                        if (AccountData.verifyLogin(this@LoginActivity, cookies)) {
                                            val data = AccountData.loadData(this@LoginActivity)

                                            val preferences = this@LoginActivity.getSharedPreferences(
                                                PREFERENCE_NAME,
                                                MODE_PRIVATE,
                                            )
                                            print(preferences.toString())

                                            AlertDialog
                                                .Builder(this@LoginActivity)
                                                .apply {
                                                    setTitle("登录成功")
                                                    setMessage("欢迎回来，${data.username}")
                                                    setPositiveButton("OK") { _, _ ->
                                                    }
                                                }.create()
                                                .show()
                                            AccountData.saveData(this@LoginActivity, data)
                                            telemetry(this@LoginActivity, "login")
                                            // back to the main activity
                                            this@LoginActivity.finish()
                                            return@runBlocking true
                                        } else {
                                            AlertDialog
                                                .Builder(this@LoginActivity)
                                                .apply {
                                                    setTitle("登录失败")
                                                    setMessage("请检查用户名和密码")
                                                    setPositiveButton("OK") { _, _ ->
                                                    }
                                                }.create()
                                                .show()
                                            return@runBlocking false
                                        }
                                    }
                                }
                            }
                        }
                        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                        webView.loadUrl("https://www.zhihu.com/signin")
                    },
                )
            }
        }
    }
}
