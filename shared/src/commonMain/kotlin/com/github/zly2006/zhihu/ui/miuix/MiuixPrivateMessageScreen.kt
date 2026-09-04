/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.ZhihuPrivateMessage
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.platform.rememberZhihuWebUrlOpener
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.commentSelectionWorkaround
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.displayContent
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.miuix.components.MiuixListLoadingIndicator
import com.github.zly2006.zhihu.ui.rememberNotificationEnvironment
import com.github.zly2006.zhihu.util.formatRelativeTime
import com.github.zly2006.zhihu.viewmodel.PrivateMessageViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 私信会话页的 miuix 版本，对标 M3 [com.github.zly2006.zhihu.ui.PrivateMessageScreen]。
 *
 * 收发、分页与富文本解析全部复用 [PrivateMessageViewModel] 和共享的 [displayContent]；这里只换外壳、
 * 气泡配色与输入栏控件。草稿仍由 [rememberSaveable] 按 peerId 保存，返回再进来不丢。
 */
@Composable
fun MiuixPrivateMessageScreen(destination: Notification.Message) {
    val navigator = LocalNavigator.current
    val environment = rememberNotificationEnvironment(rememberNotificationSettingsStore())
    val viewModel = viewModel(key = "private_message_${destination.peerId}") {
        PrivateMessageViewModel(destination.peerId)
    }
    val peerName = viewModel.peer?.name?.ifBlank { destination.name } ?: destination.name
    val peerAvatar = viewModel.peer?.avatarUrl?.ifBlank { destination.avatarUrl } ?: destination.avatarUrl
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val settings = rememberSettingsStore()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    var draft by rememberSaveable(destination.peerId) { mutableStateOf("") }
    var isManualRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(destination.peerId) {
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
                title = peerName.ifBlank { "私信" },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (peerAvatar.isNotBlank()) {
                        AsyncImage(
                            model = peerAvatar,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 140.dp)
                        .testTag("private_message_input"),
                    label = "发私信",
                    useLabelAsPlaceholder = true,
                    enabled = !viewModel.isSending,
                    maxLines = 4,
                )
                IconButton(
                    onClick = {
                        val content = draft
                        coroutineScope.launch {
                            if (viewModel.sendMessage(content, environment)) {
                                draft = ""
                            } else {
                                userMessages.showMessage(viewModel.errorMessage ?: "发送失败")
                            }
                        }
                    },
                    enabled = draft.isNotBlank() && !viewModel.isSending,
                    modifier = Modifier.size(40.dp).testTag("private_message_send"),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "发送",
                        tint = if (draft.isNotBlank()) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    )
                }
            }
        },
    ) { padding ->
        PullToRefresh(
            isRefreshing = isManualRefreshing && viewModel.isLoading,
            onRefresh = {
                isManualRefreshing = true
                viewModel.refresh(environment)
            },
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
        ) {
            Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
                MiuixListLoadingIndicator(
                    isLoading = viewModel.isLoading,
                    isEmpty = viewModel.allData.isEmpty(),
                    isPullToRefresh = isManualRefreshing,
                )
                PaginatedList(
                    items = viewModel.allData,
                    listState = listState,
                    onLoadMore = { viewModel.loadMore(environment) },
                    isEnd = { viewModel.isEnd },
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .overScrollVertical()
                        .scrollEndHaptic()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 12.dp,
                        bottom = padding.calculateBottomPadding() + 12.dp,
                    ),
                    key = { it.stableId },
                ) { message ->
                    MiuixPrivateMessageBubble(
                        message = message,
                        incoming = message.sender?.let { sender ->
                            sender.id == destination.peerId || sender.urlToken == destination.peerId
                        } == true,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixPrivateMessageBubble(
    message: ZhihuPrivateMessage,
    incoming: Boolean,
) {
    val navigator = LocalNavigator.current
    val openWebUrl = rememberZhihuWebUrlOpener()
    val linkColor = MiuixTheme.colorScheme.primary
    val displayText = remember(message.content, message.plugin, linkColor, navigator, openWebUrl) {
        message.displayContent(linkColor) { url ->
            resolveContent(url)?.let(navigator.onNavigate) ?: openWebUrl(url)
        }
    }
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
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh),
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (incoming) Alignment.Start else Alignment.End,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (incoming) 1f else 0.88f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (incoming) 4.dp else 16.dp,
                            bottomEnd = if (incoming) 16.dp else 4.dp,
                        ),
                    ).background(
                        if (incoming) {
                            MiuixTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MiuixTheme.colorScheme.primaryContainer
                        },
                    ),
            ) {
                SelectionContainer {
                    Text(
                        text = displayText,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .commentSelectionWorkaround()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
            Text(
                text = formatRelativeTime(message.createdTime),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}
