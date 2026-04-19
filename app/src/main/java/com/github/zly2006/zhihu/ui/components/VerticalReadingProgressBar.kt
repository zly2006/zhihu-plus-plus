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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun calculateReadingProgress(
    scrollValue: Int,
    maxValue: Int,
): Float {
    if (maxValue <= 0 || maxValue == Int.MAX_VALUE) return 0f
    return (scrollValue.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
}

internal fun calculateScrollbarThumbHeightPx(
    viewportHeightPx: Float,
    maxScrollPx: Int,
    minThumbHeightPx: Float,
): Float {
    if (viewportHeightPx <= 0f || maxScrollPx < 0 || maxScrollPx == Int.MAX_VALUE) return 0f
    val contentHeightPx = viewportHeightPx + maxScrollPx
    if (contentHeightPx <= 0f) return 0f
    val rawThumbHeight = viewportHeightPx * (viewportHeightPx / contentHeightPx)
    return rawThumbHeight.coerceIn(minThumbHeightPx, viewportHeightPx)
}

internal fun calculateScrollbarThumbOffsetPx(
    progress: Float,
    viewportHeightPx: Float,
    thumbHeightPx: Float,
): Float {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val trackRangePx = (viewportHeightPx - thumbHeightPx).coerceAtLeast(0f)
    return (trackRangePx * clampedProgress).coerceIn(0f, trackRangePx)
}

@Composable
fun VerticalReadingProgressBar(
    scrollState: ScrollState,
    scrollValue: Int = scrollState.value,
    maxScrollValue: Int = scrollState.maxValue,
    isScrollInProgress: Boolean = scrollState.isScrollInProgress,
    modifier: Modifier = Modifier,
    fadeOutDelayMillis: Long = 1200L,
    minThumbHeight: Dp = 24.dp,
    trackColor: Color = Color.Unspecified,
    thumbColor: Color = Color.Unspecified,
) {
    val showProgressBar by remember(maxScrollValue) {
        derivedStateOf { maxScrollValue > 0 && maxScrollValue != Int.MAX_VALUE }
    }
    var shouldKeepVisible by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(showProgressBar, isScrollInProgress, isDragging, fadeOutDelayMillis) {
        if (!showProgressBar) {
            shouldKeepVisible = false
            return@LaunchedEffect
        }
        shouldKeepVisible = true
        if (!isScrollInProgress && !isDragging) {
            delay(fadeOutDelayMillis)
            if (!isScrollInProgress && !isDragging) {
                shouldKeepVisible = false
            }
        }
    }

    val progress by remember(scrollValue, maxScrollValue) {
        derivedStateOf {
            calculateReadingProgress(
                scrollValue = scrollValue,
                maxValue = maxScrollValue,
            )
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (shouldKeepVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (shouldKeepVisible) 120 else 260),
        label = "readingProgressBarAlpha",
    )
    if (alpha <= 0f && !isDragging) return

    val resolvedTrackColor = if (trackColor == Color.Unspecified) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    } else {
        trackColor
    }
    val resolvedThumbColor = if (thumbColor == Color.Unspecified) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
    } else {
        thumbColor
    }

    BoxWithConstraints(
        modifier = modifier
            .width(12.dp)
            .fillMaxHeight()
            .graphicsLayer { this.alpha = alpha },
    ) {
        val density = LocalDensity.current
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val minThumbHeightPx = with(density) { minThumbHeight.toPx() }
        val thumbHeightPx = calculateScrollbarThumbHeightPx(
            viewportHeightPx = viewportHeightPx,
            maxScrollPx = maxScrollValue,
            minThumbHeightPx = minThumbHeightPx,
        )
        if (thumbHeightPx <= 0f) return@BoxWithConstraints

        val thumbHeight = with(density) { thumbHeightPx.toDp() }
        val maxThumbOffsetPx = (viewportHeightPx - thumbHeightPx).coerceAtLeast(0f)
        val scope = rememberCoroutineScope()

        val thumbOffsetY = with(density) {
            calculateScrollbarThumbOffsetPx(
                progress = progress,
                viewportHeightPx = viewportHeightPx,
                thumbHeightPx = thumbHeightPx,
            ).toDp()
        }

        val draggableState = rememberDraggableState { delta ->
            if (!isDragging) return@rememberDraggableState
            val currentOffset = calculateScrollbarThumbOffsetPx(
                progress = progress,
                viewportHeightPx = viewportHeightPx,
                thumbHeightPx = thumbHeightPx,
            )
            val newOffset = (currentOffset + delta).coerceIn(0f, maxThumbOffsetPx)
            val newProgress = if (maxThumbOffsetPx > 0f) newOffset / maxThumbOffsetPx else 0f
            scope.launch {
                scrollState.scrollTo((newProgress * maxScrollValue).toInt())
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(2.dp)
                .clip(RoundedCornerShape(50))
                .background(resolvedTrackColor),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = thumbOffsetY)
                .width(4.dp)
                .height(thumbHeight)
                .clip(RoundedCornerShape(50))
                .background(resolvedThumbColor)
                .draggable(
                    state = draggableState,
                    startDragImmediately = true,
                    orientation = Orientation.Vertical,
                    onDragStarted = { isDragging = true },
                    onDragStopped = { isDragging = false },
                ),
        )
    }
}
