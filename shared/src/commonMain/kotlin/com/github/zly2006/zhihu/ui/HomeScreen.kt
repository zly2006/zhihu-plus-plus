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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.navigation.WritePin
import com.github.zly2006.zhihu.shared.aigc.AIGC_MARKING_ENABLED_PREFERENCE_KEY
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.shared.data.ZHIHU_ME_URL
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.ZhihuMeNotifications
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageDuration
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.ui.TopLevelReselectAction
import com.github.zly2006.zhihu.shared.ui.topLevelReselectAction
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.components.AnnouncementCard
import com.github.zly2006.zhihu.ui.components.AnnouncementCardDefaults
import com.github.zly2006.zhihu.ui.components.BlockByKeywordsDialog
import com.github.zly2006.zhihu.ui.components.BlockQuestionAuthorConfirmDialog
import com.github.zly2006.zhihu.ui.components.BlockUserConfirmDialog
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
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
import kotlinx.serialization.json.Json

const val PREFERENCE_NAME = "com.github.zly2006.zhihu_preferences"
const val ARTICLE_USE_WEBVIEW_PREFERENCE_KEY = "webviewRenderLegacy"
const val QQ_GROUP_DISMISSED_PREFERENCE_KEY = "dismissQQGroup3"
const val AIGC_MARKING_ANNOUNCEMENT_DISMISSED_PREFERENCE_KEY = "dismissAigcMarkingAnnouncement"
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

fun homeAuthorPollAnnouncementTag(pinId: Long): String = "$HOME_AUTHOR_POLL_ANNOUNCEMENT_TAG:$pinId"

private fun authorPollAnnouncementDismissedKey(announcement: HomePollAnnouncement): String =
    "dismissAuthorPollAnnouncement_${announcement.pinId}_${announcement.pollId}"

/**
 * 首页信息流页面。
 *
 * 页面顶部承载搜索、通知、账号入口等高频操作，主体是可分页的推荐信息流，底部可按设置显示可拖动刷新 FAB。
 * 设计上首页同时响应推荐算法、Duo3 账号入口迁移、更新公告、问卷提示和未读通知等状态，因此 UI 改动时要同时检查
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
    val notificationSettings = rememberNotificationSettingsStore()
    val userMessages = rememberUserMessageSink()
    val openExternalUrl = rememberExternalUrlOpener()

    val duo3HomeAccount = settings.getBoolean("duo3_home_account", false)
    val showRefreshFab = settings.getBoolean("showRefreshFab", true)
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

    val account = rememberHomeAccountState()
    val updateAnnouncement = rememberHomeUpdateAnnouncement()
    val installedAtLeastThreeHours = rememberHomeInstalledAtLeastThreeHours()
    val isDebuggable = rememberHomeIsDebuggable()
    val requestLogin = rememberHomeLoginRequester()
    val feedBlockActions = rememberFeedBlockActions()
    val viewModel: BaseFeedViewModel = when (currentRecommendationMode) {
        RecommendationMode.WEB -> viewModel { HomeFeedViewModel() }
        RecommendationMode.ANDROID -> viewModel { AndroidHomeFeedViewModel() }
        RecommendationMode.LOCAL -> viewModel { LocalHomeFeedViewModel() }
        RecommendationMode.MIXED -> viewModel { MixedHomeFeedViewModel() }
    }
    val localHomeViewModel = viewModel as? LocalHomeFeedViewModel

    val keySurveyDone = "survey_feedback_done"
    val installed3Hours = !settings.getBoolean(keySurveyDone, false) && installedAtLeastThreeHours
    var dismissedUpdateVersion by remember { mutableStateOf<String?>(null) }
    var aigcMarkingEnabled by remember {
        mutableStateOf(settings.getBoolean(AIGC_MARKING_ENABLED_PREFERENCE_KEY, false))
    }
    var showAigcMarkingAnnouncement by remember {
        mutableStateOf(!settings.getBoolean(AIGC_MARKING_ANNOUNCEMENT_DISMISSED_PREFERENCE_KEY, false))
    }
    var authorPollAnnouncements by remember {
        mutableStateOf(emptyList<HomePollAnnouncement>())
    }

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
            unreadCount = paginationEnvironment
                .fetchJson(ZHIHU_ME_URL, "")
                ?.let { ZhihuJson.decodeJson<ZhihuMeNotifications>(it) }
                ?.totalCount ?: 0
        } catch (_: Exception) {
            // 忽略错误
        }
    }

    // 初始加载
    LaunchedEffect(currentRecommendationMode, account.isLoggedIn) {
        if (!account.isLoggedIn &&
            settings.getBoolean("loginForRecommendation", true)
        ) {
            requestLogin()
        } else if (viewModel.displayItems.isEmpty()) {
            // 只在第一次加载时刷新，这样可以避免在返回时刷新
            viewModel.refresh(paginationEnvironment)
        }
    }

    LaunchedEffect(Unit) {
        authorPollAnnouncements = try {
            paginationEnvironment
                .fetchJson(ZHIHU_PLUS_AUTHOR_PINS_URL, "")
                ?.let(::decodeHomePollAnnouncements)
                ?.filterNot { it.isVoted }
                ?.take(3)
                ?: emptyList()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("HomeScreen", "Failed to load author poll announcements", e)
            emptyList()
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
    var userToBlock by remember { mutableStateOf<Pair<String, String>?>(null) } // 二元组内容为 userId 和 userName。
    var showBlockQuestionAuthorDialog by remember { mutableStateOf(false) }
    var questionAuthorToBlock by remember { mutableStateOf<Pair<String, String>?>(null) }

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
                            AnnouncementCard(
                                visible = showQQGroup,
                                title = "欢迎加入 QQ 群",
                                leadingIcon = { Icon(Icons.Default.MarkUnreadChatAlt, contentDescription = null) },
                                content = "欢迎加入 Zhihu++ QQ 群。1 & 2 群已满，我们新建了 3 群。已入群的朋友请不要重复加群。",
                                accept = { Text("加入") },
                                onAccept = {
                                    openExternalUrl("https://qm.qq.com/q/AaCml6Un4G")
                                },
                                dismiss = { Text("关闭") },
                                onDismiss = {
                                    settings.putBoolean(QQ_GROUP_DISMISSED_PREFERENCE_KEY, true)
                                    showQQGroup = false
                                },
                                colors = AnnouncementCardDefaults.colorsVariant(),
                            )
                            AnnouncementCard(
                                visible = !aigcMarkingEnabled && showAigcMarkingAnnouncement,
                                title = "AIGC 标记",
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                content = "为了减轻知乎上 AI 生成的文章对用户的困扰，你可以加入我们一起标记 AIGC。开启后会把你正在浏览的内容发送到我们的服务器，用来显示其他用户是否认为其疑似 AIGC。此功能默认关闭，不会发送隐私信息。",
                                accept = { Text("开启") },
                                onAccept = {
                                    settings.putBoolean(AIGC_MARKING_ENABLED_PREFERENCE_KEY, true)
                                    settings.putBoolean(AIGC_MARKING_ANNOUNCEMENT_DISMISSED_PREFERENCE_KEY, true)
                                    aigcMarkingEnabled = true
                                    showAigcMarkingAnnouncement = false
                                },
                                dismiss = { Text("关闭") },
                                onDismiss = {
                                    settings.putBoolean(AIGC_MARKING_ANNOUNCEMENT_DISMISSED_PREFERENCE_KEY, true)
                                    showAigcMarkingAnnouncement = false
                                },
                            )
                            authorPollAnnouncements.forEach { announcement ->
                                val dismissedKey = authorPollAnnouncementDismissedKey(announcement)
                                val visible = !settings.getBoolean(dismissedKey, false)
                                AnnouncementCard(
                                    modifier = Modifier.testTag(homeAuthorPollAnnouncementTag(announcement.pinId)),
                                    visible = visible,
                                    title = "请给未来的知乎++提出建议",
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
                                            if (announcement.isVoted) {
                                                add("已投票")
                                            }
                                        }
                                        if (details.isNotEmpty()) {
                                            append("\n")
                                            append(details.joinToString(" · "))
                                        }
                                    },
                                    accept = { Text("去投票") },
                                    onAccept = {
                                        navigator.onNavigate(Pin(announcement.pinId))
                                    },
                                    dismiss = { Text("关闭") },
                                    onDismiss = {
                                        settings.putBoolean(dismissedKey, true)
                                        authorPollAnnouncements = authorPollAnnouncements.filterNot {
                                            it.pinId == announcement.pinId
                                        }
                                    },
                                )
                            }
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
                        onBlockUser = { feedItem ->
                            feedBlockActions.handleBlockUser(viewModel, feedItem) { authorInfo ->
                                userToBlock = authorInfo
                                showBlockUserDialog = true
                            }
                        },
                        onBlockQuestionAuthor = { feedItem ->
                            feedBlockActions.handleBlockQuestionAuthor(viewModel, feedItem) { authorInfo ->
                                questionAuthorToBlock = authorInfo
                                showBlockQuestionAuthorDialog = true
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
                            (viewModel as? HomeFeedInteractionViewModel)?.onUiContentClick(paginationEnvironment, feed, item)
                        } else if (item.localContentId != null) {
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

    BlockQuestionAuthorConfirmDialog(
        showDialog = showBlockQuestionAuthorDialog,
        userToBlock = questionAuthorToBlock,
        displayItems = viewModel.displayItems,
        onDismiss = {
            showBlockQuestionAuthorDialog = false
            questionAuthorToBlock = null
        },
        onConfirm = {
            viewModel.refresh(paginationEnvironment)
            showBlockQuestionAuthorDialog = false
            questionAuthorToBlock = null
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
