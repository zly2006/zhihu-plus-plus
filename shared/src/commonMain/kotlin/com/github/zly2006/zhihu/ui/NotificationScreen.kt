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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.NotificationItem
import com.github.zly2006.zhihu.shared.data.NotificationTarget
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageDuration
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.formatRelativeTime
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.viewmodel.NotificationPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class NotificationScreenData(
    val notifications: List<NotificationItem>,
    val totalItemCount: Int,
    val unreadCount: Int,
    val isLoading: Boolean,
    val isEnd: Boolean,
    val showDebugCopy: Boolean,
    val refresh: () -> Unit,
    val loadMore: () -> Unit,
    val markAsRead: (String) -> Unit,
    val markAllAsRead: () -> Unit,
    val copyDebugData: () -> Unit,
    val showMessage: (String) -> Unit,
)

data class NotificationScreenRuntime(
    val environment: NotificationPaginationEnvironment,
    val showDebugCopy: Boolean,
    val copyDebugText: (label: String, text: String) -> Unit,
)

@Composable
expect fun rememberNotificationScreenRuntime(
    viewModel: NotificationViewModel,
    settingsStore: NotificationSettingsStore,
): NotificationScreenRuntime

@Composable
fun rememberNotificationScreenData(): NotificationScreenData {
    val settingsStore = rememberNotificationSettingsStore()
    val viewModel = viewModel<NotificationViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val runtime = rememberNotificationScreenRuntime(viewModel, settingsStore)
    return NotificationScreenData(
        notifications = viewModel.allData.filter { viewModel.shouldShowNotification(settingsStore, it) },
        totalItemCount = viewModel.allData.size,
        unreadCount = viewModel.unreadCount,
        isLoading = viewModel.isLoading,
        isEnd = viewModel.isEnd,
        showDebugCopy = runtime.showDebugCopy,
        refresh = { viewModel.refresh(runtime.environment) },
        loadMore = { viewModel.loadMore(runtime.environment) },
        markAsRead = { id -> viewModel.markAsRead(id) },
        markAllAsRead = {
            coroutineScope.launch {
                viewModel.markAllAsRead(runtime.environment)
                userMessages.showMessage("已全部标记为已读")
            }
        },
        copyDebugData = {
            val debugData = Json.encodeToString(viewModel.debugData)
            runtime.copyDebugText("data", debugData)
            userMessages.showMessage("已复制调试数据")
        },
        showMessage = { message ->
            userMessages.showMessage(message, UserMessageDuration.Long)
        },
    )
}

@Composable
fun NotificationDebugCopyButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
    if (!visible) {
        return
    }
    DraggableRefreshButton(
        onClick = onClick,
        preferenceName = "copyAll",
    ) {
        Icon(Icons.Default.CopyAll, contentDescription = "复制")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {
    val navigator = LocalNavigator.current
    val data = rememberNotificationScreenData()
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(data.notifications.size, data.totalItemCount, data.isEnd, data.isLoading) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisibleIndex >= layoutInfo.totalItemsCount - 3 &&
                data.totalItemCount > 0 &&
                !data.isEnd &&
                !data.isLoading
        }
    }

    LaunchedEffect(Unit) {
        if (data.totalItemCount == 0) {
            data.refresh()
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            data.loadMore()
        }
    }

    LaunchedEffect(data.totalItemCount, data.notifications.size, data.isEnd, data.isLoading) {
        if (data.totalItemCount > 0 && data.notifications.isEmpty() && !data.isEnd && !data.isLoading) {
            data.loadMore()
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
                        Text("通知")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (data.unreadCount > 0) {
                        IconButton(onClick = data.markAllAsRead) {
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
            isRefreshing = data.isLoading,
            onRefresh = data.refresh,
            modifier = Modifier.padding(paddingValues),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(data.notifications, key = { it.id }) { notification ->
                    NotificationItemView(
                        notification = notification,
                        onClick = {
                            data.markAsRead(notification.id)
                            when (val target = notification.target) {
                                is NotificationTarget
                                    .Comment,
                                -> {
                                    data.showMessage("暂不支持跳转到评论，将跳转到对应回答。")
                                    target.target?.navDestination?.let {
                                        navigator.onNavigate(it)
                                    } ?: data.showMessage("导航失败")
                                }

                                is NotificationTarget.Question -> {
                                    navigator.onNavigate(Question(target.id.toLong(), target.title))
                                }

                                is NotificationTarget.People -> {
                                    navigator.onNavigate(Person(target.id, target.urlToken, name = target.name))
                                }

                                is NotificationTarget.Answer -> {
                                    navigator.onNavigate(
                                        Article(
                                            title = target.title,
                                            type = ArticleType.Answer,
                                            id = target.id.toLong(),
                                            excerpt = target.excerpt,
                                        ),
                                    )
                                }
                                is NotificationTarget.Article -> {
                                    navigator.onNavigate(
                                        Article(
                                            title = target.title,
                                            type = ArticleType.Article,
                                            id = target.id.toLong(),
                                            excerpt = target.excerpt,
                                        ),
                                    )
                                }

                                null -> { }
                            }
                        },
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (data.isEnd) {
                            Text("已经到底啦")
                        } else if (data.isLoading) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
            NotificationDebugCopyButton(
                visible = data.showDebugCopy,
                onClick = data.copyDebugData,
            )
        }
    }
}

@Composable
fun NotificationItemView(
    notification: NotificationItem,
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
            // 头部：头像和时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    val extend = notification.content.extend
                    if (extend?.icon != null) {
                        AsyncImage(
                            model = extend.icon,
                            contentDescription = "",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    // 通知内容
                    Text(
                        text = buildNotificationText(notification),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 未读标记
                if (!notification.isRead) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = "未读",
                        modifier = Modifier.size(8.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 目标内容预览
            notification.target?.let { target ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        target.title?.let { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        target.content?.let { content ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = content.replace(Regex("<[^>]*>"), ""), // 简单去除HTML标签
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 时间
            Text(
                text = formatRelativeTime(notification.createTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 构建通知文本
 */
@Composable
private fun buildNotificationText(notification: NotificationItem) = buildAnnotatedString {
    val content = notification.content

    // 显示actors
    if (content.actors.isNotEmpty()) {
        content.actors.forEachIndexed { index, actor ->
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(actor.name)
            }
            if (index < content.actors.size - 1) {
                append("、")
            }
        }
        append(" ")
    }

    if (notification.mergeCount > 1 && content.actors.size != notification.mergeCount) {
        append(" 等${notification.mergeCount}人")
    }

    // 显示动作
    append(content.verb)

    // 显示目标
    content.target?.let { target ->
        append(" ")
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append(target.text)
        }
    }
}
