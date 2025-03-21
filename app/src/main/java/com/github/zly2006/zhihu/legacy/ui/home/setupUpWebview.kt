package com.github.zly2006.zhihu.legacy.ui.home

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.github.zly2006.zhihu.LegacyMainActivity
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.HistoryStorage.Companion.navigate
import com.github.zly2006.zhihu.resolveContent
import io.ktor.http.*

fun setupUpWebview(web: WebView, context: Context) {
    web.setBackgroundColor(Color.TRANSPARENT)
    web.settings.javaScriptEnabled = true
    val assetLoader = WebViewAssetLoader.Builder()
        .setDomain("zhihu-plus.internal")
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()
    web.webViewClient = object : WebViewClientCompat() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            return assetLoader.shouldInterceptRequest(request.url)
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (request.url.host == "link.zhihu.com") {
                Url(request.url.toString()).parameters["target"]?.let {
                    val intent = CustomTabsIntent.Builder().setToolbarColor(0xff66CCFF.toInt()).build()
                    intent.launchUrl(context, Uri.parse(it))
                    return true
                }
            } else if (request.url.host == "www.zhihu.com") {
                val destination = resolveContent(request.url)
                if (destination != null) {
                    if (context is LegacyMainActivity) {
                        context.navigate(destination)
                    }
                    if (context is MainActivity) {
                        context.navigate(destination)
                    }
                }
                return true
            }
            return super.shouldOverrideUrlLoading(view, request)
        }
    }
}
