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

package com.github.zly2006.zhihu.ui

enum class TopLevelReselectAction {
    ScrollToTop,
    Refresh,
}

fun topLevelReselectAction(
    triggerDelta: Int,
    isAtTop: Boolean,
): TopLevelReselectAction? = when {
    triggerDelta <= 0 -> null
    triggerDelta >= 2 || isAtTop -> TopLevelReselectAction.Refresh
    else -> TopLevelReselectAction.ScrollToTop
}
