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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.ZhihuPaging
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.ui.Collection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

data class CollectionHtmlExportProgress(
    val totalCount: Int,
    val processedCount: Int,
    val successCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val currentTitle: String = "",
)

data class CollectionHtmlExportResult(
    val totalCount: Int,
    val successCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val zipFilePath: String?,
)

interface CollectionContentEnvironment : PaginationEnvironment {
    suspend fun fetchCollection(collectionId: String): Collection {
        val json = fetchJson("https://www.zhihu.com/api/v4/collections/$collectionId", "")
            ?: throw IllegalStateException("收藏夹信息加载失败")
        return ZhihuJson.decodeJson<Collection>(json["collection"] ?: throw IllegalStateException("收藏夹信息为空"))
    }

    suspend fun exportCollectionItemsToHtmlZip(
        collectionTitle: String,
        items: List<CollectionItem>,
        includeImages: Boolean,
        onProgress: suspend (CollectionHtmlExportProgress) -> Unit,
    ): CollectionHtmlExportResult

    suspend fun handleCollectionExportFailure(error: Exception)
}

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

    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/collections/$collectionId/items"

    override fun processResponse(environment: PaginationEnvironment, data: List<CollectionItem>, rawData: JsonArray) {
        super.processResponse(environment, data, rawData)
        displayItems.addAll(data.map { createDisplayItem(it) }) // 展示用的已flatten数据
    }

    private fun createDisplayItem(item: CollectionItem): FeedDisplayItem = FeedDisplayItem(
        title = item.content.title,
        summary = item.content.excerpt,
        details = item.content.detailsText,
        navDestinationJson = item.content.navDestination?.toFeedDisplayItemNavDestinationJson(),
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

    override fun refresh(environment: PaginationEnvironment) {
        if (isLoading) return
        displayItems.clear()
        if (environment is CollectionContentEnvironment) {
            viewModelScope.launch {
                loadCollectionInfo(environment)
            }
        }
        super.refresh(environment)
    }

    fun exportAllToHtmlZip(
        environment: CollectionContentEnvironment,
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
                val items = ensureAllCollectionItemsLoaded(environment)
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

                val exportTitle = title
                val result = environment.exportCollectionItemsToHtmlZip(
                    collectionTitle = exportTitle,
                    items = items,
                    includeImages = includeImages,
                    onProgress = { progress ->
                        exportDialogState = CollectionHtmlExportDialogState(
                            phaseText = "正在导出 ${progress.processedCount} / ${progress.totalCount}",
                            totalCount = progress.totalCount,
                            processedCount = progress.processedCount,
                            successCount = progress.successCount,
                            skippedCount = progress.skippedCount,
                            failedCount = progress.failedCount,
                            currentTitle = progress.currentTitle,
                        )
                    },
                )

                val resultMessage = if (result.zipFilePath != null) {
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
                    zipFilePath = result.zipFilePath,
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
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
                environment.handleCollectionExportFailure(e)
            }
        }
    }

    private suspend fun loadCollectionInfo(environment: CollectionContentEnvironment) {
        collection = environment.fetchCollection(collectionId)
    }

    private suspend fun ensureAllCollectionItemsLoaded(environment: CollectionContentEnvironment): List<CollectionItem> {
        if (collection == null) {
            loadCollectionInfo(environment)
        }

        while (allData.isEmpty() || !isEnd) {
            val beforeCount = allData.size
            val beforePaging = lastPaging
            isLoading = true
            fetchFeeds(environment)

            val pagingAdvanced = hasPagingProgress(beforePaging, lastPaging)
            val itemCountAdvanced = allData.size > beforeCount
            if (!pagingAdvanced && !itemCountAdvanced) {
                break
            }
        }

        return allData.toList()
    }

    private fun hasPagingProgress(
        beforePaging: ZhihuPaging?,
        afterPaging: ZhihuPaging?,
    ): Boolean {
        if (afterPaging == null) return false
        if (beforePaging == null) return true
        return beforePaging.next != afterPaging.next || beforePaging.page != afterPaging.page || beforePaging.isEnd != afterPaging.isEnd
    }
}

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

// Re-export from ui package for backward compatibility with tests
