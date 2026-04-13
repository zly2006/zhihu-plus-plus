/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

@Composable
fun DummyLocalNavigator(content: @Composable () -> Unit) {
    val navigator = Navigator(
        onNavigate = {},
        onNavigateBack = {},
    )
    CompositionLocalProvider(LocalNavigator provides navigator, content)
}
