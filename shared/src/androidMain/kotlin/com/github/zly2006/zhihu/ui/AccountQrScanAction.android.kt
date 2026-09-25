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

package com.github.zly2006.zhihu.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.platform.rememberZhihuWebUrlOpener

private const val SCANNER_ACTIVITY = "com.github.zly2006.zhihu.QRCodeScanActivity"
private const val SCAN_RESULT_KEY = "scan_result"
private const val LOGIN_QR_PREFIX = "https://www.zhihu.com/account/scan/login/"

@Composable
actual fun rememberAccountQrScanAction(): (() -> Unit)? {
    val context = LocalContext.current
    val openWebView = rememberZhihuWebUrlOpener()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val scannedUrl = result.data?.getStringExtra(SCAN_RESULT_KEY)
        if (result.resultCode == Activity.RESULT_OK && scannedUrl?.startsWith(LOGIN_QR_PREFIX) == true) {
            openWebView(scannedUrl)
        }
    }
    return { launcher.launch(Intent().setClassName(context, SCANNER_ACTIVITY)) }
}
