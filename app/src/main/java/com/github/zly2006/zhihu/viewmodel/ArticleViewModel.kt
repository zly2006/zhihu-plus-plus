package com.github.zly2006.zhihu.viewmodel

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.ui.Collection
import com.github.zly2006.zhihu.ui.CollectionResponse
import com.github.zly2006.zhihu.ui.Reaction
import com.github.zly2006.zhihu.ui.VoteUpState
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArticleViewModel(
    private val article: Article,
    val httpClient: HttpClient?,
) : ViewModel() {
    val permissionRequested = MutableLiveData<Unit>()
    var title by mutableStateOf("")
    var authorId by mutableStateOf("")
    var authorUrlToken by mutableStateOf("")
    var authorName by mutableStateOf("")
    var authorBio by mutableStateOf("")
    var authorAvatarSrc by mutableStateOf("")
    var content by mutableStateOf("")
    var voteUpCount by mutableIntStateOf(0)
    var commentCount by mutableIntStateOf(0)
    var voteUpState by mutableStateOf(VoteUpState.Neutral)
    var questionId by mutableLongStateOf(0L)
    var collections = mutableStateListOf<Collection>()
    var updatedAt by mutableLongStateOf(0L)
    var createdAt by mutableLongStateOf(0L)
    var ipInfo by mutableStateOf<String?>(null)
    var nextAnswerFuture: Deferred<Feed> = CompletableDeferred()

    // scroll fix  
    var rememberedScrollY = MutableLiveData<Int>(0)
    var rememberedScrollYSync = true

    val isFavorited: Boolean
        get() = collections.any { it.isFavorited }

    // 检查存储权限
    fun hasStoragePermission(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+ 只需要媒体权限
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else {
        // Android 12及以下需要读写外部存储权限
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    // 请求存储权限
    fun requestStoragePermission(activity: Activity) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }
        ActivityCompat.requestPermissions(activity, permissions, 1001) // 使用请求码1001
    }

    // todo: replace this with sqlite
    class ArticlesSharedData : ViewModel() {
        var viewingQuestionId: Long = 0L
        var nextUrl: String = ""
        var destinations = mutableListOf<Feed>()
    }

    @OptIn(ExperimentalStdlibApi::class, DelicateCoroutinesApi::class)
    fun loadArticle(context: Context) {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (article.type == ArticleType.Answer) {
                        DataHolder.getAnswerCallback(context, httpClient, article.id) { answer ->
                            if (answer != null) {
                                title = answer.question.title
                                authorName = answer.author.name
                                authorId = answer.author.id
                                authorUrlToken = answer.author.urlToken
                                content = answer.content
                                authorBio = answer.author.headline
                                authorAvatarSrc = answer.author.avatarUrl
                                voteUpCount = answer.voteupCount
                                commentCount = answer.commentCount
                                questionId = answer.question.id
                                voteUpState = when (answer.relationship?.voting) {
                                    1 -> VoteUpState.Up
                                    -1 -> VoteUpState.Down
                                    else -> VoteUpState.Neutral
                                }
                                updatedAt = answer.updatedTime
                                createdAt = answer.createdTime
                                ipInfo = answer.ipInfo

                                (context as? MainActivity)?.postHistory(
                                    Article(
                                        id = answer.id,
                                        type = ArticleType.Answer,
                                        title = answer.question.title,
                                        authorName = answer.author.name,
                                        authorBio = answer.author.headline,
                                        avatarSrc = answer.author.avatarUrl,
                                        excerpt = answer.excerpt,
                                    ),
                                )
                                val sharedData by (context as MainActivity).viewModels<ArticlesSharedData>()
                                nextAnswerFuture = GlobalScope.async {
                                    if (sharedData.destinations.isEmpty() || sharedData.viewingQuestionId != questionId) {
                                        val url =
                                            if (questionId == sharedData.viewingQuestionId && sharedData.nextUrl.isNotEmpty()) {
                                                sharedData.nextUrl
                                            } else {
                                                "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=2"
                                            }
                                        val response =
                                            httpClient.get(url) {
                                                signFetchRequest(context)
                                            }
                                        val jojo = response.body<JsonObject>()
                                        if ("data" !in jojo) {
                                            Log.e("ArticleViewModel", "No data found in response: $jojo")
                                            context.mainExecutor.execute {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "获取回答列表失败: ${jojo["message"]?.jsonPrimitive?.content ?: "未知错误"}",
                                                        Toast.LENGTH_LONG,
                                                    ).show()
                                            }
                                        }
                                        val data = AccountData.decodeJson<List<Feed>>(jojo["data"]!!)
                                        sharedData.nextUrl =
                                            jojo["paging"]
                                                ?.jsonObject
                                                ?.get("next")
                                                ?.jsonPrimitive
                                                ?.content ?: ""
                                        sharedData.viewingQuestionId = questionId
                                        sharedData.destinations = data
                                            .filter {
                                                it.target?.navDestination is Article && it != article // filter out the current article
                                            }.toMutableList()
                                    }
                                    sharedData.destinations.removeAt(0)
                                }
                            } else {
                                content = "<h1>回答不存在</h1>"
                                Log.e("ArticleViewModel", "Answer not found")
                            }
                        }
                    } else if (article.type == ArticleType.Article) {
                        DataHolder.getArticleCallback(context, httpClient, article.id) { article ->
                            if (article != null) {
                                title = article.title
                                content = article.content
                                voteUpCount = article.voteupCount
                                commentCount = article.commentCount
                                authorId = article.author.id
                                authorUrlToken = article.author.urlToken
                                authorName = article.author.name
                                authorBio = article.author.headline
                                authorAvatarSrc = article.author.avatarUrl
                                voteUpState = when (article.relationship?.voting) {
                                    1 -> VoteUpState.Up
                                    -1 -> VoteUpState.Down
                                    else -> VoteUpState.Neutral
                                }
                                updatedAt = article.updated
                                createdAt = article.created
                                ipInfo = article.ipInfo

                                (context as? MainActivity)?.postHistory(
                                    Article(
                                        id = article.id,
                                        type = ArticleType.Article,
                                        title = article.title,
                                        authorName = article.author.name,
                                        authorBio = article.author.headline,
                                        avatarSrc = article.author.avatarUrl,
                                        excerpt = article.excerpt,
                                    ),
                                )
                            } else {
                                content = "<h1>文章不存在</h1>"
                                Log.e("ArticleViewModel", "Article not found")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ArticleViewModel", "Failed to load content", e)
                }
            }
        }
    }

    fun toggleFavorite(collectionId: String, remove: Boolean, context: Context) {
        if (httpClient == null) return
        viewModelScope.launch {
            try {
                val contentType = when (article.type) {
                    ArticleType.Answer -> "answer"
                    ArticleType.Article -> "article"
                }
                val action = if (remove) "remove" else "add"
                val url = "https://api.zhihu.com/collections/contents/$contentType/${article.id}"
                val body = "${action}_collections=$collectionId"

                val response = httpClient.put(url) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(body)
                }

                if (response.status.isSuccess()) {
                    loadCollections()
                    Toast.makeText(context, if (remove) "取消收藏成功" else "收藏成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "收藏操作失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Favorite toggle failed", e)
                Toast.makeText(context, "收藏操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val collectionOrder = mutableListOf<String>()

    fun loadCollections() {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val contentType = when (article.type) {
                        ArticleType.Answer -> "answer"
                        ArticleType.Article -> "article"
                    }
                    val collectionsUrl = "https://api.zhihu.com/collections/contents/$contentType/${article.id}"
                    val collectionsResponse = httpClient.get(collectionsUrl)

                    if (collectionsResponse.status.isSuccess()) {
                        val jojo = collectionsResponse.body<JsonObject>()
                        val collectionsData = AccountData.decodeJson<CollectionResponse>(jojo)
                        collections.clear()
                        collections.addAll(
                            collectionsData.data
                                .sortedWith { a, b ->
                                    val indexA = collectionOrder.indexOf(a.id)
                                    val indexB = collectionOrder.indexOf(b.id)
                                    when {
                                        indexA == -1 && indexB == -1 -> 0
                                        // 把新的放前面
                                        indexA == -1 -> -1
                                        indexB == -1 -> 1
                                        else -> indexA.compareTo(indexB)
                                    }
                                },
                        )
                        collectionOrder.clear()
                        collectionOrder.addAll(collections.map { it.id })
                    }
                } catch (e: Exception) {
                    Log.e("ArticleViewModel", "Failed to load collections", e)
                }
            }
        }
    }

    fun createNewCollection(context: Context, title: String, description: String = "", isPublic: Boolean = false) {
        if (httpClient == null) return
        viewModelScope.launch {
            httpClient.post("https://www.zhihu.com/api/v4/collections") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("title", title)
                        put("description", description)
                        put("is_public", isPublic)
                    },
                )
                signFetchRequest(context)
            }
            loadCollections()
        }
    }

    fun toggleVoteUp(context: Context, newState: VoteUpState) {
        if (httpClient == null) return
        viewModelScope.launch {
            try {
                val endpoint = when (article.type) {
                    ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${article.id}/voters"
                    ArticleType.Article -> "https://www.zhihu.com/api/v4/articles/${article.id}/voters"
                }

                val response = httpClient
                    .post(endpoint) {
                        when (article.type) {
                            ArticleType.Answer -> setBody(mapOf("type" to newState.key))
                            ArticleType.Article -> setBody(mapOf("voting" to if (newState == VoteUpState.Up) 1 else 0))
                        }
                        contentType(ContentType.Application.Json)
                    }.body<Reaction>()

                voteUpState = newState
                voteUpCount = response.voteup_count
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Vote up failed", e)
                Toast.makeText(context, "点赞失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 导出为PDF
    suspend fun exportToPdf(context: Context, onComplete: (Boolean) -> Unit) {
        if (!hasStoragePermission(context)) {
            withContext(Dispatchers.Main) {
                requestStoragePermission(context as Activity)
                permissionRequested.value = Unit
                Toast.makeText(context, "需要存储权限才能导出PDF，正在请求权限", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val pdfDocument = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
                    val page = pdfDocument.startPage(pageInfo)
                    var canvas = page.canvas

                    // 创建简单的文本内容
                    val paint = android.graphics.Paint().apply {
                        textSize = 14f
                        color = android.graphics.Color.BLACK
                        isAntiAlias = true
                    }

                    var yPosition = 50f
                    val lineHeight = 20f
                    val maxWidth = 545f // 留出边距

                    // 标题
                    paint.textSize = 18f
                    paint.isFakeBoldText = true
                    val titleLines = breakTextIntoLines(title, paint, maxWidth)
                    for (line in titleLines) {
                        canvas.drawText(line, 25f, yPosition, paint)
                        yPosition += lineHeight * 1.5f
                    }

                    // 作者信息
                    paint.textSize = 12f
                    paint.isFakeBoldText = false
                    yPosition += 20f
                    canvas.drawText("作者: $authorName", 25f, yPosition, paint)
                    yPosition += lineHeight

                    if (authorBio.isNotEmpty()) {
                        canvas.drawText("简介: $authorBio", 25f, yPosition, paint)
                        yPosition += lineHeight
                    }

                    yPosition += 20f

                    // 内容 - 使用HTML解析
                    paint.textSize = 14f
                    val plainText = Jsoup.parse(content).text()
                    val contentLines = breakTextIntoLines(plainText, paint, maxWidth)

                    for (line in contentLines) {
                        if (yPosition > 800f) { // 如果页面快满了，创建新页面
                            pdfDocument.finishPage(page)
                            val newPage = pdfDocument.startPage(pageInfo)
                            val newCanvas = newPage.canvas
                            yPosition = 50f
                            // 继续绘制剩余内容到新页面
                            canvas = newCanvas
                        }
                        canvas.drawText(line, 25f, yPosition, paint)
                        yPosition += lineHeight
                    }

                    pdfDocument.finishPage(page)

                    // 保存PDF文件到应用专属目录
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "Zhihu_${article.type}_${article.id}_$timeStamp.pdf"
                    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

                    FileOutputStream(file).use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }

                    pdfDocument.close()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "PDF已保存到: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                        onComplete(true)
                    }
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "PDF export failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDF导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }
        }
    }

    // 导出为图片 - 使用WebView渲染
    suspend fun exportToImage(context: Context, onComplete: (Boolean) -> Unit) {
        if (!hasStoragePermission(context)) {
            withContext(Dispatchers.Main) {
                requestStoragePermission(context as Activity)
                permissionRequested.value = Unit
                Toast.makeText(context, "需要存储权限才能导出图片，正在请求权限", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
            return
        }
        GlobalScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    // 在主线程中创建和配置WebView
                    val webView = android.webkit.WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false
                    }

                    // 创建HTML内容
                    val htmlContent = createHtmlContent(includeComments = false, commentCount = 0)

                    // 设置WebViewClient来监听加载完成
                    var isLoaded = false
                    val timeoutRunnable = Runnable {
                        if (!isLoaded) {
                            webView.destroy()
                            Toast.makeText(context, "图片导出超时", Toast.LENGTH_SHORT).show()
                            onComplete(false)
                        }
                    }
                    webView.postDelayed(timeoutRunnable, 10000) // 10秒超时
                    webView.webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (!isLoaded) {
                                //   = true
                                webView.removeCallbacks(timeoutRunnable)
                                // 确保 WebView 测量出足以容纳所有内容的尺寸
                                val contentWidth = webView.measuredWidth // 使用当前的宽度（通常是屏幕宽度）
                                // 获取整个网页内容的实际高度（很重要）
                                val contentHeight = (webView.contentHeight * webView.scale).toInt()

                                if (contentWidth > 0 && contentHeight > 0) {
                                    // 1. 手动测量
                                    webView.measure(
                                        View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
                                        View.MeasureSpec.makeMeasureSpec(contentHeight, View.MeasureSpec.EXACTLY),
                                    )

                                    // 2. 手动布局
                                    webView.layout(0, 0, contentWidth, contentHeight)
                                }
                                // 页面加载完成后，延迟一下确保渲染完成，然后截图
                                view?.postDelayed({
                                    GlobalScope.launch {
                                        captureWebViewToImage(webView, context, onComplete)
                                    }
                                }, 1000)
                            }
                        }

                        override fun onReceivedError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            if (!isLoaded) {
                                isLoaded = true
                                webView.removeCallbacks(timeoutRunnable)
                                webView.destroy()
                                Toast.makeText(context, "图片导出失败: 加载错误", Toast.LENGTH_SHORT).show()
                                onComplete(false)
                            }
                        }
                    }

                    // 加载HTML内容
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Image export failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "图片导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }
        }
    }

    // 导出为带评论的图片 - 使用WebView渲染
    suspend fun exportToImageWithComments(context: Context, commentCount: Int, onComplete: (Boolean) -> Unit) {
        if (!hasStoragePermission(context)) {
            withContext(Dispatchers.Main) {
                requestStoragePermission(context as Activity)
                permissionRequested.value = Unit
                Toast.makeText(context, "需要存储权限才能导出图片，正在请求权限", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    // 在主线程中创建和配置WebView
                    val webView = android.webkit.WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false
                    }

                    // 创建包含评论的HTML内容
                    val htmlContent = createHtmlContent(includeComments = true, commentCount = commentCount)

                    // 设置WebViewClient来监听加载完成
                    var isLoaded = false
                    val timeoutRunnable = Runnable {
                        if (!isLoaded) {
                            webView.destroy()
                            Toast.makeText(context, "带评论图片导出超时", Toast.LENGTH_SHORT).show()
                            onComplete(false)
                        }
                    }
                    webView.postDelayed(timeoutRunnable, 15000) // 15秒超时，给更多时间加载评论
                    webView.webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (!isLoaded) {
                                isLoaded = true
                                webView.removeCallbacks(timeoutRunnable)
                                // 页面加载完成后，延迟一下确保渲染完成，然后截图
                                view?.postDelayed({
                                    GlobalScope.launch {
                                        captureWebViewToImage(webView, context, onComplete, "with_comments")
                                    }
                                }, 1500) // 给更多时间加载评论
                            }
                        }

                        override fun onReceivedError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            if (!isLoaded) {
                                isLoaded = true
                                webView.removeCallbacks(timeoutRunnable)
                                webView.destroy()
                                Toast.makeText(context, "带评论图片导出失败: 加载错误", Toast.LENGTH_SHORT).show()
                                onComplete(false)
                            }
                        }
                    }

                    // 加载HTML内容
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Image with comments export failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "带评论图片导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }
        }
    }

    // 创建HTML内容
    private fun createHtmlContent(includeComments: Boolean, commentCount: Int): String {
        val css = """
            <style>
                body {
                    font-family: 'PingFang SC', 'Helvetica Neue', STHeiti, 'Microsoft Yahei', sans-serif;
                    margin: 20px;
                    padding: 0;
                    background: white;
                    color: #333;
                    line-height: 1.6;
                }
                .title {
                    font-size: 24px;
                    font-weight: bold;
                    margin-bottom: 20px;
                    color: #333;
                    line-height: 1.4;
                }
                .author {
                    font-size: 16px;
                    color: #666;
                    margin-bottom: 10px;
                }
                .bio {
                    font-size: 14px;
                    color: #999;
                    margin-bottom: 20px;
                }
                .content {
                    font-size: 16px;
                    line-height: 1.8;
                    margin-bottom: 30px;
                }
                .content img {
                    max-width: 100%;
                    height: auto;
                    margin: 10px 0;
                }
                .content p {
                    margin: 15px 0;
                }
                .comments-title {
                    font-size: 20px;
                    font-weight: bold;
                    margin: 30px 0 20px 0;
                    color: #333;
                    border-bottom: 2px solid #eee;
                    padding-bottom: 10px;
                }
                .comment {
                    margin-bottom: 20px;
                    padding: 15px;
                    background: #f8f9fa;
                    border-radius: 8px;
                    border-left: 4px solid #007bff;
                }
                .comment-author {
                    font-weight: bold;
                    color: #007bff;
                    margin-bottom: 5px;
                }
                .comment-content {
                    color: #555;
                    line-height: 1.5;
                }
                .comment-time {
                    font-size: 12px;
                    color: #999;
                    margin-top: 5px;
                }
            </style>
        """

        val titleHtml = "<div class='title'>$title</div>"
        val authorHtml = "<div class='author'>作者: $authorName</div>"
        val bioHtml = if (authorBio.isNotEmpty()) "<div class='bio'>$authorBio</div>" else ""

        // 处理内容中的图片路径
        val processedContent = content.replace(Regex("data-actualsrc=\"([^\"]+)\"")) { match ->
            val actualSrc = match.groupValues[1]
            "src=\"$actualSrc\""
        }

        val contentHtml = "<div class='content'>$processedContent</div>"

        var commentsHtml = ""
        if (includeComments && commentCount > 0) {
            commentsHtml = "<div class='comments-title'>热门评论 (前 $commentCount 条)</div>"

            // 这里应该从实际评论数据中获取评论
            // 暂时使用示例评论，实际使用时需要从CommentScreenComponent获取数据
            val sampleComments = listOf(
                mapOf("author" to "用户1", "content" to "这篇文章写得很好！", "time" to "2024-01-01"),
                mapOf("author" to "用户2", "content" to "很有启发性，谢谢分享。", "time" to "2024-01-02"),
                mapOf("author" to "用户3", "content" to "观点很独特，支持！", "time" to "2024-01-03"),
                mapOf("author" to "用户4", "content" to "学习了，感谢作者。", "time" to "2024-01-04"),
                mapOf("author" to "用户5", "content" to "内容详实，值得一看。", "time" to "2024-01-05"),
            )

            for (i in 0 until minOf(commentCount, sampleComments.size)) {
                val comment = sampleComments[i]
                commentsHtml += """
                    <div class='comment'>
                        <div class='comment-author'>${comment["author"]}</div>
                        <div class='comment-content'>${comment["content"]}</div>
                        <div class='comment-time'>${comment["time"]}</div>
                    </div>
                """
            }
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                $css
            </head>
            <body>
                $titleHtml
                $authorHtml
                $bioHtml
                $contentHtml
                $commentsHtml
            </body>
            </html>
        """
    }

    // 捕获WebView内容为图片
    private suspend fun captureWebViewToImage(webView: android.webkit.WebView, context: Context, onComplete: (Boolean) -> Unit, suffix: String = "") {
        try {
            // 获取WebView的实际内容高度
            val contentHeight = (webView.contentHeight * webView.scale).toInt()
            val width = webView.width
            val height = if (contentHeight > 0) contentHeight else 1920 // 默认高度

            // 创建足够大的位图
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 设置白色背景
            canvas.drawColor(android.graphics.Color.WHITE)

            // 让WebView绘制到Canvas上
            webView.draw(canvas)

            // 使用MediaStore保存图片到公共目录
            saveImageToMediaStore(context, bitmap, suffix)

            bitmap.recycle()
            webView.destroy()

            withContext(Dispatchers.Main) {
                val message = if (suffix.isNotEmpty()) "带评论图片已保存到相册" else "图片已保存到相册"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                onComplete(true)
            }
        } catch (e: Exception) {
            Log.e("ArticleViewModel", "Capture WebView failed", e)
            webView.destroy()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "图片导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
        }
    }

    // 使用MediaStore保存图片到公共目录
    private fun saveImageToMediaStore(context: Context, bitmap: Bitmap, suffix: String = "") {
        val contentResolver = context.contentResolver
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val suffixStr = if (suffix.isNotEmpty()) "_$suffix" else ""
        val displayName = "Zhihu_${article.type}_${article.id}_$timeStamp$suffixStr.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Zhihu")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let { imageUri ->
            try {
                contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Failed to save image to MediaStore", e)
                throw e
            }
        } ?: throw Exception("Failed to create MediaStore entry")
    }

    // 辅助方法：将文本按宽度分割成行
    private fun breakTextIntoLines(text: String, paint: android.graphics.Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ", limit = 4)
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}
