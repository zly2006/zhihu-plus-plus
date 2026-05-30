/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 *
 * 结构照抄 KernelSU SuperUserMiuix（GPL-3.0）：
 *   - 头像放 TopAppBar 的 actions（右上角）
 *   - SearchBarFake 放 TopAppBar 的 bottomContent，用 onGloballyPositioned 上报 offsetY
 *   - SearchPager 展开时真框从 offsetY 位置开始动画 → 假框/真框位置一致
 *   - TopAppBarAnim 包裹 TopAppBar，提供消失回弹（alpha 切换）
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.RecommendationMode
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.ui.miuix.components.SearchBarFake
import com.github.zly2006.zhihu.ui.miuix.components.SearchBox
import com.github.zly2006.zhihu.ui.miuix.components.SearchPager
import com.github.zly2006.zhihu.ui.miuix.components.SearchStatus
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.ui.IHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.local.LocalHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.AndroidHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixHomeScreen(
    scrollToTopTrigger: Int = 0,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val navigator = LocalNavigator.current
    val context = LocalActivity.current as MainActivity
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE) }

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
    var searchStatus by remember { mutableStateOf(SearchStatus(label = "搜索知乎")) }
    val showAccountSheet = remember { mutableStateOf(false) }

    // 本地推荐 VM（like/dislike 反馈、本地内容打开）
    val localHomeViewModel = viewModel as? LocalHomeFeedViewModel

    // 屏蔽相关 state（沿用 HomeScreen 逻辑）
    var showBlockUserDialog by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showBlockByKeywordsDialog by remember { mutableStateOf(false) }
    var feedToBlockByKeywords by remember { mutableStateOf<Pair<String, String?>?>(null) }

    LaunchedEffect(currentRecommendationMode, AccountData.data.login) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    val blurEnabled = remember { preferences.getBoolean("blurEnabled", true) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    // 外层 Box：让 SearchPager 覆盖整个屏幕（从真正的屏幕顶部算），
    // 而不是被困在 Scaffold 内容区（已被 topBar 推下去，会导致不靠顶 + 返回闪现）
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            // TopAppBarAnim：消失回弹（alpha 切换 + 背景层）
            searchStatus.TopAppBarAnim(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                backgroundColor = backdrop.getMiuixAppBarColor(),
            ) {
                TopAppBar(
                    color = backdrop.getMiuixAppBarColor(),
                    title = "主页",
                    scrollBehavior = scrollBehavior,
                    // 头像移到右上角 actions
                    actions = {
                        IconButton(
                            onClick = { showAccountSheet.value = true },
                            modifier = Modifier.size(48.dp).padding(end = 8.dp),
                        ) {
                            val avatarUrl = AccountData.data.self?.avatarUrl
                            if (avatarUrl != null) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "账号",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(32.dp)
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
                    // SearchBarFake 放 bottomContent，上报 offsetY 供真框对齐
                    bottomContent = {
                        Box(
                            modifier = Modifier
                                .alpha(if (searchStatus.isCollapsed()) 1f else 0f)
                                .onGloballyPositioned { coordinates ->
                                    // 只在折叠态上报 offsetY，锁定假框真实位置；
                                    // 展开/收起期间布局会变，不能更新，否则收起会弹到错误位置
                                    if (searchStatus.isCollapsed()) {
                                        with(density) {
                                            val newOffsetY = coordinates.positionInWindow().y.toDp()
                                            if (searchStatus.offsetY != newOffsetY) {
                                                searchStatus = searchStatus.copy(offsetY = newOffsetY)
                                            }
                                        }
                                    }
                                }
                                .then(
                                    if (searchStatus.isCollapsed()) {
                                        Modifier.pointerInput(Unit) {
                                            detectTapGestures {
                                                searchStatus = searchStatus.copy(current = SearchStatus.Status.EXPANDING)
                                            }
                                        }
                                    } else Modifier,
                                ),
                        ) {
                            SearchBarFake(
                                label = searchStatus.label,
                                searchBarTopPadding = 0.dp,
                                onClick = {
                                    searchStatus = searchStatus.copy(current = SearchStatus.Status.EXPANDING)
                                },
                            )
                        }
                    },
                )
            }
        },
    ) { padding ->
        // Scaffold 内容区：feed + 下拉刷新
        searchStatus.SearchBox {
            PullToRefresh(
                isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
                onRefresh = { coroutineScope.launch { viewModel.pullToRefresh(context) } },
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
                refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
            ) {
                Box(
                    modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
                ) {
                    PaginatedList(
                        items = viewModel.displayItems,
                        listState = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .overScrollVertical()
                            .scrollEndHaptic()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            top = padding.calculateTopPadding() + 6.dp,
                            bottom = innerPadding.calculateBottomPadding() + 12.dp,
                        ),
                        onLoadMore = { viewModel.loadMore(context) },
                        key = { item -> item.stableKey },
                    ) { item ->
                        MiuixFeedCard(
                            item = item,
                            onLike = {
                                if (localHomeViewModel != null && it.localContentId != null) {
                                    localHomeViewModel.onLocalItemFeedback(context, it, 1.0)
                                }
                            },
                            onDislike = {
                                if (localHomeViewModel != null && it.localContentId != null) {
                                    localHomeViewModel.onLocalItemFeedback(context, it, -1.0)
                                }
                            },
                            onBlockUser = { feedItem ->
                                viewModel.handleBlockUser(context, feedItem) { authorInfo ->
                                    userToBlock = authorInfo
                                    showBlockUserDialog = true
                                }
                            },
                            onBlockByKeywords = { feedItem ->
                                viewModel.handleBlockByKeywords(context, feedItem) { (_, contentInfo) ->
                                    feedToBlockByKeywords = contentInfo.first to contentInfo.second
                                    showBlockByKeywordsDialog = true
                                }
                            },
                            onBlockTopic = { topicId, topicName ->
                                viewModel.handleBlockTopic(context, topicId, topicName)
                            },
                            onClick = {
                                // 默认跳转逻辑：本地内容回调 + navDestination
                                val feed = this.feed
                                if (feed != null) {
                                    (viewModel as? IHomeFeedViewModel)
                                        ?.onUiContentClick(context, feed, this)
                                } else if (localHomeViewModel != null && this.localContentId != null) {
                                    localHomeViewModel.onLocalItemOpened(context, this)
                                }
                                this.navDestination?.let { navigator.onNavigate(it) }
                            },
                        )
                    }
                }
            }
        }
    }

    // 搜索浮层：在 Scaffold 外层、覆盖全屏（从屏幕真正顶部算坐标），
    // 这样 topPadding = systemBarsPadding + 5.dp 能真正靠到屏幕顶部
    searchStatus.SearchPager(
        onSearchStatusChange = { searchStatus = it },
        // 纯间距，不含 statusBar —— statusBar 高度已由 SearchPager 内部的 topPadding 处理，
        // 这里再加 statusBar 会双重 padding，导致收起时搜索框偏下
        searchBarTopPadding = 12.dp,
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
    } // 外层 Box 结束

    // 屏蔽用户确认对话框（与 HomeScreen 同签名）
    com.github.zly2006.zhihu.ui.components.BlockUserConfirmDialog(
        showDialog = showBlockUserDialog,
        userToBlock = userToBlock,
        displayItems = viewModel.displayItems,
        context = context,
        onDismiss = {
            showBlockUserDialog = false
            userToBlock = null
        },
        onConfirm = {
            viewModel.refresh(context)
            showBlockUserDialog = false
            userToBlock = null
        },
    )

    // 按关键词屏蔽对话框
    feedToBlockByKeywords?.let { (title, excerpt) ->
        com.github.zly2006.zhihu.ui.components.BlockByKeywordsDialog(
            showDialog = showBlockByKeywordsDialog,
            feedTitle = title,
            feedExcerpt = excerpt,
            onDismiss = {
                showBlockByKeywordsDialog = false
                feedToBlockByKeywords = null
            },
            onConfirm = {
                viewModel.refresh(context)
                showBlockByKeywordsDialog = false
                feedToBlockByKeywords = null
            },
        )
    }

    MiuixAccountSheet(
        show = showAccountSheet.value,
        onDismiss = { showAccountSheet.value = false },
    )
}
