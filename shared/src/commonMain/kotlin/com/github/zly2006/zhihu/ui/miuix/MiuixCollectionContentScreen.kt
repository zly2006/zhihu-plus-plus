/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastJoinToString
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionAnswerNavigator
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.platform.PlatformBackHandler
import com.github.zly2006.zhihu.shared.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.CollectionContentScreenTestOverrides
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.ui.formatCollectionUpdatedTime
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.viewmodel.CollectionContentEnvironment
import com.github.zly2006.zhihu.viewmodel.CollectionContentViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun MiuixCollectionContentScreen(
    collectionId: String,
    testOverrides: CollectionContentScreenTestOverrides? = null,
) {
    val navigator = LocalNavigator.current
    val screenViewModel = testOverrides?.viewModel ?: viewModel { CollectionContentViewModel(collectionId) }
    val collectionEnvironment = rememberPaginationEnvironment(allowGuestAccess = false) as CollectionContentEnvironment
    val listState = rememberLazyListState()
    var showActionsMenu by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    val isEnd = testOverrides?.let { { it.isEnd } } ?: { screenViewModel.isEnd }
    val onLoadMore = testOverrides?.onLoadMore ?: { screenViewModel.loadMore(collectionEnvironment) }
    val onExportAllToHtmlZip = testOverrides?.onExportAllToHtmlZip ?: { includeImages ->
        screenViewModel.exportAllToHtmlZip(environment = collectionEnvironment, includeImages = includeImages)
    }
    val sharedData = collectionEnvironment.articleAnswerSwitchState()
    val settings = rememberSettingsStore()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(testOverrides) {
        if (testOverrides == null && screenViewModel.allData.isEmpty()) {
            screenViewModel.refresh(collectionEnvironment)
        }
    }

    PlatformBackHandler(enabled = showActionsMenu) { showActionsMenu = false }
    PlatformBackHandler(enabled = showExportOptionsDialog) { showExportOptionsDialog = false }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = screenViewModel.title,
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    val haptic = LocalHapticFeedback.current
                    Box(modifier = Modifier.padding(end = 2.dp)) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showActionsMenu = true
                            },
                            enabled = screenViewModel.exportDialogState?.isCompleted != false,
                        ) {
                            Icon(Icons.Filled.MoreVert, "更多", tint = MiuixTheme.colorScheme.onBackground)
                        }
                        WindowListPopup(
                            show = showActionsMenu,
                            popupPositionProvider = com.github.zly2006.zhihu.ui.miuix.components.ListPopupDefaults.MenuPositionProvider,
                            alignment = PopupPositionProvider.Align.TopEnd,
                            onDismissRequest = { showActionsMenu = false },
                        ) {
                            val dismissState = LocalDismissState.current
                            ListPopupColumn {
                                Text(
                                    text = "全部导出HTML",
                                    modifier = Modifier
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            dismissState?.invoke()
                                            showExportOptionsDialog = true
                                        }.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                                    color = MiuixTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        if (showExportOptionsDialog) {
            var includeImages by remember { mutableStateOf(true) }
            AlertDialog(
                onDismissRequest = { showExportOptionsDialog = false },
                title = { Text("导出收藏夹 HTML") },
                text = {
                    Column {
                        Text("可以选择是否一并导出图片。导出图片会把图片下载并内嵌到 HTML 中，速度可能更慢。")
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeImages, onCheckedChange = { includeImages = it })
                            Column(Modifier.weight(1f)) {
                                Text("导出图片（更慢）")
                                Text("关闭后保留原始图片链接", fontSize = MiuixTheme.textStyles.body2.fontSize, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showExportOptionsDialog = false
                        onExportAllToHtmlZip(includeImages)
                    }) { Text("开始导出") }
                },
                dismissButton = { TextButton(onClick = { showExportOptionsDialog = false }) { Text("取消") } },
            )
        }
        screenViewModel.exportDialogState?.let { state ->
            AlertDialog(
                onDismissRequest = { if (state.isCompleted) screenViewModel.dismissExportDialog() },
                title = { Text(if (state.isCompleted) state.phaseText else "正在导出收藏夹") },
                text = {
                    Column {
                        Text(state.phaseText)
                        if (state.currentTitle.isNotBlank()) {
                            Text("当前：${state.currentTitle}")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (state.isIndeterminate) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("成功 ${state.successCount} · 跳过 ${state.skippedCount} · 失败 ${state.failedCount}")
                        state.resultMessage?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        state.zipFilePath?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    }
                },
                confirmButton = { if (state.isCompleted) TextButton(onClick = screenViewModel::dismissExportDialog) { Text("确定") } },
            )
        }

        Box(
            modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
        ) {
            PaginatedList(
                items = screenViewModel.displayItems,
                onLoadMore = onLoadMore,
                isEnd = isEnd,
                listState = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .testTag("collection_content_list"),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding(),
                ),
                footer = ProgressIndicatorFooter,
                topContent = {
                    item(0) {
                        // 收藏夹统计单独包一张卡，左右/内边距对齐信息流卡片（horizontal=12, inside=12）。
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    listOfNotNull(
                                        "${screenViewModel.collection?.itemCount} 条收藏",
                                        "${screenViewModel.collection?.likeCount} 个赞同",
                                        "${screenViewModel.collection?.commentCount} 条评论",
                                        screenViewModel.collection?.updatedTime?.let { "${formatCollectionUpdatedTime(it)} 更新" },
                                    ).fastJoinToString(" · "),
                                    modifier = Modifier.padding(12.dp),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                },
            ) { item ->
                MiuixFeedCard(item = item) {
                    val dest = navDestination
                    val repository = collectionEnvironment.answerNavigatorRepository()
                    if (dest is Article && dest.type == ArticleType.Answer && sharedData != null && repository != null) {
                        val idx = screenViewModel.displayItems.indexOf(item)
                        val nextItems = if (idx >= 0) screenViewModel.allData.drop(idx + 1) else emptyList()
                        val prevItems = if (idx > 0) screenViewModel.allData.take(idx).reversed() else emptyList()
                        sharedData.pendingNavigator = CollectionAnswerNavigator(
                            collectionId = collectionId,
                            collectionTitle = screenViewModel.title,
                            initialNextItems = nextItems,
                            initialPreviousItems = prevItems,
                            repository = repository,
                        )
                    }
                    dest?.let { navigator.onNavigate(it) }
                }
            }
        }
    }
}
