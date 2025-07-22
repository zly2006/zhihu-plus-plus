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
import androidx.browser.customtabs.CustomTabsIntent
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
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

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
                                request: WebResourceRequest
                            ): Boolean {
                                if (request.url.toString() == "https://www.zhihu.com/") {
                                    webView.loadUrl("https://www.zhihu.com/question/11474985081")
                                    return true
                                }
                                if (request.url.host == "graph.qq.com") {
                                    // QQ login
                                    CustomTabsIntent.Builder()
                                        .setToolbarColor(0xff66CCFF.toInt())
                                        .build()
                                        .launchUrl(this@LoginActivity, request.url)
                                    return true
                                }
                                if (request.url?.scheme == "zhihu") {
                                    return true
                                }
                                return false
                            }

                            var loadedJs = false

                            override fun onLoadResource(view: WebView?, url: String) {
                                super.onLoadResource(view, url)
                                if (url.startsWith("https://static.zhihu.com/zse-ck/v4/")) {
                                    if (!loadedJs) {
                                        loadedJs = true
                                    }
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url == "https://www.zhihu.com/question/11474985081") {
                                    val cookies =
                                        CookieManager.getInstance().getCookie("https://www.zhihu.com/").split(";")
                                            .associate {
                                                it.substringBefore("=").trim() to it.substringAfter("=")
                                            }
                                    runBlocking {
                                        if (!loadedJs) {
                                            delay(1000)
                                            if (!loadedJs) {
                                                AlertDialog.Builder(this@LoginActivity).apply {
                                                    setTitle("登录失败")
                                                    setMessage("模拟正常登录环境失败，请检查网络")
                                                    setPositiveButton("OK") { _, _ ->
                                                    }
                                                }.create().show()
                                                return@runBlocking false
                                            }
                                        }
                                        if (AccountData.verifyLogin(this@LoginActivity, cookies)) {
                                            val data = AccountData.loadData(this@LoginActivity)

                                            val preferences = this@LoginActivity.getSharedPreferences(
                                                PREFERENCE_NAME,
                                                MODE_PRIVATE
                                            )
                                            print(preferences.toString())

                                            AlertDialog.Builder(this@LoginActivity).apply {
                                                setTitle("登录成功")
                                                setMessage("欢迎回来，${data.username}")
                                                setPositiveButton("OK") { _, _ ->
                                                }
                                            }.create().show()
                                            // back to the main activity
                                            scope.launch(mainExecutor.asCoroutineDispatcher()) {
                                                delay(5000)
                                                webView.evaluateJavascript("document.cookie") {
                                                    data.cookies.putAll(
                                                        it.removeSurrounding("\"")
                                                            .removeSurrounding("\'")
                                                            .split(";").associate {
                                                                it.substringBefore("=").trim() to it.substringAfter("=")
                                                            })
                                                    if ("__zse_ck" !in data.cookies) {
                                                        AlertDialog.Builder(this@LoginActivity).apply {
                                                            setTitle("登录失败")
                                                            setMessage("模拟正常登录环境失败，请检查网络")
                                                            setPositiveButton("OK") { _, _ ->
                                                            }
                                                        }.create().show()
                                                    } else {
                                                        AccountData.saveData(this@LoginActivity, data)
                                                        if (preferences.getBoolean("allowTelemetry", true)) {
                                                            scope.launch(mainExecutor.asCoroutineDispatcher()) {
                                                                runCatching {
                                                                    AccountData.httpClient(this@LoginActivity)
                                                                        .post("https://redenmc.com/api/zhihu/login") {
                                                                            setBody(Json.encodeToString(data))
                                                                            contentType(ContentType.Application.Json)
                                                                            header(
                                                                                HttpHeaders.UserAgent,
                                                                                "Zhihu++/${BuildConfig.VERSION_NAME}"
                                                                            )
                                                                        }
                                                                }
                                                            }
                                                        }
                                                        this@LoginActivity.finish()
                                                    }
                                                }
                                            }
                                            return@runBlocking true
                                        } else {
                                            AlertDialog.Builder(this@LoginActivity).apply {
                                                setTitle("登录失败")
                                                setMessage("请检查用户名和密码")
                                                setPositiveButton("OK") { _, _ ->
                                                }
                                            }.create().show()
                                            return@runBlocking false
                                        }
                                    }
                                }
                            }
                        }
                        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                        webView.loadUrl("https://www.zhihu.com/signin")
                    }
                )
            }
        }
    }

}
