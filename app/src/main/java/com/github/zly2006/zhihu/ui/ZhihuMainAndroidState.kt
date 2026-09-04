/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

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
import com.github.zly2006.zhihu.navigation.MyCollections
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.platform.androidSettingsStore
import com.github.zly2006.zhihu.platform.platformBottomBarItemLimit
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.COLLECTION_DIRECT_BROWSE_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.bottomBarItemOrderFromPreference
import com.github.zly2006.zhihu.ui.subscreens.defaultBottomBarSelectionKeys
import com.github.zly2006.zhihu.ui.subscreens.navDestinationFromName
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarSelection
import com.github.zly2006.zhihu.ui.subscreens.resolveValidStartDestinationKey

private val mainPreferenceKeys = setOf(
    "duo3_home_account",
    BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
    BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY,
    "bottomBarTapScrollToTop",
    "autoHideBottomBar",
    "autoHideTopBar",
    COLLECTION_DIRECT_BROWSE_PREFERENCE_KEY,
    START_DESTINATION_PREFERENCE_KEY,
)

/**
 * 读取 Android SharedPreferences 中会影响主壳的设置快照。
 *
 * 这些设置决定底部栏项目、启动页和自动隐藏行为。设置页退出后会重新读取这份快照，
 * 因此新增主壳设置时要同步这里和 Desktop 的读取逻辑。
 */
@Composable
fun rememberAndroidZhihuMainPreferenceState(): ZhihuMainPreferenceState {
    val context = LocalContext.current
    val settings = remember(context) {
        androidSettingsStore(context)
    }
    val allBottomBarItemKeys = remember {
        listOf(Home.name, Follow.name, HotList.name, Daily.name, OnlineHistory.name, MyCollections.name, Account.name)
    }
    return rememberZhihuMainPreferenceState(
        settings = settings,
        observedKeys = mainPreferenceKeys,
        readSnapshot = {
            val duo3HomeAccount = settings.getBoolean("duo3_home_account", false)
            val selectedKeys = normalizeBottomBarSelection(
                settings.getStringSet(
                    BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
                    defaultBottomBarSelectionKeys(duo3HomeAccount, platformBottomBarItemLimit),
                ),
                duo3HomeAccount,
                enforceMinimumSelection = true,
                maximumSelection = platformBottomBarItemLimit,
            )
            val orderedSelectedKeys = bottomBarItemOrderFromPreference(
                settings.getStringOrNull(BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY),
                selectedKeys,
            )
            ZhihuMainPreferenceSnapshot(
                duo3HomeAccount = duo3HomeAccount,
                tapToScrollToTopEnabled = settings.getBoolean("bottomBarTapScrollToTop", true),
                autoHideBottomBar = settings.getBoolean("autoHideBottomBar", false),
                autoHideTopBar = settings.getBoolean("autoHideTopBar", false),
                collectionDirectBrowseEnabled = settings.getBoolean(COLLECTION_DIRECT_BROWSE_PREFERENCE_KEY, false),
                selectedBottomBarItemKeys = orderedSelectedKeys,
                startDestination = navDestinationFromName(
                    resolveValidStartDestinationKey(
                        settings.getString(START_DESTINATION_PREFERENCE_KEY, Home.name),
                        orderedSelectedKeys.ifEmpty { allBottomBarItemKeys.filter { it in selectedKeys } },
                    ),
                ),
            )
        },
    )
}

@Composable
fun rememberAndroidZhihuMainActivity(): MainActivity = LocalActivity.current as MainActivity
