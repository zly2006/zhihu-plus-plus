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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ContactPage
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.data.MobileNotificationColumnHead
import com.github.zly2006.zhihu.data.MobileNotificationTimelineItem
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.util.formatRelativeTime
import com.github.zly2006.zhihu.viewmodel.MobileNotificationCategory
import com.github.zly2006.zhihu.viewmodel.NotificationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
expect fun rememberNotificationEnvironment(
    settingsStore: NotificationSettingsStore,
): NotificationEnvironment

@Composable
expect fun rememberNotificationShowDebugCopy(): Boolean

/**
 * 通知主页复用官方 Android `message/v3` 的信息层级：四个分类、邀请回答入口和私信会话列表。
 * 应用内显示过滤与自动已读仍由 [NotificationSettingsStore] 控制，不能借布局调整改变默认通知偏好。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {
    val navigator = LocalNavigator.current
    val settingsStore = rememberNotificationSettingsStore()
    val viewModel = viewModel { NotificationViewModel() }
    val environment = rememberNotificationEnvironment(settingsStore)
    val showDebugCopy = rememberNotificationShowDebugCopy()
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, environment) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh(environment)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("消息") },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (viewModel.unreadCount > 0) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if (viewModel.markAllAsRead(environment)) {
                                    userMessages.showMessage("已全部标记为已读")
                                } else {
                                    userMessages.showMessage("标记已读失败")
                                }
                            }
                        }) {
                            Icon(Icons.Default.MarkChatRead, contentDescription = "已读")
                        }
                    }
                    IconButton(onClick = { navigator.onNavigate(Notification.NotificationSettings) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading,
            onRefresh = { viewModel.refresh(environment) },
            modifier = Modifier.padding(paddingValues),
        ) {
            PaginatedList(
                items = viewModel.allData,
                onLoadMore = { viewModel.loadMore(environment) },
                isEnd = { viewModel.isEnd },
                modifier = Modifier.fillMaxSize(),
                footer = ProgressIndicatorFooter,
                key = { it.stableId },
                topContent = {
                    item(key = "notification_categories") {
                        NotificationCategoryRow(
                            unreadCounts = viewModel.categoryUnreadCounts,
                            showUnreadBadges = settingsStore.getUnreadBadgeEnabled(),
                            onCategoryClick = { category ->
                                navigator.onNavigate(
                                    Notification.Entry(category.entryName, category.detailTitle),
                                )
                            },
                        )
                    }
                    viewModel.invitation?.let { invitation ->
                        item(key = "notification_invitation") {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                            NotificationInvitationRow(
                                invitation = invitation,
                                showUnreadBadge = settingsStore.getUnreadBadgeEnabled(),
                                onClick = { navigator.onNavigate(Notification.Invitations) },
                            )
                        }
                    }
                    item(key = "notification_messages_divider") {
                        HorizontalDivider(
                            thickness = 8.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        )
                    }
                },
            ) { notification ->
                NotificationConversationRow(
                    notification = notification,
                    showUnreadBadge = settingsStore.getUnreadBadgeEnabled(),
                    onClick = {
                        notification.navDestination()?.let(navigator.onNavigate)
                            ?: userMessages.showMessage("暂不支持打开此消息")
                    },
                )
            }

            if (showDebugCopy) {
                DraggableRefreshButton(
                    onClick = {
                        val data = Json.encodeToString(viewModel.debugData)
                        environment.setPlainTextClipboard("data", data)
                        userMessages.showMessage("已复制调试数据")
                    },
                    preferenceName = "copyAll",
                ) {
                    Icon(Icons.Default.CopyAll, contentDescription = "复制")
                }
            }
        }
    }
}

@Composable
private fun NotificationCategoryRow(
    unreadCounts: Map<MobileNotificationCategory, Int>,
    showUnreadBadges: Boolean,
    onCategoryClick: (MobileNotificationCategory) -> Unit,
) {
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
                BadgedBox(
                    badge = {
                        val unreadCount = unreadCounts[category] ?: 0
                        if (showUnreadBadges && unreadCount > 0) {
                            Badge {
                                Text(formatUnreadCount(unreadCount))
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = category.homeIcon(),
                        contentDescription = category.detailTitle,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Text(
                    text = category.detailTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NotificationInvitationRow(
    invitation: MobileNotificationColumnHead,
    showUnreadBadge: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_invitation")
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ContactPage, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = invitation.title.ifBlank { "邀请回答" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                invitation.avatarUrls.take(2).forEachIndexed { index, avatar ->
                    AsyncImage(
                        model = avatar.url,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = if (index == 0) 0.dp else 2.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
                if (invitation.avatarUrls.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = invitation.textPrefix + invitation.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showUnreadBadge && invitation.unreadCount > 0) {
            Badge {
                Text(formatUnreadCount(invitation.unreadCount))
            }
        }
    }
}

@Composable
private fun NotificationConversationRow(
    notification: MobileNotificationTimelineItem,
    showUnreadBadge: Boolean,
    onClick: () -> Unit,
) {
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
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null)
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.displayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                notification.notificationListDate().takeIf { it.isNotBlank() }?.let { created ->
                    Text(
                        text = created,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = notification.content?.text.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showUnreadBadge && notification.unreadCount > 0) {
            Spacer(Modifier.width(8.dp))
            Badge {
                Text(formatUnreadCount(notification.unreadCount))
            }
        }
    }
}

@Composable
fun NotificationItemView(
    notification: MobileNotificationTimelineItem,
    onClick: () -> Unit,
) {
    val backgroundColor = if (notification.isRead) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = backgroundColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .padding(top = 10.dp, end = 4.dp),
                ) {
                    if (!notification.isRead) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = CircleShape,
                            modifier = Modifier.size(6.dp),
                        ) {}
                    }
                }
                val avatarUrl = notification.avatarUrl()
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.displayTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    notification.displaySubtitle().takeIf { it.isNotBlank() }?.let { subtitle ->
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val displayText = if (notification.content?.subTitle == "喜欢了你的评论") {
                        Ksoup.parse(notification.content.subText).text()
                    } else {
                        Ksoup.parse(notification.content?.text.orEmpty()).text()
                    }
                    if (displayText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Text(
                    text = formatRelativeTime(notification.created),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            notification.sourceText().takeIf { it.isNotBlank() }?.let { sourceText ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = sourceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

private fun MobileNotificationCategory.homeIcon(): ImageVector = when (this) {
    MobileNotificationCategory.Comment -> Icons.AutoMirrored.Outlined.Comment
    MobileNotificationCategory.Like -> Icons.Outlined.FavoriteBorder
    MobileNotificationCategory.Favorite -> Icons.Outlined.StarOutline
    MobileNotificationCategory.Follow -> Icons.Outlined.Info
}

internal fun MobileNotificationTimelineItem.displayTitle(): String =
    content
        ?.title
        ?.takeIf { it.isNotBlank() }
        ?: detailTitle.takeIf { it.isNotBlank() }
        ?: target?.name?.takeIf { it.isNotBlank() }
        ?: "通知"

private fun MobileNotificationTimelineItem.displaySubtitle(): String {
    val subtitle = content?.subTitle?.takeIf { it.isNotBlank() }.orEmpty()
    if ((!subtitle.endsWith("：") && subtitle.startsWith("评论了")) ||
        subtitle.startsWith("赞同了") ||
        subtitle.startsWith("喜欢了")
    ) {
        return "$subtitle："
    }
    return subtitle
}

private fun MobileNotificationTimelineItem.sourceText(): String =
    listOfNotNull(
        targetSource?.text?.takeIf { it.isNotBlank() },
        targetSource?.subText?.takeIf { it.isNotBlank() },
    ).joinToString("\n")

internal fun MobileNotificationTimelineItem.avatarUrl(): String =
    head?.avatarUrl?.takeIf { it.isNotBlank() }
        ?: head?.author?.avatarUrl?.takeIf { it.isNotBlank() }
        ?: target?.avatarUrl?.takeIf { it.isNotBlank() }
        ?: content?.subIcon?.takeIf { it.isNotBlank() }
        ?: ""

internal fun MobileNotificationTimelineItem.navDestination(): NavDestination? {
    target
        ?.takeIf { it.type == "people" && (it.urlToken.isNotBlank() || it.id.isNotBlank()) }
        ?.let {
            return Person(
                id = it.id.ifBlank { Person.EMPTY_ID },
                urlToken = it.urlToken,
                name = it.name.ifBlank { "loading..." },
            )
        }

    val destinations = listOf(
        content?.targetLink,
        content?.subTargetLink,
        targetSource?.targetLink,
        head?.targetLink,
    ).mapNotNull { link ->
        link?.takeIf { it.isNotBlank() }?.let(::resolveContent)
    }
    val destination = destinations.firstOrNull { it is Notification.Message }
        ?: destinations.firstOrNull()
    return if (destination is Notification.Message) {
        destination.copy(
            name = destination.name.ifBlank { displayTitle() },
            avatarUrl = avatarUrl(),
        )
    } else {
        destination
    }
}

@OptIn(ExperimentalTime::class)
internal fun MobileNotificationTimelineItem.notificationListDate(): String {
    val epochSeconds = created.takeIf { it > 0 } ?: createdStr.toLongOrNull()
        ?: return createdStr
    val dateTime = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.month.number.toString().padStart(2, '0')}-${dateTime.day.toString().padStart(2, '0')}"
}

private fun formatUnreadCount(count: Int): String = if (count > 99) "99+" else count.toString()
