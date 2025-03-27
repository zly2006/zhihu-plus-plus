package com.github.zly2006.zhihu.v2.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.github.chrisbanes.photoview.PhotoView
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.WebviewActivity
import com.github.zly2006.zhihu.resolveContent
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.nodes.Document

@Composable
fun WebviewComp(
    httpClient: HttpClient,
    modifier: Modifier = Modifier.fillMaxSize(),
    onLoad: (WebView) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setupUpWebview(this, ctx) {
//                    evaluateJavascript("document.body.scrollHeight") { value ->
//                        val height = value.toIntOrNull() ?: 0
//                        if (height != 0) {
//                            val layoutParams = layoutParams
//                            layoutParams.height = height
//                            this.layoutParams = layoutParams
//                        }
//                    }
                }
                onLoad(this)
                setOnLongClickListener { view ->
                    view.showContextMenu()
                }
                setOnCreateContextMenuListener { menu, v, _ ->
                    val result = (v as WebView).hitTestResult
                    if (result.type == WebView.HitTestResult.IMAGE_TYPE ||
                        result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                    ) {
                        if (!result.extra!!.startsWith("data:")) {
                            menu.add("查看图片").setOnMenuItemClickListener {
                                val dialog = object : ComponentDialog(context) {
                                    init {
                                        val url = result.extra!!
                                        requestWindowFeature(Window.FEATURE_NO_TITLE)
                                        setContentView(
                                            PhotoView(context).apply {
                                                GlobalScope.launch {
                                                    httpClient.get(url).bodyAsChannel().toInputStream()
                                                        .buffered().use {
                                                            val bitmap = BitmapFactory.decodeStream(it)
                                                            context.mainExecutor.execute {
                                                                setImageBitmap(bitmap)
                                                            }
                                                        }
                                                }
                                                setImageURI(Uri.parse(url))
                                                setBackgroundColor(android.graphics.Color.BLACK)
                                                setOnClickListener { dismiss() }
                                            }
                                        )
                                        window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
                                        setCanceledOnTouchOutside(true)
                                    }

                                    override fun onCreate(savedInstanceState: Bundle?) {
                                        super.onCreate(savedInstanceState)
                                        window?.setLayout(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                }
                                dialog.show()
                                true
                            }
                        }
                        menu.add("在浏览器中打开").setOnMenuItemClickListener {
                            result.extra?.let { url ->
                                CustomTabsIntent.Builder()
                                    .setToolbarColor(0xff66CCFF.toInt())
                                    .build()
                                    .launchUrl(context, Uri.parse(url))
                            }
                            true
                        }
                        menu.add("保存图片").setOnMenuItemClickListener {
                            result.extra?.let { url ->
                                coroutineScope.launch {
                                    try {
                                        val response = httpClient.get(url)
                                        val bytes = response.readBytes()
                                        val fileName = Uri.parse(url).lastPathSegment ?: "downloaded_image"
                                        val file = Environment.getExternalStoragePublicDirectory(
                                            Environment.DIRECTORY_PICTURES
                                        ).resolve(fileName)
                                        file.outputStream().use { it.write(bytes) }
                                        Toast.makeText(
                                            context,
                                            "图片已保存至: ${file.absolutePath}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "保存失败: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            true
                        }
                    }
                }
            }
        },
        onRelease = WebView::destroy,
        modifier = modifier
    )
}

fun WebView.loadUrl(
    url: String,
    document: Document,
) {
    loadDataWithBaseURL(
        url,
        """
        <head>
        <link rel="stylesheet" href="//zhihu-plus.internal/assets/stylesheet.css">
        <viewport content="width=device-width, initial-scale=1.0">
        </head>
        """.trimIndent() + document.toString(),
        "text/html",
        "utf-8",
        null
    )
}

fun setupUpWebview(web: WebView, context: Context, onPageFinished: (() -> Unit)? = null) {
    web.setBackgroundColor(Color.TRANSPARENT)
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
            } else if (request.url.host == "www.zhihu.com" || request.url.host == "zhuanlan.zhihu.com" || request.url.scheme == "zhihu") {
                val destination = resolveContent(request.url)
                if (destination != null) {
                    if (context is MainActivity) {
                        context.navigate(destination)
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, request.url, context, MainActivity::class.java)
                        context.startActivity(intent)
                    }
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, request.url, context, WebviewActivity::class.java)
                    context.startActivity(intent)
                }
                return true
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onPageFinished?.invoke()
        }
    }
}
