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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue

val LocalVerticalPagerScrollGate = staticCompositionLocalOf<VerticalPagerScrollGate?> { null }

@Stable
class VerticalPagerScrollGate internal constructor(
    private val triggerThresholdPx: Float,
    private val maxOverscrollPx: Float,
    private val onNavigatePrevious: () -> Unit,
    private val onNavigateNext: () -> Unit,
    private val canGoPrevious: () -> Boolean,
    private val canGoNext: () -> Boolean,
    private val performHaptic: () -> Unit,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope,
) {
    private var rawDragAccumulator by mutableFloatStateOf(0f)
    private var hasTriggeredHaptic by mutableStateOf(false)
    private var overscrollOffset by mutableFloatStateOf(0f)

    fun nestedScrollConnection(
        isAtTop: () -> Boolean,
        isAtBottom: () -> Boolean,
    ): NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (overscrollOffset != 0f) {
                val delta = available.y
                if ((overscrollOffset > 0 && delta < 0) || (overscrollOffset < 0 && delta > 0)) {
                    val newRaw = rawDragAccumulator + delta
                    if ((rawDragAccumulator > 0 && newRaw <= 0) || (rawDragAccumulator < 0 && newRaw >= 0)) {
                        resetOverscroll()
                        return Offset(0f, delta)
                    }
                    rawDragAccumulator = newRaw
                    updateOverscrollOffset()
                    return Offset(0f, delta)
                }
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            val delta = available.y
            if (delta == 0f) return Offset.Zero

            val canOverscrollDown = delta > 0 && isAtTop() && canGoPrevious()
            val canOverscrollUp = delta < 0 && isAtBottom() && canGoNext()
            if (canOverscrollDown ||
                canOverscrollUp ||
                (overscrollOffset > 0 && delta > 0) ||
                (overscrollOffset < 0 && delta < 0)
            ) {
                rawDragAccumulator += delta
                updateOverscrollOffset()
                return Offset(0f, delta)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (overscrollOffset != 0f) {
                commitOrReset()
                return available
            }
            return Velocity.Zero
        }
    }

    fun nonScrollablePointerInputModifier(): Modifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                resetOverscroll()
                var dragStarted = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break

                    val delta = change.positionChange().y
                    if (!dragStarted) {
                        val totalDx = change.position.x - down.position.x
                        val totalDy = change.position.y - down.position.y
                        if (totalDy.absoluteValue <= viewConfiguration.touchSlop ||
                            totalDy.absoluteValue <= totalDx.absoluteValue
                        ) {
                            continue
                        }
                        dragStarted = true
                    }

                    val canDragDown = delta > 0 && canGoPrevious()
                    val canDragUp = delta < 0 && canGoNext()
                    val continuing = (overscrollOffset > 0 && delta > 0) ||
                        (overscrollOffset < 0 && delta < 0)
                    val reversing = (overscrollOffset > 0 && delta < 0) ||
                        (overscrollOffset < 0 && delta > 0)

                    if (canDragDown || canDragUp || continuing || reversing) {
                        change.consume()
                        rawDragAccumulator += delta
                        if ((rawDragAccumulator > 0 && !canGoPrevious()) ||
                            (rawDragAccumulator < 0 && !canGoNext())
                        ) {
                            rawDragAccumulator = 0f
                        }
                        updateOverscrollOffset()
                    }
                }
                commitOrReset()
            }
        }
    }

    private fun updateOverscrollOffset() {
        val newOffset = dampedOverscrollOffset(
            rawDelta = rawDragAccumulator,
            maxOverscrollPx = maxOverscrollPx,
            dampingFactor = DAMPING_FACTOR,
        )
        overscrollOffset = newOffset
        if (!hasTriggeredHaptic && abs(newOffset) >= triggerThresholdPx) {
            hasTriggeredHaptic = true
            performHaptic()
        }
        if (hasTriggeredHaptic && abs(newOffset) < triggerThresholdPx) {
            hasTriggeredHaptic = false
        }
    }

    private fun commitOrReset() {
        val currentOffset = overscrollOffset
        var didNavigate = false
        if (abs(currentOffset) >= triggerThresholdPx) {
            if (currentOffset > 0 && canGoPrevious()) {
                onNavigatePrevious()
                didNavigate = true
            } else if (currentOffset < 0 && canGoNext()) {
                onNavigateNext()
                didNavigate = true
            }
        }
        if (!didNavigate) {
            resetOverscroll()
        } else {
            rawDragAccumulator = 0f
            hasTriggeredHaptic = false
            overscrollOffset = 0f
        }
    }

    private fun resetOverscroll() {
        rawDragAccumulator = 0f
        hasTriggeredHaptic = false
        overscrollOffset = 0f
    }
}

@Composable
fun rememberVerticalPagerScrollGate(
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    canGoPrevious: () -> Boolean,
    canGoNext: () -> Boolean,
    answerSwitchSensitivity: Float = DEFAULT_ANSWER_SWITCH_SENSITIVITY,
): VerticalPagerScrollGate {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val maxOverscrollPx = with(density) { MAX_OVERSCROLL_DP.dp.toPx() }
    val triggerThresholdPx = with(density) {
        (TRIGGER_THRESHOLD_DP / normalizedAnswerSwitchSensitivity(answerSwitchSensitivity)).dp.toPx()
    }
    val currentOnNavigatePrevious by rememberUpdatedState(onNavigatePrevious)
    val currentOnNavigateNext by rememberUpdatedState(onNavigateNext)
    val currentCanGoPrevious by rememberUpdatedState(canGoPrevious)
    val currentCanGoNext by rememberUpdatedState(canGoNext)

    return remember(triggerThresholdPx, maxOverscrollPx) {
        VerticalPagerScrollGate(
            triggerThresholdPx = triggerThresholdPx,
            maxOverscrollPx = maxOverscrollPx,
            onNavigatePrevious = { currentOnNavigatePrevious() },
            onNavigateNext = { currentOnNavigateNext() },
            canGoPrevious = { currentCanGoPrevious() },
            canGoNext = { currentCanGoNext() },
            performHaptic = {
                coroutineScope.launch {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            },
            coroutineScope = coroutineScope,
        )
    }
}

@Composable
fun Modifier.verticalPagerScrollGate(
    scrollState: ScrollState,
    enabled: Boolean,
): Modifier {
    val gate = LocalVerticalPagerScrollGate.current ?: return this
    if (!enabled) return this
    // 须挂在 fillMaxSize 容器上；内层 Column 也需 fillMaxSize + verticalScroll，
    // 否则短正文下方空白区域不在 scroll 命中范围内，无法切页。
    val isContentNonScrollable = scrollState.maxValue == 0
    if (isContentNonScrollable) {
        return this.then(gate.nonScrollablePointerInputModifier())
    }
    val connection = remember(gate, scrollState) {
        gate.nestedScrollConnection(
            isAtTop = { scrollState.value == 0 },
            isAtBottom = { scrollState.value >= scrollState.maxValue },
        )
    }
    return this.nestedScroll(connection)
}

private const val MAX_OVERSCROLL_DP = 200f
private const val TRIGGER_THRESHOLD_DP = 80f
private const val DAMPING_FACTOR = 1.2f

internal fun dampedOverscrollOffset(
    rawDelta: Float,
    maxOverscrollPx: Float,
    dampingFactor: Float,
): Float {
    val sign = if (rawDelta >= 0) 1f else -1f
    return sign * maxOverscrollPx * kotlin.math.tanh(abs(rawDelta) / (maxOverscrollPx * dampingFactor))
}
