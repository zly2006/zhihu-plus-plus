package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun rememberShareDialogRuntime(): ShareDialogRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        ShareDialogRuntime(
            share = { _, shareText ->
                copyDesktopShareText(shareText)
                userMessages.showMessage("已复制分享文本")
            },
            directShare = { _, shareText ->
                copyDesktopShareText(shareText)
                userMessages.showMessage("已复制分享文本")
            },
            copyLink = { _, shareText ->
                copyDesktopShareText(shareText)
                userMessages.showMessage("已复制链接")
            },
        )
    }
}

private fun copyDesktopShareText(text: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}
