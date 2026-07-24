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

package com.github.zly2006.zhihu.shared.platform

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.content.edit
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.OpenImageDialog
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.util.shareImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { url -> luoTianYiUrlLauncher(context, url.toUri()) } }
}

@Composable
actual fun rememberSystemUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { url -> context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } }
}

@Composable
actual fun rememberZhihuWebUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { url -> context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).setClassName(context, WEBVIEW_ACTIVITY_CLASS)) } }
}

@Composable
actual fun rememberImagePreviewOpener(): (String) -> Unit {
    val openGallery = rememberImageGalleryOpener()
    return remember(openGallery) { { url -> openGallery(listOf(url), 0) } }
}

@Composable
actual fun rememberImageGalleryOpener(): (List<String>, Int) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { urls, initialIndex ->
            OpenImageDialog(context, urls, initialIndex).show()
        }
    }
}

@Composable
actual fun rememberImageSaver(): (String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        { imageUrl ->
            scope.launch {
                saveImageToGallery(context, AccountData.httpClient(context), imageUrl)
            }
        }
    }
}

@Composable
actual fun rememberImageSharer(): (String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        { imageUrl ->
            scope.launch {
                shareImage(context, AccountData.httpClient(context), imageUrl)
            }
        }
    }
}

@Composable
actual fun rememberPlainTextClipboard(): (label: String, text: String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { label, text -> context.clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text)) } }
}

@Composable
actual fun rememberSettingsStore(): SettingsStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { androidSettingsStore(context) }
}

actual fun Modifier.exportTestTagsForUiAutomation(): Modifier = semantics { testTagsAsResourceId = true }

fun androidSettingsStore(context: Context): SettingsStore {
    val preferences = context.applicationContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    return SettingsStore(
        getBoolean = { key, defaultValue -> preferences.getBoolean(key, defaultValue) },
        putBoolean = { key, value -> preferences.edit { putBoolean(key, value) } },
        getString = { key, defaultValue -> preferences.getString(key, defaultValue) ?: defaultValue },
        putString = { key, value -> preferences.edit { putString(key, value) } },
        getStringOrNull = { key -> preferences.getString(key, null) },
        putStringSet = { key, value -> preferences.edit { putStringSet(key, value) } },
        getStringSet = { key, defaultValue -> preferences.getStringSet(key, defaultValue)?.toSet() ?: defaultValue },
        getInt = { key, defaultValue -> preferences.getInt(key, defaultValue) },
        putInt = { key, value -> preferences.edit { putInt(key, value) } },
        getLong = { key, defaultValue -> preferences.getLong(key, defaultValue) },
        putLong = { key, value -> preferences.edit { putLong(key, value) } },
        getFloat = { key, defaultValue -> preferences.getFloat(key, defaultValue) },
        putFloat = { key, value -> preferences.edit { putFloat(key, value) } },
        remove = { key -> preferences.edit { remove(key) } },
        observeKeyChanges = { onChanged ->
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key != null) {
                    onChanged(key)
                }
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            val unregister = {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
            unregister
        },
    )
}

fun androidUserMessageSink(context: Context): UserMessageSink {
    val appContext = context.applicationContext
    val mainHandler = Handler(Looper.getMainLooper())

    fun showToast(
        message: String,
        duration: Int,
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(appContext, message, duration).show()
        } else {
            mainHandler.post {
                Toast.makeText(appContext, message, duration).show()
            }
        }
    }

    return UserMessageSink(
        showShortMessage = { message ->
            showToast(message, Toast.LENGTH_SHORT)
        },
        showLongMessage = { message ->
            showToast(message, Toast.LENGTH_LONG)
        },
    )
}

@Composable
actual fun rememberUserMessageSink(): UserMessageSink {
    val context = LocalContext.current.applicationContext
    return remember(context) { androidUserMessageSink(context) }
}

@Composable
actual fun rememberIsLiteVariant(): Boolean {
    val context = LocalContext.current
    return remember(context) { isAndroidLiteVariantPackageName(context.packageName) }
}

internal fun isAndroidLiteVariantPackageName(packageName: String): Boolean = packageName.endsWith(".lite")

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = BackHandler(enabled = enabled, onBack = onBack)

@Composable
actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
) = PredictiveBackHandler(enabled = enabled) { progress ->
    try {
        progress.collect { backEvent ->
            onProgress(backEvent.progress)
        }
        onBack()
    } catch (e: CancellationException) {
        onCancel()
        throw e
    }
}
