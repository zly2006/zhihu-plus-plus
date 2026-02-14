package com.github.zly2006.zhihu

import androidx.compose.runtime.compositionLocalOf

/**
 * Navigator class that holds navigation callbacks
 */
data class Navigator(
    val onNavigate: (NavDestination) -> Unit,
    val onNavigateBack: () -> Unit,
)

/**
 * CompositionLocal for navigation callbacks
 * Avoids passing onNavigate and onNavigateBack parameters through every function
 */
val LocalNavigator = compositionLocalOf<Navigator> {
    error("LocalNavigator not provided")
}
