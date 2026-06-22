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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 左右回答切换容器。
 *
 * 视觉上按三页 pager 处理：左侧上一页、中央当前页、右侧下一页。拖动未触发时回弹；触发后先完成整页滑动，
 * 再把真正的 route 交给调用方替换，避免 NavHost 再播放一次入场动画。
 */
@Composable
fun AnswerHorizontalOverscroll(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    previousContent: (@Composable () -> Unit)?,
    nextContent: (@Composable () -> Unit)?,
    answerSwitchSensitivity: Float = DEFAULT_ANSWER_SWITCH_SENSITIVITY,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val minimumTriggerThresholdPx = with(density) {
        (MIN_HORIZONTAL_TRIGGER_THRESHOLD_DP / normalizedAnswerSwitchSensitivity(answerSwitchSensitivity)).dp.toPx()
    }
    val pageOffset = remember { Animatable(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    var isCommitting by remember { mutableStateOf(false) }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val commitThresholdPx = max(containerWidthPx * HORIZONTAL_COMMIT_RATIO, minimumTriggerThresholdPx)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .pointerInput(canGoPrevious, canGoNext, commitThresholdPx, containerWidthPx) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        if (isCommitting) return@detectHorizontalDragGestures
                        hasTriggeredHaptic = false
                        coroutineScope.launch { pageOffset.stop() }
                    },
                    onDragEnd = {
                        if (isCommitting) return@detectHorizontalDragGestures
                        val currentOffset = pageOffset.value
                        val targetDirection = when {
                            currentOffset >= commitThresholdPx && canGoPrevious -> 1
                            currentOffset <= -commitThresholdPx && canGoNext -> -1
                            else -> 0
                        }
                        coroutineScope.launch {
                            hasTriggeredHaptic = false
                            if (targetDirection == 0 || containerWidthPx <= 0f) {
                                pageOffset.animateTo(
                                    0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                )
                                return@launch
                            }
                            isCommitting = true
                            try {
                                pageOffset.animateTo(
                                    targetDirection * containerWidthPx,
                                    animationSpec = tween(HORIZONTAL_COMMIT_ANIMATION_MS),
                                )
                                if (targetDirection > 0) {
                                    onNavigatePrevious()
                                } else {
                                    onNavigateNext()
                                }
                            } finally {
                                isCommitting = false
                            }
                        }
                    },
                    onDragCancel = {
                        if (!isCommitting) {
                            coroutineScope.launch {
                                hasTriggeredHaptic = false
                                pageOffset.animateTo(
                                    0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                )
                            }
                        }
                    },
                ) { _, dragAmount ->
                    if (isCommitting || containerWidthPx <= 0f) return@detectHorizontalDragGestures
                    val canDragRight = dragAmount > 0 && canGoPrevious
                    val canDragLeft = dragAmount < 0 && canGoNext
                    val continuing = (pageOffset.value > 0 && dragAmount > 0) ||
                        (pageOffset.value < 0 && dragAmount < 0)
                    val reversing = (pageOffset.value > 0 && dragAmount < 0) ||
                        (pageOffset.value < 0 && dragAmount > 0)

                    if (canDragRight || canDragLeft || continuing || reversing) {
                        val newOffset = (pageOffset.value + dragAmount)
                            .coerceIn(
                                if (canGoNext) -containerWidthPx else 0f,
                                if (canGoPrevious) containerWidthPx else 0f,
                            )
                        coroutineScope.launch { pageOffset.snapTo(newOffset) }
                        if (!hasTriggeredHaptic && abs(newOffset) >= commitThresholdPx) {
                            hasTriggeredHaptic = true
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        if (hasTriggeredHaptic && abs(newOffset) < commitThresholdPx) {
                            hasTriggeredHaptic = false
                        }
                    }
                }
            },
    ) {
        // 左侧相邻回答（上一个），始终存活以避免预览重建闪动。
        if (previousContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            (pageOffset.value - containerWidthPx).roundToInt(),
                            0,
                        )
                    },
            ) {
                previousContent()
            }
        }

        // 右侧相邻回答（下一个），始终存活以避免预览重建闪动。
        if (nextContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            (pageOffset.value + containerWidthPx).roundToInt(),
                            0,
                        )
                    },
            ) {
                nextContent()
            }
        }

        // 主内容，跟随水平 overscroll 偏移。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(pageOffset.value.roundToInt(), 0) },
        ) {
            content()
        }
    }
}

private const val MIN_HORIZONTAL_TRIGGER_THRESHOLD_DP = 46f
private const val HORIZONTAL_COMMIT_RATIO = 0.25f
private const val HORIZONTAL_COMMIT_ANIMATION_MS = 180
