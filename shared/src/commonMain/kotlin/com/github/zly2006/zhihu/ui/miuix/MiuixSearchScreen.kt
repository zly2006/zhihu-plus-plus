/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.platform.UserMessageDuration
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.SEARCH_HISTORY_MAX_SIZE
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.ui.loadSearchHistory
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.miuix.components.MiuixListLoadingIndicator
import com.github.zly2006.zhihu.ui.miuix.components.MiuixSearchFilterSheet
import com.github.zly2006.zhihu.ui.miuix.components.MiuixSearchSuggestions
import com.github.zly2006.zhihu.ui.saveSearchHistory
import com.github.zly2006.zhihu.viewmodel.feed.SearchViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 搜索结果页的 miuix 版本（对齐 M3 SearchScreen）。
 *
 * 顶栏内嵌搜索输入框（miuix TopAppBar 标题仅支持字符串，故自绘顶栏）+ 返回 + 筛选；空查询时复用
 * [MiuixSearchSuggestions] 展示历史/热搜（成员搜索则显示提示），有查询时分页展示结果。筛选（排序/内容
 * 类型/时间范围）走底部弹层，复用 [SearchViewModel] 的过滤逻辑。
 */
@Composable
fun MiuixSearchScreen(search: Search) {
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()
    val settings = rememberSettingsStore()
    val viewModel = viewModel { SearchViewModel(search.query, search.restrictedMemberHashId) }
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val keyboard = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val blurEnabled = settings.getBoolean("blurEnabled", true)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)

    var searchText by remember { mutableStateOf(search.query) }
    var showFilter by remember { mutableStateOf(false) }
    val isMember = search.isRestrictedToMember
    val memberName = search.restrictedMemberName.ifBlank { "TA" }
    val placeholder = if (isMember) "搜索 $memberName 的创作" else "搜索内容"
    val showSearchHistory = settings.getBoolean("showSearchHistory", true)

    fun submitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        if (showSearchHistory && !isMember) {
            val history = loadSearchHistory(settings).toMutableList()
            history.remove(trimmed)
            history.add(0, trimmed)
            while (history.size > SEARCH_HISTORY_MAX_SIZE) history.removeAt(history.lastIndex)
            saveSearchHistory(settings, history)
        }
        navigator.onNavigate(search.copy(query = trimmed))
    }

    LaunchedEffect(search.query) {
        if (search.query.isNotEmpty() && viewModel.displayItems.isEmpty()) {
            viewModel.refresh(environment)
        }
    }
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { userMessages.showMessage(it, UserMessageDuration.Long) }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .installerMiuixBlurEffect(backdrop)
                    .background(backdrop.getMiuixAppBarColor())
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = navigator.onNavigateBack) {
                    Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                }
                // 搜索输入框
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Search, "搜索", tint = MiuixTheme.colorScheme.onSurfaceSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MiuixTheme.textStyles.body1.copy(color = MiuixTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboard?.hide()
                            submitSearch(searchText)
                        }),
                        decorationBox = { inner ->
                            if (searchText.isEmpty()) {
                                Text(placeholder, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                            }
                            inner()
                        },
                    )
                    if (searchText.isNotEmpty()) {
                        Icon(
                            Icons.Default.Clear,
                            "清除",
                            tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.size(18.dp).clickable { searchText = "" },
                        )
                    }
                }
                IconButton(onClick = { showFilter = true }, enabled = search.query.isNotEmpty()) {
                    Icon(Icons.Default.FilterList, "筛选", tint = MiuixTheme.colorScheme.onBackground)
                }
            }
        },
    ) { padding ->
        if (search.query.isEmpty()) {
            // 输入态：非成员搜索显示历史/热搜，成员搜索显示提示。
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (isMember) {
                    Text(
                        "输入关键词搜索 $memberName 的创作",
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    MiuixSearchSuggestions(onQueryClick = { submitSearch(it) })
                }
            }
        } else {
            PullToRefresh(
                isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
                onRefresh = { coroutineScope.launch { viewModel.pullToRefresh(environment) } },
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
                refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
            ) {
                Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
                    MiuixListLoadingIndicator(
                        isLoading = viewModel.isLoading,
                        isEmpty = viewModel.displayItems.isEmpty(),
                        isPullToRefresh = viewModel.isPullToRefresh,
                    )
                    PaginatedList(
                        items = viewModel.displayItems,
                        listState = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .overScrollVertical()
                            .scrollEndHaptic(),
                        contentPadding = PaddingValues(
                            top = padding.calculateTopPadding() + 6.dp,
                            bottom = padding.calculateBottomPadding() + 12.dp,
                        ),
                        onLoadMore = { viewModel.loadMore(environment) },
                        footer = ProgressIndicatorFooter,
                        key = { it.stableKey },
                        topContent = {
                            if (isMember) {
                                item {
                                    Text(
                                        "以下结果来自 $memberName 的创作",
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        },
                    ) { item ->
                        MiuixFeedCard(
                            item = item,
                            onClick = { this.navDestination?.let { navigator.onNavigate(it) } },
                        )
                    }
                }
            }
        }
    }

    // 筛选弹层：排序 / 内容类型 / 时间范围（与首页内联搜索共用）。
    MiuixSearchFilterSheet(
        show = showFilter,
        onDismiss = { showFilter = false },
        viewModel = viewModel,
        environment = environment,
    )
}
