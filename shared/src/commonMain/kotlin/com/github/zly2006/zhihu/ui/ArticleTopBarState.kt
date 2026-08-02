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
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

@Stable
internal class ArticleTopBarState {
    val offset = Animatable(0f)
    var heightPx by mutableFloatStateOf(0f)
    internal var previousScrollValue by mutableIntStateOf(0)
    internal var isSnapping by mutableStateOf(false)
}

@Composable
internal fun rememberArticleTopBarState(
    scrollState: ScrollState,
    autoHide: Boolean,
): ArticleTopBarState {
    val state = remember { ArticleTopBarState() }

    LaunchedEffect(autoHide) {
        if (!autoHide) state.offset.snapTo(0f)
    }
    LaunchedEffect(scrollState, autoHide) {
        snapshotFlow { scrollState.value }.collectLatest { currentScroll ->
            if (!state.isSnapping) {
                val delta = currentScroll - state.previousScrollValue
                if (currentScroll == 0) {
                    state.offset.snapTo(0f)
                } else if (autoHide && state.heightPx > 0f) {
                    val deltaBasedOffset = (state.offset.value - delta).coerceIn(-state.heightPx, 0f)
                    val distanceFromBottom = (scrollState.maxValue - currentScroll).coerceAtLeast(0)
                    if (distanceFromBottom < state.heightPx.toInt()) {
                        val distanceBasedOffset = (-distanceFromBottom.toFloat()).coerceIn(-state.heightPx, 0f)
                        state.offset.snapTo(maxOf(distanceBasedOffset, deltaBasedOffset))
                    } else {
                        state.offset.snapTo(deltaBasedOffset)
                    }
                }
            }
            state.previousScrollValue = currentScroll
        }
    }

    LaunchedEffect(scrollState, autoHide) {
        snapshotFlow { scrollState.isScrollInProgress }.collectLatest { isScrollInProgress ->
            if (isScrollInProgress) return@collectLatest
            val target = if (autoHide && state.heightPx > 0f) {
                if (abs(state.offset.value) > state.heightPx / 2) -state.heightPx else 0f
            } else {
                state.offset.value
            }
            val scrollDelta = if (scrollState.value <= state.heightPx) state.offset.value - target else 0f
            if (target != state.offset.value) {
                try {
                    state.isSnapping = true
                    kotlinx.coroutines.coroutineScope {
                        launch { state.offset.animateTo(target, tween(150)) }
                        if (scrollDelta != 0f) {
                            launch { scrollState.animateScrollBy(scrollDelta, tween(150)) }
                        }
                    }
                } finally {
                    state.isSnapping = false
                }
            }
        }
    }

    return state
}
