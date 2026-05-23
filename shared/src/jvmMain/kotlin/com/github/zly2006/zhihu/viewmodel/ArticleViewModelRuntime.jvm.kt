package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.navigation.AnswerNavigatorPage
import com.github.zly2006.zhihu.navigation.AnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.shared.data.CollectionItem
import com.github.zly2006.zhihu.shared.data.CollectionResponse
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.ui.ArticleAnswerSwitchState
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class DesktopArticleViewModelRuntime(
    private val store: DesktopAccountStore = DesktopAccountStore(),
) : ArticleViewModelRuntime {
    override suspend fun getContentDetail(article: Article): DataHolder.Content? = null

    override suspend fun recordOpenEvent(
        destination: Article,
        questionId: Long?,
    ) = Unit

    override fun answerNavigatorRepository(): AnswerNavigatorRepository =
        object : AnswerNavigatorRepository {
            override suspend fun fetchAnswerContent(article: Article): DataHolder.Answer? = null

            override suspend fun fetchQuestionFeeds(
                questionId: Long,
                pageUrl: String?,
            ): AnswerNavigatorPage<Feed> = AnswerNavigatorPage(emptyList(), "")

            override suspend fun fetchCollectionItems(pageUrl: String): AnswerNavigatorPage<CollectionItem> =
                AnswerNavigatorPage(emptyList(), "")

            override suspend fun getAlreadyOpenedAnswerIds(answerIds: List<Long>): Set<Long> = emptySet()
        }

    override fun articleAnswerSwitchState(): ArticleAnswerSwitchState? = null

    override fun postHistoryDestination(destination: Article) = Unit

    override suspend fun fetchGet(
        url: String,
        block: HttpRequestBuilder.() -> Unit,
    ): JsonObject? = accountHttpClient().use { client ->
        client
            .get(url) {
                block()
            }.body()
    }

    override suspend fun fetchPost(
        url: String,
        block: HttpRequestBuilder.() -> Unit,
    ): JsonObject? = accountHttpClient().use { client ->
        client
            .post(url) {
                block()
            }.body()
    }

    override fun decodeCollectionResponse(json: JsonElement): CollectionResponse =
        ZhihuJson.json.decodeFromJsonElement(CollectionResponse.serializer(), json)

    override fun configureSignedRequest(builder: HttpRequestBuilder) {
        store.load().cookies["d_c0"]?.let { dc0 ->
            builder.signZhihuFetchRequest(dc0 = dc0)
        }
    }

    override fun showMessage(message: String) {
        println(message)
    }

    override fun showLongMessage(message: String) {
        println(message)
    }

    override fun newPlainTextClip(
        label: String,
        text: String,
    ): Any = StringSelection(text)

    override fun setPrimaryClip(clip: Any) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(clip as StringSelection, null)
    }

    override fun xsrfToken(): String = store.load().cookies["_xsrf"] ?: ""

    override fun hasImageExportPermission(): Boolean = false

    override fun requiresHtmlExportPermission(): Boolean = false

    override fun requestImageExportPermission() = Unit

    override suspend fun fetchExportComments(
        article: Article,
        requestedCount: Int,
    ): List<DataHolder.Comment> = emptyList()

    override fun accountHttpClient(): HttpClient =
        store.createHttpClient(store.load().cookies)

    override fun loadExportAssetText(fileName: String): String = ""

    override fun buildArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        extraSectionsHtml: String,
    ): String = contentHtml(content) + extraSectionsHtml

    override suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String = contentHtml(content)

    override fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String = displayName

    override fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) = Unit

    override fun articleImageExportRenderer(loadAssetText: (String) -> String): ArticleImageExportRenderer =
        object : ArticleImageExportRenderer {
            override suspend fun prepareExportWebView(
                htmlContent: String,
                timeoutMs: Long,
            ): PreparedArticleExportContent =
                error("Desktop image export renderer is not implemented")

            override suspend fun captureExportBitmap(preparedWebView: PreparedArticleExportContent): Any =
                error("Desktop image export renderer is not implemented")

            override suspend fun destroyExportWebView(preparedWebView: PreparedArticleExportContent) = Unit

            override fun recycleExportBitmap(bitmap: Any) = Unit
        }

    private fun contentHtml(content: DataHolder.Content): String = when (content) {
        is DataHolder.Answer -> content.content
        is DataHolder.Article -> content.content
        else -> ""
    }
}
