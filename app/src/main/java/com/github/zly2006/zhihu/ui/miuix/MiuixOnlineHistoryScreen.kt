/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.feed.OnlineHistoryViewModel
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixOnlineHistoryScreen() {
    val navigator = LocalNavigator.current
    val viewModel: OnlineHistoryViewModel = viewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    val blurEnabled = remember { mutableStateOf(preferences.getBoolean("blurEnabled", true)) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled.value)
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "历史记录",
                scrollBehavior = scrollBehavior,
                actions = {
                    var showActionsMenu by remember { mutableStateOf(false) }
                    val haptic = LocalHapticFeedback.current
                    BackHandler(enabled = showActionsMenu) {
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
                                        }
                                        .padding(horizontal = 20.dp)
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
                                        }
                                        .padding(horizontal = 20.dp)
                                        .padding(top = 12.dp, bottom = 20.dp),
                                    color = MiuixTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("确认清除历史记录") },
                text = { Text("此操作会清除当前账号的在线和本地的全部历史记录。") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
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
                    androidx.compose.material3.TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("我再想想")
                    }
                },
            )
        }

        PullToRefresh(
            isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
            onRefresh = { coroutineScope.launch { viewModel.pullToRefresh(context) } },
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
        ) {
            Box(
                modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
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
            }
        }
    }
}
