/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.ui.FOLLOW_DYNAMIC_LIST_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_RECOMMEND_LIST_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_SCREEN_PAGER_TAG
import com.github.zly2006.zhihu.ui.FOLLOW_SCREEN_TAB_ROW_TAG
import com.github.zly2006.zhihu.ui.FOLLOWING_USERS_ROW_TAG
import com.github.zly2006.zhihu.ui.FollowScreenData
import com.github.zly2006.zhihu.ui.FollowDynamicScreen
import com.github.zly2006.zhihu.ui.FollowRecommendScreen
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.AutoHideTopBar
import com.github.zly2006.zhihu.ui.followDynamicItemTag
import com.github.zly2006.zhihu.ui.followRecommendItemTag
import com.github.zly2006.zhihu.ui.followScreenTabTag
import com.github.zly2006.zhihu.ui.followingUserItemTag
import com.github.zly2006.zhihu.viewmodel.feed.RecentMomentsViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiuixFollowScreen(
    scrollToTopTrigger: Int = 0,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onTestRecommendRefreshClick: (() -> Unit)? = null,
    onTestRecommendLoadMore: (() -> Unit)? = null,
    onTestDynamicRefreshClick: (() -> Unit)? = null,
    onTestDynamicLoadMore: (() -> Unit)? = null,
) {
    val context = LocalActivity.current as MainActivity
    val viewModel = viewModel<FollowScreenData>()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    // 模糊开关只在这里用一次：blurEnabled=false 时 backdrop 为 null，
    // 之后 getMiuixAppBarColor()/installerMiuixBlurEffect() 自动按 null 处理，调用处不再判 blurEnabled
    val blurEnabled = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).getBoolean("blurEnabled", true) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(pagerState.currentPage) { viewModel.selectedTabIndex = pagerState.currentPage }
    LaunchedEffect(viewModel.selectedTabIndex) {
        if (pagerState.currentPage != viewModel.selectedTabIndex) pagerState.animateScrollToPage(viewModel.selectedTabIndex)
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .installerMiuixBlurEffect(backdrop)
                    .padding(bottom = 8.dp),
            ) {
                TopAppBar(
                    color = backdrop.getMiuixAppBarColor(),
                    title = "关注",
                    scrollBehavior = scrollBehavior,
                )
                MiuixFollowTabRow(
                    selectedTabIndex = viewModel.selectedTabIndex,
                    onTabSelected = { index ->
                        viewModel.selectedTabIndex = index
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                )
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().testTag(FOLLOW_SCREEN_PAGER_TAG),
        ) { page ->
            when (page) {
                0 -> FollowRecommendScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == 0,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    contentTopPadding = padding.calculateTopPadding(),
                    onTestRefreshClick = onTestRecommendRefreshClick,
                    onTestLoadMore = onTestRecommendLoadMore,
                )
                1 -> FollowDynamicScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    isActive = pagerState.currentPage == 1,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    contentTopPadding = padding.calculateTopPadding(),
                    onTestRefreshClick = onTestDynamicRefreshClick,
                    onTestLoadMore = onTestDynamicLoadMore,
                )
            }
        }
    }
}

@Composable
fun MiuixFollowTopLevelPage(
    selectedTabIndex: Int, onTabSelected: (Int) -> Unit,
    scrollToTopTrigger: Int = 0, innerPadding: PaddingValues = PaddingValues(0.dp),
    isActive: Boolean = true,
    topBarVisible: Boolean = true,
) {
    val context = LocalActivity.current as MainActivity
    val blurEnabled = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).getBoolean("blurEnabled", true) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        topBar = {
            AutoHideTopBar(topBarVisible) {
                Column(
                    modifier = Modifier
                        .installerMiuixBlurEffect(backdrop)
                        .padding(bottom = 8.dp),
                ) {
                    TopAppBar(
                        color = backdrop.getMiuixAppBarColor(),
                        title = "关注",
                        scrollBehavior = scrollBehavior,
                    )
                    MiuixFollowTabRow(selectedTabIndex = selectedTabIndex, onTabSelected = onTabSelected)
                }
            }
        },
    ) { padding ->
        when (selectedTabIndex) {
            0 -> FollowRecommendScreen(
                scrollToTopTrigger = scrollToTopTrigger, isActive = isActive,
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
                contentTopPadding = padding.calculateTopPadding(),
            )
            1 -> FollowDynamicScreen(
                scrollToTopTrigger = scrollToTopTrigger, isActive = isActive,
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
                contentTopPadding = padding.calculateTopPadding(),
            )
        }
    }
}

@Composable
private fun MiuixFollowTabRow(selectedTabIndex: Int, onTabSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    TabRow(
        tabs = listOf("推荐", "动态"),
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        // 底部留白：让 topBar 背景/模糊把 TabRow 完整包住，不与内容区贴边重叠
        modifier = modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .testTag(FOLLOW_SCREEN_TAB_ROW_TAG),
    )
}

@Composable
fun MiuixFollowingUsersRow() {
    val context = LocalActivity.current as MainActivity
    val navigator = LocalNavigator.current
    val viewModel: RecentMomentsViewModel = viewModel()

    LaunchedEffect(Unit) { viewModel.load(context) }

    when {
        viewModel.errorMessage != null -> {
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text(viewModel.errorMessage!!, color = MiuixTheme.colorScheme.onSurfaceSecondary)
            }
        }
        viewModel.users.isNotEmpty() -> {
            LazyRow(
                modifier = Modifier.testTag(FOLLOWING_USERS_ROW_TAG),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(viewModel.users) { user ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.testTag(followingUserItemTag(user.actor.id))
                            .clickable {
                                navigator.onNavigate(Person(
                                    id = user.actor.id, urlToken = user.actor.urlToken,
                                    name = user.actor.name, jumpTo = "动态",
                                ))
                            }.padding(vertical = 4.dp),
                    ) {
                        Box {
                            AsyncImage(
                                model = user.actor.avatarUrl, contentDescription = user.actor.name,
                                modifier = Modifier.size(56.dp).clip(CircleShape),
                            )
                            if (user.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .then(
                                            Modifier.background(MiuixTheme.colorScheme.error)
                                        ),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = user.actor.name, fontSize = 12.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                            modifier = Modifier.size(width = 60.dp, height = 18.dp),
                        )
                    }
                }
            }
        }
    }
}
