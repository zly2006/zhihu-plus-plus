/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.viewmodel.feed.HistoryViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixLocalHistoryScreen(
    innerPadding: PaddingValues,
) {
    val context = LocalActivity.current as MainActivity
    val viewModel: HistoryViewModel = viewModel()
    val scope = rememberCoroutineScope()
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
            )
        },
    ) { padding ->
        PullToRefresh(
            isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
            onRefresh = { scope.launch { viewModel.pullToRefresh(context) } },
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
                        bottom = innerPadding.calculateBottomPadding() + padding.calculateBottomPadding(),
                    ),
                ) {
                    items(viewModel.displayItems.size, key = { viewModel.displayItems[it].stableKey }) { index ->
                        MiuixFeedCard(
                            item = viewModel.displayItems[index],
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
