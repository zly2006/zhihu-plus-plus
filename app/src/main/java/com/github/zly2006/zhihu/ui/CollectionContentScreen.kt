package com.github.zly2006.zhihu.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.navigator.CollectionAnswerNavigator
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.CollectionContentViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionContentScreen(
    collectionId: String,
    innerPadding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel = viewModel { CollectionContentViewModel(collectionId) }
    val listState = rememberLazyListState()
    var showActionsMenu by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    val sharedData = if (context is MainActivity) {
        val sd by context.viewModels<ArticleViewModel.ArticlesSharedData>()
        sd
    } else {
        null
    }
    val horizontalPadding = LocalCardHorizontalPadding.current

    LaunchedEffect(Unit) {
        if (viewModel.allData.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = {
            TopAppBar(
                title = { Text(viewModel.title) },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showActionsMenu = true },
                            enabled = viewModel.exportDialogState?.isCompleted != false,
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("全部导出HTML") },
                                enabled = viewModel.exportDialogState?.isCompleted != false,
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
                    viewModel.exportAllToHtmlZip(
                        context = context,
                        includeImages = includeImages,
                    )
                },
            )
        }
        viewModel.exportDialogState?.let { state ->
            CollectionHtmlExportDialog(
                state = state,
                onDismiss = viewModel::dismissExportDialog,
            )
        }
        PaginatedList(
            items = viewModel.displayItems,
            onLoadMore = { viewModel.loadMore(context) },
            isEnd = { viewModel.isEnd },
            listState = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = horizontalPadding).padding(innerPadding),
            footer = ProgressIndicatorFooter,
            topContent = {
                item(0) {
                    Text(
                        listOfNotNull(
                            "${viewModel.collection?.itemCount} 条收藏",
                            "${viewModel.collection?.likeCount} 个赞同",
                            "${viewModel.collection?.commentCount} 条评论",
                            viewModel.collection?.updatedTime?.let { "${YMDHMS.format(Date(it * 1000))} 更新" },
                        ).fastJoinToString(" · "),
                    )
                }
            },
        ) { item ->
            FeedCard(
                item,
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                val dest = navDestination
                if (dest is Article && dest.type == ArticleType.Answer && sharedData != null) {
                    val idx = viewModel.displayItems.indexOf(item)
                    val nextItems = if (idx >= 0) viewModel.allData.drop(idx + 1) else emptyList()
                    val prevItems = if (idx > 0) viewModel.allData.take(idx).reversed() else emptyList()
                    sharedData.pendingNavigator = CollectionAnswerNavigator(
                        collectionId = collectionId,
                        collectionTitle = viewModel.title,
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
            ) {
                Text("开始导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun CollectionHtmlExportDialog(
    state: CollectionContentViewModel.CollectionHtmlExportDialogState,
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
