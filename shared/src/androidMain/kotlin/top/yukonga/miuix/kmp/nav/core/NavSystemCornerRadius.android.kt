// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reads the device screen corner radius. Prefers the standard [RoundedCorner] window-insets API
 * (API 31+, bottom-left position); otherwise falls back to the framework `rounded_corner_radius_bottom`
 * dimen, and finally to `0.dp` for flat-corner screens.
 */
@Composable
actual fun rememberNavSystemCornerRadius(): Dp {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val view = LocalView.current
    val insets = view.rootWindowInsets
    val radiusPx = remember(context, view, insets) {
        val fromInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            insets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius?.takeIf { it > 0 }
        } else {
            null
        }
        fromInsets ?: bottomCornerRadiusFromResources(context)
    }
    return (radiusPx / density).dp
}

/** Framework `android`-package dimen fallback for the bottom screen corner radius; 0 when absent. */
@SuppressLint("DiscouragedApi")
private fun bottomCornerRadiusFromResources(context: Context): Int {
    val id = context.resources.getIdentifier("rounded_corner_radius_bottom", "dimen", "android")
    return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
}
