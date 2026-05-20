package com.github.zly2006.zhihu.ui

import android.content.Context
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
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.defaultBottomBarSelectionKeys
import com.github.zly2006.zhihu.ui.subscreens.navDestinationFromName
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarSelection
import com.github.zly2006.zhihu.ui.subscreens.resolveValidStartDestinationKey

@Composable
fun rememberAndroidZhihuMainPreferenceState(): ZhihuMainPreferenceState {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }
    val allBottomBarItemKeys = remember {
        listOf(Home.name, Follow.name, HotList.name, Daily.name, OnlineHistory.name, Account.name)
    }
    return rememberZhihuMainPreferenceState {
        val duo3HomeAccount = preferences.getBoolean("duo3_home_account", false)
        val selectedKeys = normalizeBottomBarSelection(
            preferences
                .getStringSet(BOTTOM_BAR_ITEMS_PREFERENCE_KEY, defaultBottomBarSelectionKeys(duo3HomeAccount))
                ?.toSet() ?: defaultBottomBarSelectionKeys(duo3HomeAccount),
            duo3HomeAccount,
            enforceMinimumSelection = true,
        )
        ZhihuMainPreferenceSnapshot(
            duo3HomeAccount = duo3HomeAccount,
            duo3NavStyle = preferences.getBoolean("duo3_nav_style", false),
            tapToScrollToTopEnabled = preferences.getBoolean("bottomBarTapScrollToTop", true),
            autoHideBottomBar = preferences.getBoolean("autoHideBottomBar", false),
            selectedBottomBarItemKeys = selectedKeys,
            startDestination = navDestinationFromName(
                resolveValidStartDestinationKey(
                    preferences.getString(START_DESTINATION_PREFERENCE_KEY, Home.name),
                    allBottomBarItemKeys.filter { it in selectedKeys },
                ),
            ),
        )
    }
}

@Composable
fun rememberAndroidZhihuMainActivityState(): ZhihuMainActivityState {
    val activity = LocalActivity.current as MainActivity
    return ZhihuMainActivityState(
        mainTabNavigationTarget = activity.mainTabNavigationTarget,
        navigate = activity::navigate,
        setCurrentMainTabOpenFrom = activity::setCurrentMainTabOpenFrom,
        consumeMainTabNavigationTarget = activity::consumeMainTabNavigationTarget,
    )
}

@Composable
fun rememberAndroidZhihuMainActivity(): MainActivity = LocalActivity.current as MainActivity
