@file:Suppress("unused")

package com.github.zly2006.zhihu.ui.components

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
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
        signFetchRequest(context)
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

    var document: Document? = null
        private set
    var contentId: String? = null
    private var htmlClickListener: HtmlClickListener? = null

    // JavaScript 接口类
    inner class JsInterface {
        @JavascriptInterface
        fun onElementClick(outerHtml: String) {
            val clicked = Jsoup.parse(outerHtml).body().child(0)
            findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
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

    /**
     * 应用主题样式到WebView
     * 根据应用的主题设置为body添加或移除dark-theme类
     */
    fun applyThemeStyle() {
        val preferences = context.getSharedPreferences("com.github.zly2006.zhihu_preferences", Context.MODE_PRIVATE)
        val themeModeValue = preferences.getString("themeMode", "SYSTEM") ?: "SYSTEM"

        // 判断是否应该应用暗色主题
        val shouldApplyDarkTheme = when (themeModeValue) {
            "LIGHT" -> false
            "DARK" -> true
            else -> { // SYSTEM
                // 检查系统是否为暗色模式
                val currentNightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }

        val jsCode = if (shouldApplyDarkTheme) {
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
     * 注入脚注处理的 JavaScript 代码
     * 处理 data-draft-type="reference" 的 sup 标签，生成脚注列表
     */
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
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val fontSize = preferences.getInt("webviewFontSize", 100)
        val lineHeight = preferences.getInt("webviewLineHeight", 160)
        val customFontFile = java.io.File(context.filesDir, "custom_font")
        val customFontCss = if (preferences.contains("webviewCustomFontName") && customFontFile.exists()) {
            val base64 = android.util.Base64.encodeToString(customFontFile.readBytes(), android.util.Base64.NO_WRAP)
            "@font-face { font-family: 'ZhihuCustomFont'; src: url('data:font/truetype;base64,$base64'); }\n" +
                "body { font-family: 'ZhihuCustomFont', sans-serif; }"
        } else {
            ""
        }

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
            <body>
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
                            withContext(Dispatchers.Main) {
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

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun WebviewComp(
    modifier: Modifier = Modifier.fillMaxSize(),
    existingWebView: CustomWebView? = null,
    onLoad: (CustomWebView) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val httpClient = AccountData.httpClient(context)
    val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    val useHardwareAcceleration = preferences.getBoolean("webviewHardwareAcceleration", true)

    AndroidView(
        factory = { ctx ->
            val wv = if (existingWebView != null) {
                (existingWebView.parent as? ViewGroup)?.removeView(existingWebView)
                existingWebView
            } else {
                CustomWebView(ctx).apply {
                    if (useHardwareAcceleration) {
                        setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                    } else {
                        setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
                    }
                }
            }
            wv.apply {
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
                        val url = imgElement?.let { extractImageUrl(it) }
                            ?: result.extra?.takeIf { !it.startsWith("data") }
                        if (url != null) {
                            menu.add("查看图片").setOnMenuItemClickListener {
                                openImage(httpClient, url)
                                true
                            }
                            menu.add("在浏览器中打开").setOnMenuItemClickListener {
                                luoTianYiUrlLauncher(context, url.toUri())
                                true
                            }
                            menu.add("保存图片").setOnMenuItemClickListener {
                                coroutineScope.launch {
                                    saveImageToGallery(context, httpClient, url)
                                }
                                true
                            }
                            menu.add("分享图片").setOnMenuItemClickListener {
                                coroutineScope.launch {
                                    shareImage(context, httpClient, url)
                                }
                                true
                            }
                        }
                    }
                }
            }
        },
        update = {
            onLoad(it)
        },
        modifier = modifier,
        onRelease = {
            it.stopLoading()
            it.webChromeClient = null
            it.clearHistory()
            it.clearCache(true)
            it.destroy()
        },
    )
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
            }

            onPageFinished?.invoke(url)
        }
    }
}
