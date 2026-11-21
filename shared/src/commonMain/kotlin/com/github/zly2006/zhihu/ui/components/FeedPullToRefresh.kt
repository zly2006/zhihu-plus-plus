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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.platform.isAnswerSwipeSupported
import com.github.zly2006.zhihu.ui.article.voteUpNeutralContentDuo3
import com.github.zly2006.zhihu.ui.subscreens.rememberDuo3PointerInteractionEnabled
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

val LocalPullToRefreshViewModel = compositionLocalOf<BaseFeedViewModel?> {
    null
}

@Stable
class FeedPullToRefreshHandle internal constructor() {
    private var canScrollBackwardProvider: (() -> Boolean)? = null

    internal fun setCanScrollBackwardProvider(provider: (() -> Boolean)?) {
        canScrollBackwardProvider = provider
    }

    internal fun canChildScrollBackward(): Boolean = canScrollBackwardProvider?.invoke() ?: false
}

val LocalFeedPullToRefreshHandle = compositionLocalOf<FeedPullToRefreshHandle?> {
    null
}

private const val FEED_REFRESH_DRAG_THRESHOLD_DP = 76f
private const val FEED_REFRESH_REVEAL_PROGRESS = 0.22f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedPullToRefresh(
    viewModel: BaseFeedViewModel,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    FeedPullToRefresh(
        viewModel = viewModel,
        environment = rememberPaginationEnvironment(viewModel.allowGuestAccess),
        padding = padding,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedPullToRefresh(
    viewModel: BaseFeedViewModel,
    environment: PaginationEnvironment,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val useDuo3PointerInteraction = rememberDuo3PointerInteractionEnabled()
    val enableMousePullToRefresh = useDuo3PointerInteraction && isAnswerSwipeSupported

    val triggerThresholdPx = with(density) { FEED_REFRESH_DRAG_THRESHOLD_DP.dp.toPx() }
    val mouseSlopPx = with(density) { 6.dp.toPx() }
    val maxSlideOffsetPx = with(density) { 28.dp.toPx() }
    val isRefreshing = viewModel.isPullToRefresh && viewModel.isLoading

    val refreshHandle = remember { FeedPullToRefreshHandle() }
    var rawDragPx by remember { mutableFloatStateOf(0f) }
    var mouseDragProgress by remember { mutableFloatStateOf(0f) }
    var isMouseDragging by remember { mutableStateOf(false) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    val currentViewModel by rememberUpdatedState(viewModel)
    val currentEnvironment by rememberUpdatedState(environment)
    val currentThresholdPx by rememberUpdatedState(triggerThresholdPx)

    RegisterFeedRefreshTargetEffect(active = useDuo3PointerInteraction) {
        if (!currentViewModel.isLoading) {
            scope.launch {
                currentViewModel.pullToRefresh(currentEnvironment)
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && !isMouseDragging && mouseDragProgress > 0f) {
            mouseDragProgress = 0f
            state.animateToHidden()
        }
    }

    val effectiveProgress = maxOf(mouseDragProgress, state.distanceFraction)

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                viewModel.pullToRefresh(environment)
            }
        },
        indicator = {
            if (useDuo3PointerInteraction) {
                // 平时完全隐藏，仅在列表上止点下拉超过显隐阈值或处于刷新中时才浮现 123Duo3 双层药丸提示卡
                if (isRefreshing || effectiveProgress >= FEED_REFRESH_REVEAL_PROGRESS) {
                    val revealFraction = if (isRefreshing) {
                        1f
                    } else {
                        ((effectiveProgress - FEED_REFRESH_REVEAL_PROGRESS) / (1f - FEED_REFRESH_REVEAL_PROGRESS))
                            .coerceIn(0f, 1f)
                    }
                    Duo3PullToRefreshPill(
                        isRefreshing = isRefreshing,
                        isTriggered = effectiveProgress >= 1f,
                        revealFraction = revealFraction,
                        slideOffsetPx = maxSlideOffsetPx * effectiveProgress.coerceIn(0f, 1.25f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(padding)
                            .padding(top = 6.dp),
                    )
                }
            } else {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(padding),
                    isRefreshing = isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    state = state,
                )
            }
        },
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (enableMousePullToRefresh) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                                if (down.type != PointerType.Mouse) continue

                                var dragging = false
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
                                            if (totalDx.absoluteValue >= totalDy.absoluteValue ||
                                                totalDy <= 0f ||
                                                refreshHandle.canChildScrollBackward()
                                            ) {
                                                rejected = true
                                                continue
                                            }
                                            dragging = true
                                            isMouseDragging = true
                                            rawDragPx = totalDy
                                            hasTriggeredHaptic = false
                                            change.consume()
                                        } else {
                                            change.consume()
                                            rawDragPx = (rawDragPx + deltaY).coerceAtLeast(0f)
                                        }

                                        val progress = (rawDragPx / currentThresholdPx.coerceAtLeast(1f)).coerceIn(0f, 1.5f)
                                        mouseDragProgress = progress
                                        scope.launch { state.snapTo(progress) }
                                        if (!hasTriggeredHaptic && progress >= 1f) {
                                            hasTriggeredHaptic = true
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } else if (hasTriggeredHaptic && progress < 1f) {
                                            hasTriggeredHaptic = false
                                        }
                                    }
                                } finally {
                                    if (dragging) {
                                        isMouseDragging = false
                                        val shouldRefresh = mouseDragProgress >= 1f && !currentViewModel.isLoading
                                        rawDragPx = 0f
                                        hasTriggeredHaptic = false
                                        if (shouldRefresh) {
                                            mouseDragProgress = 1f
                                            scope.launch {
                                                try {
                                                    state.animateToThreshold()
                                                    currentViewModel.pullToRefresh(currentEnvironment)
                                                } finally {
                                                    mouseDragProgress = 0f
                                                    state.animateToHidden()
                                                }
                                            }
                                        } else {
                                            mouseDragProgress = 0f
                                            scope.launch {
                                                state.animateToHidden()
                                            }
                                        }
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
        CompositionLocalProvider(
            LocalPullToRefreshViewModel provides viewModel,
            LocalFeedPullToRefreshHandle provides refreshHandle,
        ) {
            content()
        }
    }
}

@Composable
private fun Duo3PullToRefreshPill(
    isRefreshing: Boolean,
    isTriggered: Boolean,
    revealFraction: Float,
    slideOffsetPx: Float,
    modifier: Modifier = Modifier,
) {
    val highlighted = isRefreshing || isTriggered
    val innerBgColor by animateColorAsState(
        targetValue = if (highlighted) {
            voteUpNeutralContentDuo3()
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "duo3RefreshInnerBg",
    )
    val innerContentColor by animateColorAsState(
        targetValue = if (highlighted) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "duo3RefreshInnerContent",
    )
    val label = when {
        isRefreshing -> "正在刷新"
        isTriggered -> "松手立即刷新"
        else -> "继续下拉刷新"
    }

    DisableSelection {
        Box(
            modifier = modifier
                .graphicsLayer {
                    alpha = revealFraction.coerceIn(0f, 1f)
                    translationY = slideOffsetPx - (1f - revealFraction) * 16.dp.toPx()
                    val scale = 0.92f + 0.08f * revealFraction.coerceIn(0f, 1f)
                    scaleX = scale
                    scaleY = scale
                }.clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(4.dp),
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(innerBgColor)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = innerContentColor,
                    )
                } else {
                    Icon(
                        imageVector = if (isTriggered) Icons.Filled.Refresh else Icons.Filled.ArrowDownward,
                        contentDescription = label,
                        tint = innerContentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = innerContentColor,
                    fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}
