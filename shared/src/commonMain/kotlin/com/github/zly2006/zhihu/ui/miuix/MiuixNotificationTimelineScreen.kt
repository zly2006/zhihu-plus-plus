/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.data.MobileNotificationTimelineItem
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.INVITATION_ENTRY_NAME
import com.github.zly2006.zhihu.ui.avatarUrl
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.displayTitle
import com.github.zly2006.zhihu.ui.invitationTime
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.miuix.components.MiuixListLoadingIndicator
import com.github.zly2006.zhihu.ui.navDestination
import com.github.zly2006.zhihu.ui.notificationListDate
import com.github.zly2006.zhihu.ui.rememberNotificationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationTimelineViewModel
import top.yukonga.miuix.kmp.basic.Button
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
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 通知分类详情页（含「邀请回答」）的 miuix 版本，对标 M3
 * [com.github.zly2006.zhihu.ui.NotificationTimelineScreen]。
 *
 * 分类逻辑、过滤开关和跳转目标完全复用 [NotificationTimelineViewModel] 与共享扩展，这里只换外壳与行样式。
 */
@Composable
fun MiuixNotificationTimelineScreen(
    entryName: String,
    title: String,
) {
    val navigator = LocalNavigator.current
    val settings = rememberSettingsStore()
    val settingsStore = rememberNotificationSettingsStore()
    val environment = rememberNotificationEnvironment(settingsStore)
    val userMessages = rememberUserMessageSink()
    val viewModel = viewModel(key = "notification_timeline_$entryName") {
        NotificationTimelineViewModel(entryName)
    }
    val invitations = entryName == INVITATION_ENTRY_NAME
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    // 区分「下拉刷新」和「首次加载」：下拉时不再叠一个中心转圈（与 MiuixNotificationScreen 一致）。
    var isManualRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(entryName) {
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
                title = title,
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
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
                    modifier = Modifier
                        .fillMaxSize()
                        .overScrollVertical()
                        .scrollEndHaptic()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 6.dp,
                        bottom = padding.calculateBottomPadding() + 12.dp,
                    ),
                    key = { it.stableId },
                ) { notification ->
                    when {
                        notification.type == "empty" -> {
                            Text(
                                text = notification.emptyInfo?.text.orEmpty(),
                                style = MiuixTheme.textStyles.body1,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }

                        invitations -> MiuixInvitationAnswerItem(
                            notification = notification,
                            onQuestionClick = {
                                notification.target?.id?.toLongOrNull()?.let { questionId ->
                                    navigator.onNavigate(
                                        Question(
                                            questionId = questionId,
                                            title = notification.target.title.ifBlank {
                                                notification.targetSource?.text.orEmpty()
                                            },
                                        ),
                                    )
                                } ?: userMessages.showMessage("无法打开这个问题")
                            },
                            onAnswerClick = {
                                val target = notification.target
                                val destination = target
                                    ?.myAnswerUrl
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let(::resolveContent)
                                if (destination != null) {
                                    navigator.onNavigate(destination)
                                } else {
                                    target?.id?.toLongOrNull()?.let { questionId ->
                                        navigator.onNavigate(
                                            WriteAnswer(
                                                questionId = questionId,
                                                questionTitle = target.title.ifBlank {
                                                    notification.targetSource?.text.orEmpty()
                                                },
                                            ),
                                        )
                                    } ?: userMessages.showMessage("无法回答这个问题")
                                }
                            },
                        )

                        viewModel.shouldShowNotification(settingsStore, notification) -> {
                            MiuixNotificationTimelineRow(
                                notification = notification,
                                onClick = {
                                    notification.navDestination()?.let(navigator.onNavigate)
                                        ?: userMessages.showMessage("暂不支持打开此通知")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 普通通知行：未读用主色底标出，其余信息与 M3 NotificationItemView 一致。 */
@Composable
private fun MiuixNotificationTimelineRow(
    notification: MobileNotificationTimelineItem,
    onClick: () -> Unit,
) {
    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notification_item_${notification.stableId}")
                .clickable(onClick = onClick)
                .background(
                    if (notification.isRead) {
                        MiuixTheme.colorScheme.surface
                    } else {
                        MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    },
                ).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiuixNotificationAvatar(notification.avatarUrl())
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = notification.displayTitle(),
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                notification.content?.text?.takeIf { it.isNotBlank() }?.let { text ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = text,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            notification.notificationListDate().takeIf { it.isNotBlank() }?.let { created ->
                Text(
                    text = created,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** 邀请回答行：上半是邀请人，下半是问题卡片 + 「写回答 / 查看回答」按钮（对齐 M3 InvitationAnswerItem）。 */
@Composable
private fun MiuixInvitationAnswerItem(
    notification: MobileNotificationTimelineItem,
    onQuestionClick: () -> Unit,
    onAnswerClick: () -> Unit,
) {
    val author = notification.head?.author
    val authorName = notification.content
        ?.title
        ?.takeIf { it.isNotBlank() }
        ?: author?.name?.takeIf { it.isNotBlank() }
        ?: notification.detailTitle
    val rewardText = notification.additionalInfo
        .firstOrNull()
        ?.text
        ?.takeIf { it.isNotBlank() }
        ?.let { Ksoup.parse(it).text() }
        .orEmpty()
    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("invitation_${notification.stableId}")
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiuixNotificationAvatar(notification.avatarUrl(), size = 44)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = authorName,
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        notification.head?.labels?.firstOrNull()?.text?.takeIf { it.isNotBlank() }?.let { label ->
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MiuixTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = listOf(notification.content?.subTitle.orEmpty(), notification.invitationTime())
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onQuestionClick)
                    .padding(14.dp),
            ) {
                if (rewardText.isNotBlank()) {
                    Text(
                        text = rewardText,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = notification.target
                        ?.title
                        ?.ifBlank { notification.targetSource?.text.orEmpty() }
                        .orEmpty(),
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = notification.targetSource?.subText.orEmpty(),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = onAnswerClick,
                        modifier = Modifier.testTag("invitation_answer_${notification.stableId}"),
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (notification.target?.hasAnswer == true) "查看回答" else "写回答")
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixNotificationAvatar(avatarUrl: String, size: Int = 52) {
    val modifier = Modifier
        .size(size.dp)
        .clip(CircleShape)
        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
    if (avatarUrl.isBlank()) {
        Box(modifier)
    } else {
        AsyncImage(model = avatarUrl, contentDescription = null, modifier = modifier)
    }
}
