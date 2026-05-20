package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.TopLevelDestination

data class ZhihuMainPreferenceSnapshot(
    val duo3HomeAccount: Boolean,
    val duo3NavStyle: Boolean,
    val tapToScrollToTopEnabled: Boolean,
    val autoHideBottomBar: Boolean,
    val selectedBottomBarItemKeys: Set<String>,
    val startDestination: TopLevelDestination,
)

class ZhihuMainPreferenceState(
    private val readSnapshot: () -> ZhihuMainPreferenceSnapshot,
) {
    private val initialSnapshot = readSnapshot()

    var duo3HomeAccount by mutableStateOf(initialSnapshot.duo3HomeAccount)
        private set
    var duo3NavStyle by mutableStateOf(initialSnapshot.duo3NavStyle)
        private set
    var tapToScrollToTopEnabled by mutableStateOf(initialSnapshot.tapToScrollToTopEnabled)
        private set
    var autoHideBottomBar by mutableStateOf(initialSnapshot.autoHideBottomBar)
        private set
    var selectedBottomBarItemKeys by mutableStateOf(initialSnapshot.selectedBottomBarItemKeys)
        private set
    var startDestination by mutableStateOf(initialSnapshot.startDestination)
        private set

    fun reload() {
        val snapshot = readSnapshot()
        duo3HomeAccount = snapshot.duo3HomeAccount
        duo3NavStyle = snapshot.duo3NavStyle
        tapToScrollToTopEnabled = snapshot.tapToScrollToTopEnabled
        autoHideBottomBar = snapshot.autoHideBottomBar
        selectedBottomBarItemKeys = snapshot.selectedBottomBarItemKeys
        startDestination = snapshot.startDestination
    }
}

@Composable
fun rememberZhihuMainPreferenceState(
    readSnapshot: () -> ZhihuMainPreferenceSnapshot,
): ZhihuMainPreferenceState = remember {
    ZhihuMainPreferenceState(readSnapshot)
}

data class ZhihuMainActivityState(
    val mainTabNavigationTarget: TopLevelDestination?,
    val navigate: (NavDestination) -> Unit,
    val setCurrentMainTabOpenFrom: (String?) -> Unit,
    val consumeMainTabNavigationTarget: (TopLevelDestination) -> Unit,
)
