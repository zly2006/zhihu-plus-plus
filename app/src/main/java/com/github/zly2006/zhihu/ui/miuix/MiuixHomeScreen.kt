/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 *
 * B 版（最小可用）：同页搜索动画跑通 + 头像入口接好；
 * feed 先用现有 ViewModel 简单渲染占位，验证搜索 OK 后再补 MiuixFeedCard + PullToRefresh。
 */

package com.github.zly2006.zhihu.ui.miuix

import android.content.Context.MODE_PRIVATE
import androidx.activity.compose.LocalActivity
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.RecommendationMode
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.miuix.components.SearchBox
import com.github.zly2006.zhihu.ui.miuix.components.SearchBarFake
import com.github.zly2006.zhihu.ui.miuix.components.SearchPager
import com.github.zly2006.zhihu.ui.miuix.components.SearchStatus
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.local.LocalHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.AndroidHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixHomeScreen(
    scrollToTopTrigger: Int = 0,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val navigator = LocalNavigator.current
    val context = LocalActivity.current as MainActivity
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE) }

    // 复用现有 ViewModel 选择逻辑（跟 HomeScreen 一致）
    val currentRecommendationMode = RecommendationMode.entries.find {
        it.key == preferences.getString("recommendationMode", RecommendationMode.MIXED.key)
    } ?: RecommendationMode.MIXED
    val viewModel: BaseFeedViewModel by when (currentRecommendationMode) {
        RecommendationMode.WEB -> context.viewModels<HomeFeedViewModel>()
        RecommendationMode.ANDROID -> context.viewModels<AndroidHomeFeedViewModel>()
        RecommendationMode.LOCAL -> context.viewModels<LocalHomeFeedViewModel>()
        RecommendationMode.MIXED -> context.viewModels<MixedHomeFeedViewModel>()
    }

    val listState = rememberLazyListState()

    // 搜索状态机
    var searchStatus by remember { mutableStateOf(SearchStatus(label = "搜索知乎")) }

    // 头像 BottomSheet
    val showAccountSheet = remember { mutableStateOf(false) }

    // 初始加载
    LaunchedEffect(currentRecommendationMode, AccountData.data.login) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    Scaffold(
        topBar = {
            SearchBarFake(
                label = searchStatus.label,
                onClick = {
                    // 触发展开（效果 B：展开过渡动画）
                    searchStatus = searchStatus.copy(current = SearchStatus.Status.EXPANDING)
                },
                trailingContent = {
                    // 头像入口：原 HomeScreen 的头像逻辑搬过来，点击弹 sheet（不跳转）
                    IconButton(
                        onClick = { showAccountSheet.value = true },
                        modifier = Modifier.size(44.dp),
                    ) {
                        val avatarUrl = AccountData.data.self?.avatarUrl
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "账号",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .border(0.5.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                                    .clip(CircleShape),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "账号",
                                tint = MiuixTheme.colorScheme.onBackground,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            // ① 折叠态：feed（B 版先用简单 LazyColumn 占位渲染，验证搜索后再换 MiuixFeedCard + PullToRefresh）
            searchStatus.SearchBox {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 6.dp,
                        bottom = innerPadding.calculateBottomPadding() + 12.dp,
                    ),
                ) {
                    items(viewModel.displayItems.size, key = { viewModel.displayItems[it].stableKey }) { index ->
                        val item = viewModel.displayItems[index]
                        // 占位：先用纯文本卡，验证搜索动画用；后续换 MiuixFeedCard
                        top.yukonga.miuix.kmp.basic.Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Box(Modifier.padding(16.dp)) {
                                Text(
                                    text = item.title,
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            // ② 搜索浮层（展开态）。B 版结果区先占位
            searchStatus.SearchPager(
                onSearchStatusChange = { searchStatus = it },
                defaultResult = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("输入关键词搜索", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                },
                result = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("搜索结果占位：${searchStatus.searchText}", color = MiuixTheme.colorScheme.onSurface)
                    }
                },
            )
        }
    }

    // 头像 BottomSheet（复用现有 MiuixAccountSheet）
    MiuixAccountSheet(
        show = showAccountSheet,
        onDismissRequest = { showAccountSheet.value = false },
    )
}
