@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.tanh

/**
 * 上下 overscroll 切换回答容器。
 * 当内容滚动到顶部/底部边界后继续滑动，显示预览卡片并提供切换。
 * 预览卡片固定在视口边缘，主内容滑动露出卡片。
 */
@Composable
fun AnswerVerticalOverscroll(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    previousAuthorName: String,
    previousExcerpt: String,
    previousAvatarUrl: String,
    nextAuthorName: String,
    nextExcerpt: String,
    nextAvatarUrl: String,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    isAtTop: () -> Boolean,
    isAtBottom: () -> Boolean,
    scrollState: ScrollState,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val maxOverscrollPx = with(density) { MAX_OVERSCROLL_DP.dp.toPx() }
    val triggerThresholdPx = with(density) { TRIGGER_THRESHOLD_DP.dp.toPx() }

    val overscrollOffset = remember { Animatable(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    var rawDragAccumulator by remember { mutableFloatStateOf(0f) }

    fun dampedOffset(rawDelta: Float): Float {
        val sign = if (rawDelta >= 0) 1f else -1f
        return sign * maxOverscrollPx * tanh(abs(rawDelta) / (maxOverscrollPx * DAMPING_FACTOR)).toFloat()
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (overscrollOffset.value != 0f && !overscrollOffset.isRunning) {
                    val delta = available.y
                    val currentOffset = overscrollOffset.value
                    if ((currentOffset > 0 && delta < 0) || (currentOffset < 0 && delta > 0)) {
                        val newRaw = rawDragAccumulator + delta
                        if ((rawDragAccumulator > 0 && newRaw <= 0) || (rawDragAccumulator < 0 && newRaw >= 0)) {
                            rawDragAccumulator = 0f
                            coroutineScope.launch { overscrollOffset.snapTo(0f) }
                            hasTriggeredHaptic = false
                            return Offset(0f, delta)
                        }
                        rawDragAccumulator = newRaw
                        val newOffset = dampedOffset(rawDragAccumulator)
                        coroutineScope.launch { overscrollOffset.snapTo(newOffset) }
                        if (hasTriggeredHaptic && abs(newOffset) < triggerThresholdPx) {
                            hasTriggeredHaptic = false
                        }
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

                val atTop = isAtTop()
                val atBottom = isAtBottom()
                val canOverscrollDown = delta > 0 && atTop && canGoPrevious
                val canOverscrollUp = delta < 0 && atBottom && canGoNext
                if (canOverscrollDown ||
                    canOverscrollUp ||
                    (overscrollOffset.value > 0 && delta > 0) ||
                    (overscrollOffset.value < 0 && delta < 0)
                ) {
                    rawDragAccumulator += delta
                    val newOffset = dampedOffset(rawDragAccumulator)
                    coroutineScope.launch { overscrollOffset.snapTo(newOffset) }
                    if (!hasTriggeredHaptic && abs(newOffset) >= triggerThresholdPx) {
                        hasTriggeredHaptic = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (overscrollOffset.value != 0f) {
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
                    overscrollOffset.animateTo(
                        0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                    return available
                }
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    // When content fits viewport (maxValue==0), verticalScroll doesn't generate
    // nested scroll events. Use a fallback pointerInput for direct drag handling.
    val isContentNonScrollable = scrollState.maxValue == 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .nestedScroll(nestedScrollConnection)
            .then(
                if (isContentNonScrollable) {
                    Modifier.pointerInput(canGoPrevious, canGoNext) {
                        detectVerticalDragGestures(
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
                            val canDragDown = dragAmount > 0 && canGoPrevious
                            val canDragUp = dragAmount < 0 && canGoNext
                            val continuing = (overscrollOffset.value > 0 && dragAmount > 0) ||
                                (overscrollOffset.value < 0 && dragAmount < 0)
                            val reversing = (overscrollOffset.value > 0 && dragAmount < 0) ||
                                (overscrollOffset.value < 0 && dragAmount > 0)

                            if (canDragDown || canDragUp || continuing || reversing) {
                                rawDragAccumulator += dragAmount
                                // Clamp to zero crossing
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
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        // 预览卡片在底层，被主内容覆盖，主内容滑动时露出

        // 上方预览卡片（上一个回答）—— 固定在顶部
        if (overscrollOffset.value > 0 && canGoPrevious) {
            val progress = (overscrollOffset.value / triggerThresholdPx).coerceIn(0f, 1.5f)
            AnswerPreviewCard(
                authorName = previousAuthorName,
                excerpt = previousExcerpt,
                avatarUrl = previousAvatarUrl,
                label = "上一个回答",
                icon = Icons.Filled.ArrowUpward,
                isTriggered = overscrollOffset.value >= triggerThresholdPx,
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
            )
        }

        // 下方预览卡片（下一个回答）—— 固定在底部，内容顺序反转
        if (overscrollOffset.value < 0 && canGoNext) {
            val progress = (abs(overscrollOffset.value) / triggerThresholdPx).coerceIn(0f, 1.5f)
            AnswerPreviewCard(
                authorName = nextAuthorName,
                excerpt = nextExcerpt,
                avatarUrl = nextAvatarUrl,
                label = "下一个回答",
                icon = Icons.Filled.ArrowDownward,
                isTriggered = abs(overscrollOffset.value) >= triggerThresholdPx,
                progress = progress,
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            )
        }

        // 主内容在最上层，跟随 overscroll 偏移，滑动露出预览卡片
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, overscrollOffset.value.roundToInt()) },
        ) {
            content()
        }
    }
}

@Composable
private fun AnswerPreviewCard(
    authorName: String,
    excerpt: String,
    avatarUrl: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isTriggered: Boolean,
    progress: Float,
    reverseLayout: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isTriggered) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isTriggered) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .alpha((progress).coerceIn(0f, 1f))
            .padding(12.dp),
    ) {
        val labelRow: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isTriggered) "松手切换" else label,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    fontWeight = if (isTriggered) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
        val authorRow: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "头像",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (authorName.isNotEmpty()) {
                        Text(
                            text = authorName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                        )
                    }
                    if (excerpt.isNotEmpty()) {
                        Text(
                            text = excerpt,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }
        Column {
            if (reverseLayout) {
                authorRow()
                Spacer(Modifier.height(8.dp))
                labelRow()
            } else {
                labelRow()
                Spacer(Modifier.height(8.dp))
                authorRow()
            }
        }
    }
}

private const val MAX_OVERSCROLL_DP = 200f
private const val TRIGGER_THRESHOLD_DP = 80f
private const val DAMPING_FACTOR = 1.2f
