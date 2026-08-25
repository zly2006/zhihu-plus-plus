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

package com.github.zly2006.zhihu.platform

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
import com.github.zly2006.zhihu.account.androidZhihuAccountStore
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.OpenImageDialog
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.util.shareImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.io.files.Path

private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"

@Composable
actual fun rememberExternalUrlOpener(): ExternalUrlOpener {
    val context = LocalContext.current
    return remember(context) {
        object : ExternalUrlOpener {
            override fun invoke(url: String) = luoTianYiUrlLauncher(context, url.toUri())
        }
    }
}

internal actual val platformBottomBarItemLimit: Int? = 5

actual val platformName: String = "Android"

actual val isAigcVoteSupported: Boolean = true

actual val isBlocklistNlpSupported: Boolean = true

actual val isSentenceSimilaritySupported: Boolean = true

actual val isArticleHtmlExportSupported: Boolean = true

actual val isArticleImageExportSupported: Boolean = true

@Composable
actual fun rememberSystemUrlOpener(): SystemUrlOpener {
    val context = LocalContext.current
    return remember(context) {
        object : SystemUrlOpener {
            override fun invoke(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
    }
}

@Composable
actual fun rememberZhihuWebUrlOpener(): ZhihuWebUrlOpener {
    val context = LocalContext.current
    return remember(context) {
        object : ZhihuWebUrlOpener {
            override fun invoke(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).setClassName(context, WEBVIEW_ACTIVITY_CLASS))
        }
    }
}

@Composable
actual fun rememberImagePreviewOpener(): ImagePreviewOpener {
    val openGallery = rememberImageGalleryOpener()
    return remember(openGallery) {
        object : ImagePreviewOpener {
            override fun invoke(url: String) = openGallery(listOf(url), 0)
        }
    }
}

@Composable
actual fun rememberImageGalleryOpener(): ImageGalleryOpener {
    val context = LocalContext.current
    return remember(context) {
        object : ImageGalleryOpener {
            override fun invoke(urls: List<String>, initialIndex: Int) {
                OpenImageDialog(context, urls, initialIndex).show()
            }
        }
    }
}

@Composable
actual fun rememberImageSaver(): ImageSaver {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        object : ImageSaver {
            override fun invoke(url: String) {
                scope.launch {
                    saveImageToGallery(context, androidZhihuAccountStore(context).client.httpClient(), url)
                }
            }
        }
    }
}

@Composable
actual fun rememberImageSharer(): ImageSharer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        object : ImageSharer {
            override fun invoke(url: String) {
                scope.launch {
                    shareImage(context, androidZhihuAccountStore(context).client.httpClient(), url)
                }
            }
        }
    }
}

@Composable
actual fun rememberPlainTextClipboard(): PlainTextClipboard {
    val context = LocalContext.current
    return remember(context) {
        object : PlainTextClipboard {
            override fun invoke(label: String, text: String) = context.clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
        }
    }
}

@Composable
actual fun rememberSettingsStore(): SettingsStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { androidSettingsStore(context) }
}

actual fun Modifier.exportTestTagsForUiAutomation(): Modifier = semantics { testTagsAsResourceId = true }

@Composable
actual fun rememberAppPrivateDirectory(): Path {
    val context = LocalContext.current.applicationContext
    return remember(context) { Path(context.filesDir.absolutePath) }
}

fun androidSettingsStore(context: Context): SettingsStore {
    val preferences = context.applicationContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    return object : SettingsStore {
        override fun getBoolean(key: String, defaultValue: Boolean) = preferences.getBoolean(key, defaultValue)

        override fun putBoolean(key: String, value: Boolean) = preferences.edit { putBoolean(key, value) }

        override fun getString(key: String, defaultValue: String) = preferences.getString(key, defaultValue) ?: defaultValue

        override fun putString(key: String, value: String) = preferences.edit { putString(key, value) }

        override fun getStringOrNull(key: String) = preferences.getString(key, null)

        override fun putStringSet(key: String, value: Set<String>) = preferences.edit { putStringSet(key, value) }

        override fun getStringSet(key: String, defaultValue: Set<String>) = preferences.getStringSet(key, defaultValue)?.toSet() ?: defaultValue

        override fun getInt(key: String, defaultValue: Int) = preferences.getInt(key, defaultValue)

        override fun putInt(key: String, value: Int) = preferences.edit { putInt(key, value) }

        override fun getLong(key: String, defaultValue: Long) = preferences.getLong(key, defaultValue)

        override fun putLong(key: String, value: Long) = preferences.edit { putLong(key, value) }

        override fun getFloat(key: String, defaultValue: Float) = preferences.getFloat(key, defaultValue)

        override fun putFloat(key: String, value: Float) = preferences.edit { putFloat(key, value) }

        override fun remove(key: String) = preferences.edit { remove(key) }

        override fun observeKeyChanges(onChanged: (String) -> Unit): AutoCloseable {
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key != null) {
                    onChanged(key)
                }
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            return AutoCloseable {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
    }
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

    return object : UserMessageSink {
        override fun showShortMessage(message: String) = showToast(message, Toast.LENGTH_SHORT)

        override fun showLongMessage(message: String) = showToast(message, Toast.LENGTH_LONG)
    }
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
