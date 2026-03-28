package com.github.zly2006.zhihu.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.ui.Collection
import com.github.zly2006.zhihu.util.ResolvedCollectionHtmlExportItem
import com.github.zly2006.zhihu.util.buildArticleExportFileName
import com.github.zly2006.zhihu.util.buildOfflineArticleExportHtml
import com.github.zly2006.zhihu.util.exportCollectionItemsToZip
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.CollectionContentViewModel.CollectionItem
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel.FeedDisplayItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf
import com.github.zly2006.zhihu.Article as ArticleDestination

class CollectionContentViewModel(
    val collectionId: String,
) : PaginationViewModel<CollectionItem>(typeOf<CollectionItem>()) {
    val displayItems = mutableStateListOf<FeedDisplayItem>()
    var collection by mutableStateOf<Collection?>(null)
    val title by derivedStateOf {
        collection?.title ?: "收藏夹"
    }
    var exportDialogState by mutableStateOf<CollectionHtmlExportDialogState?>(null)
        private set

    @Serializable
    class CollectionItem(
        val created: String,
        val content: Feed.Target,
    )

    @Stable
    data class CollectionHtmlExportDialogState(
        val phaseText: String,
        val totalCount: Int,
        val processedCount: Int,
        val successCount: Int,
        val skippedCount: Int,
        val failedCount: Int,
        val currentTitle: String = "",
        val isIndeterminate: Boolean = false,
        val isCompleted: Boolean = false,
        val resultMessage: String? = null,
        val zipFilePath: String? = null,
    ) {
        val progress: Float
            get() = if (totalCount <= 0) 0f else (processedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
    }

    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/collections/$collectionId/items"

    override fun processResponse(context: Context, data: List<CollectionItem>, rawData: JsonArray) {
        super.processResponse(context, data, rawData)
        displayItems.addAll(data.map { createDisplayItem(it) }) // 展示用的已flatten数据
    }

    private fun createDisplayItem(item: CollectionItem): FeedDisplayItem = FeedDisplayItem(
        title = item.content.title,
        summary = item.content.excerpt,
        details = item.content.detailsText,
        navDestination = item.content.navDestination,
        feed = null,
        avatarSrc = when (item.content) {
            is Feed.AnswerTarget -> item.content.author?.avatarUrl
            is Feed.ArticleTarget -> item.content.author.avatarUrl
            is Feed.QuestionTarget -> item.content.author?.avatarUrl
            else -> null
        },
    )

    fun dismissExportDialog() {
        exportDialogState = null
    }

    override fun refresh(context: Context) {
        if (isLoading) return
        displayItems.clear()
        viewModelScope.launch {
            loadCollectionInfo(context)
        }
        super.refresh(context)
    }

    fun exportAllToHtmlZip(
        context: Context,
        includeImages: Boolean,
    ) {
        if (exportDialogState?.isCompleted == false) return

        viewModelScope.launch {
            exportDialogState = CollectionHtmlExportDialogState(
                phaseText = "正在加载收藏夹条目",
                totalCount = 0,
                processedCount = 0,
                successCount = 0,
                skippedCount = 0,
                failedCount = 0,
                isIndeterminate = true,
            )

            try {
                val items = ensureAllCollectionItemsLoaded(context)
                if (items.isEmpty()) {
                    exportDialogState = CollectionHtmlExportDialogState(
                        phaseText = "没有可导出的内容",
                        totalCount = 0,
                        processedCount = 0,
                        successCount = 0,
                        skippedCount = 0,
                        failedCount = 0,
                        isCompleted = true,
                        resultMessage = "收藏夹为空，或内容加载失败。",
                    )
                    return@launch
                }

                val outputDir = context.getExternalFilesDir(null)
                    ?: throw IllegalStateException("外部文件目录不可用")
                val exportHttpClient = httpClient(context)
                val exportTitle = title
                val result = withContext(Dispatchers.IO) {
                    exportCollectionItemsToZip(
                        collectionTitle = exportTitle,
                        items = items,
                        cacheDir = context.cacheDir,
                        outputDir = outputDir,
                        displayTitle = { item ->
                            item.content.title.ifBlank { item.content.description() }
                        },
                        resolveItem = { item ->
                            resolveCollectionItemForHtmlExport(
                                context = context,
                                item = item,
                                exportHttpClient = exportHttpClient,
                                includeImages = includeImages,
                            )
                        },
                        onProgress = { progress ->
                            withContext(Dispatchers.Main) {
                                exportDialogState = CollectionHtmlExportDialogState(
                                    phaseText = "正在导出 ${progress.processedCount} / ${progress.totalCount}",
                                    totalCount = progress.totalCount,
                                    processedCount = progress.processedCount,
                                    successCount = progress.successCount,
                                    skippedCount = progress.skippedCount,
                                    failedCount = progress.failedCount,
                                    currentTitle = progress.currentTitle,
                                )
                            }
                        },
                    )
                }

                val resultMessage = if (result.zipFile != null) {
                    "已导出 ${result.successCount} 篇，跳过 ${result.skippedCount} 条，失败 ${result.failedCount} 条。"
                } else {
                    "没有可导出的回答或文章，已跳过 ${result.skippedCount} 条，失败 ${result.failedCount} 条。"
                }
                exportDialogState = CollectionHtmlExportDialogState(
                    phaseText = "导出完成",
                    totalCount = result.totalCount,
                    processedCount = result.totalCount,
                    successCount = result.successCount,
                    skippedCount = result.skippedCount,
                    failedCount = result.failedCount,
                    currentTitle = "",
                    isCompleted = true,
                    resultMessage = resultMessage,
                    zipFilePath = result.zipFile?.absolutePath,
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("CollectionContentViewModel", "Failed to export collection HTML zip", e)
                exportDialogState = CollectionHtmlExportDialogState(
                    phaseText = "导出失败",
                    totalCount = exportDialogState?.totalCount ?: 0,
                    processedCount = exportDialogState?.processedCount ?: 0,
                    successCount = exportDialogState?.successCount ?: 0,
                    skippedCount = exportDialogState?.skippedCount ?: 0,
                    failedCount = exportDialogState?.failedCount ?: 0,
                    currentTitle = exportDialogState?.currentTitle.orEmpty(),
                    isCompleted = true,
                    resultMessage = e.message ?: "未知错误",
                )
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun loadCollectionInfo(context: Context) {
        val jojo = AccountData.fetchGet(context, "https://www.zhihu.com/api/v4/collections/$collectionId") {
            signFetchRequest()
        } ?: throw IllegalStateException("收藏夹信息加载失败")
        collection = AccountData.decodeJson<Collection>(jojo["collection"] ?: throw IllegalStateException("收藏夹信息为空"))
    }

    private suspend fun ensureAllCollectionItemsLoaded(context: Context): List<CollectionItem> {
        if (collection == null) {
            loadCollectionInfo(context)
        }

        while (allData.isEmpty() || !isEnd) {
            val beforeCount = allData.size
            val beforePaging = lastPaging
            isLoading = true
            fetchFeeds(context)

            val pagingAdvanced = hasPagingProgress(beforePaging, lastPaging)
            val itemCountAdvanced = allData.size > beforeCount
            if (!pagingAdvanced && !itemCountAdvanced) {
                break
            }
        }

        return allData.toList()
    }

    private fun hasPagingProgress(
        beforePaging: Paging?,
        afterPaging: Paging?,
    ): Boolean {
        if (afterPaging == null) return false
        if (beforePaging == null) return true
        return beforePaging.next != afterPaging.next || beforePaging.page != afterPaging.page || beforePaging.isEnd != afterPaging.isEnd
    }

    private suspend fun resolveCollectionItemForHtmlExport(
        context: Context,
        item: CollectionItem,
        exportHttpClient: io.ktor.client.HttpClient,
        includeImages: Boolean,
    ): ResolvedCollectionHtmlExportItem? {
        val navDestination = item.content.navDestination as? ArticleDestination ?: return null
        val content = ContentDetailCache.getOrFetch(context, navDestination)
            ?: throw IllegalStateException("无法加载「${item.content.title}」详情")
        if (content !is DataHolder.Answer && content !is DataHolder.Article) {
            return null
        }

        return ResolvedCollectionHtmlExportItem(
            htmlFileName = buildArticleExportFileName(content, "html"),
            htmlContent = buildOfflineArticleExportHtml(
                context = context,
                content = content,
                includeAppAttribution = true,
                httpClient = exportHttpClient,
                includeImages = includeImages,
            ),
        )
    }
}
