package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel

@Composable
actual fun rememberHomeScreenRuntime(recommendationMode: RecommendationMode): HomeScreenRuntime {
    val userMessages = rememberUserMessageSink()
    val viewModel = remember { HomeFeedViewModel() }
    return remember(userMessages, viewModel) {
        HomeScreenRuntime(
            account = HomeAccountState(isLoggedIn = false, avatarUrl = null),
            updateAnnouncement = null,
            installedAtLeastThreeHours = false,
            isDebuggable = false,
            viewModel = viewModel,
            // TODO: iOS 登录
            requestLogin = { userMessages.showMessage("iOS 登录暂未实现") },
            openExternalUrl = { openIosUrl(it) },
            // TODO: iOS 复制调试数据
            copyDebugData = { userMessages.showMessage("iOS 复制暂未实现") },
            // TODO: iOS 本地推荐记录
            recordLocalItemOpened = { },
            recordLocalItemFeedback = { _, _ -> false },
        )
    }
}
