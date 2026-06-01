/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import android.content.ClipData
import android.content.Context
import android.widget.Toast
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
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.data.NotificationItem
import com.github.zly2006.zhihu.data.NotificationTarget
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MiuixNotificationScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel = viewModel<NotificationViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    val blurEnabled = remember { mutableStateOf(preferences.getBoolean("blurEnabled", true)) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled.value)
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (viewModel.allData.isEmpty()) {
            viewModel.refresh(context)
        }
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
                                viewModel.markAllAsRead(context)
                                Toast.makeText(context, "已全部标记为已读", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.MarkChatRead, "已读", tint = MiuixTheme.colorScheme.onBackground)
                        }
                    }
                    IconButton(onClick = { navigator.onNavigate(Notification.NotificationSettings) }) {
                        Icon(Icons.Default.Settings, "设置", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        PullToRefresh(
            isRefreshing = viewModel.isLoading,
            onRefresh = { coroutineScope.launch { viewModel.refresh(context) } },
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
        ) {
            Box(
                modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding(),
                    ),
                ) {
                    items(viewModel.allData, key = { it.id }) { notification ->
                        if (viewModel.shouldShowNotification(context, notification)) {
                            NotificationItemCard(
                                notification = notification,
                                onClick = {
                                    viewModel.markAsRead(context, notification.id)
                                    when (notification.target) {
                                        is NotificationTarget.Comment -> {
                                            Toast.makeText(context, "暂不支持跳转到评论，将跳转到对应回答。", Toast.LENGTH_LONG).show()
                                            notification.target.target?.navDestination?.let {
                                                navigator.onNavigate(it)
                                            } ?: Toast.makeText(context, "导航失败", Toast.LENGTH_LONG).show()
                                        }
                                        is NotificationTarget.Question -> {
                                            navigator.onNavigate(Question(notification.target.id.toLong(), notification.target.title))
                                        }
                                        is NotificationTarget.People -> {
                                            navigator.onNavigate(Person(notification.target.id, notification.target.urlToken, name = notification.target.name))
                                        }
                                        is NotificationTarget.Answer -> {
                                            navigator.onNavigate(Article(
                                                title = notification.target.title,
                                                type = ArticleType.Answer,
                                                id = notification.target.id.toLong(),
                                                excerpt = notification.target.excerpt,
                                            ))
                                        }
                                        is NotificationTarget.Article -> {
                                            navigator.onNavigate(Article(
                                                title = notification.target.title,
                                                type = ArticleType.Article,
                                                id = notification.target.id.toLong(),
                                                excerpt = notification.target.excerpt,
                                            ))
                                        }
                                        null -> {}
                                    }
                                },
                            )
                        }
                    }
                    if (!viewModel.isEnd) {
                        item {
                            LaunchedEffect(Unit) { viewModel.loadMore(context) }
                        }
                    }
                }

                if (BuildConfig.DEBUG) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        IconButton(
                            onClick = {
                                val data = Json.encodeToString(viewModel.debugData)
                                val clip = ClipData.newPlainText("data", data)
                                context.clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "已复制调试数据", Toast.LENGTH_SHORT).show()
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
private fun NotificationItemCard(
    notification: NotificationItem,
    onClick: () -> Unit,
) {
    val bgColor = if (notification.isRead) Color.Transparent
    else MiuixTheme.colorScheme.primary.copy(alpha = 0.08f)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(bgColor, RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    if (notification.content.extend?.icon != null) {
                        AsyncImage(
                            model = notification.content.extend.icon,
                            contentDescription = "",
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(MiuixTheme.colorScheme.surface),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = buildNotificationText(notification),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (!notification.isRead) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = "未读",
                        modifier = Modifier.size(8.dp),
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            }

            notification.target?.let { target ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        target.title?.let { title ->
                            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        target.content?.let { content ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = content.replace(Regex("<[^>]*>"), ""),
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(formatTime(notification.createTime), fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

@Composable
private fun buildNotificationText(notification: NotificationItem) = buildAnnotatedString {
    val content = notification.content
    if (content.actors.isNotEmpty()) {
        content.actors.forEachIndexed { index, actor ->
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(actor.name) }
            if (index < content.actors.size - 1) append("、")
        }
        append(" ")
    }
    if (notification.mergeCount > 1 && content.actors.size != notification.mergeCount) {
        append(" 等${notification.mergeCount}人")
    }
    append(content.verb)
    content.target?.let { target ->
        append(" ")
        withStyle(style = SpanStyle(color = MiuixTheme.colorScheme.primary)) { append(target.text) }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp
    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> "${diff / 3600}小时前"
        diff < 604800 -> "${diff / 86400}天前"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(timestamp * 1000))
    }
}
