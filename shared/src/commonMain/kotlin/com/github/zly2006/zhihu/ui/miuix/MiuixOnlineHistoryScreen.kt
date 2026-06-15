/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.shared.platform.PlatformBackHandler
import com.github.zly2006.zhihu.shared.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.components.AutoHideTopBar
import com.github.zly2006.zhihu.ui.miuix.components.MiuixConfirmDialog
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.miuix.components.MiuixListLoadingIndicator
import com.github.zly2006.zhihu.viewmodel.feed.OnlineHistoryViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun MiuixOnlineHistoryScreen(showBackButton: Boolean = false) {
    val navigator = LocalNavigator.current
    val viewModel: OnlineHistoryViewModel = viewModel()
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val settings = rememberSettingsStore()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(environment)
        }
    }

    PlatformBackHandler(enabled = showClearHistoryDialog) {
        showClearHistoryDialog = false
    }

    Scaffold(
        topBar = {
            AutoHideTopBar {
                TopAppBar(
                    modifier = Modifier.installerMiuixBlurEffect(backdrop),
                    color = backdrop.getMiuixAppBarColor(),
                    title = "历史记录",
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        // 作为独立页面被 push 进来时（如账号页"浏览历史"入口）显示返回箭头；
                        // 作为底栏 tab 时不显示。
                        if (showBackButton) {
                            IconButton(onClick = { navigator.onNavigateBack() }) {
                                Icon(
                                    MiuixIconsEmbedded.Back,
                                    contentDescription = "返回",
                                    tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    },
                    actions = {
                        var showActionsMenu by remember { mutableStateOf(false) }
                        val haptic = LocalHapticFeedback.current
                        PlatformBackHandler(enabled = showActionsMenu) {
                            showActionsMenu = false
                        }
                        Box(modifier = Modifier.padding(end = 4.dp)) {
                            IconButton(onClick = { showActionsMenu = true }) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = "更多选项",
                                    tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground,
                                )
                            }
                            WindowListPopup(
                                show = showActionsMenu,
                                popupPositionProvider = com.github.zly2006.zhihu.ui.miuix.components.ListPopupDefaults.MenuPositionProvider,
                                alignment = PopupPositionProvider.Align.TopEnd,
                                onDismissRequest = { showActionsMenu = false },
                            ) {
                                val dismissState = LocalDismissState.current
                                ListPopupColumn {
                                    // 照抄官方 DropdownDefaults：横向 20dp，首尾 20dp、中间 12dp。
                                    // 无勾位、左对齐、fillMaxWidth 撑满（ListPopupColumn 最小宽 200dp）。
                                    Text(
                                        text = "查看本地历史记录",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(interactionSource = null, indication = null) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                dismissState?.invoke()
                                                navigator.onNavigate(History)
                                            }.padding(horizontal = 20.dp)
                                            .padding(top = 20.dp, bottom = 12.dp),
                                        color = MiuixTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                    )
                                    Text(
                                        text = "清除历史记录",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(interactionSource = null, indication = null) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                dismissState?.invoke()
                                                showClearHistoryDialog = true
                                            }.padding(horizontal = 20.dp)
                                            .padding(top = 12.dp, bottom = 20.dp),
                                        color = MiuixTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        MiuixConfirmDialog(
            show = showClearHistoryDialog,
            title = "确认清除历史记录",
            summary = "此操作会清除当前账号的在线和本地的全部历史记录。",
            confirmText = "确认",
            cancelText = "我再想想",
            onConfirm = {
                showClearHistoryDialog = false
                coroutineScope.launch {
                    environment.clearAllHistory()
                    viewModel.displayItems.clear()
                    userMessages.showShortMessage("已清除所有历史记录")
                }
            },
            onDismiss = { showClearHistoryDialog = false },
        )

        PullToRefresh(
            isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
            onRefresh = { coroutineScope.launch { viewModel.pullToRefresh(environment) } },
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
        ) {
            Box(
                modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxHeight()
                        .overScrollVertical()
                        .scrollEndHaptic()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 6.dp,
                        bottom = padding.calculateBottomPadding(),
                    ),
                ) {
                    items(viewModel.displayItems.size, key = { viewModel.displayItems[it].stableKey }) { index ->
                        MiuixFeedCard(
                            item = viewModel.displayItems[index],
                        )
                    }
                }
                MiuixListLoadingIndicator(
                    isLoading = viewModel.isLoading,
                    isEmpty = viewModel.displayItems.isEmpty(),
                    isPullToRefresh = viewModel.isPullToRefresh,
                )
            }
        }
    }
}
