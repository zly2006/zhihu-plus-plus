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
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.ZhihuPrivateMessage
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.platform.rememberZhihuWebUrlOpener
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.util.formatFullDateTime
import com.github.zly2006.zhihu.util.formatRelativeTime
import com.github.zly2006.zhihu.viewmodel.PrivateMessageViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun PrivateMessageScreen(destination: Notification.Message) {
    val navigator = LocalNavigator.current
    val environment = rememberNotificationEnvironment(rememberNotificationSettingsStore())
    val viewModel = viewModel(key = "private_message_${destination.peerId}") {
        PrivateMessageViewModel(destination.peerId)
    }
    val peerName = viewModel.peer?.name?.ifBlank { destination.name } ?: destination.name
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val lifecycleOwner = LocalLifecycleOwner.current
    val selfAvatarUrl = rememberAccountSettingsAccountState().value.avatarUrl.orEmpty()
    val listState = rememberLazyListState()
    var draft by rememberSaveable(destination.peerId) { mutableStateOf("") }
    var nowEpochSeconds by remember { mutableLongStateOf(Clock.System.now().epochSeconds) }

    LaunchedEffect(viewModel, listState) {
        var previousLatestId = viewModel.allData.firstOrNull()?.stableId
        snapshotFlow { viewModel.allData.firstOrNull()?.stableId }.collect { latestId ->
            if (latestId != null && latestId != previousLatestId && !listState.isScrollInProgress) {
                // 倒序列表插入消息后可能已按 key 锚定旧首项；仍贴着旧最新消息才继续跟随底部。
                val anchoredToPreviousLatest = previousLatestId != null &&
                    listState.layoutInfo.visibleItemsInfo
                        .firstOrNull()
                        ?.key == previousLatestId &&
                    listState.firstVisibleItemScrollOffset == 0
                if (!listState.canScrollBackward || anchoredToPreviousLatest) {
                    listState.requestScrollToItem(0)
                }
            }
            previousLatestId = latestId
        }
    }

    LaunchedEffect(lifecycleOwner, destination.peerId, environment) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            nowEpochSeconds = Clock.System.now().epochSeconds
            if (viewModel.allData.isEmpty()) {
                viewModel.refresh(environment)
            }
            while (true) {
                delay(PRIVATE_MESSAGE_POLL_INTERVAL_MILLIS)
                nowEpochSeconds = Clock.System.now().epochSeconds
                viewModel.refreshLatest(environment)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = peerName.ifBlank { "私信" },
                        fontWeight = FontWeight.SemiBold,
                    )
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
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("private_message_input"),
                        placeholder = { Text("发私信") },
                        enabled = !viewModel.isSending,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                    )
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            Icons.Outlined.SentimentSatisfied,
                            contentDescription = "表情（暂不可用）",
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                    IconButton(
                        onClick = {
                            val content = draft
                            coroutineScope.launch {
                                if (viewModel.sendMessage(content, environment)) {
                                    draft = ""
                                    listState.requestScrollToItem(0)
                                } else {
                                    userMessages.showMessage(viewModel.errorMessage ?: "发送失败")
                                }
                            }
                        },
                        enabled = draft.isNotBlank() && !viewModel.isSending,
                        modifier = Modifier.testTag("private_message_send"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "发送",
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
                // 保留列表淡入，由新消息高度连续变化带动旧消息让位，避免独立的位置动画造成重叠。
                animateItemPlacement = false,
                listState = listState,
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxSize(),
                footer = ProgressIndicatorFooter,
                key = { it.stableId },
            ) { message ->
                var appeared by rememberSaveable(message.stableId) { mutableStateOf(false) }
                val visibility = remember(message.stableId) {
                    MutableTransitionState(appeared).apply { targetState = true }
                }
                LaunchedEffect(message.stableId) { appeared = true }
                AnimatedVisibility(
                    visibleState = visibility,
                    modifier = Modifier.clipToBounds(),
                    enter = slideInVertically(tween(240)) { it } +
                        expandVertically(tween(240), expandFrom = Alignment.Bottom, clip = true),
                ) {
                    PrivateMessageBubble(
                        message = message,
                        incoming = message.sender?.let { sender ->
                            sender.id == destination.peerId || sender.urlToken == destination.peerId
                        } == true,
                        selfAvatarUrl = selfAvatarUrl,
                        nowEpochSeconds = nowEpochSeconds,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivateMessageBubble(
    message: ZhihuPrivateMessage,
    incoming: Boolean,
    selfAvatarUrl: String,
    nowEpochSeconds: Long,
) {
    val navigator = LocalNavigator.current
    val openWebUrl = rememberZhihuWebUrlOpener()
    val linkColor = MaterialTheme.colorScheme.primary
    val displayText = remember(message.content, message.plugin, linkColor, navigator, openWebUrl) {
        message.displayContent(linkColor) { url ->
            resolveContent(url)?.let(navigator.onNavigate) ?: openWebUrl(url)
        }
    }
    var showFullTime by rememberSaveable(message.stableId) { mutableStateOf(false) }
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
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (incoming) Alignment.Start else Alignment.End,
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = if (incoming) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = maxWidth * 0.88f),
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
                    SelectionContainer {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .commentSelectionWorkaround()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
            Text(
                text = if (showFullTime) {
                    formatFullDateTime(message.createdTime)
                } else {
                    formatRelativeTime(message.createdTime, nowEpochSeconds)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .testTag("private_message_time_${message.stableId}")
                    .clickable { showFullTime = !showFullTime }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
        if (!incoming) {
            Spacer(Modifier.width(8.dp))
            AsyncImage(
                model = selfAvatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

private const val PRIVATE_MESSAGE_POLL_INTERVAL_MILLIS = 5_000L
