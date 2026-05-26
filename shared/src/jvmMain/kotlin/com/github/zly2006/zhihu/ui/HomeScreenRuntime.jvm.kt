package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopLoginRequests
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.ui.subscreens.SystemUpdateState
import com.github.zly2006.zhihu.ui.subscreens.desktopSystemUpdateState
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.local.LocalHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.AndroidHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel

@Composable
actual fun rememberHomeScreenRuntime(recommendationMode: RecommendationMode): HomeScreenRuntime {
    val accountStore = remember { DesktopAccountStore() }
    var account by remember { mutableStateOf(accountStore.load()) }
    val updateState by desktopSystemUpdateState.collectAsState()
    val viewModel: BaseFeedViewModel = when (recommendationMode) {
        RecommendationMode.ANDROID -> viewModel<AndroidHomeFeedViewModel>()
        RecommendationMode.LOCAL -> viewModel<LocalHomeFeedViewModel>()
        RecommendationMode.MIXED -> viewModel<MixedHomeFeedViewModel>()
        RecommendationMode.WEB -> viewModel<HomeFeedViewModel>()
    }
    val localHomeViewModel = viewModel as? LocalHomeFeedViewModel
    val updateAnnouncement = (updateState as? SystemUpdateState.UpdateAvailable)?.let {
        HomeUpdateAnnouncement(
            version = it.version,
            isNightly = it.isNightly,
        )
    }
    return HomeScreenRuntime(
        account = HomeAccountState(
            isLoggedIn = account.login,
            avatarUrl = account.profile?.avatarUrl,
        ),
        updateAnnouncement = updateAnnouncement,
        installedAtLeastThreeHours = false,
        isDebuggable = true,
        viewModel = viewModel,
        requestLogin = {
            DesktopLoginRequests.requestLogin()
            account = accountStore.load()
        },
        openExternalUrl = { url ->
            runCatching {
                openDesktopExternalUrl(url)
            }
        },
        copyDebugData = { data ->
            runCatching {
                copyDesktopPlainText(data)
            }
        },
        recordLocalItemOpened = { item ->
            localHomeViewModel?.onLocalItemOpened(item)
        },
        recordLocalItemFeedback = { item, feedback ->
            if (localHomeViewModel != null && item.localContentId != null) {
                localHomeViewModel.onLocalItemFeedback(item, feedback)
                true
            } else {
                false
            }
        },
    )
}
