/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under the GNU Affero General Public License version 3.
 */

package com.github.zly2006.zhihu.account

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

private var currentAccountStore: ZhihuAccountStore? = null
private var currentApplicationContext: Context? = null

@Synchronized
fun androidZhihuAccountStore(context: Context): ZhihuAccountStore {
    currentApplicationContext = context.applicationContext
    currentAccountStore?.let { return it }
    return ZhihuAccountStore(
        repository = androidAccountRepository(checkNotNull(currentApplicationContext)),
    ).also { currentAccountStore = it }
}

internal fun currentAndroidZhihuAccountStore(): ZhihuAccountStore = checkNotNull(currentAccountStore)

internal fun currentAndroidApplicationContext(): Context = checkNotNull(currentApplicationContext)

@Synchronized
@VisibleForTesting
fun replaceAndroidZhihuAccountStoreForTesting(store: ZhihuAccountStore?) {
    currentAccountStore?.close()
    currentAccountStore = store
}

@Composable
actual fun rememberZhihuAccountStore(): ZhihuAccountStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { androidZhihuAccountStore(context) }
}

private fun androidAccountRepository(context: Context) = ZhihuAccountRepository(
    AndroidAccountSessionStore(File(context.filesDir, "account.json")),
)

private class AndroidAccountSessionStore(
    private val file: File,
) : ZhihuAccountSessionStore {
    override fun readText(): String? = if (file.exists()) file.readText() else null

    override fun writeText(text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    override fun delete() {
        file.delete()
    }
}
