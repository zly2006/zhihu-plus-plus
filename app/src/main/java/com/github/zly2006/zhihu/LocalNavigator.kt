package com.github.zly2006.zhihu

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal for navigation callback
 * Avoids passing onNavigate parameter through every function
 */
val LocalNavigator = compositionLocalOf<(NavDestination) -> Unit> {
    error("LocalNavigator not provided")
}
