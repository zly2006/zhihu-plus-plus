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

import android.content.ClipData
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.viewModels
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CopyAll
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.RecommendationMode
import com.github.zly2006.zhihu.data.ZhihuMeNotifications
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Search
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
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.local.LocalHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.AndroidHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

const val PREFERENCE_NAME = "com.github.zly2006.zhihu_preferences"
const val ARTICLE_USE_WEBVIEW_PREFERENCE_KEY = "webviewRender"
const val ARTICLE_WEBVIEW_CHANGE_ANNOUNCEMENT_DISMISSED_PREFERENCE_KEY =
    "articleWebviewChangeAnnouncementDismissed"

interface IHomeFeedViewModel {
    suspend fun recordContentInteraction(context: Context, feed: Feed)

    fun onUiContentClick(context: Context, feed: Feed, item: BaseFeedViewModel.FeedDisplayItem)

    /**
     * 发送"已读"状态到知乎服务器的通用实现
     */
    suspend fun sendReadStatusToServer(context: Context, feed: Feed) {
        try {
            AccountData.fetchPost(context, "https://www.zhihu.com/lastread/touch") {
                header("x-requested-with", "fetch")
                signFetchRequest()
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "items",
                                buildJsonArray {
                                    when (val target = feed.target) {
                                        is Feed.AnswerTarget -> {
                                            add(
                                                buildJsonArray {
                                                    add("answer")
                                                    add(target.id.toString())
                                                    add("read")
                                                },
                                            )
                                        }

                                        is Feed.ArticleTarget -> {
                                            add(
                                                buildJsonArray {
                                                    add("article")
                                                    add(target.id.toString())
                                                    add("read")
                                                },
                                            )
                                        }

                                        is Feed.PinTarget -> {
                                            add(
                                                buildJsonArray {
                                                    add("pin")
                                                    add(target.id.toString())
                                                    add("read")
                                                },
                                            )
                                        }

                                        else -> {}
                                    }
                                }.toString(),
                            )
                        },
                    ),
                )
            }
        } catch (e: Exception) {
            Log.e("IHomeFeedViewModel", "Failed to send read status", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(scrollToTopTrigger: Int = 0, innerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    val context = LocalActivity.current as MainActivity
    val preferences = remember {
        context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE)
    }

    val duo3HomeAccount = preferences.getBoolean("duo3_home_account", false)
    val showRefreshFab = preferences.getBoolean("showRefreshFab", true)
    var showAccountBottomSheet by remember { mutableStateOf(false) }

    // 获取当前推荐算法设置
    val currentRecommendationMode =
        RecommendationMode.entries.find {
            it.key == preferences.getString("recommendationMode", RecommendationMode.MIXED.key)
        } ?: RecommendationMode.MIXED

    // 根据设置选择对应的ViewModel
    val viewModel: BaseFeedViewModel by when (currentRecommendationMode) {
        RecommendationMode.WEB -> context.viewModels<HomeFeedViewModel>()
        RecommendationMode.ANDROID -> context.viewModels<AndroidHomeFeedViewModel>()
        RecommendationMode.LOCAL -> context.viewModels<LocalHomeFeedViewModel>()
        RecommendationMode.MIXED -> context.viewModels<MixedHomeFeedViewModel>() // 暂时使用在线推荐，因为相似度推荐还未实现
    }
    val localHomeViewModel = viewModel as? LocalHomeFeedViewModel

    val keySurveyDone = "survey_feedback_done"
    var installed3Hours by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!preferences.getBoolean(keySurveyDone, false)) {
            val installTime = try {
                context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
            if (System.currentTimeMillis() - installTime >= 3 * 60 * 60 * 1000L) {
                installed3Hours = true
            }
        }
    }

    val updateState by UpdateManager.updateState.collectAsState()
    var dismissedUpdateVersion by remember { mutableStateOf<String?>(null) }

    // 首次启动提示
    var showFilterExplainDialog by remember {
        mutableStateOf(!preferences.getBoolean("filterExplainDialogShown", false))
    }
    var showArticleRenderChangeAnnouncement by remember {
        mutableStateOf(
            !preferences.getBoolean(
                ARTICLE_WEBVIEW_CHANGE_ANNOUNCEMENT_DISMISSED_PREFERENCE_KEY,
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
            TopLevelReselectAction.Refresh -> viewModel.refresh(context)
            TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
            null -> {}
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    // 通知 ViewModel
    var unreadCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            val jojo = AccountData.fetchGet(context, "https://www.zhihu.com/api/v4/me") {
                signFetchRequest()
            }!!
            unreadCount = AccountData.decodeJson<ZhihuMeNotifications>(jojo).totalCount
        } catch (_: Exception) {
            // 忽略错误
        }
    }

    // 初始加载
    LaunchedEffect(currentRecommendationMode, AccountData.data.login) {
        if (!AccountData.data.login &&
            preferences.getBoolean("loginForRecommendation", true)
        ) {
            val myIntent = Intent(context, LoginActivity::class.java)
            context.startActivity(myIntent)
        } else if (viewModel.displayItems.isEmpty()) {
            // 只在第一次加载时刷新，这样可以避免在返回时刷新
            viewModel.refresh(context)
        }
    }

    // 显示错误信息
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
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
                            .fillMaxWidth()
                            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                            .padding(16.dp, 8.dp, 16.dp, 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
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
                                    modifier = Modifier.size(64.dp),
                                ) {
                                    Box(Modifier.padding(12.dp)) {
                                        BadgedBox(
                                            badge = {
                                                if (unreadCount > 0) {
                                                    Badge { }
                                                }
                                            },
                                        ) {
                                            val avatarUrl = AccountData.data.self?.avatarUrl
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
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
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
                        IconButton(onClick = { navigator.onNavigate(Notification) }) {
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
                contentPadding = PaddingValues(
                    top = scaffoldPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding(),
                ),
                onLoadMore = { viewModel.loadMore(context) },
                footer = ProgressIndicatorFooter,
                key = { item -> item.stableKey },
                topContent = {
                    item {
                        val availableUpdate = updateState as? UpdateManager.UpdateState.UpdateAvailable

                        AnnouncementCard(
                            visible = availableUpdate != null && dismissedUpdateVersion != availableUpdate.version.toString(),
                            title = "发现新版本：${availableUpdate?.version}${if (availableUpdate?.isNightly == true) " (Nightly)" else ""}",
                            leadingIcon = { Icon(Icons.Default.ArrowCircleUp, contentDescription = null) },
                            accept = { Text("查看更新") },
                            onAccept = {
                                context.navigate(Account.SystemAndUpdateSettings)
                            },
                            dismiss = { Text("以后") },
                            onDismiss = {
                                availableUpdate?.version?.toString()?.let { versionStr ->
                                    dismissedUpdateVersion = versionStr
                                }
                            },
                            colors = AnnouncementCardDefaults.colorsImportant(),
                        )
                        AnnouncementCard(
                            visible = showArticleRenderChangeAnnouncement,
                            title = "文章渲染默认已改为 Compose",
                            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                            content = "我们已默认关闭 WebView 渲染文章，以提供代码高亮等高级渲染能力，并修复了许多由 WebView 带来的 bug。" +
                                "新的渲染方式可能存在 bug，如遇到请在 GitHub 反馈，也可在设置中重新开启 WebView。",
                            accept = { Text("去设置") },
                            onAccept = {
                                preferences.edit {
                                    putBoolean(
                                        ARTICLE_WEBVIEW_CHANGE_ANNOUNCEMENT_DISMISSED_PREFERENCE_KEY,
                                        true,
                                    )
                                }
                                showArticleRenderChangeAnnouncement = false
                                context.navigate(Account.AppearanceSettings(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY))
                            },
                            dismiss = { Text("知道了") },
                            onDismiss = {
                                preferences.edit {
                                    putBoolean(
                                        ARTICLE_WEBVIEW_CHANGE_ANNOUNCEMENT_DISMISSED_PREFERENCE_KEY,
                                        true,
                                    )
                                }
                                showArticleRenderChangeAnnouncement = false
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
                                preferences.edit { putBoolean("filterExplainDialogShown", true) }
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
                        if (localHomeViewModel != null && it.localContentId != null) {
                            localHomeViewModel.onLocalItemFeedback(context, it, 1.0)
                            Toast.makeText(context, "已记录喜欢，本地推荐会逐步学习", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "收到喜欢，功能正在优化", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDislike = {
                        if (localHomeViewModel != null && it.localContentId != null) {
                            localHomeViewModel.onLocalItemFeedback(context, it, -1.0)
                            Toast.makeText(context, "已降低这类本地推荐的优先级", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "收到反馈，功能正在优化", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBlockUser = { feedItem ->
                        viewModel.handleBlockUser(context, feedItem) { authorInfo ->
                            userToBlock = authorInfo
                            showBlockUserDialog = true
                        }
                    },
                    onBlockByKeywords = { feedItem ->
                        viewModel.handleBlockByKeywords(context, feedItem) { (item, contentInfo) ->
                            feedToBlockByKeywords = contentInfo.first to contentInfo.second
                            showBlockByKeywordsDialog = true
                        }
                    },
                    onBlockTopic = { topicId, topicName ->
                        viewModel.handleBlockTopic(context, topicId, topicName)
                    },
                ) {
                    if (feed != null) {
//                            DataHolder.putFeed(feed)
                        (viewModel as IHomeFeedViewModel).onUiContentClick(context, feed, item)
                    } else if (localHomeViewModel != null && item.localContentId != null) {
                        localHomeViewModel.onLocalItemOpened(context, item)
                    }
                    if (navDestination != null) {
                        navigator.onNavigate(navDestination)
                    }
                }
            }

            if (showRefreshFab) {
                if (BuildConfig.DEBUG) {
                    DraggableRefreshButton(
                        onClick = {
                            val data = Json.encodeToString(viewModel.debugData)
                            val clip = ClipData.newPlainText("data", data)
                            context.clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "已复制调试数据", Toast.LENGTH_SHORT).show()
                        },
                        preferenceName = "copyAll",
                    ) {
                        Icon(Icons.Default.CopyAll, contentDescription = "复制")
                    }
                }
                DraggableRefreshButton(
                    onClick = { viewModel.refresh(context) },
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
        BlockByKeywordsDialog(
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
}
