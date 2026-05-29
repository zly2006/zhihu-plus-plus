package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink

// TODO: iOS 用户页面完整实现
@Composable
actual fun rememberPeopleScreenRuntime(): PeopleScreenRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        PeopleScreenRuntime(
            loadProfile = { error("People profile not available on iOS yet") },
            toggleFollow = { _, _, _ -> error("Follow not available on iOS yet") },
            toggleBlock = { _, _ -> error("Block not available on iOS yet") },
            toggleRecommendationBlock = { error("Recommendation block not available on iOS yet") },
            showShortMessage = { userMessages.showShortMessage(it) },
            openWebUrl = { openIosUrl(it) },
            openImage = { openIosUrl(it) },
        )
    }
}
