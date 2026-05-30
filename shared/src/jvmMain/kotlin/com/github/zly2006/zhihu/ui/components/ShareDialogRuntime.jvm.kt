package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink

@Composable
actual fun rememberShareDialogRuntime(): ShareDialogRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        ShareDialogRuntime(
            share = { _, shareText ->
                copyDesktopPlainText(shareText)
                userMessages.showMessage("已复制分享文本")
            },
            directShare = { _, shareText ->
                copyDesktopPlainText(shareText)
                userMessages.showMessage("已复制分享文本")
            },
            copyLink = { _, shareText ->
                copyDesktopPlainText(shareText)
                userMessages.showMessage("已复制链接")
            },
        )
    }
}
