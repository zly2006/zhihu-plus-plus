/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.MarkUnreadChatAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.shared.data.ZHIHU_LAST_READ_TOUCH_URL
import com.github.zly2006.zhihu.shared.data.encodeZhihuLastReadTouchItems
import com.github.zly2006.zhihu.shared.data.fetchZhihuUnreadNotificationCount
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.data.zhihuLastReadTouchItem
import com.github.zly2006.zhihu.shared.platform.UserMessageDuration
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.ui.TopLevelReselectAction
import com.github.zly2006.zhihu.shared.ui.topLevelReselectAction
import com.github.zly2006.zhihu.ui.components.AnnouncementCard
import com.github.zly2006.zhihu.ui.components.AnnouncementCardDefaults
import com.github.zly2006.zhihu.ui.components.BlockByKeywordsDialog
import com.github.zly2006.zhihu.ui.components.BlockUserConfirmDialog
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.ui.components.rememberFeedBlockActions
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedInteractionViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json

const val PREFERENCE_NAME = "com.github.zly2006.zhihu_preferences"
const val ARTICLE_USE_WEBVIEW_PREFERENCE_KEY = "webviewRender"
const val QQ_GROUP_DISMISSED_PREFERENCE_KEY = "dismissQQGroup11"
const val HOME_TOP_ACTIONS_TAG = "home_top_actions"
const val HOME_SEARCH_BUTTON_TAG = "home_search_button"
const val HOME_NOTIFICATION_BUTTON_TAG = "home_notification_button"
const val HOME_ACCOUNT_BUTTON_TAG = "home_account_button"
const val HOME_FEED_LIST_TAG = "home_feed_list"
const val HOME_REFRESH_BUTTON_TAG = "home_refresh_button"

interface IHomeFeedViewModel {
    suspend fun recordContentInteraction(environment: PaginationEnvironment, feed: Feed)

    fun onUiContentClick(environment: PaginationEnvironment, feed: Feed, item: FeedDisplayItem)

    /**
     * 发送"已读"状态到知乎服务器的通用实现
     */
    suspend fun sendReadStatusToServer(environment: PaginationEnvironment, feed: Feed) {
        try {
            val payloadItem = zhihuLastReadTouchItem(feed, "read") ?: return
            environment.httpClient().post(ZHIHU_LAST_READ_TOUCH_URL) {
                environment.configureSignedRequest(this)
                header("x-requested-with", "fetch")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "items",
                                encodeZhihuLastReadTouchItems(listOf(payloadItem)),
                            )
                        },
                    ),
                )
            }
        } catch (_: Exception) {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(scrollToTopTrigger: Int, innerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = true)
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    val openExternalUrl = rememberExternalUrlOpener()

    val duo3HomeAccount = settings.getBoolean("duo3_home_account", false)
    val showRefreshFab = settings.getBoolean("showRefreshFab", true)
    var showAccountBottomSheet by remember { mutableStateOf(false) }

    // 获取当前推荐算法设置
    val currentRecommendationMode =
        RecommendationMode.entries.find {
            it.key == settings.getString("recommendationMode", RecommendationMode.MIXED.key)
        } ?: RecommendationMode.MIXED

    val runtime = rememberHomeScreenRuntime(currentRecommendationMode)
    val feedBlockActions = rememberFeedBlockActions()
    val viewModel: BaseFeedViewModel = runtime.viewModel

    val keySurveyDone = "survey_feedback_done"
    val installed3Hours = !settings.getBoolean(keySurveyDone, false) && runtime.installedAtLeastThreeHours
    var dismissedUpdateVersion by remember { mutableStateOf<String?>(null) }

    // 首次启动提示
    var showFilterExplainDialog by remember {
        mutableStateOf(!settings.getBoolean("filterExplainDialogShown", false))
    }
    var showQQGroup by remember {
        mutableStateOf(
            !settings.getBoolean(
                QQ_GROUP_DISMISSED_PREFERENCE_KEY,
                false,
            ),
        )
    }

    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }
    LaunchedEffect(scrollToTopTrigger) {
        when (
            topLevelReselectAction(
                triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
                isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
            )
        ) {
            TopLevelReselectAction.Refresh -> viewModel.refresh(paginationEnvironment)
            TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
            null -> {}
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    // 通知 ViewModel
    var unreadCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            unreadCount = fetchZhihuUnreadNotificationCount(paginationEnvironment.httpClient()) {
                paginationEnvironment.configureSignedRequest(this)
            }
        } catch (_: Exception) {
            // 忽略错误
        }
    }

    // 初始加载
    LaunchedEffect(currentRecommendationMode, runtime.account.isLoggedIn) {
        if (!runtime.account.isLoggedIn &&
            settings.getBoolean("loginForRecommendation", true)
        ) {
            runtime.requestLogin()
        } else if (viewModel.displayItems.isEmpty()) {
            // 只在第一次加载时刷新，这样可以避免在返回时刷新
            viewModel.refresh(paginationEnvironment)
        }
    }

    // 显示错误信息
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            userMessages.showMessage(it, UserMessageDuration.Long)
        }
    }

    // 屏蔽用户确认对话框
    var showBlockUserDialog by remember { mutableStateOf(false) }
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) } // Pair of userId and userName

    // 按关键词屏蔽对话框
    var showBlockByKeywordsDialog by remember { mutableStateOf(false) }
    var feedToBlockByKeywords by remember { mutableStateOf<Pair<String, String?>?>(null) } // Pair of title and excerpt

    Scaffold(
        modifier = if (duo3HomeAccount) {
            Modifier.fillMaxSize()
        } else {
            // master旧版需要pad掉状态栏
            Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        },
        topBar = {
            if (duo3HomeAccount) {
                Box {
                    Surface(
                        modifier = Modifier
                            .height(
                                WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp + 32.dp,
                            ).fillMaxWidth(),
                    ) { }
                    Row(
                        modifier = Modifier
                            .testTag(HOME_TOP_ACTIONS_TAG)
                            .fillMaxWidth()
                            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                            .padding(16.dp, 8.dp, 16.dp, 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag(HOME_SEARCH_BUTTON_TAG),
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            onClick = {
                                navigator.onNavigate(
                                    Search(query = ""),
                                )
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "搜索",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "搜索",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )

                                IconButton(
                                    onClick = { showAccountBottomSheet = true },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .testTag(HOME_ACCOUNT_BUTTON_TAG),
                                ) {
                                    Box(Modifier.padding(12.dp)) {
                                        BadgedBox(
                                            badge = {
                                                if (unreadCount > 0) {
                                                    Badge { }
                                                }
                                            },
                                        ) {
                                            val avatarUrl = runtime.account.avatarUrl
                                            if (avatarUrl != null) {
                                                AsyncImage(
                                                    model = avatarUrl,
                                                    contentDescription = "账号",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                                                        .clip(CircleShape),
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.AccountCircle,
                                                    contentDescription = "账号",
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(40.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(shadowElevation = 4.dp) {
                    Row(
                        modifier = Modifier
                            .testTag(HOME_TOP_ACTIONS_TAG)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag(HOME_SEARCH_BUTTON_TAG),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                navigator.onNavigate(
                                    Search(query = ""),
                                )
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "搜索",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "搜索内容",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { navigator.onNavigate(Notification) },
                            modifier = Modifier.testTag(HOME_NOTIFICATION_BUTTON_TAG),
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge { Text("$unreadCount") }
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "通知",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { scaffoldPadding ->
        if (duo3HomeAccount && showAccountBottomSheet) {
            MyModalBottomSheet(
                onDismissRequest = { showAccountBottomSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                AccountSettingScreen(
                    innerPadding = PaddingValues(0.dp),
                    unreadCount = unreadCount,
                    onDismissRequest = { showAccountBottomSheet = false },
                )
            }
        }

        FeedPullToRefresh(viewModel, PaddingValues(top = scaffoldPadding.calculateTopPadding())) {
            PaginatedList(
                items = viewModel.displayItems,
                listState = listState,
                modifier = Modifier.testTag(HOME_FEED_LIST_TAG),
                contentPadding = PaddingValues(
                    top = scaffoldPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding(),
                ),
                onLoadMore = { viewModel.loadMore(paginationEnvironment) },
                footer = ProgressIndicatorFooter,
                key = { item -> item.stableKey },
                topContent = {
                    item {
                        val availableUpdate = runtime.updateAnnouncement

                        AnnouncementCard(
                            visible = availableUpdate != null && dismissedUpdateVersion != availableUpdate.version,
                            title = "发现新版本：${availableUpdate?.version}${if (availableUpdate?.isNightly == true) " (Nightly)" else ""}",
                            leadingIcon = { Icon(Icons.Default.ArrowCircleUp, contentDescription = null) },
                            accept = { Text("查看更新") },
                            onAccept = {
                                navigator.onNavigate(Account.SystemAndUpdateSettings)
                            },
                            dismiss = { Text("以后") },
                            onDismiss = {
                                availableUpdate?.version?.let { versionStr ->
                                    dismissedUpdateVersion = versionStr
                                }
                            },
                            colors = AnnouncementCardDefaults.colorsImportant(),
                        )
                        AnnouncementCard(
                            visible = showQQGroup,
                            title = "欢迎加入 QQ 群",
                            leadingIcon = { Icon(Icons.Default.MarkUnreadChatAlt, contentDescription = null) },
                            content = "欢迎加入 Zhihu++ QQ 群。1 群已满，我们新建了 2 群。已加入 1 群的朋友请不要重复加群。",
                            accept = { Text("加入") },
                            onAccept = {
                                openExternalUrl("https://qm.qq.com/q/trN5cJbWpk")
                            },
                            dismiss = { Text("关闭") },
                            onDismiss = {
                                settings.putBoolean(QQ_GROUP_DISMISSED_PREFERENCE_KEY, true)
                                showQQGroup = false
                            },
                            colors = AnnouncementCardDefaults.colorsVariant(),
                        )
                        AnnouncementCard(
                            visible = showFilterExplainDialog,
                            title = "为什么有的内容突然消失了？",
                            leadingIcon = { Icon(Icons.AutoMirrored.Default.HelpOutline, contentDescription = null) },
                            content = "知乎++会默认屏蔽知乎盐选、知乎广告平台、知乎学堂、微信公众号文章。" +
                                "除此之外，您也可以手动屏蔽的用户、话题、问题等内容。" +
                                "由于我们需要更详细的数据来精准屏蔽，而获取数据需要时间，所以他们会闪一下然后消失。",
                            dismiss = { Text("好") },
                            onDismiss = {
                                settings.putBoolean("filterExplainDialogShown", true)
                                showFilterExplainDialog = false
                            },
                        )
                    }
                },
            ) { item ->
                FeedCard(
                    item,
                    thumbnailUrl = when (val target = item.feed?.target) {
                        is Feed.AnswerTarget -> target.thumbnail
                        else -> null
                    },
                    onLike = {
                        if (runtime.recordLocalItemFeedback(it, 1.0)) {
                            userMessages.showShortMessage("已记录喜欢，本地推荐会逐步学习")
                        } else {
                            userMessages.showShortMessage("收到喜欢，功能正在优化")
                        }
                    },
                    onDislike = {
                        if (runtime.recordLocalItemFeedback(it, -1.0)) {
                            userMessages.showShortMessage("已降低这类本地推荐的优先级")
                        } else {
                            userMessages.showShortMessage("收到反馈，功能正在优化")
                        }
                    },
                    onBlockUser = { feedItem ->
                        feedBlockActions.handleBlockUser(viewModel, feedItem) { authorInfo ->
                            userToBlock = authorInfo
                            showBlockUserDialog = true
                        }
                    },
                    onBlockByKeywords = { feedItem ->
                        feedBlockActions.handleBlockByKeywords(viewModel, feedItem) { (_, contentInfo) ->
                            feedToBlockByKeywords = contentInfo.first to contentInfo.second
                            showBlockByKeywordsDialog = true
                        }
                    },
                    onBlockTopic = { topicId, topicName ->
                        feedBlockActions.handleBlockTopic(viewModel, topicId, topicName)
                    },
                ) {
                    val feed = this.feed
                    val destination = navDestination
                    if (feed != null) {
//                            DataHolder.putFeed(feed)
                        (viewModel as HomeFeedInteractionViewModel).onUiContentClick(paginationEnvironment, feed, item)
                    } else if (item.localContentId != null) {
                        runtime.recordLocalItemOpened(item)
                    }
                    if (destination != null) {
                        navigator.onNavigate(destination)
                    }
                }
            }

            if (showRefreshFab) {
                if (runtime.isDebuggable) {
                    DraggableRefreshButton(
                        onClick = {
                            val data = Json.encodeToString(viewModel.debugData)
                            paginationEnvironment.setPlainTextClipboard("data", data)
                            userMessages.showShortMessage("已复制调试数据")
                        },
                        preferenceName = "copyAll",
                    ) {
                        Icon(Icons.Default.CopyAll, contentDescription = "复制")
                    }
                }
                DraggableRefreshButton(
                    modifier = Modifier.testTag(HOME_REFRESH_BUTTON_TAG),
                    onClick = { viewModel.refresh(paginationEnvironment) },
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(30.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            }
        }
    }

    // 屏蔽用户确认对话框
    BlockUserConfirmDialog(
        showDialog = showBlockUserDialog,
        userToBlock = userToBlock,
        displayItems = viewModel.displayItems,
        onDismiss = {
            showBlockUserDialog = false
            userToBlock = null
        },
        onConfirm = {
            viewModel.refresh(paginationEnvironment)
            showBlockUserDialog = false
            userToBlock = null
        },
    )

    // 按关键词屏蔽对话框
    feedToBlockByKeywords?.let { (title, excerpt) ->
        BlockByKeywordsDialog(
            showDialog = showBlockByKeywordsDialog,
            feedTitle = title,
            feedExcerpt = excerpt,
            onDismiss = {
                showBlockByKeywordsDialog = false
                feedToBlockByKeywords = null
            },
            onConfirm = {
                viewModel.refresh(paginationEnvironment)
                showBlockByKeywordsDialog = false
                feedToBlockByKeywords = null
            },
        )
    }
}
