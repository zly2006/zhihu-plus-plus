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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.shared.data.MobileNotificationTimelineItem
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.formatRelativeTime
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.MobileNotificationCategory
import com.github.zly2006.zhihu.viewmodel.NotificationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
expect fun rememberNotificationEnvironment(
    viewModel: NotificationViewModel,
    settingsStore: NotificationSettingsStore,
): NotificationEnvironment

@Composable
expect fun rememberNotificationShowDebugCopy(): Boolean

/**
 * 通知中心页面。
 *
 * 页面按通知设置过滤应用内展示项，用户显式开启时才会在进入页面后自动标记已读。系统通知、应用内显示和自动已读由
 * `NotificationSettingsStore` 管理，不走普通 `SettingsStore`，改动时要同时检查通知设置页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {
    val navigator = LocalNavigator.current
    val settingsStore = rememberNotificationSettingsStore()
    val viewModel = viewModel { NotificationViewModel() }
    val environment = rememberNotificationEnvironment(viewModel, settingsStore)
    val showDebugCopy = rememberNotificationShowDebugCopy()
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()

    LaunchedEffect(Unit) {
        if (viewModel.allData.isEmpty()) {
            viewModel.refresh(environment)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("消息")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                            Icon(Icons.Default.MarkChatRead, contentDescription = "已读")
                        }
                    }
                    IconButton(onClick = {
                        navigator.onNavigate(Notification.NotificationSettings)
                    }) {
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
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                NotificationCategoryRow(
                    selectedCategory = viewModel.selectedCategory,
                    unreadCounts = viewModel.categoryUnreadCounts,
                    showUnreadBadges = settingsStore.getUnreadBadgeEnabled(),
                    onCategoryClick = { category -> viewModel.selectCategory(category, environment) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                PaginatedList(
                    items = viewModel.allData,
                    onLoadMore = { viewModel.loadMore(environment) },
                    isEnd = { viewModel.isEnd },
                    modifier = Modifier.weight(1f),
                    footer = ProgressIndicatorFooter,
                ) { notification ->
                    if (viewModel.shouldShowNotification(settingsStore, notification)) {
                        NotificationItemView(
                            notification = notification,
                            onClick = {
                                notification.navDestination()?.let { navigator.onNavigate(it) }
                                    ?: userMessages.showMessage("暂不支持打开此通知")
                            },
                        )
                    }
                }
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
    selectedCategory: MobileNotificationCategory,
    unreadCounts: Map<MobileNotificationCategory, Int>,
    showUnreadBadges: Boolean,
    onCategoryClick: (MobileNotificationCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MobileNotificationCategory.entries.forEach { category ->
            NotificationCategoryButton(
                category = category,
                unreadCount = unreadCounts[category] ?: 0,
                showUnreadBadge = showUnreadBadges,
                selected = category == selectedCategory,
                onClick = { onCategoryClick(category) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NotificationCategoryButton(
    category: MobileNotificationCategory,
    unreadCount: Int,
    showUnreadBadge: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .testTag("notification_category_${category.entryName}")
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            BadgedBox(
                badge = {
                    if (showUnreadBadge && unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ) {
                            Text(formatUnreadCount(unreadCount))
                        }
                    }
                },
            ) {
                Surface(
                    shape = CircleShape,
                    color = containerColor,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = category.icon(),
                            contentDescription = category.detailTitle,
                            modifier = Modifier.size(30.dp),
                            tint = contentColor,
                        )
                    }
                }
            }
        }
        Text(
            text = category.detailTitle,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
                        contentDescription = "",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Row(
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
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
                        val displayText =
                            if (notification.content?.subTitle == "喜欢了你的评论") {
                                // 对评论特殊处理
                                Ksoup.parse(notification.content.subText).text()
                            } else {
                                Ksoup.parse(notification.content?.text ?: "").text()
                            }
                        if (displayText != "") {
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
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            text = sourceText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun MobileNotificationCategory.icon(): ImageVector = when (this) {
    MobileNotificationCategory.Comment -> Icons.AutoMirrored.Outlined.Comment
    MobileNotificationCategory.Like -> Icons.Filled.Favorite
    MobileNotificationCategory.Favorite -> Icons.Filled.Bookmark
    MobileNotificationCategory.Follow -> Icons.Filled.PersonAddAlt1
}

private fun MobileNotificationTimelineItem.displayTitle(): String =
    content
        ?.title
        ?.takeIf { it.isNotBlank() }
        ?: detailTitle.takeIf { it.isNotBlank() }
        ?: target?.name?.takeIf { it.isNotBlank() }
        ?: "通知"

/**
 * 数据字段中的 `sub_title`，主要是行为的动作
 */
private fun MobileNotificationTimelineItem.displaySubtitle(): String {
    val subtitle = content
        ?.subTitle
        ?.takeIf { it.isNotBlank() }
        ?: ""
    if (!subtitle.endsWith("：") && subtitle.startsWith("评论了") || subtitle.startsWith("赞同了") || subtitle.startsWith("喜欢了")) {
        return "$subtitle："
    }
    return subtitle
}

private fun MobileNotificationTimelineItem.sourceText(): String =
    listOfNotNull(
        targetSource
            ?.text
            ?.takeIf { it.isNotBlank() },
        targetSource
            ?.subText
            ?.takeIf { it.isNotBlank() },
    ).joinToString("\n")

private fun MobileNotificationTimelineItem.avatarUrl(): String =
    head
        ?.avatarUrl
        ?.takeIf { it.isNotBlank() }
        ?: head?.author?.avatarUrl?.takeIf { it.isNotBlank() }
        ?: target?.avatarUrl?.takeIf { it.isNotBlank() }
        ?: content?.subIcon?.takeIf { it.isNotBlank() }
        ?: ""

private fun MobileNotificationTimelineItem.navDestination(): NavDestination? {
    target
        ?.takeIf { it.type == "people" && (it.urlToken.isNotBlank() || it.id.isNotBlank()) }
        ?.let {
            return Person(
                id = it.id.ifBlank { Person.EMPTY_ID },
                urlToken = it.urlToken,
                name = it.name.ifBlank { "loading..." },
            )
        }
    return listOf(
        content?.targetLink,
        content?.subTargetLink,
        targetSource?.targetLink,
        head?.targetLink,
    ).firstNotNullOfOrNull { link ->
        link?.takeIf { it.isNotBlank() }?.let(::resolveContent)
    }
}

private fun formatUnreadCount(count: Int): String =
    if (count > 99) {
        "99+"
    } else {
        count.toString()
    }
