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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

@Stable
internal class ArticleBottomBarState {
    val offset = Animatable(0f)
    var heightPx by mutableFloatStateOf(0f)
    var isScrollingUp by mutableStateOf(false)
    var showSlot by mutableStateOf(false)
        internal set
    internal var navigationBarHeightPx by mutableFloatStateOf(0f)
    internal var previousDirectionScrollValue by mutableIntStateOf(0)
    internal var previousOffsetScrollValue by mutableIntStateOf(0)
    internal var isSnapping by mutableStateOf(false)

    val obscuredHeightPx by derivedStateOf {
        val visibleHeight = if (showSlot) {
            (heightPx - offset.value).coerceIn(0f, heightPx)
        } else {
            0f
        }
        navigationBarHeightPx + visibleHeight
    }
}

@Composable
internal fun rememberArticleBottomBarState(
    scrollState: ScrollState,
    autoHide: Boolean,
    scrollDeltaThreshold: Float,
    showSlot: Boolean,
    navigationBarHeightPx: Float,
): ArticleBottomBarState {
    val state = remember { ArticleBottomBarState() }

    SideEffect {
        state.showSlot = showSlot
        state.navigationBarHeightPx = navigationBarHeightPx
    }
    LaunchedEffect(autoHide) {
        if (!autoHide) state.offset.snapTo(0f)
    }
    LaunchedEffect(scrollState, autoHide) {
        snapshotFlow { scrollState.value }.collectLatest { currentScroll ->
            if (abs(currentScroll - state.previousDirectionScrollValue) > scrollDeltaThreshold) {
                state.isScrollingUp = currentScroll < state.previousDirectionScrollValue
                state.previousDirectionScrollValue = currentScroll
            }

            if (!state.isSnapping) {
                val delta = currentScroll - state.previousOffsetScrollValue
                if (currentScroll == 0) {
                    state.offset.snapTo(0f)
                } else if (autoHide && state.heightPx > 0f) {
                    val deltaBasedOffset = (state.offset.value + delta).coerceIn(0f, state.heightPx)
                    val distanceFromBottom = (scrollState.maxValue - currentScroll).coerceAtLeast(0)
                    if (distanceFromBottom < state.heightPx.toInt()) {
                        val distanceBasedOffset = distanceFromBottom.toFloat().coerceIn(0f, state.heightPx)
                        state.offset.snapTo(minOf(distanceBasedOffset, deltaBasedOffset))
                    } else {
                        state.offset.snapTo(deltaBasedOffset)
                    }
                }
            }
            state.previousOffsetScrollValue = currentScroll
        }
    }

    LaunchedEffect(scrollState, autoHide) {
        snapshotFlow { scrollState.isScrollInProgress }.collectLatest { isScrollInProgress ->
            if (isScrollInProgress) return@collectLatest
            val target = if (autoHide && state.heightPx > 0f) {
                if (state.offset.value > state.heightPx / 2) state.heightPx else 0f
            } else {
                state.offset.value
            }
            if (target != state.offset.value) {
                try {
                    state.isSnapping = true
                    state.offset.animateTo(target, tween(150))
                } finally {
                    state.isSnapping = false
                }
            }
        }
    }

    return state
}
