/*
 * Original work from Kyant0/AndroidLiquidGlass (v1.0.6, commit 896a94a3ade1cc1a940b92365f942a34971fecda)
 * Copyright (C) Kyant0 <https://github.com/Kyant0/AndroidLiquidGlass>
 * Licensed under the Apache License, Version 2.0 (https://www.apache.org/licenses/LICENSE-2.0)
 *
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

package com.github.zly2006.zhihu.ui.liquidglass.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.rememberDefaultLiquidBackdrop
import com.github.zly2006.zhihu.ui.liquidglass.utils.DampedDragAnimation
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@Composable
fun AndroidLiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    visibilityThreshold: Float = (valueRange.endInclusive - valueRange.start) / 1000f,
    backdrop: Backdrop = rememberDefaultLiquidBackdrop(),
    enabled: Boolean = true,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    isLightTheme: Boolean = !ThemeManager.isDarkTheme(),
) {
    require(steps >= 0)
    if (valueRange.endInclusive <= valueRange.start) {
        androidx.compose.material3.Slider(value(), onValueChange, modifier, enabled = false, valueRange = valueRange)
        return
    }

    fun snapValue(raw: Float): Float {
        val bounded = raw.coerceIn(valueRange)
        if (steps <= 0) return bounded
        val increment = (valueRange.endInclusive - valueRange.start) / (steps + 1)
        return (valueRange.start + ((bounded - valueRange.start) / increment).roundToInt() * increment)
            .coerceIn(valueRange)
    }

    val accentColor =
        if (isLightTheme) {
            Color(0xFF0088FF)
        } else {
            Color(0xFF0091FF)
        }
    val trackColor =
        if (isLightTheme) {
            Color(0xFF787878).copy(0.2f)
        } else {
            Color(0xFF787880).copy(0.36f)
        }

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (enabled) Modifier.systemGestureExclusion() else Modifier)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value().coerceIn(valueRange), valueRange, steps)
                if (!enabled) disabled()
                setProgress { proposed ->
                    if (enabled) {
                        val snapped = snapValue(proposed)
                        onValueChange(snapped)
                        onValueChangeFinished?.invoke()
                    }
                    enabled
                }
            }.then(if (enabled) Modifier else Modifier.alpha(0.4f)),
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = constraints.maxWidth

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        var isDragging by remember { mutableStateOf(false) }
        val dampedDragAnimation = remember(animationScope, valueRange, steps) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {
                    isDragging = true
                    didDrag = false
                },
                onDragStopped = {
                    isDragging = false
                    if (didDrag) {
                        val snapped = snapValue(targetValue)
                        if (snapped != targetValue) {
                            animateToValue(snapped)
                        }
                        onValueChange(snapped)
                    }
                    onValueChangeFinished?.invoke()
                },
                onDragCancelled = {
                    isDragging = false
                    didDrag = false
                    animateToValue(value())
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidth)
                    val nextRaw =
                        if (isLtr) {
                            (targetValue + delta).coerceIn(valueRange)
                        } else {
                            (targetValue - delta).coerceIn(valueRange)
                        }
                    updateValue(nextRaw)
                    onValueChange(snapValue(nextRaw))
                },
            )
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value() }
                .collectLatest { externalValue ->
                    if (!isDragging && dampedDragAnimation.targetValue != externalValue) {
                        dampedDragAnimation.updateValue(externalValue)
                    }
                }
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(Capsule())
                    .background(trackColor)
                    .then(
                        if (enabled) {
                            Modifier.pointerInput(animationScope, valueRange, steps) {
                                detectTapGestures { position ->
                                    val delta = (valueRange.endInclusive - valueRange.start) * (position.x / trackWidth)
                                    val rawTarget =
                                        if (isLtr) {
                                            valueRange.start + delta
                                        } else {
                                            valueRange.endInclusive - delta
                                        }
                                    val targetValue = snapValue(rawTarget)
                                    dampedDragAnimation.animateToValue(targetValue)
                                    onValueChange(targetValue)
                                    onValueChangeFinished?.invoke()
                                }
                            }
                        } else {
                            Modifier
                        },
                    ).height(6f.dp)
                    .fillMaxWidth(),
            )

            Box(
                Modifier
                    .clip(Capsule())
                    .background(accentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    },
            )
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }.then(if (enabled) dampedDragAnimation.modifier else Modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        },
                    ),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true,
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress,
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f),
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress,
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    },
                ).size(40f.dp, 24f.dp),
        )
    }
}
