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

import android.content.ClipData
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.SentenceSimilarityTest
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.SettingItemOverall
import com.github.zly2006.zhihu.util.PowerSaveModeCompat
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val DEVELOPER_SETTINGS_BACK_BUTTON_TAG = "developerSettings/backButton"
internal const val DEVELOPER_SETTINGS_MODE_TAG = "developerSettings/modeToggle"
internal const val DEVELOPER_SETTINGS_SENTENCE_SIMILARITY_TAG = "developerSettings/sentenceSimilarity"
internal const val DEVELOPER_SETTINGS_COLOR_SCHEME_TAG = "developerSettings/colorScheme"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeveloperSettingsScreen(
    innerPadding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember {
        context.getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE,
        )
    }
    val dataState by AccountData.asState()
    val data = dataState
    val mainActivity = context as? MainActivity
    var developerModeEnabled by remember {
        mutableStateOf(preferences.getBoolean("developer", false))
    }
    val continuousUsageDurationMs by produceState(
        initialValue = mainActivity?.currentContinuousUsageDurationMs() ?: 0L,
        key1 = mainActivity,
    ) {
        while (true) {
            value = mainActivity?.currentContinuousUsageDurationMs() ?: 0L
            delay(1_000L)
        }
    }

    var showCookieDialog by remember { mutableStateOf(false) }
    var showSignedRequestDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text(context.getString(R.string.developer_options)) },
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.testTag(DEVELOPER_SETTINGS_BACK_BUTTON_TAG),
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
                .padding(16.dp),
        ) {
            SettingItemOverall(
                modifier = Modifier.testTag(DEVELOPER_SETTINGS_MODE_TAG),
                title = { Text(context.getString(R.string.developer_mode)) },
                checked = developerModeEnabled,
                onCheckedChange = {
                    developerModeEnabled = it
                    preferences.edit {
                        putBoolean("developer", it)
                    }
                    if (!it) {
                        navigator.onNavigateBack()
                    }
                },
            )
            SelectionContainer {
                Column {
                    val networkStatus = remember(context) {
                        buildString {
                            val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
                            val activeNetwork = connectivityManager.activeNetwork
                            append(context.getString(R.string.network_status))
                            append(": ")
                            if (activeNetwork != null) {
                                append(context.getString(R.string.network_connected))
                                if (connectivityManager.getNetworkCapabilities(activeNetwork)!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                    append(" ")
                                    append(context.getString(R.string.network_mobile))
                                } else if (connectivityManager.getNetworkCapabilities(activeNetwork)!!.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                                    append(" ")
                                    append(context.getString(R.string.network_wifi))
                                } else if (connectivityManager.getNetworkCapabilities(activeNetwork)!!.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                                    append(" ")
                                    append(context.getString(R.string.network_vpn))
                                }
                            } else {
                                append(context.getString(R.string.network_disconnected))
                            }
                        }
                    }
                    Text(networkStatus)

                    when (PowerSaveModeCompat.getPowerSaveMode(context)) {
                        PowerSaveModeCompat.POWER_SAVE -> Text(context.getString(R.string.power_save_mode))
                        PowerSaveModeCompat.HUAWEI_POWER_SAVE -> Text(context.getString(R.string.huawei_power_save))
                        else -> {}
                    }
                    Text(
                        context.getString(
                            R.string.continuous_usage_duration,
                            formatContinuousUsageDuration(context, continuousUsageDurationMs),
                        ),
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    coroutineScope.launch {
                        if (AccountData.verifyLogin(context, data.cookies)) {
                            Toast.makeText(context, context.getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.login_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text(context.getString(R.string.verify_login)) }

                Button(onClick = {
                    coroutineScope.launch {
                        val httpClient = AccountData.httpClient(context)
                        ZhihuCredentialRefresher.refreshZhihuToken(ZhihuCredentialRefresher.fetchRefreshToken(httpClient), httpClient)
                        Toast.makeText(context, context.getString(R.string.token_refreshed), Toast.LENGTH_SHORT).show()
                    }
                }) { Text(context.getString(R.string.refresh_token)) }

                Button(onClick = { showCookieDialog = true }) { Text(context.getString(R.string.manual_cookie)) }

                Button(onClick = { showSignedRequestDialog = true }) { Text(context.getString(R.string.signed_request)) }

                Button(
                    modifier = Modifier.testTag(DEVELOPER_SETTINGS_SENTENCE_SIMILARITY_TAG),
                    onClick = {
                        navigator.onNavigate(SentenceSimilarityTest)
                    },
                ) { Text(context.getString(R.string.sentence_similarity)) }

                Button(
                    modifier = Modifier.testTag(DEVELOPER_SETTINGS_COLOR_SCHEME_TAG),
                    onClick = {
                        navigator.onNavigate(Account.DeveloperSettings.ColorScheme)
                    },
                ) { Text(context.getString(R.string.color_scheme)) }
            }

            // TTS引擎信息显示
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        context.getString(R.string.tts_engine_info),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            context.getString(R.string.tts_current_engine),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            when (mainActivity?.ttsEngine) {
                                MainActivity.TtsEngine.Pico -> "Pico TTS"
                                MainActivity.TtsEngine.Google -> "Google TTS"
                                MainActivity.TtsEngine.Sherpa -> "Sherpa TTS"
                                else -> context.getString(R.string.tts_not_ready)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            context.getString(R.string.tts_engine_status),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (mainActivity?.isSpeaking() == true) {
                                context.getString(R.string.tts_speaking)
                            } else if (mainActivity?.ttsEngine != MainActivity.TtsEngine.Uninitialized) {
                                context.getString(R.string.tts_ready)
                            } else {
                                context.getString(R.string.tts_not_ready)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                mainActivity?.isSpeaking() == true -> MaterialTheme.colorScheme.tertiary
                                mainActivity?.ttsEngine != MainActivity.TtsEngine.Uninitialized -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            context.getString(R.string.tts_engine_list),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            (context as? MainActivity)?.textToSpeech?.engines?.joinToString { it.name } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                mainActivity?.isSpeaking() == true -> MaterialTheme.colorScheme.tertiary
                                mainActivity?.ttsEngine != MainActivity.TtsEngine.Uninitialized -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCookieDialog) {
        var cookieInputText by remember { mutableStateOf("") }
        var showCookieText by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = {
                showCookieDialog = false
                cookieInputText = ""
                showCookieText = false
            },
            title = { Text(context.getString(R.string.manual_cookie)) },
            text = {
                Column {
                    Text(
                        context.getString(R.string.cookie_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    OutlinedTextField(
                        value = cookieInputText,
                        onValueChange = { cookieInputText = it },
                        label = { Text(context.getString(R.string.cookie_input_label)) },
                        placeholder = { Text("name1=value1; name2=value2; name3=value3") },
                        visualTransformation = if (showCookieText) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCookieText = !showCookieText }) {
                                Icon(
                                    imageVector = if (showCookieText) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showCookieText) context.getString(R.string.hide) else context.getString(R.string.show),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cookieInputText.isNotBlank()) {
                            try {
                                // 解析cookie字符串
                                val cookies = mutableMapOf<String, String>()
                                cookieInputText.split("; ").forEach { cookieItem ->
                                    val parts = cookieItem.split("=", limit = 2)
                                    if (parts.size == 2) {
                                        cookies[parts[0].trim()] = parts[1].trim()
                                    }
                                }

                                if (cookies.isNotEmpty()) {
                                    // 保存cookie数据
                                    val currentData = AccountData.data
                                    AccountData.saveData(
                                        context,
                                        currentData.copy(
                                            cookies = cookies,
                                            login = true,
                                        ),
                                    )

                                    // 验证登录状态
                                    coroutineScope.launch {
                                        try {
                                            if (AccountData.verifyLogin(context, cookies)) {
                                                Toast.makeText(context, context.getString(R.string.cookie_set_success), Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.cookie_set_but_verify_failed), Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, context.getString(R.string.cookie_verify_error, e.message.orEmpty()), Toast.LENGTH_LONG).show()
                                        }
                                    }

                                    showCookieDialog = false
                                    cookieInputText = ""
                                    showCookieText = false
                                } else {
                                    Toast.makeText(context, context.getString(R.string.cookie_parse_failed), Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.cookie_parse_error, e.message.orEmpty()), Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.cookie_input_required), Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text(context.getString(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCookieDialog = false
                        cookieInputText = ""
                        showCookieText = false
                    },
                ) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }

    if (showSignedRequestDialog) {
        var urlInput by remember { mutableStateOf("https://www.zhihu.com/api/v4/me") }
        var responseText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showSignedRequestDialog = false
                urlInput = "https://www.zhihu.com/api/v4/me"
                responseText = ""
                isLoading = false
            },
            title = { Text(context.getString(R.string.signed_get_request)) },
            text = {
                Column {
                    Text(
                        context.getString(R.string.signed_request_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text(context.getString(R.string.request_url)) },
                        placeholder = { Text("https://www.zhihu.com/api/v4/me") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        enabled = !isLoading,
                    )
                    if (responseText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            context.getString(R.string.response),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        SelectionContainer {
                            Text(
                                responseText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                                maxLines = 10,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (urlInput.isNotBlank() && !isLoading) {
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val httpClient = AccountData.httpClient(context)
                                    val response = httpClient.get(urlInput) {
                                        signFetchRequest()
                                    }
                                    val body = response.bodyAsText()
                                    responseText = body

                                    // 复制到剪贴板
                                    val clip = ClipData.newPlainText("Signed Request Response", body)
                                    context.clipboardManager.setPrimaryClip(clip)

                                    Toast.makeText(context, context.getString(R.string.response_copied), Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    responseText = context.getString(R.string.response_error, e.message.orEmpty())
                                    Toast.makeText(context, context.getString(R.string.request_failed, e.message.orEmpty()), Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.valid_url_required), Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isLoading,
                ) {
                    Text(if (isLoading) context.getString(R.string.requesting) else context.getString(R.string.send_request))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSignedRequestDialog = false
                        urlInput = "https://www.zhihu.com/api/v4/me"
                        responseText = ""
                        isLoading = false
                    },
                    enabled = !isLoading,
                ) {
                    Text(context.getString(R.string.close))
                }
            },
        )
    }
}

private fun formatContinuousUsageDuration(context: Context, durationMs: Long): String {
    val safeDurationMs = durationMs.coerceAtLeast(0L)
    val totalSeconds = safeDurationMs / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0 -> context.getString(R.string.duration_hours_minutes_seconds, hours, minutes, seconds)
        minutes > 0 -> context.getString(R.string.duration_minutes_seconds, minutes, seconds)
        else -> context.getString(R.string.duration_seconds, seconds)
    }
}
