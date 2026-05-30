package com.github.zly2006.zhihu.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.local.LocalHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.AndroidHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel

@Composable
actual fun rememberHomeScreenRuntime(recommendationMode: RecommendationMode): HomeScreenRuntime {
    val context = LocalContext.current
    val accountData by AccountData.asState()
    val updateState by UpdateManager.updateState.collectAsState()
    val viewModel: BaseFeedViewModel = when (recommendationMode) {
        RecommendationMode.WEB -> viewModel<HomeFeedViewModel>()
        RecommendationMode.ANDROID -> viewModel<AndroidHomeFeedViewModel>()
        RecommendationMode.LOCAL -> viewModel<LocalHomeFeedViewModel>()
        RecommendationMode.MIXED -> viewModel<MixedHomeFeedViewModel>()
    }
    val localHomeViewModel = viewModel as? LocalHomeFeedViewModel
    val installTime = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
    val updateAnnouncement = (updateState as? UpdateManager.UpdateState.UpdateAvailable)?.let {
        HomeUpdateAnnouncement(
            version = it.version.toString(),
            isNightly = it.isNightly,
        )
    }

    return HomeScreenRuntime(
        account = HomeAccountState(
            isLoggedIn = accountData.login,
            avatarUrl = accountData.self?.avatarUrl,
        ),
        updateAnnouncement = updateAnnouncement,
        installedAtLeastThreeHours = System.currentTimeMillis() - installTime >= 3 * 60 * 60 * 1000L,
        isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
        viewModel = viewModel,
        requestLogin = {
            val intent = Intent().setClassName(context.packageName, "com.github.zly2006.zhihu.LoginActivity")
            context.startActivity(intent)
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
