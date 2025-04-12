package com.github.zly2006.zhihu.v2.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
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


private class CustomWebView : WebView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var document: Document? = null
}

@Composable
fun WebviewComp(
    httpClient: HttpClient,
    modifier: Modifier = Modifier.fillMaxSize(),
    onLoad: (WebView) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
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
                                val dialog = object : ComponentDialog(context) {
                                    init {
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
                                                setBackgroundColor(Color.BLACK)
                                                setOnClickListener { dismiss() }
                                            }
                                        )
                                        window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
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
                            menu.add("在浏览器中打开").setOnMenuItemClickListener {
                                CustomTabsIntent.Builder()
                                    .setToolbarColor(0xff66CCFF.toInt())
                                    .build()
                                    .launchUrl(context, Uri.parse(url))
                                true
                            }
                            menu.add("保存图片").setOnMenuItemClickListener {
                                coroutineScope.launch {
                                    try {
                                        val response = httpClient.get(url)
                                        val bytes = response.readBytes()
                                        val fileName = Uri.parse(url).lastPathSegment ?: "downloaded_image.jpg"

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

                                            Toast.makeText(
                                                context,
                                                "图片已保存到相册",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "保存失败: ${e.message}",
                                            Toast.LENGTH_SHORT
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
        onRelease = WebView::destroy,
        modifier = modifier
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                "body {font-family: \"${Typeface.DEFAULT.systemFontFamilyName}\", sans-serif;}" else ""
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
        null
    )
    if (this is CustomWebView) {
        this.document = document
    }
}

fun WebView.setupUpWebviewClient(onPageFinished: (() -> Unit)? = null) {
    setBackgroundColor(Color.TRANSPARENT)
    val context = this.context
    val assetLoader = WebViewAssetLoader.Builder()
        .setDomain("zhihu-plus.internal")
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()
    this.webViewClient = object : WebViewClientCompat() {
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
