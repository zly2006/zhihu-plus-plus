/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.navDestination
import com.github.zly2006.zhihu.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberZhihuWebUrlOpener
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.FollowedQuestion
import com.github.zly2006.zhihu.ui.FollowedTopic
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_SUBSCRIPTION_TITLES
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_TITLES
import com.github.zly2006.zhihu.ui.PersonViewModel
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.peopleScreenInitialPage
import com.github.zly2006.zhihu.ui.webUrl
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import com.github.zly2006.zhihu.navigation.Pin as PinNav
import com.github.zly2006.zhihu.navigation.Question as QuestionNav
import com.github.zly2006.zhihu.navigation.Search as SearchDestination
import com.github.zly2006.zhihu.navigation.Topic as TopicNav

/** 「关注订阅」在 [PEOPLE_SCREEN_TITLES] 里的下标：它没有单一 feed，由页内二级 tab 各自加载。 */
private val SUBSCRIPTIONS_PAGE = PEOPLE_SCREEN_TITLES.lastIndex

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiuixPeopleScreen(
    person: Person,
) {
    val navigator = LocalNavigator.current
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = false)
    val viewModel = viewModel { PersonViewModel(person) }
    val coroutineScope = rememberCoroutineScope()
    val settings = rememberSettingsStore()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    var subscriptionTab by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = peopleScreenInitialPage(person),
        pageCount = { PEOPLE_SCREEN_TITLES.size },
    )
    val searchMemberHashId = viewModel.memberHashId
        .takeUnless { it.isBlank() || it == Person.EMPTY_ID }

    LaunchedEffect(viewModel) {
        try {
            viewModel.load(paginationEnvironment)
        } catch (e: Exception) {
            Log.e("MiuixPeopleScreen", "Error loading person data", e)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .installerMiuixBlurEffect(backdrop)
                    .padding(bottom = 6.dp),
            ) {
                TopAppBar(
                    color = backdrop.getMiuixAppBarColor(),
                    title = viewModel.name,
                    navigationIcon = {
                        IconButton(onClick = navigator.onNavigateBack) {
                            Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
                // TabRow inside topBar, shared blur — always visible
                TabRow(
                    tabs = PEOPLE_SCREEN_TITLES,
                    selectedTabIndex = pagerState.currentPage,
                    onTabSelected = { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val subModel = if (page == SUBSCRIPTIONS_PAGE) {
                when (subscriptionTab) {
                    0 -> viewModel.followingColumnsFeedModel
                    1 -> viewModel.followingTopicsFeedModel
                    2 -> viewModel.followingQuestionsFeedModel
                    else -> viewModel.followingCollectionsFeedModel
                }
            } else {
                viewModel.subFeedModels.getOrNull(page)
            }
            if (subModel != null) {
                LaunchedEffect(page, subModel) {
                    if (page == 2) {
                        val vm = subModel as BaseFeedViewModel
                        if (vm.displayItems.isEmpty()) vm.loadMore(paginationEnvironment)
                    } else {
                        if (subModel.allData.isEmpty()) subModel.loadMore(paginationEnvironment)
                    }
                }
                val items = if (page == 2) {
                    (subModel as BaseFeedViewModel).displayItems.takeIf { it.isNotEmpty() } ?: emptyList()
                } else {
                    subModel.allData
                }
                Box(
                    modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
                ) {
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier
                            .fillMaxSize()
                            .overScrollVertical()
                            .scrollEndHaptic()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            top = padding.calculateTopPadding() + 6.dp,
                            bottom = padding.calculateBottomPadding() + 8.dp,
                        ),
                    ) {
                        // Profile Card — LazyColumn 第一个 item，滚动时消失
                        item(key = "profileCard") {
                            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(model = viewModel.avatar, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape))
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(viewModel.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            if (viewModel.headline.isNotEmpty()) {
                                                Text(viewModel.headline, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        StatItem("回答", viewModel.answerCount) { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                                        StatItem("文章", viewModel.articleCount) { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                                        StatItem("关注者", viewModel.followerCount) { coroutineScope.launch { pagerState.animateScrollToPage(7) } }
                                        StatItem("关注", viewModel.followingCount) { coroutineScope.launch { pagerState.animateScrollToPage(8) } }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (searchMemberHashId != null) {
                                            Card(
                                                modifier = Modifier.weight(1f).clickable {
                                                    val memberName = viewModel.name.takeIf { it.isNotBlank() } ?: person.name
                                                    navigator.onNavigate(
                                                        SearchDestination(
                                                            restrictedMemberHashId = searchMemberHashId,
                                                            restrictedMemberName = memberName,
                                                        ),
                                                    )
                                                },
                                            ) {
                                                Row(
                                                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Icon(Icons.Default.Search, "搜索 TA 的创作", tint = MiuixTheme.colorScheme.primary)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("搜索", fontSize = 14.sp, color = MiuixTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                        val followText = if (viewModel.isFollowing) "已关注" else "关注"
                                        Card(
                                            modifier = Modifier.weight(1f).clickable {
                                                coroutineScope.launch {
                                                    try {
                                                        viewModel.toggleFollow(paginationEnvironment)
                                                    } catch (_: Exception) {
                                                    }
                                                }
                                            },
                                        ) {
                                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Text(followText, fontSize = 14.sp, color = MiuixTheme.colorScheme.primary) }
                                        }
                                        val blockText = if (viewModel.isBlocking) "已屏蔽" else "屏蔽"
                                        Card(
                                            modifier = Modifier.weight(1f).clickable {
                                                coroutineScope.launch {
                                                    try {
                                                        viewModel.toggleBlock(paginationEnvironment)
                                                    } catch (_: Exception) {
                                                    }
                                                }
                                            },
                                        ) {
                                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Text(blockText, fontSize = 14.sp, color = MiuixTheme.colorScheme.error) }
                                        }
                                    }
                                }
                            }
                        }

                        if (page == SUBSCRIPTIONS_PAGE) {
                            item(key = "subscriptionTabs") {
                                TabRow(
                                    tabs = PEOPLE_SCREEN_SUBSCRIPTION_TITLES,
                                    selectedTabIndex = subscriptionTab,
                                    onTabSelected = { subscriptionTab = it },
                                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp),
                                )
                            }
                        }

                        if (items.isEmpty()) {
                            // 首次加载列表为空时显示转圈，加载结束仍为空才显示「暂无内容」
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    if (subModel.isLoading) {
                                        CircularProgressIndicator()
                                    } else {
                                        Text("暂无内容", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                                    }
                                }
                            }
                        } else {
                            items(items.size, key = { items[it].hashCode() }) { index ->
                                // 用户、专栏、话题都带头像或独立跳转目标，套信息流卡片会退化成一行 toString()
                                when (val item = items[index]) {
                                    is DataHolder.People -> MiuixPeopleRow(item)
                                    is DataHolder.Column -> MiuixColumnRow(item)
                                    is FollowedTopic -> MiuixTopicRow(item)
                                    else -> {
                                        val feedItem = coerceToFeedItem(item)
                                        MiuixFeedCard(
                                            item = feedItem,
                                            onClick = { feedItem.navDestination?.let { navigator.onNavigate(it) } },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun coerceToFeedItem(item: Any): FeedDisplayItem = when (item) {
    is FeedDisplayItem -> item
    is DataHolder.Answer -> FeedDisplayItem(
        title = item.question.title,
        summary = item.excerpt,
        details = "",
        feed = null,
        navDestinationJson = Article(id = item.id, type = ArticleType.Answer, title = item.question.title)
            .toFeedDisplayItemNavDestinationJson(),
    )
    is DataHolder.Article -> FeedDisplayItem(
        title = item.title,
        summary = item.excerpt,
        details = "",
        feed = null,
        navDestinationJson = Article(id = item.id, type = ArticleType.Article, title = item.title)
            .toFeedDisplayItemNavDestinationJson(),
    )
    is DataHolder.Collection -> FeedDisplayItem(
        title = item.title,
        summary = null,
        details = "",
        feed = null,
        navDestinationJson = CollectionContent(collectionId = item.id)
            .toFeedDisplayItemNavDestinationJson(),
    )
    is DataHolder.Question -> FeedDisplayItem(
        title = item.title,
        summary = null,
        details = "",
        feed = null,
        navDestinationJson = QuestionNav(questionId = item.id, title = item.title)
            .toFeedDisplayItemNavDestinationJson(),
    )
    is FollowedQuestion -> FeedDisplayItem(
        title = item.title,
        summary = null,
        details = "",
        feed = null,
        navDestinationJson = item.id
            .toLongOrNull()
            ?.let { QuestionNav(questionId = it, title = item.title).toFeedDisplayItemNavDestinationJson() },
    )
    is DataHolder.Pin -> {
        val pinId = item.id.toLongOrNull() ?: 0L
        FeedDisplayItem(
            title = item.content.joinToString("") { it.toString() }.take(80),
            summary = null,
            details = "",
            feed = null,
            navDestinationJson = PinNav(id = pinId).toFeedDisplayItemNavDestinationJson(),
        )
    }
    else -> FeedDisplayItem(
        title = item.toString().take(80),
        summary = null,
        details = "",
        feed = null,
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

/** 粉丝 / 关注列表里的用户行，对标 M3 `PeopleListItem`。 */
@Composable
private fun MiuixPeopleRow(people: DataHolder.People) {
    val navigator = LocalNavigator.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .clickable {
                navigator.onNavigate(
                    Person(id = people.id, name = people.name, urlToken = people.urlToken ?: ""),
                )
            },
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = people.avatarUrl,
                contentDescription = "用户头像",
                modifier = Modifier.size(48.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(people.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (people.headline.isNotEmpty()) {
                    Text(
                        people.headline,
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "${people.answerCount} 回答 · ${people.articlesCount} 文章 · ${people.followerCount} 粉丝",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
            }
        }
    }
}

/** 「我订阅的专栏」列表行。专栏没有站内页面，点开的是知乎网页版。 */
@Composable
private fun MiuixColumnRow(column: DataHolder.Column) {
    val openZhihuWebUrl = rememberZhihuWebUrlOpener()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .clickable { openZhihuWebUrl(column.webUrl()) },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(column.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (column.description.isNotEmpty()) {
                Text(
                    column.description,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${column.articlesCount} 文章 · ${column.followerCount.coerceAtLeast(column.followers)} 关注",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
            )
        }
    }
}

/** 「关注的话题」列表行。 */
@Composable
private fun MiuixTopicRow(topic: FollowedTopic) {
    val navigator = LocalNavigator.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .clickable { navigator.onNavigate(TopicNav(topic.displayId, topic.displayName)) },
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = topic.displayAvatarUrl,
                contentDescription = "话题头像",
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Text(topic.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
