package com.github.zly2006.zhihu

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.enableEdgeToEdgeCompat

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
                WebviewComp(
                    onLoad = { webView ->
                        webView.setupUpWebviewClient() // 覆盖掉原有的 WebViewClient，因为我们需要全屏显示
                        setupCookies(webView)
                        webView.settings.javaScriptEnabled = true
                        webView.loadUrl(url)
                    }
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
                "$name=$value; domain=.zhihu.com; path=/"
            )
        }
        cookieManager.flush()
    }
}
