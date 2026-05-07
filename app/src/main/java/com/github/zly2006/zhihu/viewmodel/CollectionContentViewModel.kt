/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
import com.github.zly2006.zhihu.R
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
import com.github.zly2006.zhihu.viewmodel.feed.localizedDescription
import com.github.zly2006.zhihu.viewmodel.feed.localizedDetailsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf
import com.github.zly2006.zhihu.navigation.Article as ArticleDestination

class CollectionContentViewModel(
    val collectionId: String,
) : PaginationViewModel<CollectionItem>(typeOf<CollectionItem>()) {
    val displayItems = mutableStateListOf<FeedDisplayItem>()
    var collection by mutableStateOf<Collection?>(null)
    val title by derivedStateOf {
        collection?.title.orEmpty()
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
        displayItems.addAll(data.map { createDisplayItem(context, it) }) // 展示用的已flatten数据
    }

    private fun createDisplayItem(context: Context, item: CollectionItem): FeedDisplayItem = FeedDisplayItem(
        title = item.content.title,
        summary = item.content.excerpt,
        details = item.content.localizedDetailsText(context),
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
                phaseText = context.getString(R.string.collection_loading_items),
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
                        phaseText = context.getString(R.string.collection_no_exportable_content),
                        totalCount = 0,
                        processedCount = 0,
                        successCount = 0,
                        skippedCount = 0,
                        failedCount = 0,
                        isCompleted = true,
                        resultMessage = context.getString(R.string.collection_empty_or_load_failed),
                    )
                    return@launch
                }

                val outputDir = context.getExternalFilesDir(null)
                    ?: throw IllegalStateException(context.getString(R.string.external_files_dir_unavailable))
                val exportHttpClient = httpClient(context)
                val exportTitle = title
                val result = withContext(Dispatchers.IO) {
                    exportCollectionItemsToZip(
                        collectionTitle = exportTitle,
                        items = items,
                        cacheDir = context.cacheDir,
                        outputDir = outputDir,
                        fallbackCollectionTitle = context.getString(R.string.collection_default_title),
                        createCacheDirFailedMessage = context.getString(R.string.collection_export_cache_dir_failed),
                        createZipDirFailedMessage = context.getString(R.string.collection_export_zip_dir_failed),
                        displayTitle = { item ->
                            item.content.title.ifBlank { item.content.localizedDescription(context) }
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
                                    phaseText = context.getString(
                                        R.string.collection_export_progress,
                                        progress.processedCount,
                                        progress.totalCount,
                                    ),
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
                    context.getString(R.string.collection_export_finished_counts, result.successCount, result.skippedCount, result.failedCount)
                } else {
                    context.getString(R.string.collection_export_no_articles_counts, result.skippedCount, result.failedCount)
                }
                exportDialogState = CollectionHtmlExportDialogState(
                    phaseText = context.getString(R.string.export_complete),
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
                    phaseText = context.getString(R.string.export_failed_short),
                    totalCount = exportDialogState?.totalCount ?: 0,
                    processedCount = exportDialogState?.processedCount ?: 0,
                    successCount = exportDialogState?.successCount ?: 0,
                    skippedCount = exportDialogState?.skippedCount ?: 0,
                    failedCount = exportDialogState?.failedCount ?: 0,
                    currentTitle = exportDialogState?.currentTitle.orEmpty(),
                    isCompleted = true,
                    resultMessage = e.message ?: context.getString(R.string.unknown_error),
                )
                Toast.makeText(context, context.getString(R.string.export_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun loadCollectionInfo(context: Context) {
        val jojo = AccountData.fetchGet(context, "https://www.zhihu.com/api/v4/collections/$collectionId") {
            signFetchRequest()
        } ?: throw IllegalStateException(context.getString(R.string.collection_info_load_failed))
        collection = AccountData.decodeJson<Collection>(
            jojo["collection"] ?: throw IllegalStateException(context.getString(R.string.collection_info_empty)),
        )
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
            ?: throw IllegalStateException(context.getString(R.string.collection_item_detail_load_failed, item.content.title))
        if (content !is DataHolder.Answer && content !is DataHolder.Article) {
            return null
        }

        return ResolvedCollectionHtmlExportItem(
            htmlFileName = buildArticleExportFileName(context, content, "html"),
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
