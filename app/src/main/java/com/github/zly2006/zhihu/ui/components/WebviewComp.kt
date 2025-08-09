@file:Suppress("unused")

package com.github.zly2006.zhihu.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.Window
import android.webkit.JavascriptInterface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.github.chrisbanes.photoview.PhotoView
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.WebviewActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.resolveContent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

// HTML 点击事件监听器接口
fun interface HtmlClickListener {
    fun onElementClick(element: Element)
}

class CustomWebView : WebView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var document: Document? = null
    private var htmlClickListener: HtmlClickListener? = null

    // JavaScript 接口类
    inner class JsInterface {
        @OptIn(DelicateCoroutinesApi::class)
        @JavascriptInterface
        fun onElementClick(outerHtml: String) {
            val clicked = Jsoup.parse(outerHtml).body().child(0)
            GlobalScope.launch(Dispatchers.Main) {
                htmlClickListener?.onElementClick(clicked)
            }
        }
    }

    /**
     * 设置 HTML 点击事件监听器
     * 通过注入 JavaScript 监听页面中所有元素的点击事件
     */
    fun setHtmlClickListener(listener: HtmlClickListener?) {
        this.htmlClickListener = listener

        if (listener != null) {
            // 添加 JavaScript 接口
            addJavascriptInterface(JsInterface(), "AndroidInterface")

            // 启用 JavaScript
            @Suppress("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
        } else {
            // 移除 JavaScript 接口
            removeJavascriptInterface("AndroidInterface")
        }
    }

    fun defaultHtmlClickListener(): HtmlClickListener = HtmlClickListener { clicked ->
        if (clicked.tagName() == "img") {
            val url =
                clicked.attr("data-original").takeIf { it.isNotBlank() }
                    ?: clicked.attr("data-default-watermark-src").takeIf { it.isNotBlank() }
                    ?: clicked.attr("src").takeIf { it.isNotBlank() }
            if (url != null) {
                val httpClient = AccountData.httpClient(context)
                this.openImage(httpClient, url)
            }
        }
    }

    /**
     * 注入点击监听的 JavaScript 代码
     * 这个方法应该在页面加载完成后调用
     */
    fun injectClickListenerScript() {
        if (htmlClickListener != null) {
            val jsCode =
                """
                (function() {
                    // 移除之前的监听器（如果存在）
                    if (window.zhihuPlusClickListener) {
                        document.removeEventListener('click', window.zhihuPlusClickListener, true);
                    }
                    
                    // 创建新的监听器
                    window.zhihuPlusClickListener = function(event) {
                        try {
                            // 获取被点击元素的 outerHTML
                            var outerHtml = event.target.outerHTML;
                            // 调用 Android 接口
                            if (window.AndroidInterface) {
                                AndroidInterface.onElementClick(outerHtml);
                            }
                        } catch (e) {
                            console.error('Error in click listener:', e);
                        }
                    };
                    
                    // 添加点击事件监听器
                    document.addEventListener('click', window.zhihuPlusClickListener, true);
                })();
                """.trimIndent()

            evaluateJavascript(jsCode, null)
        }
    }

    fun openImage(httpClient: HttpClient, url: String) {
        val dialog = object : ComponentDialog(context) {
            init {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(
                    PhotoView(context).apply {
                        GlobalScope.launch {
                            httpClient
                                .get(url)
                                .bodyAsChannel()
                                .toInputStream()
                                .buffered()
                                .use {
                                    val bitmap = BitmapFactory.decodeStream(it)
                                    context.mainExecutor.execute {
                                        setImageBitmap(bitmap)
                                    }
                                }
                        }
                        setImageURI(url.toUri())
                        setBackgroundColor(Color.BLACK)
                        setOnClickListener { dismiss() }
                    },
                )
                window?.setBackgroundDrawable(Color.BLACK.toDrawable())
                setCanceledOnTouchOutside(true)
            }

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        }
        dialog.show()
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun WebviewComp(
    modifier: Modifier = Modifier.fillMaxSize(),
    onLoad: (CustomWebView) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val httpClient = AccountData.httpClient(context)
    val preferences = context.getSharedPreferences("com.github.zly2006.zhihu.preferences", Context.MODE_PRIVATE)
    val useHardwareAcceleration = preferences.getBoolean("webviewHardwareAcceleration", true)

    AndroidView(
        factory = { ctx ->
            CustomWebView(ctx).apply {
                // 根据用户设置决定是否启用硬件加速
                if (useHardwareAcceleration) {
                    setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                } else {
                    setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
                }

                this.setupUpWebviewClient {
                }
                this.setHtmlClickListener(this.defaultHtmlClickListener())
                onLoad(this)
                setOnLongClickListener { view ->
                    view.showContextMenu()
                }
                setOnCreateContextMenuListener { menu, v, _ ->
                    val result = hitTestResult
                    if (result.type == WebView.HitTestResult.IMAGE_TYPE ||
                        result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                    ) {
                        val imgElement = document?.select("img[src='${result.extra}']")?.first()
                        val dataOriginalUrl = imgElement?.attr("data-original")?.takeIf { it.isNotBlank() }
                        val url = dataOriginalUrl ?: result.extra?.takeIf { !it.startsWith("data") }
                        if (url != null) {
                            menu.add("查看图片").setOnMenuItemClickListener {
                                openImage(httpClient, url)
                                true
                            }
                            menu.add("在浏览器中打开").setOnMenuItemClickListener {
                                CustomTabsIntent
                                    .Builder()
                                    .setToolbarColor(0xff66CCFF.toInt())
                                    .build()
                                    .launchUrl(context, url.toUri())
                                true
                            }
                            menu.add("保存图片").setOnMenuItemClickListener {
                                coroutineScope.launch {
                                    try {
                                        val response = httpClient.get(url)
                                        val bytes = response.readRawBytes()
                                        val fileName = url.toUri().lastPathSegment ?: "downloaded_image.jpg"

                                        val contentValues = ContentValues().apply {
                                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                                                put(MediaStore.MediaColumns.IS_PENDING, 1)
                                            }
                                        }

                                        val resolver = context.contentResolver
                                        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                                        } else {
                                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                        }

                                        val imageUri = resolver.insert(collection, contentValues)
                                        if (imageUri != null) {
                                            resolver.openOutputStream(imageUri).use { os ->
                                                os?.write(bytes)
                                            }

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                contentValues.clear()
                                                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                                resolver.update(imageUri, contentValues, null, null)
                                            }

                                            Toast
                                                .makeText(
                                                    context,
                                                    "图片已保存到相册",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast
                                            .makeText(
                                                context,
                                                "保存失败: ${e.message}",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    }
                                }
                                true
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier,
    )
}

fun WebView.loadZhihu(
    url: String,
    document: Document,
    additionalStyle: String = "",
) {
    loadDataWithBaseURL(
        url,
        """
            <head>
            <link rel="stylesheet" href="https://zhihu-plus.internal/assets/stylesheet.css">
            <viewport content="width=device-width, initial-scale=1.0">
            </viewport>
            <style>
            ${
            // This is a workaround for the issue where the system font family name is not available in the WebView.
            // https://github.com/zly2006/zhihu-plus-plus/issues/9
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                "body {font-family: \"${Typeface.DEFAULT.systemFontFamilyName}\", sans-serif;}"
            } else {
                ""
            }
        }
            ${additionalStyle.replace("\n", "")}
            </style>
            </head>
            <body>
            ${document.body().html()}
            </body>
        """.trimIndent(),
        "text/html",
        "utf-8",
        null,
    )
    if (this is CustomWebView) {
        this.document = document
    }
}

fun WebView.setupUpWebviewClient(onPageFinished: (() -> Unit)? = null) {
    setBackgroundColor(Color.TRANSPARENT)
    val context = this.context
    val assetLoader = WebViewAssetLoader
        .Builder()
        .setDomain("zhihu-plus.internal")
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()
    this.webViewClient = object : WebViewClientCompat() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (request.url.host == "link.zhihu.com") {
                Url(request.url.toString()).parameters["target"]?.let {
                    val intent = CustomTabsIntent.Builder().setToolbarColor(0xff66CCFF.toInt()).build()
                    intent.launchUrl(context, it.toUri())
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

            // 如果是 CustomWebView，在页面加载完成后注入点击监听脚本
            if (view is CustomWebView) {
                view.injectClickListenerScript()
            }

            onPageFinished?.invoke()
        }
    }
}
