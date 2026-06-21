/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 分享弹层的 miuix 版本，对标 M3 [com.github.zly2006.zhihu.ui.components.ShareDialog]：
 * 系统分享 / 复制链接 / 分享设置三个动作，复用同一套 [rememberShareDialogRuntime] 逻辑。
 */
@Composable
fun MiuixShareSheet(
    content: NavDestination,
    shareText: String,
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val runtime = rememberShareDialogRuntime()
    WindowBottomSheet(
        show = showDialog,
        title = "分享",
        insideMargin = DpSize(16.dp, 0.dp),
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MiuixSheetActionRow("分享", icon = Icons.Filled.Share, onClick = {
                onDismissRequest()
                runtime.share(content, shareText)
            })
            MiuixSheetActionRow("复制链接", icon = Icons.Filled.ContentCopy, onClick = {
                onDismissRequest()
                runtime.copyLink(content, shareText)
            })
            MiuixSheetActionRow("分享设置", icon = Icons.Filled.Settings, onClick = {
                onDismissRequest()
                navigator.onNavigate(Account.AppearanceSettings(setting = "shareAction"))
            })
            Spacer(Modifier.height(8.dp))
        }
    }
}
