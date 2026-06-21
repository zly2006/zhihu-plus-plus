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

// 来源：androidx.compose.material3:material3-android:1.5.0-alpha20；保留本项目 usePlatformWindow 分支。
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "INVISIBLE_SETTER")

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetDialog
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.draggableAnchors
import androidx.compose.material3.internal.getString
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.value
import androidx.compose.material3.verticalScaleDown
import androidx.compose.material3.verticalScaleUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.github.zly2006.zhihu.shared.platform.PlatformPredictiveBackHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
@ExperimentalMaterial3Api
fun MyModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    usePlatformWindow: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val animateToDismiss: () -> Unit = {
        // 1.5.0-alpha20 仍不能直接访问 confirmValueChange；改由 SheetState.hide() 内部执行确认。
        scope
            .launch { sheetState.hide() }
            .invokeOnCompletion {
                if (!sheetState.isVisible) {
                    onDismissRequest()
                }
            }
    }
    val predictiveBackProgress = remember { Animatable(initialValue = 0f) }

    @Composable
    fun SheetContent() {
        Box(modifier = Modifier.fillMaxSize().imePadding().semantics { isTraversalGroup = true }) {
            MyBottomSheetScrim(
                color = scrimColor,
                onDismissRequest = animateToDismiss,
                visible = sheetState.targetValue != Hidden,
                dismissEnabled = properties.shouldDismissOnClickOutside,
            )
            BottomSheet(
                predictiveBackProgress = predictiveBackProgress,
                scope = scope,
                animateToDismiss = animateToDismiss,
                onDismissRequest = onDismissRequest,
                modifier = modifier.align(Alignment.TopCenter),
                state = sheetState,
                maxWidth = sheetMaxWidth,
                gesturesEnabled = sheetGesturesEnabled,
                shape = shape,
                containerColor = containerColor,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                dragHandle = dragHandle,
                contentWindowInsets = contentWindowInsets,
                content = content,
            )
        }
    }

    if (usePlatformWindow) {
        ModalBottomSheetDialog(
            properties = properties,
            contentColor = contentColor,
            onDismissRequest = {
                // 修复返回键需要按两次才关闭的问题。
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
            },
        ) {
            SheetContent()
        }
    } else {
        PlatformPredictiveBackHandler(
            enabled = properties.shouldDismissOnBackPress && sheetState.targetValue != Hidden,
            onProgress = { progress ->
                scope.launch { predictiveBackProgress.snapTo(progress) }
            },
            onCancel = {
                scope.launch { predictiveBackProgress.animateTo(0f) }
            },
            onBack = animateToDismiss,
        )
        SheetContent()
    }
    if (sheetState.hasExpandedState) {
        LaunchedEffect(sheetState) { sheetState.show() }
    }
}

@Composable
@ExperimentalMaterial3Api
fun BottomSheet(
    predictiveBackProgress: Animatable<Float, AnimationVector1D> = remember { Animatable(initialValue = 0f) },
    scope: CoroutineScope = rememberCoroutineScope(),
    animateToDismiss: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    modifier: Modifier = Modifier,
    state: SheetState = rememberModalBottomSheetState(),
    maxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    gesturesEnabled: Boolean = true,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = {
        BottomSheetDefaults.windowInsets
    },
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    shadowElevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val anchoredDraggableMotion: FiniteAnimationSpec<Float> =
        androidx.compose.material3.tokens.MotionSchemeKeyTokens.DefaultSpatial
            .value()
    val showMotion: FiniteAnimationSpec<Float> =
        androidx.compose.material3.tokens.MotionSchemeKeyTokens.DefaultSpatial
            .value()
    val hideMotion: FiniteAnimationSpec<Float> =
        androidx.compose.material3.tokens.MotionSchemeKeyTokens.FastEffects
            .value()
    SideEffect {
        state.showMotionSpec = showMotion
        state.hideMotionSpec = hideMotion
        state.anchoredDraggableMotionSpec = anchoredDraggableMotion
    }

    val bottomSheetPaneTitle = getString(string = Strings.BottomSheetPaneTitle)
    val viewConfiguration = LocalViewConfiguration.current
    val density = LocalDensity.current
    val anchoredDraggableFlingBehavior =
        AnchoredDraggableDefaults.flingBehavior(
            state = state.anchoredDraggableState,
            positionalThreshold = { _ -> state.positionalThreshold.invoke() },
            animationSpec = anchoredDraggableMotion,
        )
    val modalBottomSheetFlingBehavior =
        remember(anchoredDraggableFlingBehavior, state, viewConfiguration, density) {
            object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    val maxSystemVelocity = viewConfiguration.maximumFlingVelocity
                    var safeVelocity = initialVelocity.coerceIn(-maxSystemVelocity, maxSystemVelocity)

                    if (safeVelocity > 0f && state.anchoredDraggableState.anchors.hasPositionFor(Hidden)) {
                        val hiddenAnchor = state.anchoredDraggableState.anchors.positionOf(Hidden)
                        val currentOffset = state.requireOffset()
                        val distanceToFloor = max(0f, hiddenAnchor - currentOffset)
                        val dampeningZone = with(density) { BottomSheetDefaults.BoundaryDampeningZone.toPx() }

                        if (distanceToFloor < dampeningZone) {
                            val factor = distanceToFloor / dampeningZone
                            safeVelocity *= factor

                            val velocityThresholdPx = with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                            if (initialVelocity >= velocityThresholdPx) {
                                safeVelocity = max(safeVelocity, velocityThresholdPx)
                            }
                        }
                    }

                    return try {
                        with(anchoredDraggableFlingBehavior) { performFling(safeVelocity) }
                    } finally {
                        if (!state.isVisible) onDismissRequest()
                    }
                }
            }
        }

    Surface(
        modifier =
            modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .then(
                    if (gesturesEnabled) {
                        Modifier.nestedScroll(
                            remember(state) {
                                ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                                    sheetState = state,
                                    orientation = Orientation.Vertical,
                                    flingBehavior = modalBottomSheetFlingBehavior,
                                )
                            },
                        )
                    } else {
                        Modifier
                    },
                ).draggableAnchors(state.anchoredDraggableState, Orientation.Vertical) { sheetSize, constraints ->
                    val fullHeight = constraints.maxHeight.toFloat()
                    val newAnchors = DraggableAnchors {
                        Hidden at fullHeight
                        if (sheetSize.height > (fullHeight / 2) && !state.skipPartiallyExpanded) {
                            PartiallyExpanded at fullHeight / 2f
                        }
                        if (sheetSize.height != 0) {
                            Expanded at max(0f, fullHeight - sheetSize.height)
                        }
                    }
                    val newTarget =
                        when (state.targetValue) {
                            Hidden -> Hidden
                            PartiallyExpanded -> {
                                val hasPartiallyExpandedState = newAnchors.hasPositionFor(PartiallyExpanded)
                                if (hasPartiallyExpandedState) {
                                    PartiallyExpanded
                                } else if (newAnchors.hasPositionFor(Expanded)) {
                                    Expanded
                                } else {
                                    Hidden
                                }
                            }
                            Expanded -> {
                                if (newAnchors.hasPositionFor(Expanded)) Expanded else Hidden
                            }
                        }
                    return@draggableAnchors newAnchors to newTarget
                }.anchoredDraggable(
                    state = state.anchoredDraggableState,
                    orientation = Orientation.Vertical,
                    enabled = gesturesEnabled && state.currentValue != Hidden,
                    flingBehavior = modalBottomSheetFlingBehavior,
                ).semantics {
                    paneTitle = bottomSheetPaneTitle
                    traversalIndex = 0f
                }.consumeWindowInsets(WindowInsets(top = state.offset.toInt().coerceAtLeast(0)))
                .graphicsLayer {
                    val sheetOffset = state.anchoredDraggableState.offset
                    val sheetHeight = size.height
                    if (!sheetOffset.isNaN() && !sheetHeight.isNaN() && sheetHeight != 0f) {
                        val progress = predictiveBackProgress.value
                        scaleX = calculatePredictiveBackScaleX(progress)
                        scaleY = calculatePredictiveBackScaleY(progress)
                        transformOrigin = TransformOrigin(0.5f, (sheetOffset + sheetHeight) / sheetHeight)
                    }
                }.verticalScaleUp(state),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(contentWindowInsets())
                .graphicsLayer {
                    val progress = predictiveBackProgress.value
                    val predictiveBackScaleX = calculatePredictiveBackScaleX(progress)
                    val predictiveBackScaleY = calculatePredictiveBackScaleY(progress)

                    scaleY =
                        if (predictiveBackScaleY != 0f) {
                            predictiveBackScaleX / predictiveBackScaleY
                        } else {
                            1f
                        }
                    transformOrigin = PredictiveBackChildTransformOrigin
                }.verticalScaleDown(state),
        ) {
            if (dragHandle != null) {
                val collapseActionLabel = getString(Strings.BottomSheetPartialExpandDescription)
                val dismissActionLabel = getString(Strings.BottomSheetDismissDescription)
                val expandActionLabel = getString(Strings.BottomSheetExpandDescription)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                if (gesturesEnabled) {
                                    with(state) {
                                        dismiss(dismissActionLabel) {
                                            animateToDismiss()
                                            true
                                        }
                                        if (currentValue == PartiallyExpanded) {
                                            expand(expandActionLabel) {
                                                scope.launch { state.expand() }
                                                true
                                            }
                                        } else if (hasPartiallyExpandedState) {
                                            collapse(collapseActionLabel) {
                                                scope.launch { partialExpand() }
                                                true
                                            }
                                        }
                                    }
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    dragHandle()
                }
            }
            content()
        }
    }
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleX(progress: Float): Float {
    val width = size.width
    return if (width.isNaN() || width == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleXDistance.toPx(), width), progress) / width
    }
}

private fun GraphicsLayerScope.calculatePredictiveBackScaleY(progress: Float): Float {
    val height = size.height
    return if (height.isNaN() || height == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleYDistance.toPx(), height), progress) / height
    }
}

private val PredictiveBackMaxScaleXDistance = 48.dp
private val PredictiveBackMaxScaleYDistance = 24.dp
private val PredictiveBackChildTransformOrigin = TransformOrigin(0.5f, 0f)

@Composable
private fun MyBottomSheetScrim(
    color: Color,
    onDismissRequest: () -> Unit,
    visible: Boolean,
    dismissEnabled: Boolean,
) {
    if (color.isSpecified) {
        val alpha by
            animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = androidx.compose.material3.tokens.MotionSchemeKeyTokens.DefaultEffects
                    .value(),
                label = "ScrimAlphaAnimation",
            )
        val closeSheet = getString(Strings.CloseSheet)
        val dismissSheet =
            if (dismissEnabled) {
                Modifier
                    .pointerInput(onDismissRequest) { detectTapGestures { onDismissRequest() } }
                    .semantics(mergeDescendants = true) {
                        traversalIndex = 1f
                        contentDescription = closeSheet
                        onClick {
                            onDismissRequest()
                            true
                        }
                    }
            } else {
                Modifier
            }
        Canvas(Modifier.fillMaxSize().then(dismissSheet)) {
            drawRect(color = color, alpha = alpha.coerceIn(0f, 1f))
        }
    }
}
