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

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.ui.components.SettingItemGroup
import com.github.zly2006.zhihu.ui.components.SettingItemWithSwitch

enum class NotificationType(
    @StringRes val displayNameResId: Int,
    val defaultValue: Boolean,
    val regex: Regex,
) {
    LIKE_ANSWER(R.string.notification_type_like_answer, true, Regex("喜欢了你的回答")),
    LIKE_COMMENT(R.string.notification_type_like_comment, true, Regex("喜欢了.*你的评论")),
    REPLY_COMMENT(R.string.notification_type_reply_comment, true, Regex("回复了.*你的评论")),
    INVITE_ANSWER(R.string.notification_type_invite_answer, false, Regex("\\s?(邀请你回答问题|的提问等你来答|邀请你回答)")),
}

object NotificationPreferences {
    private const val PREF_NAME = "notification_settings"
    private const val KEY_SYSTEM_NOTIFICATION = "system_notification_"
    private const val KEY_DISPLAY_IN_APP = "display_in_app_"
    private const val KEY_AUTO_MARK_AS_READ = "auto_mark_notifications_read"

    fun getSystemNotificationEnabled(context: Context, type: NotificationType): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("${KEY_SYSTEM_NOTIFICATION}${type.name}", false)
    }

    fun setSystemNotificationEnabled(context: Context, type: NotificationType, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean("${KEY_SYSTEM_NOTIFICATION}${type.name}", enabled) }
    }

    fun getDisplayInAppEnabled(context: Context, type: NotificationType): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("${KEY_DISPLAY_IN_APP}${type.name}", type.defaultValue)
    }

    fun setDisplayInAppEnabled(context: Context, type: NotificationType, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean("${KEY_DISPLAY_IN_APP}${type.name}", enabled) }
    }

    fun getAutoMarkAsReadEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_MARK_AS_READ, true)
    }

    fun setAutoMarkAsReadEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_AUTO_MARK_AS_READ, enabled) }
    }

    fun matchNotificationType(verb: String): NotificationType? = NotificationType.entries.find { it.regex.matches(verb) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    innerPadding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
    var autoMarkAsRead by remember { mutableStateOf(NotificationPreferences.getAutoMarkAsReadEnabled(context)) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text(context.getString(R.string.notification_settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(vertical = 16.dp),
        ) {
            SettingItemGroup(
                title = context.getString(R.string.notification_reading_behavior),
                footer = { Text(context.getString(R.string.notification_auto_mark_read_desc)) },
            ) {
                SettingItemWithSwitch(
                    title = { Text(context.getString(R.string.notification_auto_mark_read)) },
                    checked = autoMarkAsRead,
                    onCheckedChange = { checked ->
                        autoMarkAsRead = checked
                        NotificationPreferences.setAutoMarkAsReadEnabled(context, checked)
                    },
                )
            }

            SettingItemGroup(title = context.getString(R.string.system_notifications)) {
                NotificationType.entries.forEach { type ->
                    SettingItemWithSwitch(
                        title = { Text(context.getString(type.displayNameResId)) },
                        checked = systemNotificationSettings[type] ?: false,
                        onCheckedChange = { checked ->
                            systemNotificationSettings = systemNotificationSettings.toMutableMap().apply {
                                put(type, checked)
                            }
                            NotificationPreferences.setSystemNotificationEnabled(context, type, checked)
                        },
                    )
                }
            }

            SettingItemGroup(
                title = context.getString(R.string.notification_display_in_app),
                footer = { Text(context.getString(R.string.notification_display_in_app_desc)) },
            ) {
                NotificationType.entries.forEach { type ->
                    SettingItemWithSwitch(
                        title = { Text(context.getString(type.displayNameResId)) },
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
