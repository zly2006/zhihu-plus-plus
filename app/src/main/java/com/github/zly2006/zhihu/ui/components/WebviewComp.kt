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
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.webkit.*
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
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.github.chrisbanes.photoview.PhotoView
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.WebviewActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.resolveContent
import io.ktor.client.*
import io.ktor.client.request.*
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

fun WebView.setupUpWebviewClient(onPageFinished: ((String) -> Unit)? = null) {
    setBackgroundColor(Color.TRANSPARENT)
    val context = this.context
    val assetLoader = WebViewAssetLoader
        .Builder()
        .setDomain("zhihu-plus.internal")
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    // 设置WebChromeClient来监控控制台消息和加载进度
    this.webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            consoleMessage?.let { msg ->
                val logLevel = when (msg.messageLevel()) {
                    ConsoleMessage.MessageLevel.ERROR -> Log.ERROR
                    ConsoleMessage.MessageLevel.WARNING -> Log.WARN
                    ConsoleMessage.MessageLevel.DEBUG -> Log.DEBUG
                    else -> Log.INFO
                }
                Log.println(logLevel, "WebView-Console", "${msg.sourceId()}:${msg.lineNumber()} - ${msg.message()}")
            }
            return true
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            Log.d("WebView-Progress", "Page loading progress: $newProgress%")
        }
    }

    this.webViewClient = object : WebViewClientCompat() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val url = request.url.toString()

            // 记录所有资源请求
            Log.d("WebView-Resource", "Requesting: $url")

            // 特别记录图片请求
            if (url.contains(".jpg", true) ||
                url.contains(".jpeg", true) ||
                url.contains(".png", true) ||
                url.contains(".gif", true) ||
                url.contains(".webp", true) ||
                url.contains(".svg", true)
            ) {
                Log.i("WebView-Image", "Loading image: $url")
            }

            return assetLoader.shouldInterceptRequest(request.url)
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
            super.onReceivedError(view, request, error)
            val url = request.url.toString()
            val errorDescription = "${error.errorCode}: ${error.description}"

            // 记录资源加载错误
            Log.e("WebView-Error", "Failed to load resource: $url - $errorDescription")

            // 特别记录图片加载失败
            if (url.contains(".jpg", true) ||
                url.contains(".jpeg", true) ||
                url.contains(".png", true) ||
                url.contains(".gif", true) ||
                url.contains(".webp", true) ||
                url.contains(".svg", true)
            ) {
                Log.e("WebView-Image-Error", "Failed to load image: $url - $errorDescription")
            }
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            super.onReceivedHttpError(view, request, errorResponse)
            val url = request.url.toString()
            val statusCode = errorResponse.statusCode
            val reasonPhrase = errorResponse.reasonPhrase ?: "unknown"

            // 记录HTTP错误
            Log.e("WebView-HTTP-Error", "HTTP error for: $url - $statusCode $reasonPhrase")

            // 特别记录图片HTTP错误
            if (url.contains(".jpg", true) ||
                url.contains(".jpeg", true) ||
                url.contains(".png", true) ||
                url.contains(".gif", true) ||
                url.contains(".webp", true) ||
                url.contains(".svg", true)
            ) {
                Log.e("WebView-Image-HTTP-Error", "Image HTTP error: $url - $statusCode $reasonPhrase")
            }
        }

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

        override fun onPageFinished(view: WebView?, url: String) {
            super.onPageFinished(view, url)
            Log.i("WebView-Page", "Page finished loading: $url")

            // 注入JavaScript来监控图片加载状态
            if (BuildConfig.DEBUG) {
                val imageMonitorScript =
                    """
                    (function() {
                        // 监控所有图片元素的加载状态
                        function monitorImages() {
                            const images = document.querySelectorAll('img');
                            images.forEach(function(img, index) {
                                // 如果图片还没有加载完成
                                if (!img.complete) {
                                    img.addEventListener('load', function() {
                                        console.log('Image loaded successfully: ' + img.src);
                                    });
                                    
                                    img.addEventListener('error', function() {
                                        console.error('Image failed to load: ' + img.src);
                                        console.error('Image alt text: ' + img.alt);
                                        console.error('Image data-original: ' + img.getAttribute('data-original'));
                                    });
                                } else if (img.naturalWidth === 0) {
                                    // 图片已经"完成"但实际上是损坏的
                                    console.error('Image appears to be broken: ' + img.src);
                                }
                            });
                        }
                        
                        // 立即执行一次
                        monitorImages();
                        
                        // 监控动态添加的图片
                        const observer = new MutationObserver(function(mutations) {
                            mutations.forEach(function(mutation) {
                                mutation.addedNodes.forEach(function(node) {
                                    if (node.nodeType === 1) { // Element node
                                        if (node.tagName === 'IMG') {
                                            // 新添加的图片元素
                                            node.addEventListener('load', function() {
                                                console.log('Dynamic image loaded successfully: ' + node.src);
                                            });
                                            node.addEventListener('error', function() {
                                                console.error('Dynamic image failed to load: ' + node.src);
                                            });
                                        } else {
                                            // 检查新添加元素内的图片
                                            const imgs = node.querySelectorAll && node.querySelectorAll('img');
                                            if (imgs) {
                                                imgs.forEach(function(img) {
                                                    img.addEventListener('load', function() {
                                                        console.log('Nested image loaded successfully: ' + img.src);
                                                    });
                                                    img.addEventListener('error', function() {
                                                        console.error('Nested image failed to load: ' + img.src);
                                                    });
                                                });
                                            }
                                        }
                                    }
                                });
                            });
                        });
                        
                        observer.observe(document.body, {
                            childList: true,
                            subtree: true
                        });
                    })();
                    """.trimIndent()

                view?.evaluateJavascript(imageMonitorScript, null)
            }

            // 如果是 CustomWebView，在页面加载完成后注入点击监听脚本
            if (view is CustomWebView) {
                view.injectClickListenerScript()
            }

            onPageFinished?.invoke(url)
        }
    }
}
