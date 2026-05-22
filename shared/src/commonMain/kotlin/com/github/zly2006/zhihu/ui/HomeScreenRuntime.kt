package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel

data class HomeAccountState(
    val isLoggedIn: Boolean,
    val avatarUrl: String?,
)

data class HomeUpdateAnnouncement(
    val version: String,
    val isNightly: Boolean,
)

data class HomeScreenRuntime(
    val account: HomeAccountState,
    val updateAnnouncement: HomeUpdateAnnouncement?,
    val installedAtLeastThreeHours: Boolean,
    val isDebuggable: Boolean,
    val viewModel: BaseFeedViewModel,
    val requestLogin: () -> Unit,
    val openExternalUrl: (String) -> Unit,
    val copyDebugData: (String) -> Unit,
    val recordLocalItemOpened: (FeedDisplayItem) -> Unit,
    val recordLocalItemFeedback: (FeedDisplayItem, Double) -> Boolean,
)

@Composable
expect fun rememberHomeScreenRuntime(recommendationMode: RecommendationMode): HomeScreenRuntime
