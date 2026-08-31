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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MarkUnreadChatAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.RecommendationMode
import com.github.zly2006.zhihu.data.ZHIHU_ME_URL
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.ZhihuMeNotifications
import com.github.zly2006.zhihu.data.navDestination
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.WritePin
import com.github.zly2006.zhihu.navigation.requestLoginNavigation
import com.github.zly2006.zhihu.notification.HOME_NOTIFICATION_ACTION_OPEN_ANSWER
import com.github.zly2006.zhihu.notification.HOME_NOTIFICATION_ACTION_OPEN_ARTICLE
import com.github.zly2006.zhihu.notification.HOME_NOTIFICATION_ACTION_OPEN_PIN
import com.github.zly2006.zhihu.notification.HOME_NOTIFICATION_ACTION_OPEN_UPDATE_SETTINGS
import com.github.zly2006.zhihu.notification.HOME_NOTIFICATION_ACTION_OPEN_URL
import com.github.zly2006.zhihu.notification.HOME_NOTIFICATION_ACTION_SET_SETTING
import com.github.zly2006.zhihu.notification.HOME_NOTIFICATION_REFRESH_INTERVAL_MILLIS
import com.github.zly2006.zhihu.notification.OnlineHomeNotification
import com.github.zly2006.zhihu.notification.OnlineHomeNotificationRepository
import com.github.zly2006.zhihu.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingString
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.HOME_CREATE_FAB_TAG
import com.github.zly2006.zhihu.ui.HOME_CREATE_MENU_TAG
import com.github.zly2006.zhihu.ui.HOME_REFRESH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HOME_WRITE_ANSWER_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HOME_WRITE_PIN_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HOME_WRITE_QUESTION_BUTTON_TAG
import com.github.zly2006.zhihu.ui.LocalReadingPlayerOverlayPadding
import com.github.zly2006.zhihu.ui.SEARCH_HISTORY_MAX_SIZE
import com.github.zly2006.zhihu.ui.components.AnnouncementCard
import com.github.zly2006.zhihu.ui.components.AnnouncementCardDefaults
import com.github.zly2006.zhihu.ui.components.AutoHideTopBar
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedAuthorBlockConfirmDialog
import com.github.zly2006.zhihu.ui.components.FeedAuthorBlockRequest
import com.github.zly2006.zhihu.ui.components.FeedAuthorBlockType
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.homeOnlineNotificationTag
import com.github.zly2006.zhihu.ui.loadSearchHistory
import com.github.zly2006.zhihu.ui.miuix.components.MiuixAccountSheet
import com.github.zly2006.zhihu.ui.miuix.components.MiuixConfirmDialog
import com.github.zly2006.zhihu.ui.miuix.components.MiuixFeedCard
import com.github.zly2006.zhihu.ui.miuix.components.MiuixListLoadingIndicator
import com.github.zly2006.zhihu.ui.miuix.components.MiuixSearchFilterSheet
import com.github.zly2006.zhihu.ui.miuix.components.MiuixSearchSuggestions
import com.github.zly2006.zhihu.ui.miuix.components.MiuixSheetActionRow
import com.github.zly2006.zhihu.ui.miuix.components.SearchBarFake
import com.github.zly2006.zhihu.ui.miuix.components.SearchBox
import com.github.zly2006.zhihu.ui.miuix.components.SearchPager
import com.github.zly2006.zhihu.ui.miuix.components.SearchStatus
import com.github.zly2006.zhihu.ui.miuix.components.searchFilterSummary
import com.github.zly2006.zhihu.ui.rememberAccountSettingsAccountState
import com.github.zly2006.zhihu.ui.rememberAppVersionInfo
import com.github.zly2006.zhihu.ui.rememberHomeIsDebuggable
import com.github.zly2006.zhihu.ui.saveSearchHistory
import com.github.zly2006.zhihu.ui.subscreens.SystemUpdateState
import com.github.zly2006.zhihu.ui.subscreens.rememberSystemUpdateState
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedInteractionViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.SearchViewModel
import com.github.zly2006.zhihu.viewmodel.local.LocalHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.za.AndroidHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
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

/**
 * 跨导航持久化首页搜索浮层状态的 Saver。保存查询词、是否展开（动画态归一到稳定态）和假框位置 offsetY。
 * offsetY 必须保留：收起动画的目标位置取自它，恢复的展开态若丢了 offsetY 会弹向屏幕顶而非假框位置。
 * resultStatus 不保存，返回后由搜索结果重新驱动。
 */
private val SearchStatusSaver = listSaver<SearchStatus, Any>(
    save = { listOf(it.searchText, it.shouldExpand(), it.offsetY.value) },
    restore = {
        SearchStatus(
            label = "",
            searchText = it[0] as String,
            current = if (it[1] as Boolean) SearchStatus.Status.EXPANDED else SearchStatus.Status.COLLAPSED,
            offsetY = (it[2] as Float).dp,
        )
    },
)

@Composable
fun MiuixHomeScreen(
    scrollToTopTrigger: Int = 0,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val readingPlayerOverlayPadding = LocalReadingPlayerOverlayPadding.current
    val navigator = LocalNavigator.current
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = true)
    val settings = rememberSettingsStore()
    val notificationSettings = rememberNotificationSettingsStore()
    val userMessages = rememberUserMessageSink()
    val openExternalUrl = rememberExternalUrlOpener()
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val duo3HomeAccount = rememberSettingBoolean("duo3_home_account", false, settings)
    val showRefreshFab = rememberSettingBoolean("showRefreshFab", true, settings)
    val showUnreadBadge = notificationSettings.getUnreadBadgeEnabled()
    val recommendationModeKey = rememberSettingString("recommendationMode", RecommendationMode.MIXED.key, settings)

    val currentRecommendationMode = RecommendationMode.entries.find {
        it.key == recommendationModeKey
    } ?: RecommendationMode.MIXED
    val account = rememberAccountSettingsAccountState().value
    // 登录态缺 d_c0 时给出明确提示，否则用户只会看到一直刷不出来的空列表（对标 M3 HomeScreen）。
    if (account.login && !account.hasRequiredCookie) {
        MiuixConfirmDialog(
            show = true,
            title = "Cookie 不完整",
            summary = "当前登录信息缺少必要的 Cookie d_c0，请重新登录。",
            confirmText = "重新登录",
            cancelText = "稍后",
            onConfirm = { requestLoginNavigation() },
            onDismiss = {},
        )
    }
    val updateState by rememberSystemUpdateState().collectAsState()
    val updateAnnouncement = updateState as? SystemUpdateState.UpdateAvailable
    val isDebuggable = rememberHomeIsDebuggable()
    val viewModel: BaseFeedViewModel = when (currentRecommendationMode) {
        RecommendationMode.WEB -> viewModel { HomeFeedViewModel() }
        RecommendationMode.ANDROID -> viewModel { AndroidHomeFeedViewModel() }
        RecommendationMode.LOCAL -> viewModel { LocalHomeFeedViewModel() }
        RecommendationMode.MIXED -> viewModel { MixedHomeFeedViewModel() }
    }
    val localHomeViewModel = viewModel as? LocalHomeFeedViewModel

    val listState = rememberLazyListState()
    // 跨导航完整保留搜索浮层状态（展开/折叠、查询词、假框位置 offsetY）：点搜索结果进入文章/回答详情后，
    // 首页会被 NavHost dispose，普通 remember 会丢失，导致返回时回到折叠主页；且 offsetY 必须一并保留，
    // 否则恢复的展开态 offsetY=0，再收起时动画会弹向屏幕顶而非假框位置（抽风）。用 rememberSaveable + Saver
    // 把整个状态存进 MainTabs 返回栈条目的 saved-state，导航往返完整复原。
    var searchStatus by rememberSaveable(stateSaver = SearchStatusSaver) {
        mutableStateOf(SearchStatus(label = ""))
    }
    val showAccountSheet = remember { mutableStateOf(false) }
    var unreadCount by remember { mutableIntStateOf(0) }
    var showSearchFilter by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var dismissedUpdateVersion by remember { mutableStateOf<String?>(null) }
    val versionName = rememberAppVersionInfo().substringBefore(' ').takeIf { it.firstOrNull()?.isDigit() == true }
    val onlineNotificationRepository = remember(settings) { OnlineHomeNotificationRepository(settings) }
    var onlineNotifications by remember { mutableStateOf(emptyList<OnlineHomeNotification>()) }

    // 屏蔽相关 state（沿用 HomeScreen 逻辑）
    var feedAuthorBlockRequest by remember { mutableStateOf<FeedAuthorBlockRequest?>(null) }
    var showBlockByKeywordsDialog by remember { mutableStateOf(false) }
    var feedToBlockByKeywords by remember { mutableStateOf<Pair<String, String?>?>(null) }

    LaunchedEffect(currentRecommendationMode, account.login) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(paginationEnvironment)
        }
    }

    LaunchedEffect(lifecycleOwner, paginationEnvironment, versionName) {
        if (versionName != null) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    try {
                        onlineNotifications = onlineNotificationRepository.load(
                            versionName = versionName,
                            httpClient = paginationEnvironment.httpClient(),
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e("MiuixHomeScreen", "Failed to load online notifications", e)
                    }
                    delay(HOME_NOTIFICATION_REFRESH_INTERVAL_MILLIS)
                }
            }
        }
    }

    // 拉取未读通知数（与 M3 HomeScreen 行为一致）
    LaunchedEffect(Unit) {
        try {
            unreadCount = paginationEnvironment
                .fetchJson(ZHIHU_ME_URL, "")
                ?.let { ZhihuJson.decodeJson<ZhihuMeNotifications>(it) }
                ?.totalCount ?: 0
        } catch (_: Exception) {
        }
    }

    // 点搜索框展开时立即把 feed 滚回顶部，避免展开动画期间背景文字造成模糊穿帮
    LaunchedEffect(searchStatus.current) {
        if (searchStatus.isAnimatingExpand() && listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    // ── 同页搜索 ──
    // 防抖后的查询词：searchText 停止变化 350ms 后才真正搜索，避免每打一个字就请求
    // 用恢复的查询词初始化，返回时立即定位到已缓存结果，避免 350ms 防抖期间闪现历史/热搜
    var debouncedQuery by remember { mutableStateOf(searchStatus.searchText) }
    LaunchedEffect(searchStatus.searchText) {
        val q = searchStatus.searchText.trim()
        if (q.isEmpty()) {
            debouncedQuery = ""
        } else {
            kotlinx.coroutines.delay(350)
            debouncedQuery = q
        }
    }
    // query 变化时切换 SearchViewModel（query 是构造参数，不可变）。用 viewModel(key=query) 而非
    // remember，把 viewModel scope 到 MainTabs 返回栈条目的 ViewModelStore：进入文章/回答详情再返回时，
    // 同 query 的 viewModel 连同已加载结果一起存活复用，配合下方“已有结果不重复 refresh”守卫，做到返回
    // 不重新请求（与首页 feed viewModel 同一机制）。
    val searchViewModel = if (debouncedQuery.isEmpty()) {
        null
    } else {
        viewModel(key = "home_search_$debouncedQuery") { SearchViewModel(debouncedQuery) }
    }
    val searchListState = rememberLazyListState()
    // 触发搜索 + 驱动 resultStatus。已有结果（viewModel 跨导航复用）时直接显示，不重新请求。
    LaunchedEffect(searchViewModel) {
        val vm = searchViewModel
        when {
            vm == null -> searchStatus = searchStatus.copy(resultStatus = SearchStatus.ResultStatus.DEFAULT)
            vm.displayItems.isNotEmpty() -> searchStatus = searchStatus.copy(resultStatus = SearchStatus.ResultStatus.SHOW)
            else -> {
                searchStatus = searchStatus.copy(resultStatus = SearchStatus.ResultStatus.LOAD)
                vm.refresh(paginationEnvironment)
            }
        }
    }
    // 搜索结果加载完成后切换 SHOW / EMPTY
    val showSearchHistory = rememberSettingBoolean("showSearchHistory", true, settings)
    LaunchedEffect(searchViewModel?.displayItems?.size, searchViewModel?.isLoading) {
        val vm = searchViewModel ?: return@LaunchedEffect
        if (!vm.isLoading) {
            val hasResult = vm.displayItems.isNotEmpty()
            // 出结果即写入历史；defaultResult 重挂载会自动重读
            if (hasResult && showSearchHistory && debouncedQuery.isNotBlank()) {
                val history = loadSearchHistory(settings).toMutableList()
                history.remove(debouncedQuery)
                history.add(0, debouncedQuery)
                while (history.size > SEARCH_HISTORY_MAX_SIZE) history.removeAt(history.lastIndex)
                saveSearchHistory(settings, history)
            }
            searchStatus = searchStatus.copy(
                resultStatus = if (hasResult) SearchStatus.ResultStatus.SHOW else SearchStatus.ResultStatus.EMPTY,
            )
        }
    }

    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    // 外层 Box：让 SearchPager 覆盖整个屏幕（从真正的屏幕顶部算），
    // 而不是被困在 Scaffold 内容区（已被 topBar 推下去，会导致不靠顶 + 返回闪现）
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                AutoHideTopBar {
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
                                // duo3_home_account 开启时显示头像（弹账号面板），关闭时显示通知入口（与 M3 HomeScreen 对齐）
                                Box(modifier = Modifier.padding(end = 8.dp)) {
                                    IconButton(
                                        onClick = {
                                            if (duo3HomeAccount) {
                                                showAccountSheet.value = true
                                            } else {
                                                navigator.onNavigate(Notification)
                                            }
                                        },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        if (duo3HomeAccount) {
                                            val avatarUrl = account.avatarUrl
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
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "通知",
                                                tint = MiuixTheme.colorScheme.onBackground,
                                                modifier = Modifier.size(28.dp),
                                            )
                                        }
                                    }
                                    if (showUnreadBadge && unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MiuixTheme.colorScheme.error),
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
                                        }.then(
                                            if (searchStatus.isCollapsed()) {
                                                Modifier.pointerInput(Unit) {
                                                    detectTapGestures {
                                                        searchStatus = searchStatus.copy(current = SearchStatus.Status.EXPANDING)
                                                    }
                                                }
                                            } else {
                                                Modifier
                                            },
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
                }
            },
        ) { padding ->
            // Scaffold 内容区：feed + 下拉刷新
            searchStatus.SearchBox {
                PullToRefresh(
                    isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading,
                    onRefresh = { coroutineScope.launch { viewModel.pullToRefresh(paginationEnvironment) } },
                    contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
                    refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
                ) {
                    Box(
                        modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
                    ) {
                        // 首次加载信息流为空时显示转圈，避免白屏；下拉刷新时不显示，避免叠加
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
                                .scrollEndHaptic()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = PaddingValues(
                                top = padding.calculateTopPadding() + 6.dp,
                                bottom = innerPadding.calculateBottomPadding() + 12.dp,
                            ),
                            onLoadMore = { viewModel.loadMore(paginationEnvironment) },
                            key = { item -> item.stableKey },
                            topContent = {
                                item {
                                    val availableUpdate = updateAnnouncement

                                    AnnouncementCard(
                                        visible = availableUpdate != null && dismissedUpdateVersion != availableUpdate.version,
                                        title = "发现新版本：${availableUpdate?.version}${if (availableUpdate?.isNightly == true) " (Nightly)" else ""}",
                                        leadingIcon = {
                                            androidx.compose.material3.Icon(
                                                Icons.Default.ArrowCircleUp,
                                                contentDescription = null,
                                            )
                                        },
                                        accept = { androidx.compose.material3.Text("查看更新") },
                                        onAccept = { navigator.onNavigate(Account.SystemAndUpdateSettings()) },
                                        dismiss = { androidx.compose.material3.Text("以后") },
                                        onDismiss = {
                                            availableUpdate?.version?.let { version ->
                                                dismissedUpdateVersion = version
                                            }
                                        },
                                        colors = AnnouncementCardDefaults.colorsImportant(),
                                    )
                                }
                                onlineNotifications.forEach { notification ->
                                    val markRead = {
                                        onlineNotificationRepository.markRead(notification)
                                        onlineNotifications = onlineNotifications.filterNot { it.uuid == notification.uuid }
                                    }
                                    item(notification.uuid) {
                                        AnnouncementCard(
                                            modifier = Modifier.testTag(homeOnlineNotificationTag(notification.uuid)),
                                            visible = true,
                                            title = notification.title,
                                            leadingIcon = {
                                                androidx.compose.material3.Icon(
                                                    Icons.Default.Notifications,
                                                    contentDescription = null,
                                                )
                                            },
                                            content = notification.content,
                                            accept = notification.accept?.let { accept ->
                                                { androidx.compose.material3.Text(accept.text) }
                                            },
                                            onAccept = {
                                                val accept = notification.accept
                                                markRead()
                                                when (accept?.key) {
                                                    HOME_NOTIFICATION_ACTION_OPEN_URL -> {
                                                        accept.value
                                                            ?.jsonPrimitive
                                                            ?.contentOrNull
                                                            ?.let { openExternalUrl(it) }
                                                    }
                                                    HOME_NOTIFICATION_ACTION_OPEN_UPDATE_SETTINGS -> {
                                                        navigator.onNavigate(Account.SystemAndUpdateSettings())
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
                                            dismiss = { androidx.compose.material3.Text(notification.dismiss) },
                                            onDismiss = markRead,
                                        )
                                    }
                                }
                            },
                        ) { item ->
                            MiuixFeedCard(
                                item = item,
                                onLike = { localHomeViewModel?.onLocalItemFeedback(it, 1.0) },
                                onDislike = { localHomeViewModel?.onLocalItemFeedback(it, -1.0) },
                                onBlockUser = { feedItem ->
                                    viewModel.handleBlockUser(paginationEnvironment, userMessages, feedItem) { authorInfo ->
                                        feedAuthorBlockRequest = FeedAuthorBlockRequest(
                                            type = FeedAuthorBlockType.CONTENT_AUTHOR,
                                            userId = authorInfo.first,
                                            userName = authorInfo.second,
                                        )
                                    }
                                },
                                onBlockByKeywords = { feedItem ->
                                    viewModel.handleBlockByKeywords(paginationEnvironment, userMessages, feedItem) { (_, contentInfo) ->
                                        feedToBlockByKeywords = contentInfo.first to contentInfo.second
                                        showBlockByKeywordsDialog = true
                                    }
                                },
                                onBlockTopic = { topicId, topicName ->
                                    viewModel.handleBlockTopic(userMessages, topicId, topicName)
                                },
                                onClick = {
                                    // 默认跳转逻辑：本地内容回调 + navDestination
                                    val feed = this.feed
                                    if (feed != null) {
                                        (viewModel as HomeFeedInteractionViewModel)
                                            .onUiContentClick(paginationEnvironment, feed, this)
                                    } else {
                                        localHomeViewModel?.onLocalItemOpened(this)
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
                // 重挂载时自动从 prefs 重读历史（搜索完成会写入），无需共享 state
                MiuixSearchSuggestions(
                    onQueryClick = { q -> searchStatus = searchStatus.copy(searchText = q) },
                )
            },
            loadingResult = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    top.yukonga.miuix.kmp.basic
                        .CircularProgressIndicator()
                }
            },
            emptyResult = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有找到相关结果", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            },
            result = {
                val vm = searchViewModel
                if (vm != null) {
                    PaginatedList(
                        items = vm.displayItems,
                        listState = searchListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 6.dp,
                            bottom = innerPadding.calculateBottomPadding() + 12.dp,
                        ),
                        onLoadMore = { vm.loadMore(paginationEnvironment) },
                        key = { item -> item.stableKey },
                        topContent = {
                            // 内联筛选入口：显示当前筛选并打开筛选弹层（省去独立搜索页）
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showSearchFilter = true }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = "筛选",
                                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                        searchFilterSummary(vm),
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        },
                    ) { item ->
                        MiuixFeedCard(
                            item = item,
                            onClick = {
                                this.navDestination?.let { navigator.onNavigate(it) }
                            },
                        )
                    }
                }
            },
        )

        // 创作入口：对标 M3 HomeScreen 的 FAB + 展开菜单（提问题 / 写回答 / 发想法）。
        // 放在刷新 FAB 之前渲染，保证展开时的遮罩不会盖住刷新按钮的拖拽热区。
        MiuixHomeCreateFab(
            expanded = showCreateMenu,
            onExpandedChange = { showCreateMenu = it },
            bottomPadding = innerPadding.calculateBottomPadding() + readingPlayerOverlayPadding,
            onWriteQuestion = { userMessages.showShortMessage("正在施工") },
            onWriteAnswer = { userMessages.showShortMessage("正在施工") },
            onWritePin = { navigator.onNavigate(WritePin()) },
        )

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
                    top.yukonga.miuix.kmp.basic
                        .CircularProgressIndicator()
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        }
    } // 外层 Box 结束

    // 内联搜索筛选弹层（排序/内容类型/时间范围），仅在有搜索结果时存在
    searchViewModel?.let { vm ->
        MiuixSearchFilterSheet(
            show = showSearchFilter,
            onDismiss = { showSearchFilter = false },
            viewModel = vm,
            environment = paginationEnvironment,
        )
    }

    // 屏蔽用户确认对话框（与 HomeScreen 同签名）
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
        com.github.zly2006.zhihu.ui.components.BlockByKeywordsDialog(
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

    MiuixAccountSheet(
        show = showAccountSheet.value,
        unreadCount = unreadCount,
        showUnreadBadge = showUnreadBadge,
        onDismiss = { showAccountSheet.value = false },
    )
}

/**
 * 首页创作入口的 miuix 版本，对标 M3 [com.github.zly2006.zhihu.ui.HomeScreen] 的 FAB + 展开菜单。
 *
 * 展开时先铺一层可点击遮罩吞掉外部点击（点空白收起），菜单本身用 miuix Card + [MiuixSheetActionRow]，
 * 与其它 miuix 弹层的行距、图标尺寸保持一致。
 */
@Composable
private fun BoxScope.MiuixHomeCreateFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    bottomPadding: Dp,
    onWriteQuestion: () -> Unit,
    onWriteAnswer: () -> Unit,
    onWritePin: () -> Unit,
) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(120)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.windowDimming)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onExpandedChange(false) },
        )
    }

    Column(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = bottomPadding + 16.dp),
        horizontalAlignment = Alignment.End,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(120)) +
                scaleIn(tween(180), initialScale = 0.92f, transformOrigin = TransformOrigin(1f, 1f)),
            exit = fadeOut(tween(90)) +
                scaleOut(tween(120), targetScale = 0.96f, transformOrigin = TransformOrigin(1f, 1f)),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Card(modifier = Modifier.width(180.dp).testTag(HOME_CREATE_MENU_TAG)) {
                    MiuixSheetActionRow(
                        text = "提问题",
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        modifier = Modifier.testTag(HOME_WRITE_QUESTION_BUTTON_TAG),
                        onClick = {
                            onExpandedChange(false)
                            onWriteQuestion()
                        },
                    )
                    MiuixSheetActionRow(
                        text = "写回答",
                        icon = Icons.Default.Edit,
                        modifier = Modifier.testTag(HOME_WRITE_ANSWER_BUTTON_TAG),
                        onClick = {
                            onExpandedChange(false)
                            onWriteAnswer()
                        },
                    )
                    MiuixSheetActionRow(
                        text = "发想法",
                        icon = Icons.Default.MarkUnreadChatAlt,
                        modifier = Modifier.testTag(HOME_WRITE_PIN_BUTTON_TAG),
                        onClick = {
                            onExpandedChange(false)
                            onWritePin()
                        },
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        FloatingActionButton(
            modifier = Modifier.testTag(HOME_CREATE_FAB_TAG),
            onClick = { onExpandedChange(!expanded) },
        ) {
            Icon(Icons.Default.Add, contentDescription = "创作", tint = MiuixTheme.colorScheme.onPrimary)
        }
    }
}
