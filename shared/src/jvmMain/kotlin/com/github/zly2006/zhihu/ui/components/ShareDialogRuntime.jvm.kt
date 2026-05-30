package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink

@Composable
actual fun rememberShareDialogRuntime(): ShareDialogRuntime {
    val copyPlainText = rememberPlainTextClipboard()
    val userMessages = rememberUserMessageSink()
    return remember(copyPlainText, userMessages) { clipboardShareDialogRuntime(copyPlainText, userMessages) }
}
