/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.viewmodel.CollectionContentEnvironment
import com.github.zly2006.zhihu.viewmodel.CollectionContentViewModel
import com.github.zly2006.zhihu.viewmodel.formatArticleDateTime
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun MiuixCollectionContentScreen(
    collectionId: String,
) {
    val navigator = LocalNavigator.current
    val screenViewModel = viewModel { CollectionContentViewModel(collectionId) }
    val collectionEnvironment = rememberPaginationEnvironment(allowGuestAccess = false) as CollectionContentEnvironment
    val listState = rememberLazyListState()
    var showActionsMenu by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    val isEnd = { screenViewModel.isEnd }
    val onLoadMore = { screenViewModel.loadMore(collectionEnvironment) }
    val onExportAllToHtmlZip = { includeImages: Boolean ->
        screenViewModel.exportAllToHtmlZip(environment = collectionEnvironment, includeImages = includeImages)
    }
    val sharedData = collectionEnvironment.articleAnswerSwitchState()
    val settings = rememberSettingsStore()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(Unit) {
        if (screenViewModel.allData.isEmpty()) {
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
        var includeImages by remember { mutableStateOf(true) }
        WindowDialog(
            show = showExportOptionsDialog,
            title = "导出收藏夹 HTML",
            summary = "可以选择是否一并导出图片。导出图片会把图片下载并内嵌到 HTML 中，速度可能更慢。",
            onDismissRequest = { showExportOptionsDialog = false },
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("导出图片（更慢）", color = MiuixTheme.colorScheme.onSurface)
                        Text("关闭后保留原始图片链接", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = includeImages, onCheckedChange = { includeImages = it })
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(text = "取消", onClick = { showExportOptionsDialog = false }, modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            showExportOptionsDialog = false
                            onExportAllToHtmlZip(includeImages)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) { Text("开始导出", color = androidx.compose.ui.graphics.Color.White) }
                }
            }
        }
        screenViewModel.exportDialogState?.let { state ->
            WindowDialog(
                show = true,
                title = if (state.isCompleted) state.phaseText else "正在导出收藏夹",
                onDismissRequest = { if (state.isCompleted) screenViewModel.dismissExportDialog() },
            ) {
                Column {
                    if (state.currentTitle.isNotBlank()) {
                        Text("当前：${state.currentTitle}", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        Spacer(Modifier.height(8.dp))
                    }
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = state.progress.takeUnless { state.isIndeterminate },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("成功 ${state.successCount} · 跳过 ${state.skippedCount} · 失败 ${state.failedCount}", color = MiuixTheme.colorScheme.onSurface)
                    state.resultMessage?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    state.zipFilePath?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    if (state.isCompleted) {
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = screenViewModel::dismissExportDialog, colors = ButtonDefaults.buttonColorsPrimary()) {
                                Text("确定", color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                }
            }
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
                                        screenViewModel.collection?.updatedTime?.let { "${formatArticleDateTime(it)} 更新" },
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
                    if (dest is Article && dest.type == ArticleType.Answer && sharedData != null) {
                        val idx = screenViewModel.displayItems.indexOf(item)
                        val nextItems = if (idx >= 0) screenViewModel.allData.drop(idx + 1) else emptyList()
                        val prevItems = if (idx > 0) screenViewModel.allData.take(idx).reversed() else emptyList()
                        sharedData.pendingNavigator = CollectionAnswerNavigator(
                            collectionId = collectionId,
                            collectionTitle = screenViewModel.title,
                            initialNextItems = nextItems,
                            initialPreviousItems = prevItems,
                            environment = collectionEnvironment,
                        )
                    }
                    dest?.let { navigator.onNavigate(it) }
                }
            }
        }
    }
}
