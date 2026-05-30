package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberUserMessageSink(): UserMessageSink = remember {
    UserMessageSink(::showDesktopMessage)
}

private fun showDesktopMessage(message: String) {
    println(message)
    runCatching {
        ProcessBuilder("terminal-notifier", "-message", message, "-sound", "default")
            .start()
    }
}
