package com.github.zly2006.zhihu.ui

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.NotificationSettings
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.NotificationItem
import com.github.zly2006.zhihu.data.NotificationTarget
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel,
    onBack: () -> Unit,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (viewModel.allData.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    fun shouldShowNotification(notification: NotificationItem): Boolean {
        val verb = notification.content.verb
        val type = NotificationPreferences.matchNotificationType(verb)
        return if (type != null) {
            NotificationPreferences.getDisplayInAppEnabled(context, type)
        } else {
            true
        }
    }

    Scaffold(
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (viewModel.unreadCount > 0) {
                        IconButton(onClick = {
                            viewModel.markAllAsRead(context)
                        }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "全部已读")
                        }
                    }
                    IconButton(onClick = {
                        viewModel.refresh(context)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = {
                        onNavigate(NotificationSettings)
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading,
            onRefresh = { viewModel.refresh(context) },
            modifier = Modifier.padding(paddingValues),
        ) {
            PaginatedList(
                items = viewModel.allData,
                onLoadMore = { viewModel.loadMore(context) },
                isEnd = { viewModel.isEnd },
                modifier = Modifier.fillMaxSize(),
                footer = ProgressIndicatorFooter,
            ) { notification ->
                if (shouldShowNotification(notification)) {
                    NotificationItemView(
                        notification = notification,
                        onClick = {
                            viewModel.markAsRead(context, notification.id)
                            // 处理点击事件 - 跳转到对应内容
                            when (notification.target) {
                                is NotificationTarget.Comment -> {
                                    Toast.makeText(context, "暂不支持跳转到评论，将跳转到对应回答。", Toast.LENGTH_LONG).show()
                                    notification.target.target?.navDestination?.let {
                                        onNavigate(it)
                                    } ?: Toast.makeText(context, "导航失败", Toast.LENGTH_LONG).show()
                                }

                                is NotificationTarget.Question -> {
                                    onNavigate(Question(notification.target.id.toLong(), notification.target.title))
                                }

                                null -> { }
                            }
                        },
                    )
                }
            }
            if (BuildConfig.DEBUG) {
                DraggableRefreshButton(
                    onClick = {
                        val data = Json.encodeToString(viewModel.debugData)
                        val clip = ClipData.newPlainText("data", data)
                        context.clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制调试数据", Toast.LENGTH_SHORT).show()
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
                    if (notification.content.extend?.icon != null) {
                        AsyncImage(
                            model = notification.content.extend.icon,
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
                text = formatTime(notification.createTime),
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

/**
 * 格式化时间
 */
private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp

    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> "${diff / 3600}小时前"
        diff < 604800 -> "${diff / 86400}天前"
        else -> {
            val date = Date(timestamp * 1000)
            SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(date)
        }
    }
}
