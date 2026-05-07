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

package com.github.zly2006.zhihu.ui

import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionAnswerNavigator
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.CollectionContentViewModel
import java.util.Date

/**
 * Instrumented tests inject a prefilled ViewModel plus side-effect stubs here so the screen can
 * be exercised deterministically without triggering refresh/export network work.
 */
data class CollectionContentScreenTestOverrides(
    val viewModel: CollectionContentViewModel,
    val isEnd: Boolean = true,
    val onLoadMore: (() -> Unit)? = null,
    val onExportAllToHtmlZip: ((Boolean) -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionContentScreen(
    collectionId: String,
    innerPadding: PaddingValues,
    testOverrides: CollectionContentScreenTestOverrides? = null,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val screenViewModel = testOverrides?.viewModel ?: viewModel { CollectionContentViewModel(collectionId) }
    val collectionTitle = screenViewModel.title.ifBlank { context.getString(R.string.collection_default_title) }
    val listState = rememberLazyListState()
    var showActionsMenu by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    val isEnd = testOverrides?.let { { it.isEnd } } ?: { screenViewModel.isEnd }
    val onLoadMore = testOverrides?.onLoadMore ?: { screenViewModel.loadMore(context) }
    val onExportAllToHtmlZip = testOverrides?.onExportAllToHtmlZip ?: { includeImages ->
        screenViewModel.exportAllToHtmlZip(
            context = context,
            includeImages = includeImages,
        )
    }
    val sharedData = if (context is MainActivity) {
        val sd by context.viewModels<ArticleViewModel.ArticlesSharedData>()
        sd
    } else {
        null
    }

    LaunchedEffect(testOverrides) {
        if (testOverrides == null && screenViewModel.allData.isEmpty()) {
            screenViewModel.refresh(context)
        }
    }

    BackHandler(enabled = showActionsMenu) {
        showActionsMenu = false
    }

    BackHandler(enabled = showExportOptionsDialog) {
        showExportOptionsDialog = false
    }

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = collectionTitle,
                        modifier = Modifier.testTag("collection_content_title"),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        modifier = Modifier.testTag("collection_content_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.back))
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showActionsMenu = true },
                            enabled = screenViewModel.exportDialogState?.isCompleted != false,
                            modifier = Modifier.testTag("collection_content_more_button"),
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = context.getString(R.string.article_more_options))
                        }
                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false },
                            modifier = Modifier.testTag("collection_content_more_menu"),
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.testTag("collection_content_export_action"),
                                text = { Text(context.getString(R.string.collection_export_all_html)) },
                                enabled = screenViewModel.exportDialogState?.isCompleted != false,
                                onClick = {
                                    showActionsMenu = false
                                    showExportOptionsDialog = true
                                },
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        if (showExportOptionsDialog) {
            CollectionHtmlExportOptionsDialog(
                onDismiss = { showExportOptionsDialog = false },
                onConfirm = { includeImages ->
                    showExportOptionsDialog = false
                    onExportAllToHtmlZip(includeImages)
                },
            )
        }
        screenViewModel.exportDialogState?.let { state ->
            CollectionHtmlExportDialog(
                state = state,
                onDismiss = screenViewModel::dismissExportDialog,
            )
        }
        PaginatedList(
            items = screenViewModel.displayItems,
            onLoadMore = onLoadMore,
            isEnd = isEnd,
            listState = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(innerPadding)
                .testTag("collection_content_list"),
            footer = ProgressIndicatorFooter,
            topContent = {
                item(0) {
                    Text(
                        listOfNotNull(
                            context.getString(R.string.collection_item_count, screenViewModel.collection?.itemCount ?: 0),
                            context.getString(R.string.collection_like_count, screenViewModel.collection?.likeCount ?: 0),
                            context.getString(R.string.collection_comment_count, screenViewModel.collection?.commentCount ?: 0),
                            screenViewModel.collection?.updatedTime?.let {
                                context.getString(R.string.collection_updated_at, YMDHMS.format(Date(it * 1000)))
                            },
                        ).fastJoinToString(" · "),
                        modifier = Modifier.testTag("collection_content_stats"),
                    )
                }
            },
        ) { item ->
            FeedCard(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("collection_content_item_${item.stableKey}"),
            ) {
                val dest = navDestination
                if (dest is Article && dest.type == ArticleType.Answer && sharedData != null) {
                    val idx = screenViewModel.displayItems.indexOf(item)
                    val nextItems = if (idx >= 0) screenViewModel.allData.drop(idx + 1) else emptyList()
                    val prevItems = if (idx > 0) screenViewModel.allData.take(idx).reversed() else emptyList()
                    sharedData.pendingNavigator = CollectionAnswerNavigator(
                        collectionId = collectionId,
                        collectionTitle = collectionTitle,
                        initialNextItems = nextItems,
                        initialPreviousItems = prevItems,
                    )
                }
                dest?.let { navigator.onNavigate(it) }
            }
        }
    }
}

@Composable
private fun CollectionHtmlExportOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
) {
    var includeImages by remember { mutableStateOf(true) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.collection_export_html_title)) },
        text = {
            Column {
                Text(context.getString(R.string.collection_export_html_desc))
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
                        Text(context.getString(R.string.collection_export_include_images))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = context.getString(R.string.collection_export_include_images_desc),
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
                Text(context.getString(R.string.collection_start_export))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("collection_content_export_cancel"),
            ) {
                Text(context.getString(R.string.cancel))
            }
        },
    )
}

@Composable
private fun CollectionHtmlExportDialog(
    state: CollectionContentViewModel.CollectionHtmlExportDialogState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {
            if (state.isCompleted) {
                onDismiss()
            }
        },
        title = {
            Text(if (state.isCompleted) state.phaseText else context.getString(R.string.collection_exporting))
        },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(state.phaseText)
                if (state.currentTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(context.getString(R.string.collection_export_current, state.currentTitle))
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
                Text(
                    context.getString(
                        R.string.collection_export_result_counts,
                        state.successCount,
                        state.skippedCount,
                        state.failedCount,
                    ),
                )
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
                    Text(context.getString(R.string.ok))
                }
            }
        },
    )
}
