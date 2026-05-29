package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.navigation.AnswerNavigatorPage
import com.github.zly2006.zhihu.navigation.AnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.shared.data.CollectionItem
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.ui.ArticleAnswerSwitchState
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.json.JsonObject

// TODO: iOS ArticleViewModelRuntime 完整实现
class IosArticleViewModelRuntime(
    private val userMessages: UserMessageSink,
) : ArticleViewModelRuntime {
    override suspend fun getContentDetail(article: Article): DataHolder.Content? = null

    override suspend fun recordOpenEvent(destination: Article, questionId: Long?) = Unit

    override fun answerNavigatorRepository(): AnswerNavigatorRepository = object : AnswerNavigatorRepository {
        override suspend fun fetchAnswerContent(article: Article): DataHolder.Answer? = null

        override suspend fun fetchQuestionFeeds(questionId: Long, pageUrl: String?): AnswerNavigatorPage<Feed> =
            AnswerNavigatorPage(emptyList(), "")

        override suspend fun fetchCollectionItems(pageUrl: String): AnswerNavigatorPage<CollectionItem> =
            AnswerNavigatorPage(emptyList(), "")

        override suspend fun getAlreadyOpenedAnswerIds(answerIds: List<Long>): Set<Long> = emptySet()
    }

    override fun articleAnswerSwitchState(): ArticleAnswerSwitchState? = null

    override fun postHistoryDestination(destination: Article) = Unit

    override suspend fun fetchGet(url: String, block: HttpRequestBuilder.() -> Unit): JsonObject? = null

    override suspend fun fetchPost(url: String, block: HttpRequestBuilder.() -> Unit): JsonObject? = null

    override fun configureSignedRequest(builder: HttpRequestBuilder) = Unit

    override fun showMessage(message: String) = userMessages.showShortMessage(message)

    override fun showLongMessage(message: String) = userMessages.showLongMessage(message)

    override fun setPlainTextClipboard(label: String, text: String) {
        platform.UIKit.UIPasteboard.generalPasteboard.string = text
    }

    override fun xsrfToken(): String = ""

    override fun hasImageExportPermission(): Boolean = false

    override fun requiresHtmlExportPermission(): Boolean = false

    override fun requestImageExportPermission() = Unit

    override suspend fun fetchExportComments(article: Article, requestedCount: Int) = emptyList<DataHolder.Comment>()

    // TODO: iOS HTTP 客户端
    override fun accountHttpClient(): HttpClient = error("accountHttpClient not available on iOS")

    override fun loadExportAssetText(fileName: String) = ""

    override fun buildArticleExportHtml(content: DataHolder.Content, includeAppAttribution: Boolean, extraSectionsHtml: String) = ""

    override suspend fun buildOfflineArticleExportHtml(content: DataHolder.Content, includeAppAttribution: Boolean, httpClient: HttpClient) = ""

    override fun saveHtmlToDownloads(displayName: String, htmlContent: String) = ""

    override fun saveImageToMediaStore(displayName: String, bitmap: Any) = Unit

    // TODO: iOS 图片导出渲染器
    override fun articleImageExportRenderer(loadAssetText: (String) -> String) = error("articleImageExportRenderer not available on iOS")
}
