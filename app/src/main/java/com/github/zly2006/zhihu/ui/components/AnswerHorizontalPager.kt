@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import kotlin.math.roundToInt
import kotlin.math.tanh

/**
 * 左右 overscroll 切换回答容器。
 * 主内容水平偏移，相邻回答的完整内容（标题、作者、WebView）从侧边露出。
 * 使用 tanh 阻尼、震动反馈、spring 回弹。
 */
@Composable
fun AnswerHorizontalOverscroll(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    previousContent: (@Composable () -> Unit)?,
    nextContent: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val maxOverscrollPx = with(density) { MAX_HORIZONTAL_OVERSCROLL_DP.dp.toPx() }
    val triggerThresholdPx = with(density) { HORIZONTAL_TRIGGER_THRESHOLD_DP.dp.toPx() }

    val overscrollOffset = remember { Animatable(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    var rawDragAccumulator by remember { mutableFloatStateOf(0f) }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    fun dampedOffset(rawDelta: Float): Float {
        val sign = if (rawDelta >= 0) 1f else -1f
        return sign * maxOverscrollPx *
            tanh(abs(rawDelta) / (maxOverscrollPx * HORIZONTAL_DAMPING_FACTOR)).toFloat()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .pointerInput(canGoPrevious, canGoNext) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        rawDragAccumulator = 0f
                        hasTriggeredHaptic = false
                    },
                    onDragEnd = {
                        val currentOffset = overscrollOffset.value
                        if (abs(currentOffset) >= triggerThresholdPx) {
                            if (currentOffset > 0 && canGoPrevious) {
                                onNavigatePrevious()
                            } else if (currentOffset < 0 && canGoNext) {
                                onNavigateNext()
                            }
                        }
                        rawDragAccumulator = 0f
                        hasTriggeredHaptic = false
                        coroutineScope.launch {
                            overscrollOffset.animateTo(
                                0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    },
                    onDragCancel = {
                        rawDragAccumulator = 0f
                        hasTriggeredHaptic = false
                        coroutineScope.launch { overscrollOffset.snapTo(0f) }
                    },
                ) { _, dragAmount ->
                    val canDragRight = dragAmount > 0 && canGoPrevious
                    val canDragLeft = dragAmount < 0 && canGoNext
                    val continuing = (overscrollOffset.value > 0 && dragAmount > 0) ||
                        (overscrollOffset.value < 0 && dragAmount < 0)
                    val reversing = (overscrollOffset.value > 0 && dragAmount < 0) ||
                        (overscrollOffset.value < 0 && dragAmount > 0)

                    if (canDragRight || canDragLeft || continuing || reversing) {
                        rawDragAccumulator += dragAmount
                        if ((rawDragAccumulator > 0 && !canGoPrevious) ||
                            (rawDragAccumulator < 0 && !canGoNext)
                        ) {
                            rawDragAccumulator = 0f
                        }
                        val newOffset = dampedOffset(rawDragAccumulator)
                        coroutineScope.launch { overscrollOffset.snapTo(newOffset) }
                        if (!hasTriggeredHaptic && abs(newOffset) >= triggerThresholdPx) {
                            hasTriggeredHaptic = true
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        if (hasTriggeredHaptic && abs(newOffset) < triggerThresholdPx) {
                            hasTriggeredHaptic = false
                        }
                    }
                }
            },
    ) {
        // 左侧相邻回答（上一个），定位在当前内容左侧
        if (overscrollOffset.value > 0 && canGoPrevious && previousContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            (overscrollOffset.value - containerWidthPx).roundToInt(),
                            0,
                        )
                    },
            ) {
                previousContent()
            }
        }

        // 右侧相邻回答（下一个），定位在当前内容右侧
        if (overscrollOffset.value < 0 && canGoNext && nextContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            (overscrollOffset.value + containerWidthPx).roundToInt(),
                            0,
                        )
                    },
            ) {
                nextContent()
            }
        }

        // 主内容，跟随水平 overscroll 偏移
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(overscrollOffset.value.roundToInt(), 0) },
        ) {
            content()
        }
    }
}

private const val MAX_HORIZONTAL_OVERSCROLL_DP = 300f
private const val HORIZONTAL_TRIGGER_THRESHOLD_DP = 120f
private const val HORIZONTAL_DAMPING_FACTOR = 1.2f
