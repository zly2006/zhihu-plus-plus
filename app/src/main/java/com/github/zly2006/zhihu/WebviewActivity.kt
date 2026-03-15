package com.github.zly2006.zhihu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.webkit.WebViewClientCompat
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.enableEdgeToEdgeCompat
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import io.ktor.http.Url

class WebviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.data?.toString() ?: run {
            finish()
            return
        }
        enableEdgeToEdgeCompat()
        setContent {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            ) {
                AndroidView(
                    factory = { ctx ->
                        val webView = WebView(ctx)
                        webView.setupUpWebviewClient {
                            val js = intent.extras?.getString("javascript")
                            if (js != null) {
                                webView.evaluateJavascript(js, null)
                            }
                        } // 覆盖掉原有的 WebViewClient，因为我们需要全屏显示
                        setupCookies(webView)
                        webView.settings.userAgentString = AccountData.ANDROID_USER_AGENT
                        @SuppressLint("SetJavaScriptEnabled")
                        webView.settings.javaScriptEnabled = true
                        webView.loadUrl(url)
                        val js = intent.extras?.getString("javascript")
                        if (js != null) {
                            webView.evaluateJavascript(js, null)
                        }

                        webView.webViewClient = object : WebViewClientCompat() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                if (request.url.host == "link.zhihu.com") {
                                    Url(request.url.toString()).parameters["target"]?.let {
                                        luoTianYiUrlLauncher(this@WebviewActivity, it.toUri())
                                        return true
                                    }
                                } else if (request.url.host == "www.zhihu.com" || request.url.host == "zhuanlan.zhihu.com" || request.url.scheme == "zhihu") {
                                    val destination = resolveContent(request.url)
                                    if (destination != null) {
                                        val intent = Intent(this@WebviewActivity, MainActivity::class.java).apply {
                                            action = Intent.ACTION_VIEW
                                            data = request.url
                                        }
                                        this@WebviewActivity.startActivity(intent)
                                        this@WebviewActivity.finish()
                                        return true
                                    } else if (request.url.pathSegments.getOrNull(0) == "done") {
                                        this@WebviewActivity.finish()
                                    }
                                }
                                return super.shouldOverrideUrlLoading(view, request)
                            }
                        }
                        webView
                    },
                )
            }
        }
    }

    private fun setupCookies(webView: WebView) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        AccountData.loadData(this).cookies.forEach { (name, value) ->
            cookieManager.setCookie(
                "zhihu.com",
                "$name=$value; domain=.zhihu.com; path=/",
            )
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean = request.url.scheme == "zhihu" &&
                request.url.host == "webviewform"
        }
        webView.settings.userAgentString = AccountData.ANDROID_USER_AGENT
        cookieManager.flush()
    }
}
