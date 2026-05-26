package com.github.zly2006.zhihu.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.shared.platform.androidSettingsStore

@Composable
fun rememberAndroidZhihuMainPreferenceState(): ZhihuMainPreferenceState {
    val context = LocalContext.current
    val settings = remember(context) {
        androidSettingsStore(context)
    }
    val allBottomBarItemKeys = remember {
        listOf(Home.name, Follow.name, HotList.name, Daily.name, OnlineHistory.name, Account.name)
    }
    return rememberZhihuMainPreferenceState {
        val duo3HomeAccount = settings.getBoolean("duo3_home_account", false)
        val selectedKeys = normalizeBottomBarSelection(
            settings.getStringSet(
                BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
                defaultBottomBarSelectionKeys(duo3HomeAccount),
            ),
            duo3HomeAccount,
            enforceMinimumSelection = true,
        )
        ZhihuMainPreferenceSnapshot(
            duo3HomeAccount = duo3HomeAccount,
            duo3NavStyle = settings.getBoolean("duo3_nav_style", false),
            tapToScrollToTopEnabled = settings.getBoolean("bottomBarTapScrollToTop", true),
            autoHideBottomBar = settings.getBoolean("autoHideBottomBar", false),
            selectedBottomBarItemKeys = selectedKeys,
            startDestination = navDestinationFromName(
                resolveValidStartDestinationKey(
                    settings.getString(START_DESTINATION_PREFERENCE_KEY, Home.name),
                    allBottomBarItemKeys.filter { it in selectedKeys },
                ),
            ),
        )
    }
}

@Composable
fun rememberAndroidZhihuMainNavigationState(): ZhihuMainNavigationState {
    val activity = LocalActivity.current as MainActivity
    return ZhihuMainNavigationState(
        mainTabNavigationTarget = activity.mainTabNavigationTarget,
        navigate = activity::navigate,
        setCurrentMainTabOpenFrom = activity::setCurrentMainTabOpenFrom,
        consumeMainTabNavigationTarget = activity::consumeMainTabNavigationTarget,
    )
}

@Composable
fun rememberAndroidZhihuMainActivity(): MainActivity = LocalActivity.current as MainActivity
