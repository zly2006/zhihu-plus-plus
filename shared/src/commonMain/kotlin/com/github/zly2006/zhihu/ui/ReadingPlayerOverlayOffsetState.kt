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

package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
internal class ReadingPlayerOverlayOffsetState {
    private var activeRouteId: String? = null

    var verticalOffsetPx by mutableFloatStateOf(0f)
        private set

    fun beginRoute(routeId: String) {
        activeRouteId = routeId
        verticalOffsetPx = 0f
    }

    fun update(routeId: String, offsetPx: Float) {
        if (activeRouteId == routeId) {
            verticalOffsetPx = offsetPx.coerceAtMost(0f)
        }
    }

    fun endRoute(routeId: String) {
        if (activeRouteId == routeId) {
            activeRouteId = null
            verticalOffsetPx = 0f
        }
    }

    fun resetOffset() {
        verticalOffsetPx = 0f
    }

    fun clearRoute() {
        activeRouteId = null
        verticalOffsetPx = 0f
    }
}

internal val LocalReadingPlayerOverlayOffsetState =
    staticCompositionLocalOf<ReadingPlayerOverlayOffsetState?> { null }
