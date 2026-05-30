package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink

@Composable
actual fun rememberPeopleScreenRuntime(): PeopleScreenRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        PeopleScreenRuntime(
            showShortMessage = { message ->
                userMessages.showShortMessage(message)
            },
            openWebUrl = { url ->
                openDesktopExternalUrl(url)
            },
            openImage = { url ->
                openDesktopExternalUrl(url)
            },
        )
    }
}
