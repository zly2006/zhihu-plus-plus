/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin as PinNav
import com.github.zly2006.zhihu.navigation.Question as QuestionNav
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.PeopleScreenTestOverrides
import com.github.zly2006.zhihu.ui.PersonViewModel
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val PEOPLE_SCREEN_TITLES = listOf(
    "回答", "文章", "动态", "收藏", "提问", "想法", "专栏", "粉丝", "关注", "关注订阅",
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiuixPeopleScreen(
    person: Person,
    testOverrides: PeopleScreenTestOverrides? = null,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel = viewModel { PersonViewModel(person) }
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    val blurEnabled = remember { mutableStateOf(preferences.getBoolean("blurEnabled", true)) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled.value)
    val scrollBehavior = MiuixScrollBehavior()

    val initialPage = testOverrides?.initialPage ?: 0
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { PEOPLE_SCREEN_TITLES.size },
    )

    LaunchedEffect(viewModel, testOverrides) {
        if (testOverrides != null) return@LaunchedEffect
        try {
            viewModel.load(context)
            AccountData.addReadHistory(context, person.id, "profile")
        } catch (e: Exception) {
            Log.e("MiuixPeopleScreen", "Error loading person data", e)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
            ) {
                TopAppBar(
                    color = backdrop.getMiuixAppBarColor(),
                    title = viewModel.name,
                    navigationIcon = {
                        IconButton(onClick = navigator.onNavigateBack) {
                            Icon(MiuixIcons.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
                // Profile Card inside topBar — blurs together with TopAppBar
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = viewModel.avatar,
                                contentDescription = "用户头像",
                                modifier = Modifier.size(56.dp).clip(CircleShape),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (viewModel.headline.isNotEmpty()) {
                                    Text(
                                        viewModel.headline,
                                        fontSize = 14.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            StatItem("回答", viewModel.answerCount) { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                            StatItem("文章", viewModel.articleCount) { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                            StatItem("关注者", viewModel.followerCount) { coroutineScope.launch { pagerState.animateScrollToPage(7) } }
                            StatItem("关注", viewModel.followingCount) { coroutineScope.launch { pagerState.animateScrollToPage(8) } }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val followText = if (viewModel.isFollowing) "已关注" else "关注"
                            Card(
                                modifier = Modifier.weight(1f).clickable {
                                    coroutineScope.launch {
                                        try { viewModel.toggleFollow(context) }
                                        catch (e: Exception) { Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show() }
                                    }
                                },
                            ) {
                                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(followText, fontSize = 14.sp, color = MiuixTheme.colorScheme.primary)
                                }
                            }
                            val blockText = if (viewModel.isBlocking) "已屏蔽" else "屏蔽"
                            Card(
                                modifier = Modifier.weight(1f).clickable {
                                    coroutineScope.launch {
                                        try { viewModel.toggleBlock(context) }
                                        catch (e: Exception) { Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show() }
                                    }
                                },
                            ) {
                                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(blockText, fontSize = 14.sp, color = MiuixTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                // TabRow inside topBar, shared blur
                TabRow(
                    tabs = PEOPLE_SCREEN_TITLES,
                    selectedTabIndex = pagerState.currentPage,
                    onTabSelected = { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) { padding ->
        // Pager as direct content — same pattern as FollowScreen
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
                val subModel = viewModel.subFeedModels.getOrNull(page)
                if (subModel != null) {
                    LaunchedEffect(page) {
                        if (page == 2) {
                            val vm = subModel as BaseFeedViewModel
                            if (vm.displayItems.isEmpty()) vm.loadMore(context)
                        } else {
                            if (subModel.allData.isEmpty()) subModel.loadMore(context)
                        }
                    }
                    val items = if (page == 2) {
                        (subModel as BaseFeedViewModel).displayItems.takeIf { it.isNotEmpty() } ?: emptyList()
                    } else {
                        subModel.allData
                    }
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        if (items.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("暂无内容", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                                }
                            }
                        } else {
                            items(items.size, key = { items[it].hashCode() }) { index ->
                                val item = items[index]
                                val feedItem = coerceToFeedItem(item)
                                MiuixFeedCard(
                                    item = feedItem,
                                    onClick = {
                                        feedItem.navDestination?.let { navigator.onNavigate(it) }
                                    },
                                )
                            }
                        }
                    }
            }
        }
    }
}

private fun coerceToFeedItem(item: Any): BaseFeedViewModel.FeedDisplayItem = when (item) {
    is BaseFeedViewModel.FeedDisplayItem -> item
    is DataHolder.Answer -> BaseFeedViewModel.FeedDisplayItem(
        title = item.question.title,
        summary = item.excerpt,
        details = "",
        feed = null,
        localFeedId = item.id.toString(),
        navDestination = Article(id = item.id, type = ArticleType.Answer, title = item.question.title),
    )
    is DataHolder.Article -> BaseFeedViewModel.FeedDisplayItem(
        title = item.title,
        summary = item.excerpt,
        details = "",
        feed = null,
        localFeedId = item.id.toString(),
        navDestination = Article(id = item.id, type = ArticleType.Article, title = item.title),
    )
    is DataHolder.Collection -> BaseFeedViewModel.FeedDisplayItem(
        title = item.title,
        summary = null,
        details = "",
        feed = null,
        localFeedId = item.id,
        navDestination = CollectionContent(collectionId = item.id),
    )
    is DataHolder.Question -> BaseFeedViewModel.FeedDisplayItem(
        title = item.title,
        summary = null,
        details = "",
        feed = null,
        localFeedId = item.id.toString(),
        navDestination = QuestionNav(questionId = item.id, title = item.title),
    )
    is DataHolder.Pin -> {
        val pinId = item.id.toLongOrNull() ?: 0L
        BaseFeedViewModel.FeedDisplayItem(
            title = item.content.joinToString("") { it.toString() }.take(80),
            summary = null,
            details = "",
            feed = null,
            localFeedId = item.id,
            navDestination = PinNav(id = pinId),
        )
    }
    else -> BaseFeedViewModel.FeedDisplayItem(
        title = item.toString().take(80),
        summary = null,
        details = "",
        feed = null,
        localFeedId = item.hashCode().toString(),
    )
}

@Composable
private fun StatItem(label: String, count: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp),
    ) {
        Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
    }
}
