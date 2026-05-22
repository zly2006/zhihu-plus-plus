package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel

@Composable
actual fun rememberHomeScreenRuntime(recommendationMode: RecommendationMode): HomeScreenRuntime {
    val viewModel = viewModel<HomeFeedViewModel>()
    return HomeScreenRuntime(
        account = HomeAccountState(
            isLoggedIn = true,
            avatarUrl = null,
        ),
        updateAnnouncement = null,
        installedAtLeastThreeHours = false,
        isDebuggable = false,
        viewModel = viewModel,
        requestLogin = {},
        openExternalUrl = {},
        copyDebugData = {},
        recordLocalItemOpened = {},
        recordLocalItemFeedback = { _, _ -> false },
    )
}

@Composable
actual fun HomeAccountSettingSheetContent(
    innerPadding: PaddingValues,
    unreadCount: Int,
    onDismissRequest: () -> Unit,
) {
    AccountSettingScreen(innerPadding)
}
