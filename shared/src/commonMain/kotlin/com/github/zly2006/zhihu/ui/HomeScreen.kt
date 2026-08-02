/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MarkUnreadChatAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.navigation.WritePin
import com.github.zly2006.zhihu.shared.announcement.HOME_NOTIFICATION_ACTION_OPEN_ANSWER
import com.github.zly2006.zhihu.shared.announcement.HOME_NOTIFICATION_ACTION_OPEN_ARTICLE
import com.github.zly2006.zhihu.shared.announcement.HOME_NOTIFICATION_ACTION_OPEN_PIN
import com.github.zly2006.zhihu.shared.announcement.HOME_NOTIFICATION_ACTION_OPEN_UPDATE_SETTINGS
import com.github.zly2006.zhihu.shared.announcement.HOME_NOTIFICATION_ACTION_OPEN_URL
import com.github.zly2006.zhihu.shared.announcement.HOME_NOTIFICATION_ACTION_SET_SETTING
import com.github.zly2006.zhihu.shared.announcement.HOME_NOTIFICATION_CACHE_FILE_NAME
import com.github.zly2006.zhihu.shared.announcement.HOME_NOTIFICATION_CHECK_INTERVAL_MILLIS
import com.github.zly2006.zhihu.shared.announcement.OnlineHomeNotificationRepository
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.shared.data.ZHIHU_ME_URL
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.ZhihuMeNotifications
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageDuration
import com.github.zly2006.zhihu.shared.platform.rememberAppPrivateDirectory
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberIsLiteVariant
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.ui.TopLevelReselectAction
import com.github.zly2006.zhihu.shared.ui.topLevelReselectAction
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.components.AnnouncementCard
import com.github.zly2006.zhihu.ui.components.AnnouncementCardDefaults
import com.github.zly2006.zhihu.ui.components.BlockByKeywordsDialog
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedAuthorBlockConfirmDialog
import com.github.zly2006.zhihu.ui.components.FeedAuthorBlockRequest
import com.github.zly2006.zhihu.ui.components.FeedAuthorBlockType
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.ui.components.rememberFeedBlockActions
import com.github.zly2006.zhihu.ui.subscreens.DEFAULT_FAB_OPACITY
import com.github.zly2006.zhihu.ui.subscreens.PREF_FAB_OPACITY
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedInteractionViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.local.LocalHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.za.AndroidHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val PREFERENCE_NAME = "com.github.zly2006.zhihu_preferences"
const val ARTICLE_USE_WEBVIEW_PREFERENCE_KEY = "webviewRenderLegacy"
const val HOME_TOP_ACTIONS_TAG = "home_top_actions"
const val HOME_SEARCH_BUTTON_TAG = "home_search_button"
const val HOME_CREATE_FAB_TAG = "home_create_fab"
const val HOME_CREATE_MENU_TAG = "home_create_menu"
const val HOME_WRITE_QUESTION_BUTTON_TAG = "home_write_question_button"
const val HOME_WRITE_ANSWER_BUTTON_TAG = "home_write_answer_button"
const val HOME_WRITE_PIN_BUTTON_TAG = "home_write_pin_button"
const val HOME_NOTIFICATION_BUTTON_TAG = "home_notification_button"
const val HOME_ACCOUNT_BUTTON_TAG = "home_account_button"
const val HOME_FEED_LIST_TAG = "home_feed_list"
const val HOME_REFRESH_BUTTON_TAG = "home_refresh_button"
const val HOME_AUTHOR_POLL_ANNOUNCEMENT_TAG = "home_author_poll_announcement"
const val HOME_ONLINE_NOTIFICATION_TAG = "home_online_notification"
private const val MAX_HOME_PIN_ANNOUNCEMENTS = 3

fun homeAuthorPollAnnouncementTag(pinId: Long): String = "$HOME_AUTHOR_POLL_ANNOUNCEMENT_TAG:$pinId"

fun homeOnlineNotificationTag(uuid: String): String = "$HOME_ONLINE_NOTIFICATION_TAG:$uuid"

fun homePinAnnouncementReadKey(pinId: Long): String = "readHomePinAnnouncement_$pinId"

/**
 * 首页信息流页面。
 *
 * 页面顶部承载搜索、通知、账号入口等高频操作，主体是可分页的推荐信息流，底部可按设置显示可拖动刷新 FAB。
 * 设计上首页同时响应推荐算法、Duo3 账号入口迁移、更新公告、在线通知、作者动态和未读通知等状态，因此 UI 改动时要同时检查
 * `recommendationMode`、`duo3_home_account`、`showRefreshFab` 和账号面板相关路径。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    scrollToTopTrigger: Int,
    innerPadding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = true)
    val settings = rememberSettingsStore()
    val appPrivateDirectory = rememberAppPrivateDirectory()
    val notificationSettings = rememberNotificationSettingsStore()
    val userMessages = rememberUserMessageSink()
    val openExternalUrl = rememberExternalUrlOpener()
    val lifecycleOwner = LocalLifecycleOwner.current

    val duo3HomeAccount = settings.getBoolean("duo3_home_account", false)
    val showRefreshFab = settings.getBoolean("showRefreshFab", true)
    val autoRefreshOnStartup = settings.getBoolean(AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY, true)
    val showUnreadBadge = notificationSettings.getUnreadBadgeEnabled()
    var showAccountBottomSheet by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    val createMenuBlurRadius by animateDpAsState(
        targetValue = if (showCreateMenu) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "createMenuBlurRadius",
    )

    // 获取当前推荐算法设置
    val currentRecommendationMode =
        RecommendationMode.entries.find {
            it.key == settings.getString("recommendationMode", RecommendationMode.MIXED.key)
        } ?: RecommendationMode.MIXED
    val startupCache = rememberHomeFeedStartupCache(currentRecommendationMode)

    val account = rememberHomeAccountState()
    val updateAnnouncement = rememberHomeUpdateAnnouncement()
    val versionName = rememberAppVersionInfo().substringBefore(' ').takeIf { it.firstOrNull()?.isDigit() == true }
    val onlineNotificationRepository = remember(settings, appPrivateDirectory) {
        OnlineHomeNotificationRepository(
            settings,
            Path(appPrivateDirectory, HOME_NOTIFICATION_CACHE_FILE_NAME),
        )
    }
    var onlineNotifications by remember(onlineNotificationRepository) {
        mutableStateOf(onlineNotificationRepository.cachedNotifications())
    }
    val isDebuggable = rememberHomeIsDebuggable()
    val requestLogin = rememberHomeLoginRequester()
    val feedBlockActions = rememberFeedBlockActions()
    val isLiteVariant = rememberIsLiteVariant()
    val viewModel: BaseFeedViewModel = when (currentRecommendationMode) {
        RecommendationMode.WEB -> viewModel { HomeFeedViewModel() }
        RecommendationMode.ANDROID -> viewModel { AndroidHomeFeedViewModel() }
        RecommendationMode.LOCAL -> viewModel { LocalHomeFeedViewModel() }
        RecommendationMode.MIXED -> viewModel { MixedHomeFeedViewModel() }
    }
    val localHomeViewModel = viewModel as? LocalHomeFeedViewModel

    var dismissedUpdateVersion by remember { mutableStateOf<String?>(null) }
    var authorPinAnnouncements by remember { mutableStateOf(emptyList<HomePinAnnouncement>()) }

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
            unreadCount = paginationEnvironment
                .fetchJson(ZHIHU_ME_URL, "")
                ?.let { ZhihuJson.decodeJson<ZhihuMeNotifications>(it) }
                ?.totalCount ?: 0
        } catch (_: Exception) {
            // 忽略错误
        }
    }

    val latestLoadedDisplayItems = viewModel.latestLoadedDisplayItems.value
    LaunchedEffect(latestLoadedDisplayItems) {
        if (latestLoadedDisplayItems.isNotEmpty()) {
            startupCache.writeHomeFeedStartupCache(latestLoadedDisplayItems)
        }
    }

    // 初始加载
    LaunchedEffect(currentRecommendationMode, account.isLoggedIn, autoRefreshOnStartup) {
        if (!account.isLoggedIn &&
            settings.getBoolean("loginForRecommendation", true)
        ) {
            requestLogin()
        } else if (viewModel.displayItems.isEmpty()) {
            val cachedItems = if (autoRefreshOnStartup) {
                emptyList()
            } else {
                startupCache.readHomeFeedStartupCache()
            }
            if (viewModel.displayItems.isEmpty() && cachedItems.isNotEmpty()) {
                viewModel.addDisplayItems(cachedItems)
            } else if (viewModel.displayItems.isEmpty()) {
                // 只在第一次加载时刷新，这样可以避免在返回时刷新
                viewModel.refresh(paginationEnvironment)
            }
        }
    }

    LaunchedEffect(lifecycleOwner, scrollToTopTrigger) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val loadedAnnouncements = try {
                paginationEnvironment
                    .fetchJson(ZHIHU_PLUS_AUTHOR_PINS_URL, "")
                    ?.let(::decodeHomePinAnnouncements)
                    ?.filterNot { settings.getBoolean(homePinAnnouncementReadKey(it.pinId), false) }
                    ?.take(MAX_HOME_PIN_ANNOUNCEMENTS)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HomeScreen", "Failed to load home pin announcements", e)
                null
            }
            if (loadedAnnouncements != null) {
                authorPinAnnouncements = loadedAnnouncements
            }
        }
    }

    LaunchedEffect(lifecycleOwner, paginationEnvironment, versionName) {
        if (versionName != null) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    onlineNotifications = onlineNotificationRepository.load(
                        versionName = versionName,
                        httpClient = paginationEnvironment.httpClient(),
                    )
                    delay(HOME_NOTIFICATION_CHECK_INTERVAL_MILLIS)
                }
            }
        }
    }

    // 显示错误信息
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            userMessages.showMessage(it, UserMessageDuration.Long)
        }
    }

    var feedAuthorBlockRequest by remember { mutableStateOf<FeedAuthorBlockRequest?>(null) }

    // 按关键词屏蔽对话框
    var showBlockByKeywordsDialog by remember { mutableStateOf(false) }
    var feedToBlockByKeywords by remember { mutableStateOf<Pair<String, String?>?>(null) } // 二元组内容为标题和摘要。

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = if (duo3HomeAccount) {
                Modifier
                    .fillMaxSize()
                    .blur(createMenuBlurRadius)
            } else {
                // master旧版需要pad掉状态栏
                Modifier
                    .fillMaxSize()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .blur(createMenuBlurRadius)
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
                                                    if (showUnreadBadge && unreadCount > 0) {
                                                        Badge { }
                                                    }
                                                },
                                            ) {
                                                val avatarUrl = account.avatarUrl
                                                if (avatarUrl != null) {
                                                    AsyncImage(
                                                        model = avatarUrl,
                                                        contentDescription = "账号",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .border(
                                                                0.5.dp,
                                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                                CircleShape,
                                                            ).clip(CircleShape),
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
                                        if (showUnreadBadge && unreadCount > 0) {
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
                        showUnreadBadge = showUnreadBadge,
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
                            val availableUpdate = updateAnnouncement

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
                            onlineNotifications.forEach { notification ->
                                val markRead = {
                                    onlineNotificationRepository.markRead(notification)
                                    onlineNotifications = onlineNotifications.filterNot { it.uuid == notification.uuid }
                                }
                                AnnouncementCard(
                                    modifier = Modifier.testTag(homeOnlineNotificationTag(notification.uuid)),
                                    visible = true,
                                    title = notification.title,
                                    leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                                    content = notification.content,
                                    accept = notification.accept?.let { accept ->
                                        { Text(accept.text) }
                                    },
                                    onAccept = {
                                        val accept = notification.accept
                                        markRead()
                                        when (accept?.key) {
                                            HOME_NOTIFICATION_ACTION_OPEN_URL -> {
                                                accept.value
                                                    ?.jsonPrimitive
                                                    ?.contentOrNull
                                                    ?.let(openExternalUrl)
                                            }
                                            HOME_NOTIFICATION_ACTION_OPEN_UPDATE_SETTINGS -> {
                                                navigator.onNavigate(Account.SystemAndUpdateSettings)
                                            }
                                            HOME_NOTIFICATION_ACTION_OPEN_PIN -> {
                                                accept.value?.jsonPrimitive?.contentOrNull?.toLongOrNull()?.let {
                                                    navigator.onNavigate(Pin(it))
                                                }
                                            }
                                            HOME_NOTIFICATION_ACTION_OPEN_ANSWER -> {
                                                accept.value?.jsonPrimitive?.contentOrNull?.toLongOrNull()?.let {
                                                    navigator.onNavigate(Article(type = ArticleType.Answer, id = it))
                                                }
                                            }
                                            HOME_NOTIFICATION_ACTION_OPEN_ARTICLE -> {
                                                accept.value?.jsonPrimitive?.contentOrNull?.toLongOrNull()?.let {
                                                    navigator.onNavigate(Article(type = ArticleType.Article, id = it))
                                                }
                                            }
                                            HOME_NOTIFICATION_ACTION_SET_SETTING -> {
                                                val setting = accept.value?.jsonObject
                                                val name = setting?.get("setting_name")?.jsonPrimitive?.contentOrNull
                                                when (setting?.get("value_type")?.jsonPrimitive?.contentOrNull) {
                                                    "boolean" -> setting["value"]?.jsonPrimitive?.booleanOrNull?.let {
                                                        settings.putBoolean(name!!, it)
                                                    }
                                                    "string" -> setting["value"]?.jsonPrimitive?.contentOrNull?.let {
                                                        settings.putString(name!!, it)
                                                    }
                                                    "int" -> setting["value"]?.jsonPrimitive?.intOrNull?.let {
                                                        settings.putInt(name!!, it)
                                                    }
                                                }
                                            }
                                            else -> userMessages.showShortMessage("当前版本不支持此通知操作")
                                        }
                                    },
                                    dismiss = { Text(notification.dismiss) },
                                    onDismiss = markRead,
                                )
                            }
                            authorPinAnnouncements.forEach { announcement ->
                                AnnouncementCard(
                                    modifier = Modifier.testTag(homeAuthorPollAnnouncementTag(announcement.pinId)),
                                    visible = true,
                                    title = if (announcement.kind == HomePinAnnouncementKind.Poll) {
                                        "请给未来的知乎++提出建议"
                                    } else {
                                        "知乎++新动态"
                                    },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                    content = buildString {
                                        append(announcement.title)
                                        val details = buildList {
                                            if (announcement.optionCount > 0) {
                                                add("${announcement.optionCount} 个选项")
                                            }
                                            if (announcement.memberCount > 0) {
                                                add("${announcement.memberCount} 人已参与")
                                            }
                                        }
                                        if (details.isNotEmpty()) {
                                            append("\n")
                                            append(details.joinToString(" · "))
                                        }
                                    },
                                    accept = {
                                        Text(if (announcement.kind == HomePinAnnouncementKind.Poll) "去投票" else "查看")
                                    },
                                    onAccept = {
                                        settings.putBoolean(homePinAnnouncementReadKey(announcement.pinId), true)
                                        authorPinAnnouncements = authorPinAnnouncements.filterNot {
                                            it.pinId == announcement.pinId
                                        }
                                        navigator.onNavigate(Pin(announcement.pinId))
                                    },
                                    dismiss = { Text("关闭") },
                                    onDismiss = {
                                        settings.putBoolean(homePinAnnouncementReadKey(announcement.pinId), true)
                                        authorPinAnnouncements = authorPinAnnouncements.filterNot {
                                            it.pinId == announcement.pinId
                                        }
                                    },
                                )
                            }
                        }
                    },
                ) { item ->
                    FeedCard(
                        item,
                        thumbnailUrl = when (val target = item.feed?.target) {
                            is Feed.AnswerTarget -> target.thumbnail
                            else -> null
                        },
                        menuItems = { dismissMenu ->
                            if (!isLiteVariant) {
                                DropdownMenuItem(
                                    text = { Text("按关键词屏蔽") },
                                    onClick = {
                                        dismissMenu()
                                        feedBlockActions.handleBlockByKeywords(viewModel, item) { (_, contentInfo) ->
                                            feedToBlockByKeywords = contentInfo.first to contentInfo.second
                                            showBlockByKeywordsDialog = true
                                        }
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("屏蔽用户") },
                                onClick = {
                                    dismissMenu()
                                    feedBlockActions.handleBlockUser(viewModel, item) { authorInfo ->
                                        feedAuthorBlockRequest = FeedAuthorBlockRequest(
                                            type = FeedAuthorBlockType.CONTENT_AUTHOR,
                                            userId = authorInfo.first,
                                            userName = authorInfo.second,
                                        )
                                    }
                                },
                            )
                            val canBlockQuestionAuthor = when (item.feed?.target) {
                                is Feed.AnswerTarget, is Feed.QuestionTarget -> true
                                else -> item.raw is DataHolder.Answer || item.raw is DataHolder.Question
                            }
                            if (canBlockQuestionAuthor) {
                                DropdownMenuItem(
                                    text = { Text("屏蔽提问者") },
                                    onClick = {
                                        dismissMenu()
                                        feedBlockActions.handleBlockQuestionAuthor(viewModel, item) { authorInfo ->
                                            feedAuthorBlockRequest = FeedAuthorBlockRequest(
                                                type = FeedAuthorBlockType.QUESTION_AUTHOR,
                                                userId = authorInfo.first,
                                                userName = authorInfo.second,
                                            )
                                        }
                                    },
                                )
                            }
                            val topics = when (val raw = item.raw) {
                                is DataHolder.Answer -> raw.question.topics
                                is DataHolder.Question -> raw.topics
                                is DataHolder.Article -> raw.topics ?: emptyList()
                                is DataHolder.Pin -> raw.topics ?: emptyList()
                                else -> emptyList()
                            }
                            topics.forEach { topic ->
                                DropdownMenuItem(
                                    text = { Text("屏蔽「${topic.name}」") },
                                    onClick = {
                                        dismissMenu()
                                        feedBlockActions.handleBlockTopic(viewModel, topic.id, topic.name)
                                    },
                                )
                            }
                        },
                    ) {
                        val feed = this.feed
                        val destination = navDestination
                        if (feed != null) {
//                            DataHolder.putFeed(feed)
                            (viewModel as? HomeFeedInteractionViewModel)?.onUiContentClick(paginationEnvironment, feed, item)
                        } else {
                            localHomeViewModel?.onLocalItemOpened(item)
                        }
                        if (destination != null) {
                            navigator.onNavigate(destination)
                        }
                    }
                }

                if (showRefreshFab) {
                    if (isDebuggable) {
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

        AnimatedVisibility(
            visible = showCreateMenu,
            enter = fadeIn(animationSpec = tween(durationMillis = 120)),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.16f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        showCreateMenu = false
                    },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
            horizontalAlignment = Alignment.End,
        ) {
            AnimatedVisibility(
                visible = showCreateMenu,
                enter = fadeIn(animationSpec = tween(durationMillis = 120)) +
                    scaleIn(
                        initialScale = 0.92f,
                        transformOrigin = TransformOrigin(1f, 1f),
                        animationSpec = tween(durationMillis = 180),
                    ) +
                    slideInVertically(animationSpec = tween(durationMillis = 180)) { it / 8 },
                exit = fadeOut(animationSpec = tween(durationMillis = 90)) +
                    scaleOut(
                        targetScale = 0.96f,
                        transformOrigin = TransformOrigin(1f, 1f),
                        animationSpec = tween(durationMillis = 120),
                    ) +
                    slideOutVertically(animationSpec = tween(durationMillis = 120)) { it / 8 },
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        modifier = Modifier
                            .width(180.dp)
                            .testTag(HOME_CREATE_MENU_TAG),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                    ) {
                        Column {
                            DropdownMenuItem(
                                modifier = Modifier.testTag(HOME_WRITE_QUESTION_BUTTON_TAG),
                                text = { Text("提问题") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Default.HelpOutline, contentDescription = null)
                                },
                                onClick = {
                                    showCreateMenu = false
                                    userMessages.showShortMessage("正在施工")
                                },
                            )
                            DropdownMenuItem(
                                modifier = Modifier.testTag(HOME_WRITE_ANSWER_BUTTON_TAG),
                                text = { Text("写回答") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                                onClick = {
                                    showCreateMenu = false
                                    userMessages.showShortMessage("正在施工")
                                },
                            )
                            DropdownMenuItem(
                                modifier = Modifier.testTag(HOME_WRITE_PIN_BUTTON_TAG),
                                text = { Text("发想法") },
                                leadingIcon = {
                                    Icon(Icons.Default.MarkUnreadChatAlt, contentDescription = null)
                                },
                                onClick = {
                                    showCreateMenu = false
                                    navigator.onNavigate(WritePin)
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            val createFabOpacity = remember(settings) {
                settings.getInt(PREF_FAB_OPACITY, DEFAULT_FAB_OPACITY).coerceIn(10, 100) / 100f
            }
            FloatingActionButton(
                modifier = Modifier.testTag(HOME_CREATE_FAB_TAG),
                onClick = { showCreateMenu = !showCreateMenu },
                shape = CircleShape,
                containerColor = FloatingActionButtonDefaults.containerColor.copy(alpha = createFabOpacity),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = createFabOpacity),
                elevation = if (createFabOpacity < 1f) {
                    FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                } else {
                    FloatingActionButtonDefaults.elevation()
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "创作")
            }
        }
    }

    FeedAuthorBlockConfirmDialog(
        request = feedAuthorBlockRequest,
        displayItems = viewModel.displayItems,
        onDismiss = { feedAuthorBlockRequest = null },
        onConfirm = {
            viewModel.refresh(paginationEnvironment)
            feedAuthorBlockRequest = null
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
