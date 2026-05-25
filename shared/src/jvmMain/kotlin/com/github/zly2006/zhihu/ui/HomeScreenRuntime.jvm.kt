package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.local.LocalHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.AndroidHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

@Composable
actual fun rememberHomeScreenRuntime(recommendationMode: RecommendationMode): HomeScreenRuntime {
    val accountStore = remember { DesktopAccountStore() }
    var account by remember { mutableStateOf(accountStore.load()) }
    val viewModel: BaseFeedViewModel = when (recommendationMode) {
        RecommendationMode.ANDROID -> viewModel<AndroidHomeFeedViewModel>()
        RecommendationMode.LOCAL -> viewModel<LocalHomeFeedViewModel>()
        RecommendationMode.MIXED -> viewModel<MixedHomeFeedViewModel>()
        RecommendationMode.WEB -> viewModel<HomeFeedViewModel>()
    }
    val localHomeViewModel = viewModel as? LocalHomeFeedViewModel
    return HomeScreenRuntime(
        account = HomeAccountState(
            isLoggedIn = account.login,
            avatarUrl = account.profile?.avatarUrl,
        ),
        updateAnnouncement = null,
        installedAtLeastThreeHours = false,
        isDebuggable = true,
        viewModel = viewModel,
        requestLogin = {
            account = accountStore.load()
        },
        openExternalUrl = { url ->
            runCatching {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(url))
                }
            }
        },
        copyDebugData = { data ->
            runCatching {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(data), null)
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
