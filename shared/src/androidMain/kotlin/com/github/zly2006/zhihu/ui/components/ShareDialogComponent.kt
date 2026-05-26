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

package com.github.zly2006.zhihu.ui.components

import android.content.ClipData
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.ui.articleHost
import com.github.zly2006.zhihu.util.clipboardManager

@Composable
actual fun rememberShareDialogRuntime(): ShareDialogRuntime {
    val context = LocalContext.current
    return remember(context) {
        ShareDialogRuntime(
            share = { _, shareText ->
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                val chooserIntent = Intent.createChooser(shareIntent, "分享到")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            },
            directShare = { content, shareText ->
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_TITLE, getShareTitle(content))
                }
                val chooserIntent = Intent.createChooser(shareIntent, "分享到")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            },
            copyLink = { content, shareText ->
                context.articleHost()?.clipboardDestination = content
                context.clipboardManager.setPrimaryClip(ClipData.newPlainText("Link", shareText))
                androidUserMessageSink(context).showShortMessage("已复制链接")
            },
        )
    }
}
