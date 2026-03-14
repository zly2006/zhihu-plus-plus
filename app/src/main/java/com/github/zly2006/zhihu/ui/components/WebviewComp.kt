@file:Suppress("unused")

package com.github.zly2006.zhihu.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentDialog
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.size
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.github.chrisbanes.photoview.PhotoView
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.Video
import com.github.zly2006.zhihu.WebviewActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.AccountData.json
import com.github.zly2006.zhihu.resolveContent
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.util.blacklist
import com.github.zly2006.zhihu.util.extractImageUrl
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.util.shareImage
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

// HTML 点击事件监听器接口
fun interface HtmlClickListener {
    fun onElementClick(element: Element)
}

/**
 * 获取最高清晰度的视频URL
 */
@OptIn(DelicateCoroutinesApi::class)
suspend fun getHighestQualityVideoUrl(context: Context, httpClient: HttpClient, videoId: String, contentId: String, contentType: String = "answer"): String? = try {
    val response = httpClient.post("https://www.zhihu.com/api/v4/video/play_info?r=$videoId") {
        contentType(ContentType.Application.Json)
        header("x-xsrftoken", AccountData.data.cookies["_xsrf"])
        header("x-app-za", "OS=webplayer")
        header("x-referer", "")
        setBody(
            """{"content_id":"$contentId","content_type_str":"$contentType","video_id":"$videoId","scene_code":"answer_detail_web","is_only_video":true}""",
        )
        signFetchRequest()
    }

    val responseText = response.bodyAsText()
    val jsonResponse = json
        .parseToJsonElement(responseText)
        .jsonObject

    val videoPlay = jsonResponse["video_play"]?.jsonObject
    val playlist = videoPlay?.get("playlist")?.jsonObject
    val mp4List = playlist?.get("mp4")?.jsonArray

    // 找到最高质量的视频
    var bestVideo: JsonObject? = null
    var maxBitrate = -1

    mp4List?.forEach { videoElement ->
        val video = videoElement.jsonObject
        val bitrate = video["bitrate"]?.jsonPrimitive?.intOrNull ?: 0
        if (bitrate > maxBitrate) {
            maxBitrate = bitrate
            bestVideo = video
        }
    }

    // 获取视频URL
    bestVideo
        ?.get("url")
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonPrimitive
        ?.content
} catch (e: Exception) {
    Log.e("VideoDownload", "Error getting video URL: ${e.message}")
    null
}

class CustomWebView : WebView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * Constructs a CustomWebView and prepare everything for displaying content, then call onLoad callback when ready.
     */
    @OptIn(DelicateCoroutinesApi::class)
    constructor(context: Context, onLoad: (CustomWebView) -> Unit, useHardwareAcceleration: Boolean) : this(context) {
        if (useHardwareAcceleration) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        } else {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
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
            if (result.type == HitTestResult.IMAGE_TYPE ||
                result.type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            ) {
                val imgElement = document?.select("img[src='${result.extra}']")?.first()
                val url = imgElement?.let { extractImageUrl(it) }
                    ?: result.extra?.takeIf { !it.startsWith("data") }
                if (url != null) {
                    menu.add("查看图片").setOnMenuItemClickListener {
                        openImage(AccountData.httpClient(context), url)
                        true
                    }
                    menu.add("在浏览器中打开").setOnMenuItemClickListener {
                        luoTianYiUrlLauncher(context, url.toUri())
                        true
                    }
                    menu.add("保存图片").setOnMenuItemClickListener {
                        GlobalScope.launch(Dispatchers.Main) {
                            saveImageToGallery(context, AccountData.httpClient(context), url)
                        }
                        true
                    }
                    menu.add("分享图片").setOnMenuItemClickListener {
                        GlobalScope.launch(Dispatchers.Main) {
                            shareImage(context, AccountData.httpClient(context), url)
                        }
                        true
                    }
                }
            }
        }
    }

    var document: Document? = null
        private set
    var contentId: String? = null
    private var htmlClickListener: HtmlClickListener? = null
    var scrollToHeightCallback: ((Int, Int) -> Unit)? = null
    var onContentHeightCallback: ((Int) -> Unit)? = null
    var onPageStartedCallback: (() -> Unit)? = null

    override fun scrollTo(x: Int, y: Int) {
        // 禁止 WebView 自己滚动，所有滚动都通过 scrollToHeightCallback 回调到 Compose 层处理
        super.scrollTo(0, 0)
    }

    /**
     * 在 WebView ，allow 父 View 拦截触摸事件，
     * 使 Compose 的 nestedScroll/overscroll 能接管所有的拖动。
     */
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(false)
        if (event.actionMasked == android.view.MotionEvent.ACTION_MOVE) {
            super.scrollTo(0, 0)
            return super.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    // JavaScript 接口类
    inner class JsInterface {
        @JavascriptInterface
        fun onElementClick(outerHtml: String) {
            val clicked = Jsoup.parse(outerHtml).body().child(0)
            findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                htmlClickListener?.onElementClick(clicked)
            }
        }

        @JavascriptInterface
        fun scrollToHeight(y: Int, maxY: Int) {
            scrollToHeightCallback?.invoke(y, maxY)
        }

        @JavascriptInterface
        fun onContentHeight(height: Int) {
            onContentHeightCallback?.invoke(height)
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
            val url = extractImageUrl(clicked)
            if (url != null) {
                val httpClient = AccountData.httpClient(context)
                this.openImage(httpClient, url)
            }
        } else if (clicked.tagName() == "a" && clicked.hasClass("video-box")) {
            // 处理视频链接点击
            val videoId = clicked.attr("data-lens-id")
            (context as? MainActivity)?.navigate(Video(videoId.toLong()))
        }
    }

    fun applyThemeStyle() {
        val jsCode = if (ThemeManager.isDarkTheme) {
            "document.body.classList.add('dark-theme');"
        } else {
            "document.body.classList.remove('dark-theme');"
        }

        evaluateJavascript(jsCode, null)
    }

    /**
     * 从 assets 文件夹加载 JavaScript 文件内容
     */
    private fun loadJavaScriptFromAssets(fileName: String): String = try {
        context.assets.open(fileName).use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        }
    } catch (e: Exception) {
        Log.e("WebView-Assets", "Failed to load JavaScript file: $fileName", e)
        ""
    }

    /**
     * 注入点击监听的 JavaScript 代码
     * 这个方法应该在页面加载完成后调用
     */
    fun injectClickListenerScript() {
        if (htmlClickListener != null) {
            val jsCode = loadJavaScriptFromAssets("click-listener.js")
            if (jsCode.isNotEmpty()) {
                evaluateJavascript(jsCode, null)
            }
        }
    }

    /**
     * 注入高度上报脚本：立即上报一次，并在所有图片加载完成后再上报。
     * 使用 document.body.scrollHeight（不含 margin），通过 AndroidInterface.onContentHeight 回调。
     */
    fun injectContentHeightReporter() {
        if (onContentHeightCallback == null) return
        val js =
            """
            (function() {
                function report() {
                    if (!document.getElementById('end')) {
                        const div = document.createElement('div');
                        div.id = 'end';
                        document.body.appendChild(div);
                    }
                    const bottom = document.getElementById('end').getBoundingClientRect().bottom;
                    if (window.AndroidInterface && AndroidInterface.onContentHeight) {
                        AndroidInterface.onContentHeight(bottom);
                    }
                }
                report();
                // 图片加载完成后再上报一次（处理懒加载图片导致高度变化的情况）
                var imgs = document.querySelectorAll('img');
                var pending = imgs.length;
                if (pending === 0) return;
                function onImgDone() {
                    report();
                }
                imgs.forEach(function(img) {
                    if (img.complete) { onImgDone(); }
                    else { img.addEventListener('load', onImgDone); img.addEventListener('error', onImgDone); }
                });
                setTimeout(report, 3000); // 3秒后强制上报一次，防止有些图片既不触发 load 也不触发 error
            })();
            """.trimIndent()
        evaluateJavascript(js, null)
    }

    fun injectFootnoteScript() {
        val jsCode = loadJavaScriptFromAssets("footnotes.js")
        if (jsCode.isNotEmpty()) {
            evaluateJavascript(jsCode, null)
        }
    }

    fun openImage(httpClient: HttpClient, url: String) {
        OpenImageDislog(context, httpClient, url).show()
    }

    fun loadZhihu(
        url: String,
        document: Document,
        additionalStyle: String = "",
    ) {
        if (this.document?.html() == document.html()) {
            // same content
            return
        }
        Log.i("CustomWebView", "Loading content for URL: $url with document title: ${document.title()}")
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val fontSize = preferences.getInt("webviewFontSize", 100)
        val lineHeight = preferences.getInt("webviewLineHeight", 160)
        val customFontFile = java.io.File(context.filesDir, "custom_font")
        val customFontCss = if (preferences.contains("webviewCustomFontName") && customFontFile.exists()) {
            val fontName = preferences.getString("webviewCustomFontName", "") ?: ""
            val format = if (fontName.endsWith(".otf", ignoreCase = true)) "opentype" else "truetype"
            "@font-face { font-family: 'ZhihuCustomFont'; src: url('https://zhihu-plus.internal/user-files/custom_font') format('$format'); }\n" +
                "body { font-family: 'ZhihuCustomFont', sans-serif; }"
        } else {
            ""
        }

        val bodyClass = if (ThemeManager.isDarkTheme) " class=\"dark-theme\" " else ""

        loadDataWithBaseURL(
            url,
            """
            <head>
            <link rel="stylesheet" href="https://zhihu-plus.internal/assets/stylesheet.css">
            <viewport content="width=device-width, initial-scale=1.0">
            </viewport>
            <style>
            body {
                font-size: $fontSize%;
                line-height: ${lineHeight / 100f};
            }
            $customFontCss
            ${additionalStyle.replace("\n", "")}
            </style>
            </head>
            <body $bodyClass>
            ${document.body().html()}
            </body>
            """.trimIndent(),
            "text/html",
            "utf-8",
            null,
        )
        this.document = document
    }

    override fun startActionMode(callback: ActionMode.Callback): ActionMode? =
        super.startActionMode(CustomActionModeCallback(callback))

    override fun startActionMode(callback: ActionMode.Callback, type: Int): ActionMode? =
        super.startActionMode(CustomActionModeCallback(callback), type)

    override fun startActionModeForChild(originalView: View, callback: ActionMode.Callback): ActionMode? =
        super.startActionModeForChild(originalView, CustomActionModeCallback(callback))

    override fun startActionModeForChild(originalView: View, callback: ActionMode.Callback, type: Int): ActionMode? =
        super.startActionModeForChild(originalView, CustomActionModeCallback(callback), type)

    class CustomActionModeCallback(
        private val originalCallback: ActionMode.Callback,
    ) : ActionMode.Callback2() {
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val result = originalCallback.onPrepareActionMode(mode, menu)

            menu?.let {
                val size = it.size
                for (i in size - 1 downTo 0) {
                    val item = it.getItem(i)
                    val packageName = item.intent?.component?.packageName ?: return@let
                    // 过滤黑名单
                    if (blacklist.any { it in packageName }) {
                        item.isVisible = false
                    }
                }
            }
            return result
        }

        override fun onCreateActionMode(p0: ActionMode?, p1: Menu?) =
            originalCallback.onCreateActionMode(p0, p1)

        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?) =
            originalCallback.onActionItemClicked(p0, p1)

        override fun onDestroyActionMode(p0: ActionMode?) =
            originalCallback.onDestroyActionMode(p0)

        override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: Rect?) {
            if (originalCallback is ActionMode.Callback2) {
                originalCallback.onGetContentRect(mode, view, outRect)
            } else {
                super.onGetContentRect(mode, view, outRect)
            }
        }
    }
}

class OpenImageDislog(
    context: Context,
    private val httpClient: HttpClient,
    private val url: String,
) : ComponentDialog(context) {
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val photoView = PhotoView(context).apply {
            setBackgroundColor(Color.BLACK)
            setOnClickListener { dismiss() }
        }
        setContentView(photoView)
        window?.setBackgroundDrawable(Color.BLACK.toDrawable())
        setCanceledOnTouchOutside(true)
        lifecycleScope.launch {
            httpClient
                .get(url)
                .bodyAsChannel()
                .toInputStream()
                .buffered()
                .use {
                    val bitmap = BitmapFactory.decodeStream(it)
                    withContext(Dispatchers.Main) {
                        photoView.setImageBitmap(bitmap)
                    }
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun WebviewComp(
    modifier: Modifier = Modifier.fillMaxSize(),
    scrollState: ScrollState? = null,
    existingWebView: CustomWebView? = null,
    onLoad: (CustomWebView) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    val useHardwareAcceleration = preferences.getBoolean("webviewHardwareAcceleration", true)
    // JS 上报的内容高度（CSS 像素 = dp，WebView viewport 默认 1 CSS px = 1 dp）；
    // 0 表示尚未收到上报（等待 onPageFinished），此时用 wrapContentSize 避免撑满未知高度
    var contentHeightDp by remember { mutableIntStateOf(0) }

    AndroidView(
        factory = { ctx ->
            val webView = if (existingWebView != null) {
                (existingWebView.parent as? ViewGroup)?.removeView(existingWebView)
                existingWebView
            } else {
                CustomWebView(ctx, onLoad, useHardwareAcceleration)
            }
            FrameLayout(ctx).apply { addView(webView) }
        },
        update = { frameLayout ->
            val view = frameLayout.getChildAt(0) as? CustomWebView ?: run {
                frameLayout.removeAllViews()
                val newWebView = CustomWebView(frameLayout.context, onLoad, useHardwareAcceleration)
                frameLayout.addView(newWebView)
                newWebView
            }
            if (scrollState != null) {
                view.scrollToHeightCallback = { elementY, maxY ->
                    coroutineScope.launch {
                        scrollState.animateScrollTo(elementY * scrollState.maxValue / maxY)
                    }
                }
            }
            view.onContentHeightCallback = { height ->
                coroutineScope.launch(Dispatchers.Main) { contentHeightDp = height }
            }
            view.onPageStartedCallback = { coroutineScope.launch(Dispatchers.Main) { contentHeightDp = 0 } }
            onLoad(view)
        },
        modifier = if (contentHeightDp > 0) {
            modifier.height(contentHeightDp.dp)
        } else {
            modifier.wrapContentSize()
        },
        onRelease = { frameLayout ->
            val view = frameLayout.getChildAt(0) as? CustomWebView ?: return@AndroidView
            if (existingWebView != null) {
                (view.parent as? ViewGroup)?.removeView(view)
            } else {
                view.stopLoading()
                view.webChromeClient = null
                view.clearHistory()
                view.clearCache(true)
                view.destroy()
            }
        },
    )
}

private class UserFilesPathHandler(
    private val context: Context,
) : WebViewAssetLoader.PathHandler {
    override fun handle(path: String): WebResourceResponse? {
        val file = java.io.File(context.filesDir, path)
        if (!file.exists() || !file.isFile) return null
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val mimeType = when {
            path == "custom_font" -> {
                val fontName = preferences.getString("webviewCustomFontName", "") ?: ""
                if (fontName.endsWith(".otf", ignoreCase = true)) "font/otf" else "font/ttf"
            }
            else -> "application/octet-stream"
        }
        return WebResourceResponse(
            mimeType,
            null,
            200,
            "OK",
            mapOf("Access-Control-Allow-Origin" to "*"),
            file.inputStream(),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun WebView.setupUpWebviewClient(onPageFinished: ((String) -> Unit)? = null) {
    setBackgroundColor(Color.TRANSPARENT)
    settings.javaScriptEnabled = true
    val context = this.context
    val assetLoader = WebViewAssetLoader
        .Builder()
        .setDomain("zhihu-plus.internal")
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .addPathHandler("/user-files/", UserFilesPathHandler(context))
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

            if (request.url.host == "www.zhihu.com" && request.url.path == "/equation") {
                if (ThemeManager.isDarkTheme) {
                }
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
                    luoTianYiUrlLauncher(context, it.toUri())
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
            if (request.url.host!!.endsWith("zhihu.com") && view.context !is WebviewActivity) {
                context.startActivity(Intent(Intent.ACTION_VIEW, request.url, context, WebviewActivity::class.java))
                return true
            }
            if (!request.url.host!!.endsWith("zhihu.com")) {
                luoTianYiUrlLauncher(context, request.url)
                return true
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (view is CustomWebView) view.onPageStartedCallback?.invoke()
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            Log.i("WebView-Page", "Page finished loading: $url")

            // 如果是 CustomWebView，在页面加载完成后注入点击监听脚本和主题样式
            if (view is CustomWebView) {
                view.injectClickListenerScript()
                // 注入脚注处理脚本
                view.injectFootnoteScript()
                // 注入主题样式
                view.applyThemeStyle()
                // 上报内容高度（图片加载完成后再上报一次）
                view.injectContentHeightReporter()
            }

            onPageFinished?.invoke(url)
        }
    }
}
