package com.github.zly2006.zhihu.ui

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.History
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.feed.OnlineHistoryViewModel
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineHistoryScreen(innerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    val viewModel: OnlineHistoryViewModel = viewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                actions = {
                    var showActionsMenu by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showActionsMenu = true },
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "更多选项",
                        )

                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("查看本地历史记录") },
                                onClick = {
                                    showActionsMenu = false
                                    navigator.onNavigate(History)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("清除历史记录") },
                                onClick = {
                                    showActionsMenu = false
                                    showClearHistoryDialog = true
                                },
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { innerPadding ->
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("确认清除历史记录") },
                text = { Text("此操作会清除当前账号的在线和本地的全部历史记录。") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearHistoryDialog = false
                        coroutineScope.launch {
                            (context as? MainActivity)?.history?.clearAndSave()
                            AccountData.fetchPost(context, "https://api.zhihu.com/read_history/batch_del") {
                                signFetchRequest()
                                contentType(ContentType.Application.Json)
                                setBody(
                                    buildJsonObject {
                                        put("pairs", JsonArray(emptyList()))
                                        put("clear", true)
                                    },
                                )
                            }
                            viewModel.displayItems.clear()
                            Toast.makeText(context, "已清除所有历史记录", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("确认")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("我再想想")
                    }
                },
            )
        }
        FeedPullToRefresh(viewModel, padding = innerPadding) {
            PaginatedList(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                items = viewModel.displayItems,
                onLoadMore = { viewModel.loadMore(context) },
                isEnd = { viewModel.isEnd },
            ) { item ->
                FeedCard(
                    item,
                ) {
                    item.navDestination?.let {
                        navigator.onNavigate(it)
                    }
                }
            }
        }
    }
}
