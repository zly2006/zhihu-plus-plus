package com.github.zly2006.zhihu.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.Account
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.Search
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.SearchViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
private data class HotSearchItem(
    val query: String,
    val hotShow: String = "",
    val label: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    search: Search,
) {
    val navigator = LocalNavigator.current
    val context = LocalActivity.current as MainActivity
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    val viewModel = viewModel { SearchViewModel(search.query) }
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchText by remember { mutableStateOf(search.query) }
    val coroutineScope = rememberCoroutineScope()

    val showHotSearch = remember { mutableStateOf(preferences.getBoolean("showSearchHotSearch", true)) }
    val hotSearchItems = remember { mutableStateListOf<HotSearchItem>() }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    suspend fun fetchHotSearch() {
        val json = AccountData.fetchGet(context, "https://www.zhihu.com/api/v4/search/hot_search") ?: return
        val queries = json["hot_search_queries"] as? JsonArray ?: return
        hotSearchItems.clear()
        queries.take(15).forEach { item ->
            hotSearchItems.add(AccountData.decodeJson(item))
        }
    }

    LaunchedEffect(Unit) {
        if (showHotSearch.value) runCatching { fetchHotSearch() }
    }

    // Load search results when query is not empty
    LaunchedEffect(search.query) {
        if (search.query.isNotEmpty() && viewModel.displayItems.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
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
                                        .weight(1f),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            keyboardController?.hide()
                                            if (searchText.isNotBlank()) {
                                                navigator.onNavigate(
                                                    Search(query = searchText),
                                                )
                                            }
                                        },
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (searchText.isEmpty()) {
                                            Text(
                                                text = "搜索内容",
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
                                        modifier = Modifier.size(20.dp),
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
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (viewModel.displayItems.isEmpty() && !viewModel.isLoading && viewModel.searchQuery.isEmpty()) {
                if (showHotSearch.value && hotSearchItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
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
                                        coroutineScope.launch { runCatching { fetchHotSearch() } }
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "刷新热搜", modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { moreMenuExpanded = true },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "更多", modifier = Modifier.size(18.dp))
                                    DropdownMenu(
                                        expanded = moreMenuExpanded,
                                        onDismissRequest = { moreMenuExpanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("关闭热搜显示") },
                                            onClick = {
                                                moreMenuExpanded = false
                                                navigator.onNavigate(Account.AppearanceSettings("showSearchHotSearch"))
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        hotSearchItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        keyboardController?.hide()
                                        navigator.onNavigate(
                                            Search(query = item.query),
                                        )
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
                } else if (!showHotSearch.value) {
                    Text(
                        text = "请输入搜索内容",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            } else {
                FeedPullToRefresh(viewModel) {
                    PaginatedList(
                        items = viewModel.displayItems,
                        onLoadMore = { viewModel.loadMore(context) },
                        topContent = {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        },
                        footer = ProgressIndicatorFooter,
                    ) { item ->
                        FeedCard(
                            item,
                        ) {
                            if (navDestination != null) {
                                navigator.onNavigate(navDestination)
                            } else {
                                Toast.makeText(context, "暂不支持打开该内容", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    val showRefreshFab = remember { preferences.getBoolean("showRefreshFab", true) }
                    if (showRefreshFab) {
                        DraggableRefreshButton(
                            onClick = {
                                viewModel.refresh(context)
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
