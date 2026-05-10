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

package com.github.zly2006.zhihu.ui.subscreens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.SettingItem
import com.github.zly2006.zhihu.ui.components.SettingItemGroup
import com.github.zly2006.zhihu.ui.components.SettingItemWithSwitch
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import com.github.zly2006.zhihu.util.ContinuousUsageReminderManager
import com.github.zly2006.zhihu.util.ContinuousUsageReminderPolicy
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import kotlinx.coroutines.launch

const val SYSTEM_AND_UPDATE_SETTINGS_SCROLL_TAG = "system_and_update_settings_scroll"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemAndUpdateSettingsScreen(
    innerPadding: PaddingValues,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val preferences = remember {
        context.getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE,
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text(context.getString(R.string.system_and_update)) },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = context.getString(R.string.back),
                        )
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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .testTag(SYSTEM_AND_UPDATE_SETTINGS_SCROLL_TAG)
                .padding(innerPadding)
                .padding(vertical = 16.dp),
        ) {
            val updateState by UpdateManager.updateState.collectAsState()
            val coroutineScope = rememberCoroutineScope()
            val showUpdateBanner = updateState is UpdateState.UpdateAvailable ||
                updateState is UpdateState.Downloading ||
                updateState is UpdateState.Downloaded

            var updateVersion: String by remember { mutableStateOf("") }
            var releaseNotes: String? by remember { mutableStateOf(null) }
            LaunchedEffect(updateState) {
                val state = updateState
                if (state is UpdateState.UpdateAvailable) {
                    updateVersion = state.version.toString()
                    releaseNotes = state.releaseNotes
                }
            }

            AnimatedVisibility(visible = showUpdateBanner) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp, 12.dp)) {
                        if (updateVersion.isNotEmpty()) {
                            Text(
                                text = "${context.getString(R.string.new_version_title)}\n$updateVersion",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        } else {
                            Text(
                                text = context.getString(R.string.new_version_detected),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }

                        if (releaseNotes != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp, 8.dp)) {
                                    Text(
                                        context.getString(R.string.release_notes),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )
                                    SelectionContainer {
                                        Text(
                                            buildAnnotatedString {
                                                val prRegex = Regex("https://github.com/zly2006/zhihu-plus-plus/pull/(\\d+)")
                                                var lastIndex = 0
                                                prRegex.findAll(releaseNotes!!).forEach { matchResult ->
                                                    append(releaseNotes!!.substring(lastIndex, matchResult.range.first))
                                                    val prNumber = matchResult.groupValues[1]
                                                    withLink(LinkAnnotation.Url("https://github.com/zly2006/zhihu-plus-plus/pull/$prNumber")) {
                                                        withStyle(
                                                            MaterialTheme.typography.bodyMedium
                                                                .copy(color = MaterialTheme.colorScheme.primary)
                                                                .toSpanStyle(),
                                                        ) {
                                                            append("#$prNumber")
                                                        }
                                                    }
                                                    lastIndex = matchResult.range.last + 1
                                                }
                                                append(releaseNotes!!.substring(lastIndex))
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    TextButton(
                                        onClick = { luoTianYiUrlLauncher(context, "https://github.com/zly2006/zhihu-plus-plus/releases".toUri()) },
                                        modifier = Modifier.align(Alignment.End),
                                    ) {
                                        Text(context.getString(R.string.view_full_changelog))
                                        Icon(
                                            Icons.Default.ArrowOutward,
                                            null,
                                            Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        val cnDownloadUrl = (updateState as? UpdateState.UpdateAvailable)?.cnDownloadUrl
                        if (!cnDownloadUrl.isNullOrBlank()) {
                            Button(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, cnDownloadUrl.toUri()))
                                    }.onFailure {
                                        UpdateManager.updateState.value =
                                            UpdateState.Error(it.message ?: context.getString(R.string.cannot_open_browser))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(context.getString(R.string.cn_download), Modifier.padding(0.dp, 4.dp))
                            }
                            Text(
                                context.getString(R.string.cn_download_desc),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    val state = updateState
                                    if (state is UpdateState.UpdateAvailable) {
                                        UpdateManager.skipVersion(context, state.version.toString())
                                        UpdateManager.updateState.value = UpdateState.Latest
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(context.getString(R.string.skip_version), Modifier.padding(0.dp, 4.dp))
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        when (val state = updateState) {
                                            is UpdateState.UpdateAvailable -> UpdateManager.downloadUpdate(context, state.downloadUrl)
                                            is UpdateState.Downloaded -> UpdateManager.installUpdate(context, state.file)
                                            else -> {}
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    when (updateState) {
                                        is UpdateState.UpdateAvailable -> context.getString(R.string.download_update)
                                        is UpdateState.Downloading -> context.getString(R.string.downloading)
                                        is UpdateState.Downloaded -> context.getString(R.string.install_update)
                                        else -> context.getString(R.string.download_update)
                                    },
                                    Modifier.padding(0.dp, 4.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Github Token
            var githubToken by remember { mutableStateOf(preferences.getString("githubToken", "") ?: "") }
            var showGithubToken by remember { mutableStateOf(false) }

            SettingItemGroup {
                SettingItem(
                    title = { Text(context.getString(R.string.github_token)) },
                    description = {
                        Text(
                            context.getString(R.string.github_token_desc),
                        )
                    },
                    bottomAction = {
                        OutlinedTextField(
                            value = githubToken,
                            onValueChange = {
                                githubToken = it
                                preferences.edit { putString("githubToken", it) }
                            },
                            visualTransformation = if (showGithubToken) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showGithubToken = !showGithubToken }) {
                                    Icon(
                                        imageVector = if (showGithubToken) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            singleLine = true,
                        )
                    },
                )

                var autoCheckUpdates by remember { mutableStateOf(UpdateManager.isAutoCheckEnabled(context)) }
                SettingItemWithSwitch(
                    title = { Text(context.getString(R.string.auto_check_updates)) },
                    description = { Text(context.getString(R.string.auto_check_updates_desc)) },
                    checked = autoCheckUpdates,
                    onCheckedChange = {
                        autoCheckUpdates = it
                        UpdateManager.setAutoCheckEnabled(context, it)
                        if (!it) {
                            UpdateManager.updateState.value = UpdateState.NoUpdate
                        }
                    },
                )

                var checkNightlyUpdates by remember { mutableStateOf(preferences.getBoolean("checkNightlyUpdates", false)) }
                SettingItemWithSwitch(
                    title = { Text(context.getString(R.string.check_nightly_updates)) },
                    description = { Text(context.getString(R.string.check_nightly_updates_desc)) },
                    checked = checkNightlyUpdates,
                    onCheckedChange = {
                        checkNightlyUpdates = it
                        preferences.edit { putBoolean("checkNightlyUpdates", it) }
                    },
                )

                var customLatestUrl by remember { mutableStateOf(UpdateManager.getCustomLatestUrl(context)) }
                SettingItem(
                    title = { Text(context.getString(R.string.update_latest_url)) },
                    description = { Text(context.getString(R.string.update_latest_url_desc)) },
                    bottomAction = {
                        OutlinedTextField(
                            value = customLatestUrl,
                            onValueChange = {
                                customLatestUrl = it
                                UpdateManager.setCustomUrl(context, UpdateManager.PREF_CUSTOM_LATEST_URL, it)
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            singleLine = true,
                        )
                    },
                )

                var customRedenApiUrl by remember { mutableStateOf(UpdateManager.getCustomRedenApiUrl(context)) }
                SettingItem(
                    title = { Text(context.getString(R.string.update_reden_api_url)) },
                    description = { Text(context.getString(R.string.update_reden_api_url_desc)) },
                    bottomAction = {
                        OutlinedTextField(
                            value = customRedenApiUrl,
                            onValueChange = {
                                customRedenApiUrl = it
                                UpdateManager.setCustomUrl(context, UpdateManager.PREF_CUSTOM_REDEN_API_URL, it)
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            singleLine = true,
                        )
                    },
                )

                var customNightlyUrl by remember { mutableStateOf(UpdateManager.getCustomNightlyUrl(context)) }
                SettingItem(
                    title = { Text(context.getString(R.string.update_nightly_url)) },
                    description = { Text(context.getString(R.string.update_nightly_url_desc)) },
                    bottomAction = {
                        OutlinedTextField(
                            value = customNightlyUrl,
                            onValueChange = {
                                customNightlyUrl = it
                                UpdateManager.setCustomUrl(context, UpdateManager.PREF_CUSTOM_NIGHTLY_URL, it)
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            singleLine = true,
                        )
                    },
                )

                var allowTelemetry by remember { mutableStateOf(preferences.getBoolean("allowTelemetry", true)) }
                SettingItemWithSwitch(
                    title = { Text(context.getString(R.string.allow_telemetry)) },
                    description = { Text(context.getString(R.string.allow_telemetry_desc)) },
                    checked = allowTelemetry,
                    onCheckedChange = {
                        allowTelemetry = it
                        preferences.edit { putBoolean("allowTelemetry", it) }
                    },
                )
            }

            AnimatedVisibility(visible = !showUpdateBanner) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            when (updateState) {
                                is UpdateState.NoUpdate, is UpdateState.Error -> {
                                    UpdateManager.checkForUpdate(context)
                                    if (UpdateManager.updateState.value is UpdateState.UpdateAvailable) {
                                        scrollState.animateScrollTo(0)
                                    }
                                }
                                UpdateState.Latest -> {
                                    UpdateManager.updateState.value = UpdateState.NoUpdate
                                }
                                else -> { /* NOOP */ }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 0.dp, 16.dp, 16.dp),
                ) {
                    Text(
                        when (updateState) {
                            is UpdateState.NoUpdate -> context.getString(R.string.check_update)
                            is UpdateState.Checking -> context.getString(R.string.checking)
                            is UpdateState.Latest -> context.getString(R.string.already_latest)
                            is UpdateState.Error -> context.getString(R.string.check_update_failed_retry)
                            else -> ""
                        },
                        Modifier.padding(0.dp, 4.dp),
                    )
                }
            }

            var reminderExpanded by remember { mutableStateOf(false) }
            var reminderIntervalMinutes by remember {
                mutableIntStateOf(
                    ContinuousUsageReminderPolicy.normalizeIntervalMinutes(
                        preferences.getInt(
                            ContinuousUsageReminderManager.KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES,
                            0,
                        ),
                    ),
                )
            }
            val reminderOptions = listOf(
                0 to context.getString(R.string.anti_addiction_off),
                15 to context.getString(R.string.anti_addiction_15min),
                30 to context.getString(R.string.anti_addiction_30min),
                60 to context.getString(R.string.anti_addiction_1hour),
            )

            SettingItemGroup(
                title = context.getString(R.string.anti_addiction),
            ) {
                SettingItem(
                    title = { Text(context.getString(R.string.anti_addiction_reminder)) },
                    description = { Text(context.getString(R.string.anti_addiction_reminder_desc, 0, 0)) },
                    endAction = {
                        ExposedDropdownMenuBox(
                            expanded = reminderExpanded,
                            onExpandedChange = { reminderExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = reminderOptions
                                    .find { it.first == reminderIntervalMinutes }
                                    ?.second ?: context.getString(R.string.anti_addiction_off),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .width(160.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = reminderExpanded,
                                onDismissRequest = { reminderExpanded = false },
                            ) {
                                reminderOptions.forEach { (minutes, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            reminderIntervalMinutes = minutes
                                            preferences.edit {
                                                putInt(
                                                    ContinuousUsageReminderManager
                                                        .KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES,
                                                    minutes,
                                                )
                                            }
                                            reminderExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }

            SettingItemGroup(
                title = context.getString(R.string.community),
                footer = { Text(context.getString(R.string.community_footer)) },
            ) {
                SettingItem(
                    title = { Text(context.getString(R.string.discord_channel)) },
                    description = { Text(context.getString(R.string.discord_channel_desc)) },
                    icon = { Icon(painterResource(R.drawable.ic_discord_24dp), null) },
                    endAction = {
                        Icon(
                            Icons.Default.ArrowOutward,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { luoTianYiUrlLauncher(context, "https://discord.gg/YCPFZV5XSA".toUri()) },
                )

                SettingItem(
                    title = { Text(context.getString(R.string.telegram_group)) },
                    description = { Text(context.getString(R.string.telegram_group_desc)) },
                    icon = { Icon(painterResource(R.drawable.ic_telegram_24dp), null) },
                    endAction = {
                        Icon(
                            Icons.Default.ArrowOutward,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { luoTianYiUrlLauncher(context, "https://t.me/+_A1Yto6EpyIyODA1".toUri()) },
                )

                SettingItem(
                    title = { Text(context.getString(R.string.github_issue)) },
                    description = { Text(context.getString(R.string.github_issue_desc)) },
                    icon = { Icon(painterResource(R.drawable.ic_github_24dp), null) },
                    endAction = {
                        Icon(
                            Icons.Default.ArrowOutward,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { luoTianYiUrlLauncher(context, "https://github.com/zly2006/zhihu-plus-plus/issues".toUri()) },
                )
            }
        }
    }
}
