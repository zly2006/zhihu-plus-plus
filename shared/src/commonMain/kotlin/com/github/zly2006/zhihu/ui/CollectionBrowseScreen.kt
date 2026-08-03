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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.data.Collection
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.viewmodel.CollectionContentEnvironment
import com.github.zly2006.zhihu.viewmodel.CollectionContentViewModel
import com.github.zly2006.zhihu.viewmodel.CollectionsViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch

/**
 * 收藏直达浏览页：进入后直接展示某个收藏夹的内容瀑布流，
 * 右上角文件夹图标在页内切换当前展示的收藏夹，不跳转到新页面。
 *
 * 默认选中策略见 [pickDefaultCollectionId]（优先默认收藏夹，否则第一个）。
 *
 * @param testCollections 测试注入的收藏夹列表；非 null 时跳过收藏夹列表的网络拉取，便于仪器测试。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionBrowseScreen(
    urlToken: String?,
    showBackButton: Boolean = true,
    scrollToTopTrigger: Int = 0,
    isActive: Boolean = true,
    testCollections: List<Collection>? = null,
) {
    val navigator = LocalNavigator.current
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val contentEnvironment = environment as CollectionContentEnvironment
    val useTestCollections = testCollections != null || urlToken == null
    val collectionsViewModel: CollectionsViewModel = viewModel(key = urlToken) {
        CollectionsViewModel(urlToken.orEmpty())
    }
    val collections = testCollections ?: collectionsViewModel.allData
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }
    var randomGeneration by remember { mutableIntStateOf(0) }
    var selectedCollectionId by remember { mutableStateOf<String?>(null) }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var randomMode by remember { mutableStateOf(false) }
    var collectionPendingDeletion by remember { mutableStateOf<Collection?>(null) }

    LaunchedEffect(isActive) {
        if (shouldRefreshCollectionDataOnActivation(isActive, useTestCollections)) {
            collectionsViewModel.refresh(environment)
        }
    }
    LaunchedEffect(collections.map { it.id }, collectionsViewModel.isLoading) {
        if (collections.isNotEmpty() && collections.none { it.id == selectedCollectionId }) {
            selectedCollectionId = pickDefaultCollectionId(collections)
        } else if (collections.isEmpty() && (useTestCollections || collectionsViewModel.isEnd)) {
            selectedCollectionId = null
        }
    }

    val contentViewModel: CollectionContentViewModel? = selectedCollectionId?.let { collectionId ->
        viewModel(key = collectionId) { CollectionContentViewModel(collectionId) }
    }
    val sourceDisplayItems = contentViewModel?.displayItems?.toList().orEmpty()
    val visibleDisplayItems = remember(
        sourceDisplayItems,
        searchQuery,
        randomMode,
        randomGeneration,
        selectedCollectionId,
    ) {
        filterAndOrderCollectionItems(
            items = sourceDisplayItems,
            query = searchQuery,
            randomMode = randomMode,
            randomSeed = selectedCollectionId.hashCode() xor randomGeneration,
        )
    }
    LaunchedEffect(contentViewModel, isActive) {
        if (isActive && !useTestCollections && contentViewModel != null) {
            contentViewModel.refresh(contentEnvironment)
        }
    }
    LaunchedEffect(scrollToTopTrigger) {
        when (
            topLevelReselectAction(
                triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
                isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
            )
        ) {
            TopLevelReselectAction.Refresh -> {
                if (!useTestCollections) {
                    collectionsViewModel.refresh(environment)
                    contentViewModel?.refresh(contentEnvironment)
                }
            }
            TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
            null -> Unit
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = contentViewModel?.title ?: "收藏夹",
                            modifier = Modifier.testTag(COLLECTION_BROWSE_TITLE_TAG),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(
                                onClick = navigator.onNavigateBack,
                                modifier = Modifier.testTag(COLLECTION_BROWSE_BACK_BUTTON_TAG),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { folderMenuExpanded = true },
                                enabled = collections.isNotEmpty(),
                                modifier = Modifier.testTag(COLLECTION_BROWSE_FOLDER_SWITCH_BUTTON_TAG),
                            ) {
                                Icon(Icons.Filled.Folder, contentDescription = "切换收藏夹")
                            }
                            DropdownMenu(
                                expanded = folderMenuExpanded,
                                onDismissRequest = { folderMenuExpanded = false },
                                modifier = Modifier.testTag(COLLECTION_BROWSE_FOLDER_MENU_TAG),
                            ) {
                                collections.forEach { collection ->
                                    DropdownMenuItem(
                                        text = { Text(collection.title) },
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (collection.id == selectedCollectionId) {
                                                    Icon(Icons.Filled.Check, contentDescription = null)
                                                }
                                                if (!collection.isDefault) {
                                                    IconButton(
                                                        onClick = {
                                                            collectionsViewModel.clearMutationErrors()
                                                            collectionPendingDeletion = collection
                                                            folderMenuExpanded = false
                                                        },
                                                        enabled = collectionsViewModel.deletingCollectionId == null,
                                                        modifier = Modifier.testTag(
                                                            "collection_browse_delete_button_${collection.id}",
                                                        ),
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Delete,
                                                            contentDescription = "删除${collection.title}",
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedCollectionId = collection.id
                                            searchQuery = ""
                                            folderMenuExpanded = false
                                        },
                                        modifier = Modifier.testTag("collection_browse_folder_menu_item_${collection.id}"),
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { searchVisible = !searchVisible },
                            modifier = Modifier.testTag(COLLECTION_BROWSE_SEARCH_BUTTON_TAG),
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = "搜索收藏内容")
                        }
                        IconButton(
                            onClick = {
                                randomMode = !randomMode
                                if (randomMode) randomGeneration++
                                userMessages.showShortMessage(if (randomMode) "已切换为随机模式" else "已切换为顺序模式")
                            },
                            modifier = Modifier.testTag(COLLECTION_BROWSE_MODE_BUTTON_TAG),
                        ) {
                            Icon(
                                Icons.Filled.Shuffle,
                                contentDescription = if (randomMode) "切换为顺序模式" else "切换为随机模式",
                                tint = if (randomMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
                if (searchVisible) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag(COLLECTION_BROWSE_SEARCH_FIELD_TAG),
                        singleLine = true,
                        placeholder = { Text("搜索已加载的收藏内容") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        searchQuery = ""
                                    } else {
                                        searchVisible = false
                                    }
                                },
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "清除或关闭搜索")
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        when {
            collections.isEmpty() && (useTestCollections || collectionsViewModel.isEnd) -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("还没有收藏夹", modifier = Modifier.testTag(COLLECTION_BROWSE_EMPTY_COLLECTIONS_TAG))
                }
            }
            contentViewModel == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.testTag(COLLECTION_BROWSE_LOADING_COLLECTIONS_TAG))
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = collectionsViewModel.isLoading || contentViewModel.isLoading,
                    onRefresh = {
                        if (!useTestCollections) {
                            collectionsViewModel.refresh(environment)
                            contentViewModel.refresh(contentEnvironment)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag(COLLECTION_BROWSE_PULL_TO_REFRESH_TAG),
                ) {
                    if (contentViewModel.allData.isEmpty() && contentViewModel.isEnd) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "这个收藏夹是空的",
                                modifier = Modifier.testTag(COLLECTION_BROWSE_EMPTY_CONTENT_TAG),
                            )
                        }
                    } else {
                        CollectionContentBody(
                            viewModel = contentViewModel,
                            environment = contentEnvironment,
                            collectionId = selectedCollectionId.orEmpty(),
                            modifier = Modifier.fillMaxSize(),
                            listState = listState,
                            tagPrefix = "collection_browse",
                            displayItems = visibleDisplayItems,
                            emptyMessage = if (searchQuery.isNotBlank()) "已加载内容中没有匹配结果" else null,
                        )
                    }
                }
            }
        }
    }

    collectionPendingDeletion?.let { collection ->
        val isDeleting = collectionsViewModel.deletingCollectionId != null
        AlertDialog(
            modifier = Modifier.testTag("collection_browse_delete_dialog_${collection.id}"),
            onDismissRequest = {
                if (!isDeleting) {
                    collectionPendingDeletion = null
                }
            },
            title = { Text("删除收藏夹") },
            text = {
                Column {
                    Text("删除后无法恢复，确认删除收藏夹“${collection.title}”吗？")
                    collectionsViewModel.deleteCollectionError?.let { error ->
                        Text(
                            text = error,
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
                            if (collectionsViewModel.deleteCollection(environment, collection)) {
                                if (selectedCollectionId == collection.id) {
                                    selectedCollectionId = pickDefaultCollectionId(
                                        collections.filterNot { it.id == collection.id },
                                    )
                                }
                                collectionsViewModel.refresh(environment)
                                collectionPendingDeletion = null
                                userMessages.showShortMessage("收藏夹已删除")
                            }
                        }
                    },
                    enabled = !isDeleting,
                    modifier = Modifier.testTag("collection_browse_delete_confirm_${collection.id}"),
                ) {
                    Text(if (isDeleting) "删除中…" else "删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { collectionPendingDeletion = null },
                    enabled = !isDeleting,
                ) {
                    Text("取消")
                }
            },
        )
    }
}

/** 挑选默认展示的收藏夹 id：优先默认收藏夹，否则取第一个；列表为空返回 null。 */
internal fun pickDefaultCollectionId(collections: List<Collection>): String? =
    collections.firstOrNull { it.isDefault }?.id ?: collections.firstOrNull()?.id

internal fun shouldRefreshCollectionDataOnActivation(
    isActive: Boolean,
    useTestCollections: Boolean,
): Boolean = isActive && !useTestCollections

internal fun filterAndOrderCollectionItems(
    items: List<FeedDisplayItem>,
    query: String,
    randomMode: Boolean,
    randomSeed: Int,
): List<FeedDisplayItem> {
    val normalizedQuery = query.trim()
    val filtered = if (normalizedQuery.isEmpty()) {
        items
    } else {
        items.filter { item ->
            listOf(item.title, item.summary, item.details, item.authorName)
                .filterNotNull()
                .any { it.contains(normalizedQuery, ignoreCase = true) }
        }
    }
    return if (randomMode) {
        filtered.sortedBy { it.stableKey.hashCode() xor randomSeed }
    } else {
        filtered
    }
}

private const val COLLECTION_BROWSE_TITLE_TAG = "collection_browse_title"
private const val COLLECTION_BROWSE_BACK_BUTTON_TAG = "collection_browse_back_button"
private const val COLLECTION_BROWSE_FOLDER_SWITCH_BUTTON_TAG = "collection_browse_folder_switch_button"
private const val COLLECTION_BROWSE_FOLDER_MENU_TAG = "collection_browse_folder_menu"
private const val COLLECTION_BROWSE_EMPTY_COLLECTIONS_TAG = "collection_browse_empty_collections"
private const val COLLECTION_BROWSE_LOADING_COLLECTIONS_TAG = "collection_browse_loading_collections"
private const val COLLECTION_BROWSE_EMPTY_CONTENT_TAG = "collection_browse_empty_content"
private const val COLLECTION_BROWSE_PULL_TO_REFRESH_TAG = "collection_browse_pull_to_refresh"
private const val COLLECTION_BROWSE_SEARCH_BUTTON_TAG = "collection_browse_search_button"
private const val COLLECTION_BROWSE_SEARCH_FIELD_TAG = "collection_browse_search_field"
private const val COLLECTION_BROWSE_MODE_BUTTON_TAG = "collection_browse_mode_button"
