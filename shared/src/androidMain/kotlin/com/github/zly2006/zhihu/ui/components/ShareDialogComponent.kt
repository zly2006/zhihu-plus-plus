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
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.articleHost
import com.github.zly2006.zhihu.util.clipboardManager

/**
 * 根据用户设置处理分享操作
 * @param context Android Context
 * @param content 要分享的内容
 * @param onShowDialog 当需要显示对话框时调用
 */
fun handleShareAction(
    context: Context,
    content: NavDestination,
    onShowDialog: () -> Unit,
) {
    val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    val shareActionMode = preferences.getString("shareActionMode", "ask") ?: "ask"

    when (shareActionMode) {
        "ask" -> {
            // 显示对话框询问
            onShowDialog()
        }
        "copy" -> {
            // 直接复制链接
            context.articleHost()?.clipboardDestination = content
            context.clipboardManager.setPrimaryClip(ClipData.newPlainText("Link", getShareText(content)))
            Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
        }
        "share" -> {
            // 直接调用系统分享
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getShareText(content))
                putExtra(Intent.EXTRA_TITLE, getShareTitle(content))
            }
            val chooserIntent = Intent.createChooser(shareIntent, "分享到")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        }
    }
}

@Composable
fun ShareDialog(
    content: NavDestination,
    shareText: String,
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    context: Context,
) {
    val navigator = LocalNavigator.current

    ShareDialogContent(
        showDialog = showDialog,
        onDismissRequest = onDismissRequest,
        onShareClick = {
            onDismissRequest()
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooserIntent = Intent.createChooser(shareIntent, "分享到")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        },
        onCopyClick = {
            onDismissRequest()
            context.articleHost()?.clipboardDestination = content
            context.clipboardManager.setPrimaryClip(ClipData.newPlainText("Link", shareText))
            Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
        },
        onSettingsClick = {
            onDismissRequest()
            navigator.onNavigate(Account.AppearanceSettings(setting = "shareAction"))
        },
    )
}
