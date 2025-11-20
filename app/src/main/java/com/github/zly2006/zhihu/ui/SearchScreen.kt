package com.github.zly2006.zhihu.ui

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    search: com.github.zly2006.zhihu.Search,
    onNavigate: (NavDestination) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalActivity.current as MainActivity
    val viewModel: SearchViewModel by context.viewModels()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Initialize search query from navigation parameter
    LaunchedEffect(search.query) {
        if (search.query.isNotEmpty() && viewModel.searchQuery != search.query) {
            viewModel.updateSearchQuery(search.query)
            viewModel.performSearch(context)
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
                    OutlinedTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索内容") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                viewModel.performSearch(context)
                            },
                        ),
                        trailingIcon = {
                            if (viewModel.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                // Show empty state
                Text(
                    text = "请输入搜索内容",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
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
                        FeedCard(item) {
                            if (navDestination != null) {
                                onNavigate(navDestination)
                            } else {
                                Toast.makeText(context, "暂不支持打开该内容", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    DraggableRefreshButton(
                        onClick = {
                            viewModel.performSearch(context)
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
