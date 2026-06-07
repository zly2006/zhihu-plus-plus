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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageDuration
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.SearchViewModel
import com.github.zly2006.zhihu.viewmodel.feed.ZHIHU_HOT_SEARCH_URL
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray

@Serializable
private data class HotSearchItem(
    val query: String,
    val hotShow: String = "",
    val label: String = "",
)

private const val SEARCH_HISTORY_KEY = "searchHistoryQueries"
private const val SEARCH_HISTORY_MAX_SIZE = 20

private fun loadSearchHistory(settings: SettingsStore): List<String> =
    settings
        .getStringOrNull(SEARCH_HISTORY_KEY)
        ?.let { json ->
            runCatching { ZhihuJson.json.decodeFromString<List<String>>(json) }.getOrNull()
        }.orEmpty()

private fun saveSearchHistory(
    settings: SettingsStore,
    history: List<String>,
) {
    settings.putString(SEARCH_HISTORY_KEY, ZhihuJson.json.encodeToString(history))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    search: Search,
    testHotSearchQueries: List<String>? = null,
    onTestHotSearchRefresh: (() -> Unit)? = null,
) {
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()
    val settings = rememberSettingsStore()
    val viewModel = viewModel { SearchViewModel(search.query, search.restrictedMemberHashId) }
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = false)
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchText by remember { mutableStateOf(search.query) }
    val coroutineScope = rememberCoroutineScope()
    val isMemberSearch = search.isRestrictedToMember
    val memberSearchName = search.restrictedMemberName.ifBlank { "TA" }
    val searchPlaceholder = if (isMemberSearch) "搜索 $memberSearchName 的创作" else "搜索内容"

    val showHotSearch = remember { mutableStateOf(!isMemberSearch && settings.getBoolean("showSearchHotSearch", true)) }
    val hotSearchItems = remember(testHotSearchQueries) {
        mutableStateListOf<HotSearchItem>().apply {
            addAll(testHotSearchQueries.orEmpty().map { query -> HotSearchItem(query = query) })
        }
    }
    var hotSearchMoreMenuExpanded by remember { mutableStateOf(false) }
    var historyMoreMenuExpanded by remember { mutableStateOf(false) }
    val useTestHotSearchQueries = testHotSearchQueries != null
    val showSearchHistory = remember { mutableStateOf(!isMemberSearch && settings.getBoolean("showSearchHistory", true)) }
    val searchHistoryItems = remember {
        mutableStateListOf<String>().apply {
            if (!isMemberSearch) {
                addAll(loadSearchHistory(settings))
            }
        }
    }

    fun submitSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return
        if (showSearchHistory.value) {
            searchHistoryItems.remove(trimmedQuery)
            searchHistoryItems.add(0, trimmedQuery)
            while (searchHistoryItems.size > SEARCH_HISTORY_MAX_SIZE) {
                searchHistoryItems.removeAt(searchHistoryItems.lastIndex)
            }
            saveSearchHistory(settings, searchHistoryItems)
        }
        navigator.onNavigate(search.copy(query = trimmedQuery))
    }

    suspend fun fetchHotSearch() {
        val json = paginationEnvironment.fetchJson(ZHIHU_HOT_SEARCH_URL, "") ?: return
        val queries = json["hot_search_queries"] as? JsonArray ?: return
        hotSearchItems.clear()
        queries.take(15).forEach { item ->
            hotSearchItems.add(ZhihuJson.decodeJson(item))
        }
    }

    @Composable
    fun SearchHistoryHeader(showClearAction: Boolean) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "搜索历史",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = { historyMoreMenuExpanded = true },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("search_history_more_button"),
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多", modifier = Modifier.size(18.dp))
                DropdownMenu(
                    expanded = historyMoreMenuExpanded,
                    onDismissRequest = { historyMoreMenuExpanded = false },
                ) {
                    if (showClearAction) {
                        DropdownMenuItem(
                            text = { Text("清空搜索历史") },
                            onClick = {
                                historyMoreMenuExpanded = false
                                searchHistoryItems.clear()
                                saveSearchHistory(settings, searchHistoryItems)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("前往设置关闭搜索历史") },
                        onClick = {
                            historyMoreMenuExpanded = false
                            navigator.onNavigate(Account.AppearanceSettings("showSearchHistory"))
                        },
                    )
                }
            }
        }
    }

    LaunchedEffect(showHotSearch.value, useTestHotSearchQueries, isMemberSearch) {
        if (!isMemberSearch && showHotSearch.value && !useTestHotSearchQueries) {
            runCatching { fetchHotSearch() }
        }
    }

    // Load search results when query is not empty
    LaunchedEffect(search.query) {
        if (search.query.isNotEmpty() && viewModel.displayItems.isEmpty()) {
            viewModel.refresh(paginationEnvironment)
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            userMessages.showMessage(it, UserMessageDuration.Long)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "搜索",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("search_input"),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            keyboardController?.hide()
                                            submitSearch(searchText)
                                        },
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (searchText.isEmpty()) {
                                            Text(
                                                text = searchPlaceholder,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyLarge,
                                            )
                                        }
                                        innerTextField()
                                    },
                                )
                                if (searchText.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { searchText = "" },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("search_clear_button"),
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "清除",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        modifier = Modifier.testTag("search_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (viewModel.displayItems.isEmpty() && !viewModel.isLoading && viewModel.searchQuery.isEmpty()) {
                val shouldShowHistory = showSearchHistory.value && searchHistoryItems.isNotEmpty()
                val shouldShowHotSearch = showHotSearch.value && hotSearchItems.isNotEmpty()
                if (shouldShowHistory || shouldShowHotSearch) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                            .testTag("search_hot_list"),
                    ) {
                        if (shouldShowHistory) {
                            SearchHistoryHeader(showClearAction = true)
                            Spacer(modifier = Modifier.height(8.dp))
                            searchHistoryItems.forEach { query ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            keyboardController?.hide()
                                            submitSearch(query)
                                        }.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .width(28.dp)
                                            .size(18.dp),
                                    )
                                    Text(
                                        text = query,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                    )
                                }
                            }
                            if (shouldShowHotSearch) {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        if (shouldShowHotSearch) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "热搜",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (useTestHotSearchQueries) {
                                                onTestHotSearchRefresh?.invoke()
                                            } else {
                                                coroutineScope.launch { runCatching { fetchHotSearch() } }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("search_hot_refresh_button"),
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "刷新热搜", modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { hotSearchMoreMenuExpanded = true },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("search_hot_more_button"),
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "更多", modifier = Modifier.size(18.dp))
                                        DropdownMenu(
                                            expanded = hotSearchMoreMenuExpanded,
                                            onDismissRequest = { hotSearchMoreMenuExpanded = false },
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("关闭热搜显示") },
                                                onClick = {
                                                    hotSearchMoreMenuExpanded = false
                                                    navigator.onNavigate(Account.AppearanceSettings("showSearchHotSearch"))
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Column {
                                hotSearchItems.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                keyboardController?.hide()
                                                submitSearch(item.query)
                                            }.padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (index < 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(28.dp),
                                        )
                                        Text(
                                            text = item.query,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
                                        )
                                        if (item.hotShow.isNotEmpty()) {
                                            Text(
                                                text = item.hotShow,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (showSearchHistory.value || isMemberSearch) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            if (showSearchHistory.value) {
                                SearchHistoryHeader(showClearAction = false)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Text(
                                text = if (isMemberSearch) {
                                    "输入关键词搜索 $memberSearchName 的创作"
                                } else {
                                    "暂无搜索历史，输入关键词搜索后会保存在这里"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        Text(
                            text = "请输入搜索内容",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        )
                    }
                }
            } else {
                FeedPullToRefresh(viewModel, paginationEnvironment) {
                    PaginatedList(
                        items = viewModel.displayItems,
                        onLoadMore = { viewModel.loadMore(paginationEnvironment) },
                        topContent = {
                            item {
                                if (isMemberSearch) {
                                    Text(
                                        text = "以下结果来自 $memberSearchName 的创作",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        },
                        footer = ProgressIndicatorFooter,
                    ) { item ->
                        FeedCard(item)
                    }

                    val showRefreshFab = remember { settings.getBoolean("showRefreshFab", true) }
                    if (showRefreshFab) {
                        DraggableRefreshButton(
                            onClick = {
                                viewModel.refresh(paginationEnvironment)
                            },
                        ) {
                            if (viewModel.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "刷新")
                            }
                        }
                    }
                }
            }
        }
    }
}
