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

// TODO: iOS ArticleViewModelRuntime 完整实现
class IosArticleViewModelRuntime(
    private val userMessages: UserMessageSink,
) : ArticleViewModelRuntime {
    // TODO: iOS 获取内容详情
    override suspend fun getContentDetail(article: Article): DataHolder.Content? = null

    // TODO: iOS 记录打开事件
    override suspend fun recordOpenEvent(destination: Article, questionId: Long?) = Unit

    override fun answerNavigatorRepository(): AnswerNavigatorRepository = object : AnswerNavigatorRepository {
        // TODO: iOS 获取回答内容
        override suspend fun fetchAnswerContent(article: Article): DataHolder.Answer? = null

        // TODO: iOS 获取问题Feed
        override suspend fun fetchQuestionFeeds(questionId: Long, pageUrl: String?): AnswerNavigatorPage<Feed> =
            AnswerNavigatorPage(emptyList(), "")

        // TODO: iOS 获取收藏夹项目
        override suspend fun fetchCollectionItems(pageUrl: String): AnswerNavigatorPage<CollectionItem> =
            AnswerNavigatorPage(emptyList(), "")

        // TODO: iOS 获取已读回答ID
        override suspend fun getAlreadyOpenedAnswerIds(answerIds: List<Long>): Set<Long> = emptySet()
    }

    // TODO: iOS 回答切换状态
    override fun articleAnswerSwitchState(): ArticleAnswerSwitchState? = null

    // TODO: iOS 历史记录
    override fun postHistoryDestination(destination: Article) = Unit

    // TODO: iOS 签名请求
    override fun configureSignedRequest(builder: HttpRequestBuilder) = Unit

    override fun setPlainTextClipboard(label: String, text: String) {
        platform.UIKit.UIPasteboard.generalPasteboard.string = text
    }

    // TODO: iOS XSRF Token
    override fun xsrfToken(): String = ""

    // TODO: iOS 图片导出权限检查
    override fun hasImageExportPermission(): Boolean = false

    // TODO: iOS HTML导出权限检查
    override fun requiresHtmlExportPermission(): Boolean = false

    // TODO: iOS 请求图片导出权限
    override fun requestImageExportPermission() = Unit

    // TODO: iOS 获取导出评论
    override suspend fun fetchExportComments(article: Article, requestedCount: Int) = emptyList<DataHolder.Comment>()

    // TODO: iOS HTTP 客户端
    override fun accountHttpClient(): HttpClient = error("accountHttpClient not available on iOS")

    // TODO: iOS 加载导出资源文本
    override fun loadExportAssetText(fileName: String) = ""

    // TODO: iOS 构建文章导出HTML
    override fun buildArticleExportHtml(content: DataHolder.Content, includeAppAttribution: Boolean, extraSectionsHtml: String) = ""

    // TODO: iOS 构建离线文章导出HTML
    override suspend fun buildOfflineArticleExportHtml(content: DataHolder.Content, includeAppAttribution: Boolean, httpClient: HttpClient) = ""

    // TODO: iOS 保存HTML到下载目录
    override fun saveHtmlToDownloads(displayName: String, htmlContent: String) = ""

    // TODO: iOS 保存图片到媒体库
    override fun saveImageToMediaStore(displayName: String, bitmap: Any) = Unit

    // TODO: iOS 图片导出渲染器
    override fun articleImageExportRenderer(loadAssetText: (String) -> String) = error("articleImageExportRenderer not available on iOS")
}
