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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.data.Collection
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.CreateCollectionDialog
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.CollectionsViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    urlToken: String?,
    testCollections: List<Collection>? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showBackButton: Boolean = true,
    isActive: Boolean = true,
) {
    val navigator = LocalNavigator.current
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val viewModel: CollectionsViewModel = viewModel(key = urlToken) {
        CollectionsViewModel(urlToken.orEmpty())
    }
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val useTestCollections = testCollections != null || urlToken == null
    val collections = testCollections ?: viewModel.allData
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var collectionPendingDeletion by remember { mutableStateOf<Collection?>(null) }

    LaunchedEffect(isActive) {
        if (shouldRefreshCollectionDataOnActivation(isActive, useTestCollections)) {
            viewModel.refresh(environment)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我的收藏夹",
                        modifier = Modifier.testTag(COLLECTION_SCREEN_TITLE_TAG),
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = navigator.onNavigateBack,
                            modifier = Modifier.testTag(COLLECTION_SCREEN_BACK_BUTTON_TAG),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!viewModel.isCreatingCollection && viewModel.deletingCollectionId == null) {
                        viewModel.clearMutationErrors()
                        showCreateCollectionDialog = true
                    }
                },
                modifier = Modifier.testTag(COLLECTION_SCREEN_CREATE_BUTTON_TAG),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新建收藏夹")
            }
        },
    ) { innerPadding ->
        PaginatedList(
            items = collections,
            onLoadMore = {
                if (!useTestCollections) {
                    viewModel.loadMore(environment)
                }
            },
            isEnd = { useTestCollections || viewModel.isEnd },
            listState = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(COLLECTION_SCREEN_LIST_TAG),
            footer = ProgressIndicatorFooter,
        ) { collection ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("collection_screen_item_${collection.id}"),
                elevation = CardDefaults.cardElevation(4.dp),
                onClick = {
                    navigator.onNavigate(CollectionContent(collection.id))
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = collection.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (!collection.isDefault) {
                        IconButton(
                            onClick = {
                                viewModel.clearMutationErrors()
                                collectionPendingDeletion = collection
                            },
                            enabled = !viewModel.isCreatingCollection && viewModel.deletingCollectionId == null,
                            modifier = Modifier.testTag("collection_screen_delete_button_${collection.id}"),
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除${collection.title}",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    CreateCollectionDialog(
        showDialog = showCreateCollectionDialog,
        onDismiss = {
            showCreateCollectionDialog = false
        },
        onConfirm = { title, description, isPublic ->
            coroutineScope.launch {
                if (
                    viewModel.createCollection(
                        environment = environment,
                        title = title,
                        description = description,
                        isPublic = isPublic,
                    )
                ) {
                    viewModel.refresh(environment)
                    showCreateCollectionDialog = false
                    userMessages.showShortMessage("收藏夹已创建")
                }
            }
        },
        isSubmitting = viewModel.isCreatingCollection,
        errorMessage = viewModel.createCollectionError,
    )

    collectionPendingDeletion?.let { collection ->
        val isDeleting = viewModel.deletingCollectionId != null
        AlertDialog(
            modifier = Modifier.testTag("collection_screen_delete_dialog_${collection.id}"),
            onDismissRequest = {
                if (!isDeleting) {
                    collectionPendingDeletion = null
                }
            },
            title = { Text("删除收藏夹") },
            text = {
                Column {
                    Text("删除后无法恢复，确认删除收藏夹“${collection.title}”吗？")
                    viewModel.deleteCollectionError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (viewModel.deleteCollection(environment, collection)) {
                                viewModel.refresh(environment)
                                collectionPendingDeletion = null
                                userMessages.showShortMessage("收藏夹已删除")
                            }
                        }
                    },
                    enabled = !isDeleting,
                    modifier = Modifier.testTag("collection_screen_delete_confirm_${collection.id}"),
                ) {
                    Text(if (isDeleting) "删除中…" else "删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        collectionPendingDeletion = null
                    },
                    enabled = !isDeleting,
                ) {
                    Text("取消")
                }
            },
        )
    }
}

private const val COLLECTION_SCREEN_TITLE_TAG = "collection_screen_title"
private const val COLLECTION_SCREEN_BACK_BUTTON_TAG = "collection_screen_back_button"
private const val COLLECTION_SCREEN_LIST_TAG = "collection_screen_list"
private const val COLLECTION_SCREEN_CREATE_BUTTON_TAG = "collection_screen_create_button"
