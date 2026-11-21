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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.ui.article.voteUpNeutralContentDuo3
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.tanh

/**
 * 上下 overscroll 切换回答容器。
 * 预览卡片平时完全隐藏，仅在滚动到达顶部/底部止点后继续拖拽超过显隐阈值时才从边缘浮现，松手后自动回弹隐藏。
 * 当启用 123Duo3 键鼠增强时，支持在非正文可选区域使用鼠标左键按住拖拽触发上下回答切换，且采用 123Duo3 双层药丸卡片样式。
 */
@Composable
fun AnswerVerticalOverscroll(
    previousAnswer: CachedAnswerContent?,
    nextAnswer: CachedAnswerContent?,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    isAtTop: () -> Boolean,
    isAtBottom: () -> Boolean,
    scrollState: ScrollState,
    answerSwitchSensitivity: Float = DEFAULT_ANSWER_SWITCH_SENSITIVITY,
    isContentNonScrollable: Boolean = scrollState.maxValue == 0,
    enableMouseOverscroll: Boolean = false,
    duo3Style: Boolean = false,
    onOverscrollOffsetChange: (Float) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val maxOverscrollPx = with(density) { MAX_OVERSCROLL_DP.dp.toPx() }
    val triggerThresholdPx = with(density) {
        (TRIGGER_THRESHOLD_DP / normalizedAnswerSwitchSensitivity(answerSwitchSensitivity)).dp.toPx()
    }
    val revealThresholdPx = with(density) { REVEAL_THRESHOLD_DP.dp.toPx() }
        .coerceAtMost(triggerThresholdPx * 0.45f)
    val mouseSlopPx = with(density) { 6.dp.toPx() }

    val overscrollOffset = remember { Animatable(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    var rawDragAccumulator by remember { mutableFloatStateOf(0f) }
    val currentOnOverscrollOffsetChange by rememberUpdatedState(onOverscrollOffsetChange)

    LaunchedEffect(overscrollOffset) {
        snapshotFlow { overscrollOffset.value }
            .collect { currentOnOverscrollOffsetChange(it) }
    }

    // nestedScrollConnection 与手势处理器不带动态 key 重新 remember，统一通过 rememberUpdatedState 读取最新状态。
    val currentCanGoPrevious by rememberUpdatedState(previousAnswer != null)
    val currentCanGoNext by rememberUpdatedState(nextAnswer != null)
    val currentOnNavigatePrevious by rememberUpdatedState(onNavigatePrevious)
    val currentOnNavigateNext by rememberUpdatedState(onNavigateNext)
    val currentIsAtTop by rememberUpdatedState(isAtTop)
    val currentIsAtBottom by rememberUpdatedState(isAtBottom)
    val currentTriggerThresholdPx by rememberUpdatedState(triggerThresholdPx)
    val currentMaxOverscrollPx by rememberUpdatedState(maxOverscrollPx)

    fun finishOverscrollGesture() {
        val currentOffset = overscrollOffset.value
        var didNavigate = false
        if (abs(currentOffset) >= currentTriggerThresholdPx) {
            if (currentOffset > 0 && currentCanGoPrevious) {
                currentOnNavigatePrevious()
                didNavigate = true
            } else if (currentOffset < 0 && currentCanGoNext) {
                currentOnNavigateNext()
                didNavigate = true
            }
        }
        rawDragAccumulator = 0f
        hasTriggeredHaptic = false
        if (!didNavigate) {
            coroutineScope.launch {
                overscrollOffset.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
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
                        val newOffset = dampedOverscrollOffset(
                            rawDelta = rawDragAccumulator,
                            maxOverscrollPx = currentMaxOverscrollPx,
                            dampingFactor = DAMPING_FACTOR,
                        )
                        coroutineScope.launch { overscrollOffset.snapTo(newOffset) }
                        if (hasTriggeredHaptic && abs(newOffset) < currentTriggerThresholdPx) {
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

                val atTop = currentIsAtTop()
                val atBottom = currentIsAtBottom()
                val canOverscrollDown = delta > 0 && atTop && currentCanGoPrevious
                val canOverscrollUp = delta < 0 && atBottom && currentCanGoNext
                if (canOverscrollDown ||
                    canOverscrollUp ||
                    (overscrollOffset.value > 0 && delta > 0) ||
                    (overscrollOffset.value < 0 && delta < 0)
                ) {
                    rawDragAccumulator += delta
                    val newOffset = dampedOverscrollOffset(
                        rawDelta = rawDragAccumulator,
                        maxOverscrollPx = currentMaxOverscrollPx,
                        dampingFactor = DAMPING_FACTOR,
                    )
                    coroutineScope.launch { overscrollOffset.snapTo(newOffset) }
                    if (!hasTriggeredHaptic && abs(newOffset) >= currentTriggerThresholdPx) {
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
                    var didNavigate = false
                    if (abs(currentOffset) >= currentTriggerThresholdPx) {
                        if (currentOffset > 0 && currentCanGoPrevious) {
                            currentOnNavigatePrevious()
                            didNavigate = true
                        } else if (currentOffset < 0 && currentCanGoNext) {
                            currentOnNavigateNext()
                            didNavigate = true
                        }
                    }
                    rawDragAccumulator = 0f
                    hasTriggeredHaptic = false
                    if (!didNavigate) {
                        overscrollOffset.animateTo(
                            0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )
                    }
                    return available
                }
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .nestedScroll(nestedScrollConnection)
            .then(
                if (isContentNonScrollable) {
                    // 短回答不可滚动时的触屏手势兜底；鼠标交给下方的 Main-pass 处理器以保证正文鼠标框选文字优先。
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                if (down.type == PointerType.Mouse) continue
                                rawDragAccumulator = 0f
                                hasTriggeredHaptic = false
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

                                    val canDragDown = delta > 0 && currentCanGoPrevious
                                    val canDragUp = delta < 0 && currentCanGoNext
                                    val continuing = (overscrollOffset.value > 0 && delta > 0) ||
                                        (overscrollOffset.value < 0 && delta < 0)
                                    val reversing = (overscrollOffset.value > 0 && delta < 0) ||
                                        (overscrollOffset.value < 0 && delta > 0)

                                    if (canDragDown || canDragUp || continuing || reversing) {
                                        change.consume()
                                        rawDragAccumulator += delta
                                        if ((rawDragAccumulator > 0 && !currentCanGoPrevious) ||
                                            (rawDragAccumulator < 0 && !currentCanGoNext)
                                        ) {
                                            rawDragAccumulator = 0f
                                        }
                                        val newOffset = dampedOverscrollOffset(
                                            rawDelta = rawDragAccumulator,
                                            maxOverscrollPx = currentMaxOverscrollPx,
                                            dampingFactor = DAMPING_FACTOR,
                                        )
                                        coroutineScope.launch { overscrollOffset.snapTo(newOffset) }
                                        if (!hasTriggeredHaptic && abs(newOffset) >= currentTriggerThresholdPx) {
                                            hasTriggeredHaptic = true
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        if (hasTriggeredHaptic && abs(newOffset) < currentTriggerThresholdPx) {
                                            hasTriggeredHaptic = false
                                        }
                                    }
                                }
                                finishOverscrollGesture()
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ).then(
                if (enableMouseOverscroll) {
                    // 鼠标左键止点拖拽：在 Main 阶段监听（晚于子组件 SelectionContainer，保证正文文字框选完全不受影响），
                    // 一旦在头尾非正文区确认沿止点方向拖拽，切换至 Initial 阶段接管后续位移与抬起事件，驱动隐藏预览卡浮现。
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                                if (down.type != PointerType.Mouse) continue

                                var dragging = false
                                var dragTopDirection = false
                                var rejected = false
                                try {
                                    while (true) {
                                        val pass = if (dragging) PointerEventPass.Initial else PointerEventPass.Main
                                        val event = awaitPointerEvent(pass)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) {
                                            if (dragging) change.consume()
                                            break
                                        }
                                        if (rejected) continue
                                        if (!dragging && change.isConsumed) {
                                            // 正文 SelectionContainer 已消费鼠标拖拽用于框选文字，直接礼让
                                            rejected = true
                                            continue
                                        }

                                        val deltaY = change.positionChange().y
                                        if (!dragging) {
                                            val totalDx = change.position.x - down.position.x
                                            val totalDy = change.position.y - down.position.y
                                            if (totalDy.absoluteValue <= mouseSlopPx && totalDx.absoluteValue <= mouseSlopPx) {
                                                continue
                                            }
                                            if (totalDx.absoluteValue >= totalDy.absoluteValue) {
                                                rejected = true
                                                continue
                                            }
                                            val canPullDown = totalDy > 0f && currentIsAtTop() && currentCanGoPrevious
                                            val canPullUp = totalDy < 0f && currentIsAtBottom() && currentCanGoNext
                                            if (!canPullDown && !canPullUp) {
                                                rejected = true
                                                continue
                                            }
                                            dragging = true
                                            dragTopDirection = canPullDown
                                            rawDragAccumulator = totalDy
                                            hasTriggeredHaptic = false
                                            change.consume()
                                        } else {
                                            change.consume()
                                            rawDragAccumulator = if (dragTopDirection) {
                                                (rawDragAccumulator + deltaY).coerceAtLeast(0f)
                                            } else {
                                                (rawDragAccumulator + deltaY).coerceAtMost(0f)
                                            }
                                        }

                                        val newOffset = dampedOverscrollOffset(
                                            rawDelta = rawDragAccumulator,
                                            maxOverscrollPx = currentMaxOverscrollPx,
                                            dampingFactor = DAMPING_FACTOR,
                                        )
                                        coroutineScope.launch { overscrollOffset.snapTo(newOffset) }
                                        if (!hasTriggeredHaptic && abs(newOffset) >= currentTriggerThresholdPx) {
                                            hasTriggeredHaptic = true
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } else if (hasTriggeredHaptic && abs(newOffset) < currentTriggerThresholdPx) {
                                            hasTriggeredHaptic = false
                                        }
                                    }
                                } finally {
                                    if (dragging) {
                                        finishOverscrollGesture()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        // 预览卡片平时完全隐藏，仅在拖拽位移超过 revealThresholdPx 后才从顶部/底部边缘渐显露出

        // 上方预览卡片（上一个回答）—— 固定在顶部
        if (overscrollOffset.value >= revealThresholdPx && previousAnswer != null) {
            val progress = (
                (overscrollOffset.value - revealThresholdPx) /
                    (triggerThresholdPx - revealThresholdPx).coerceAtLeast(1f)
            ).coerceIn(0f, 1f)
            AnswerPreviewCard(
                authorName = previousAnswer.authorName,
                excerpt = previousAnswer.title,
                avatarUrl = previousAnswer.authorAvatarUrl,
                label = "继续下拉 · ${previousAnswer.sourceLabel}的上一个回答",
                icon = Icons.Filled.ArrowDownward,
                isTriggered = overscrollOffset.value >= triggerThresholdPx,
                progress = progress,
                duo3Style = duo3Style,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateTopPadding()),
            )
        }

        // 下方预览卡片（下一个回答）—— 固定在底部，内容顺序反转
        if (overscrollOffset.value <= -revealThresholdPx && nextAnswer != null) {
            val progress = (
                (abs(overscrollOffset.value) - revealThresholdPx) /
                    (triggerThresholdPx - revealThresholdPx).coerceAtLeast(1f)
            ).coerceIn(0f, 1f)
            AnswerPreviewCard(
                authorName = nextAnswer.authorName,
                excerpt = nextAnswer.title,
                avatarUrl = nextAnswer.authorAvatarUrl,
                label = "继续上拉 · ${nextAnswer.sourceLabel}的下一个回答",
                icon = Icons.Filled.ArrowUpward,
                isTriggered = abs(overscrollOffset.value) >= triggerThresholdPx,
                progress = progress,
                reverseLayout = true,
                duo3Style = duo3Style,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()),
            )
        }

        // 主内容在最上层，跟随 overscroll 偏移
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
    icon: ImageVector,
    isTriggered: Boolean,
    progress: Float,
    reverseLayout: Boolean = false,
    duo3Style: Boolean = false,
    modifier: Modifier = Modifier,
) {
    DisableSelection {
        if (duo3Style) {
            val pillBgColor by animateColorAsState(
                targetValue = if (isTriggered) {
                    voteUpNeutralContentDuo3()
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                label = "duo3PreviewPillBg",
            )
            val pillContentColor by animateColorAsState(
                targetValue = if (isTriggered) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "duo3PreviewPillContent",
            )

            Box(
                modifier = modifier
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        alpha = progress.coerceIn(0f, 1f)
                        val scale = 0.96f + 0.04f * progress.coerceIn(0f, 1f)
                        scaleX = scale
                        scaleY = scale
                    }.clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(12.dp),
            ) {
                val badgePill: @Composable () -> Unit = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(pillBgColor)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = pillContentColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isTriggered) "松手切换回答" else label,
                            style = MaterialTheme.typography.labelMedium,
                            color = pillContentColor,
                            fontWeight = if (isTriggered) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
                val authorSection: @Composable () -> Unit = {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (avatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "头像",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape),
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            if (authorName.isNotEmpty()) {
                                Text(
                                    text = authorName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (excerpt.isNotEmpty()) {
                                Text(
                                    text = excerpt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp,
                                )
                            }
                        }
                    }
                }
                Column {
                    if (reverseLayout) {
                        authorSection()
                        Spacer(Modifier.height(8.dp))
                        badgePill()
                    } else {
                        badgePill()
                        Spacer(Modifier.height(8.dp))
                        authorSection()
                    }
                }
            }
        } else {
            val bgColor = if (isTriggered) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val contentColor = if (isTriggered) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = modifier
                    .padding(horizontal = 16.dp)
                    .graphicsLayer { alpha = progress.coerceIn(0f, 1f) }
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
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
    }
}

private const val MAX_OVERSCROLL_DP = 200f
private const val TRIGGER_THRESHOLD_DP = 80f
private const val REVEAL_THRESHOLD_DP = 18f
private const val DAMPING_FACTOR = 1.2f

internal fun dampedOverscrollOffset(
    rawDelta: Float,
    maxOverscrollPx: Float,
    dampingFactor: Float,
): Float {
    val sign = if (rawDelta >= 0) 1f else -1f
    return sign * maxOverscrollPx * tanh(abs(rawDelta) / (maxOverscrollPx * dampingFactor))
}
