/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ContactPage
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.MobileNotificationColumnHead
import com.github.zly2006.zhihu.data.MobileNotificationTimelineItem
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.avatarUrl
import com.github.zly2006.zhihu.ui.displayTitle
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.miuix.components.MiuixListLoadingIndicator
import com.github.zly2006.zhihu.ui.navDestination
import com.github.zly2006.zhihu.ui.notificationListDate
import com.github.zly2006.zhihu.ui.rememberNotificationEnvironment
import com.github.zly2006.zhihu.viewmodel.MobileNotificationCategory
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.yukonga.miuix.kmp.basic.Card
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

/**
 * 通知中心的 miuix 版本，对标 M3 [com.github.zly2006.zhihu.ui.NotificationScreen]。
 *
 * 顶部是四个分类入口和邀请回答行，下面是会话列表。是否显示未读数由 `NotificationSettingsStore` 控制，
 * 这里不能自行决定默认展示哪些通知。
 */
@Composable
fun MiuixNotificationScreen() {
    val navigator = LocalNavigator.current
    val settings = rememberSettingsStore()
    val settingsStore = rememberNotificationSettingsStore()
    val viewModel = viewModel { NotificationViewModel() }
    val environment = rememberNotificationEnvironment(settingsStore)
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val showDebugCopy = rememberSettingBoolean("developer", false, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val showUnreadBadge = settingsStore.getUnreadBadgeEnabled()
    // 区分“下拉刷新”和“首次加载”：下拉刷新时不显示中心转圈，避免与刷新动画叠加
    var isManualRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.allData.isEmpty()) {
            viewModel.refresh(environment)
        }
    }
    LaunchedEffect(viewModel.isLoading) {
        if (!viewModel.isLoading) isManualRefreshing = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "通知",
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (viewModel.unreadCount > 0) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                viewModel.markAllAsRead(environment)
                                userMessages.showMessage("已全部标记为已读")
                            }
                        }) {
                            Icon(Icons.Default.MarkChatRead, "已读", tint = MiuixTheme.colorScheme.onBackground)
                        }
                    }
                    IconButton(onClick = { navigator.onNavigate(Notification.NotificationSettings()) }) {
                        Icon(Icons.Default.Settings, "设置", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        PullToRefresh(
            isRefreshing = viewModel.isLoading,
            onRefresh = {
                isManualRefreshing = true
                coroutineScope.launch { viewModel.refresh(environment) }
            },
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
        ) {
            Box(
                modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding(),
                    ),
                ) {
                    item(key = "notification_categories") {
                        MiuixNotificationCategoryRow(
                            unreadCounts = viewModel.categoryUnreadCounts,
                            showUnreadBadges = showUnreadBadge,
                            onCategoryClick = { category ->
                                navigator.onNavigate(
                                    Notification.Entry(category.entryName, category.detailTitle),
                                )
                            },
                        )
                    }
                    viewModel.invitation?.let { invitation ->
                        item(key = "notification_invitation") {
                            MiuixNotificationInvitationRow(
                                invitation = invitation,
                                showUnreadBadge = showUnreadBadge,
                                onClick = { navigator.onNavigate(Notification.Invitations) },
                            )
                        }
                    }
                    items(viewModel.allData, key = { it.stableId }) { notification ->
                        MiuixNotificationConversationRow(
                            notification = notification,
                            showUnreadBadge = showUnreadBadge,
                            onClick = {
                                notification.navDestination()?.let(navigator.onNavigate)
                                    ?: userMessages.showMessage("暂不支持打开此消息")
                            },
                        )
                    }
                    if (!viewModel.isEnd) {
                        item {
                            LaunchedEffect(Unit) { viewModel.loadMore(environment) }
                        }
                    }
                }

                MiuixListLoadingIndicator(
                    isLoading = viewModel.isLoading,
                    isEmpty = viewModel.allData.isEmpty(),
                    isPullToRefresh = isManualRefreshing,
                )

                if (showDebugCopy) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        IconButton(
                            onClick = {
                                val data = Json.encodeToString(viewModel.debugData)
                                environment.setPlainTextClipboard("data", data)
                                userMessages.showMessage("已复制调试数据")
                            },
                        ) {
                            Icon(Icons.Default.CopyAll, "复制", tint = MiuixTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixNotificationCategoryRow(
    unreadCounts: Map<MobileNotificationCategory, Int>,
    showUnreadBadges: Boolean,
    onCategoryClick: (MobileNotificationCategory) -> Unit,
) {
    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MobileNotificationCategory.entries.forEach { category ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("notification_category_${category.entryName}")
                        .clickable { onCategoryClick(category) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box {
                        Icon(
                            imageVector = category.miuixHomeIcon(),
                            contentDescription = category.detailTitle,
                            modifier = Modifier.size(36.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                        val unreadCount = unreadCounts[category] ?: 0
                        if (showUnreadBadges && unreadCount > 0) {
                            MiuixUnreadBadge(unreadCount, Modifier.align(Alignment.TopEnd))
                        }
                    }
                    Text(
                        text = category.detailTitle,
                        style = MiuixTheme.textStyles.footnote1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixNotificationInvitationRow(
    invitation: MobileNotificationColumnHead,
    showUnreadBadge: Boolean,
    onClick: () -> Unit,
) {
    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notification_invitation")
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.ContactPage,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MiuixTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = invitation.title.ifBlank { "邀请回答" },
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = invitation.textPrefix + invitation.text,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showUnreadBadge && invitation.unreadCount > 0) {
                MiuixUnreadBadge(invitation.unreadCount)
            }
        }
    }
}

@Composable
private fun MiuixNotificationConversationRow(
    notification: MobileNotificationTimelineItem,
    showUnreadBadge: Boolean,
    onClick: () -> Unit,
) {
    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notification_message_${notification.stableId}")
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val avatarUrl = notification.avatarUrl()
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.displayTitle(),
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    notification.notificationListDate().takeIf { it.isNotBlank() }?.let { created ->
                        Text(
                            text = created,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = notification.content?.text.orEmpty(),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showUnreadBadge && notification.unreadCount > 0) {
                Spacer(Modifier.width(8.dp))
                MiuixUnreadBadge(notification.unreadCount)
            }
        }
    }
}

@Composable
private fun MiuixUnreadBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.primary)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = MiuixTheme.colorScheme.onPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun MobileNotificationCategory.miuixHomeIcon(): ImageVector = when (this) {
    MobileNotificationCategory.Comment -> Icons.AutoMirrored.Outlined.Comment
    MobileNotificationCategory.Like -> Icons.Filled.Favorite
    MobileNotificationCategory.Favorite -> Icons.Filled.Bookmark
    MobileNotificationCategory.Follow -> Icons.Filled.PersonAddAlt1
}
