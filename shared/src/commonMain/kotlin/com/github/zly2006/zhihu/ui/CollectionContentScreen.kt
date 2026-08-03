/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionAnswerNavigator
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.platform.PlatformBackHandler
import com.github.zly2006.zhihu.reading.RegisterReadingQueueSource
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.CollectionContentEnvironment
import com.github.zly2006.zhihu.viewmodel.CollectionContentViewModel
import com.github.zly2006.zhihu.viewmodel.CollectionHtmlExportDialogState
import com.github.zly2006.zhihu.viewmodel.formatArticleDateTime
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionContentScreen(
    collectionId: String,
) {
    val navigator = LocalNavigator.current
    val screenViewModel = viewModel { CollectionContentViewModel(collectionId) }
    val collectionEnvironment = rememberPaginationEnvironment(allowGuestAccess = false) as CollectionContentEnvironment
    val listState = rememberLazyListState()
    var showActionsMenu by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(screenViewModel) {
        if (screenViewModel.allData.isEmpty()) {
            screenViewModel.refresh(collectionEnvironment)
        }
    }

    PlatformBackHandler(enabled = showActionsMenu) {
        showActionsMenu = false
    }

    PlatformBackHandler(enabled = showExportOptionsDialog) {
        showExportOptionsDialog = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenViewModel.title,
                        modifier = Modifier.testTag("collection_content_title"),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        modifier = Modifier.testTag("collection_content_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showActionsMenu = true },
                            enabled = screenViewModel.exportDialogState?.isCompleted != false,
                            modifier = Modifier.testTag("collection_content_more_button"),
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false },
                            modifier = Modifier.testTag("collection_content_more_menu"),
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.testTag("collection_content_export_action"),
                                text = { Text("全部导出HTML") },
                                enabled = screenViewModel.exportDialogState?.isCompleted != false,
                                onClick = {
                                    showActionsMenu = false
                                    showExportOptionsDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (showExportOptionsDialog) {
            CollectionHtmlExportOptionsDialog(
                onDismiss = { showExportOptionsDialog = false },
                onConfirm = { includeImages ->
                    showExportOptionsDialog = false
                    screenViewModel.exportAllToHtmlZip(
                        environment = collectionEnvironment,
                        includeImages = includeImages,
                    )
                },
            )
        }
        screenViewModel.exportDialogState?.let { state ->
            CollectionHtmlExportDialog(
                state = state,
                onDismiss = screenViewModel::dismissExportDialog,
            )
        }
        CollectionContentBody(
            viewModel = screenViewModel,
            environment = collectionEnvironment,
            collectionId = collectionId,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            listState = listState,
            tagPrefix = "collection_content",
        )
    }
}

@Composable
internal fun CollectionContentBody(
    viewModel: CollectionContentViewModel,
    environment: CollectionContentEnvironment,
    collectionId: String,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    tagPrefix: String,
    displayItems: List<FeedDisplayItem> = viewModel.displayItems,
    emptyMessage: String? = null,
) {
    val navigator = LocalNavigator.current
    val sharedData = environment.articleAnswerSwitchState()
    val readingQueueSourceId = "collection:$collectionId:contents"
    RegisterReadingQueueSource(
        sourceId = readingQueueSourceId,
        items = displayItems,
    )

    val visibleCollectionItems = displayItems.mapNotNull { displayItem ->
        val sourceIndex = viewModel.displayItems.indexOf(displayItem)
        viewModel.allData.getOrNull(sourceIndex)
    }

    PaginatedList(
        items = displayItems,
        onLoadMore = { viewModel.loadMore(environment) },
        isEnd = { viewModel.isEnd },
        listState = listState,
        modifier = modifier.testTag("${tagPrefix}_list"),
        footer = ProgressIndicatorFooter,
        topContent = {
            item(0) {
                Text(
                    text = listOfNotNull(
                        viewModel.collection?.itemCount?.let { "$it 条收藏" },
                        viewModel.collection?.likeCount?.let { "$it 个赞同" },
                        viewModel.collection?.commentCount?.let { "$it 条评论" },
                        viewModel.collection?.updatedTime?.let { "${formatArticleDateTime(it)} 更新" },
                    ).fastJoinToString(" · "),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("${tagPrefix}_stats"),
                    textAlign = TextAlign.Center,
                )
            }
            if (displayItems.isEmpty() && emptyMessage != null) {
                item(1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emptyMessage, modifier = Modifier.testTag("${tagPrefix}_empty_filter"))
                    }
                }
            }
        },
    ) { item ->
        FeedCard(
            item = item,
            readingQueueSourceId = readingQueueSourceId,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("${tagPrefix}_item_${item.stableKey}"),
        ) { _, destination ->
            if (destination is Article && destination.type == ArticleType.Answer && sharedData != null) {
                val index = displayItems.indexOf(item)
                val nextItems = if (index >= 0) visibleCollectionItems.drop(index + 1) else emptyList()
                val previousItems = if (index > 0) visibleCollectionItems.take(index).reversed() else emptyList()
                sharedData.pendingNavigator = CollectionAnswerNavigator(
                    collectionId = collectionId,
                    collectionTitle = viewModel.title,
                    initialNextItems = nextItems,
                    initialPreviousItems = previousItems,
                    initialNextUrl = viewModel.nextPageUrl,
                    environment = environment,
                )
            }
            destination?.let(navigator.onNavigate)
        }
    }
}

@Composable
private fun CollectionHtmlExportOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
) {
    var includeImages by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出收藏夹 HTML") },
        text = {
            Column {
                Text("可以选择是否一并导出图片。导出图片会把图片下载并内嵌到 HTML 中，速度可能更慢。")
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        modifier = Modifier.testTag("collection_content_export_include_images"),
                        checked = includeImages,
                        onCheckedChange = { includeImages = it },
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("导出图片（更慢）")
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "关闭后保留原始图片链接，不转成 base64",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(includeImages) },
                modifier = Modifier.testTag("collection_content_export_confirm"),
            ) {
                Text("开始导出")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("collection_content_export_cancel"),
            ) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun CollectionHtmlExportDialog(
    state: CollectionHtmlExportDialogState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (state.isCompleted) {
                onDismiss()
            }
        },
        title = {
            Text(if (state.isCompleted) state.phaseText else "正在导出收藏夹")
        },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(state.phaseText)
                if (state.currentTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("当前：${state.currentTitle}")
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (state.isIndeterminate) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("成功 ${state.successCount} · 跳过 ${state.skippedCount} · 失败 ${state.failedCount}")
                state.resultMessage?.takeIf { it.isNotBlank() }?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(message)
                }
                state.zipFilePath?.takeIf { it.isNotBlank() }?.let { zipFilePath ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(zipFilePath)
                }
            }
        },
        confirmButton = {
            if (state.isCompleted) {
                TextButton(onClick = onDismiss) {
                    Text("确定")
                }
            }
        },
    )
}
