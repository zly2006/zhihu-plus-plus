package com.github.zly2006.zhihu.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class NotificationType(
    val displayName: String,
    val defaultValue: Boolean,
    val regex: Regex,
) {
    LIKE_ANSWER("喜欢了你的回答", true, Regex("喜欢了你的回答")),
    LIKE_COMMENT("喜欢了你的评论", true, Regex("喜欢了.*你的评论")),
    REPLY_COMMENT("回复了你的评论", true, Regex("回复了.*你的评论")),
    INVITE_ANSWER("邀请你回答问题", false, Regex("\\s?(邀请你回答问题|的提问等你来答|邀请你回答)")),
}

object NotificationPreferences {
    private const val PREF_NAME = "notification_settings"
    private const val KEY_SYSTEM_NOTIFICATION = "system_notification_"
    private const val KEY_DISPLAY_IN_APP = "display_in_app_"

    fun getSystemNotificationEnabled(context: Context, type: NotificationType): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("${KEY_SYSTEM_NOTIFICATION}${type.name}", false)
    }

    fun setSystemNotificationEnabled(context: Context, type: NotificationType, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${KEY_SYSTEM_NOTIFICATION}${type.name}", enabled).apply()
    }

    fun getDisplayInAppEnabled(context: Context, type: NotificationType): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("${KEY_DISPLAY_IN_APP}${type.name}", type.defaultValue)
    }

    fun setDisplayInAppEnabled(context: Context, type: NotificationType, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${KEY_DISPLAY_IN_APP}${type.name}", enabled).apply()
    }

    fun matchNotificationType(verb: String): NotificationType? = NotificationType.entries.find { it.regex.matches(verb) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    var systemNotificationSettings by remember {
        mutableStateOf(
            NotificationType.entries.associateWith {
                NotificationPreferences.getSystemNotificationEnabled(context, it)
            },
        )
    }

    var displayInAppSettings by remember {
        mutableStateOf(
            NotificationType.entries.associateWith {
                NotificationPreferences.getDisplayInAppEnabled(context, it)
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // 系统通知设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "系统通知",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "选择哪些消息会发送系统通知",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NotificationType.entries.forEach { type ->
                        NotificationSettingItem(
                            label = type.displayName,
                            checked = systemNotificationSettings[type] ?: true,
                            onCheckedChange = { checked ->
                                systemNotificationSettings = systemNotificationSettings.toMutableMap().apply {
                                    put(type, checked)
                                }
                                NotificationPreferences.setSystemNotificationEnabled(context, type, checked)
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 应用内显示设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "应用内显示",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "选择在通知页面显示哪些通知",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NotificationType.entries.forEach { type ->
                        NotificationSettingItem(
                            label = type.displayName,
                            checked = displayInAppSettings[type] ?: true,
                            onCheckedChange = { checked ->
                                displayInAppSettings = displayInAppSettings.toMutableMap().apply {
                                    put(type, checked)
                                }
                                NotificationPreferences.setDisplayInAppEnabled(context, type, checked)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
