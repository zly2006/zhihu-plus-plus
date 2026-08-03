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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.data.ZhihuPrivateMessage
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.util.formatRelativeTime
import com.github.zly2006.zhihu.viewmodel.PrivateMessageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateMessageScreen(destination: Notification.Message) {
    val navigator = LocalNavigator.current
    val environment = rememberNotificationEnvironment(rememberNotificationSettingsStore())
    val viewModel = viewModel(key = "private_message_${destination.peerId}") {
        PrivateMessageViewModel(destination.peerId)
    }
    val peerName = viewModel.peer?.name?.ifBlank { destination.name } ?: destination.name
    val peerAvatar = viewModel.peer?.avatarUrl?.ifBlank { destination.avatarUrl } ?: destination.avatarUrl

    LaunchedEffect(destination.peerId) {
        if (viewModel.allData.isEmpty()) {
            viewModel.refresh(environment)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (peerAvatar.isNotBlank()) {
                            AsyncImage(
                                model = peerAvatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = peerName.ifBlank { "私信" },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Text(
                            text = "私信仅供查看",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            Icons.Outlined.SentimentSatisfied,
                            contentDescription = "表情（查看模式不可用）",
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            Icons.Outlined.AddCircleOutline,
                            contentDescription = "附件（查看模式不可用）",
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
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
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxSize(),
                footer = ProgressIndicatorFooter,
                key = { it.stableId },
            ) { message ->
                PrivateMessageBubble(
                    message = message,
                    incoming = message.sender?.let { sender ->
                        sender.id == destination.peerId || sender.urlToken == destination.peerId
                    } == true,
                )
            }
        }
    }
}

@Composable
private fun PrivateMessageBubble(
    message: ZhihuPrivateMessage,
    incoming: Boolean,
) {
    val displayText = message.plugin?.excerpt?.takeIf { it.isNotBlank() }
        ?: message.content.takeIf { it.isNotBlank() }?.let { Ksoup.parse(it).text() }
        ?: "暂不支持显示这条消息"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("private_message_${message.stableId}")
            .padding(horizontal = 14.dp, vertical = 5.dp),
        horizontalArrangement = if (incoming) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (incoming) {
            AsyncImage(
                model = message.sender?.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(
            horizontalAlignment = if (incoming) Alignment.Start else Alignment.End,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.78f),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (incoming) 4.dp else 16.dp,
                    bottomEnd = if (incoming) 16.dp else 4.dp,
                ),
                color = if (incoming) {
                    MaterialTheme.colorScheme.surfaceContainerLow
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
            Text(
                text = formatRelativeTime(message.createdTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}
